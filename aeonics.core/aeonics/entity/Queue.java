package aeonics.entity;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.manager.Executor;
import aeonics.manager.Executor.Task;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.manager.Monitor;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.StringUtils;
import aeonics.util.Tuple;

/**
 * This class represents a queue that can subscribe to one or more topics.
 * <p>The queue will send messages to the related {@link Action} and {@link Destination} based
 * on the different parameters and policy.</p>
 */
public class Queue extends Item<Queue.Type>
{
	/**
	 * This is the default entity to manage the orchestration of the different messages.
	 * While all steps are naturally queued by the {@link Executor}, this instance
	 * adds another intermediate queuing level based on the level of concurrency.
	 */
	public static class Type extends Entity implements Consumer<Message>
	{
		/**
		 * Matches the parameter but already parsed for performance reasons
		 */
		private int concurrency = 0;
		/**
		 * Matches the parameter but already parsed for performance reasons
		 */
		private int limit = 0;
		/**
		 * Internal queue
		 */
		private Deque<Message> queue = new ConcurrentLinkedDeque<>();
		
		public void accept(Message message)
		{
			if( concurrency <= 0 )
				next(message);
			
			try
			{
				while( limit > 0 && queue.size() >= limit )
				{
					synchronized(queue)
					{
						if( queue.size() < limit )
							break;
						queue.wait();
					}
				}
				queue.offer(message);
				while( checkNext() ) ;
			}
			catch(Exception e)
			{
				Manager.of(Logger.class).warning(getClass(), e);
				Manager.of(Logger.class).severe("DISCARD", "MESSAGE DISCARDED");
				// TODO : discard ?
			}
		}
		
		/**
		 * Number of messages currently being processed
		 */
		private AtomicInteger parallel = new AtomicInteger(0);
		
		/**
		 * Checks if another message can be scheduled based on the concurrency level. 
		 * This method shall be called in a loop until il returns false.
		 * @return true if a message was scheduled
		 */
		private boolean checkNext()
		{
			while( true )
			{
				int current = parallel.get();
				if( current >= concurrency ) return false;
				if( parallel.compareAndSet(current, current + 1) ) break;
			}
			
			Message message = queue.poll();
			if( message == null )
			{
				parallel.decrementAndGet();
				return false;
			}
			
			next(message).then(() ->
			{
				parallel.decrementAndGet();
				while( checkNext() ) ;
			});
			
			return true;
		}
		
		/**
		 * Prepares delivery of the specified message to all actions and destinations bound to this queue.
		 * @param message the message to deliver
		 * @return the task object that completes when the message has been fully processed
		 */
		private Task<Void> next(Message message)
		{
			Manager.of(Monitor.class).count(this);
			
			List<Tuple<Action.Type, String>> actions = new ArrayList<>();
			List<Tuple<Destination.Type, String>> destinations = new ArrayList<>();
			
			relations("actions").forEach((t) ->
			{
				if( !(t.a instanceof Action.Type) )
				{
					Manager.of(Logger.class).config(getClass(), "Action {} configured in Queue {} does not exist.", t.b.asString("id"), id());
					return;
				}

				actions.add(Tuple.of((Action.Type)t.a, t.b.asString("input")));
			});
			
			relations("destinations").forEach((t) ->
			{
				if( !(t.a instanceof Destination.Type) )
				{
					Manager.of(Logger.class).config(getClass(), "Destination {} configured in Queue {} does not exist.", t.b.asString("id"), id());
					return;
				}
				
				destinations.add(Tuple.of((Destination.Type)t.a, t.b.asString("input")));
			});
			
			// use this construct to clone the message only if necessary
			
			List<Task<?>> tasks = new LinkedList<>();
			for( int i = actions.size()-1; i >= 0; i-- )
			{
				Tuple<Action.Type, String> t = actions.get(i);
				tasks.add(next(i == 0 && destinations.isEmpty() ? message : message.clone(), t.a, t.b));
			}
			for( int i = destinations.size()-1; i >= 0; i-- )
			{
				Tuple<Destination.Type, String> t = destinations.get(i);
				tasks.add(next(i == 0 ? message : message.clone(), t.a, t.b));
			}
			return Task.all(tasks);
		}
		
		/**
		 * Runs the specified action and prepare the next steps
		 * @param message the message to deliver
		 * @param action the action to run
		 * @param input the name of the input channel of the action
		 * @return the task object that completes when the message has been fully processed
		 */
		private Task<Void> next(Message message, Action.Type action, String input)
		{
			return Manager.of(Executor.class).normal(() ->
			{
				Set<String> outputs = new HashSet<>();
				action.relations("actions").forEach((t) -> { outputs.add(t.b.asString("output")); });
				action.relations("destinations").forEach((t) -> { outputs.add(t.b.asString("output")); });
				Manager.of(Monitor.class).count(action);
				return action.accept(message, input, outputs);
			})
			.link((outputs) ->
			{
				// use this construct to clone the message only if necessary
				Map<String, Tuple<List<Tuple<Destination.Type, String>>, List<Tuple<Action.Type, String>>>> nexts = new HashMap<>();
				for( Map.Entry<String, Message> out : outputs.entrySet() )
					if( out.getValue() != null )
						nexts.put(out.getKey(), new Tuple<>(new ArrayList<>(), new ArrayList<>()));
				
				action.relations("actions").forEach((t) ->
				{
					if( !(t.a instanceof Action.Type) )
					{
						Manager.of(Logger.class).config(getClass(), "Action {} configured in Action {} for output {} does not exist.", t.b.asString("id"), action.id(), t.b.asString("output"));
						return;
					}
					
					Tuple<List<Tuple<Destination.Type, String>>, List<Tuple<Action.Type, String>>> valid = nexts.get(t.b.asString("output"));
					if( valid != null ) valid.b.add(Tuple.of((Action.Type)t.a, t.b.asString("input")));
				});
				
				action.relations("destinations").forEach((t) ->
				{
					if( !(t.a instanceof Destination.Type) )
					{
						Manager.of(Logger.class).config(getClass(), "Destination {} configured in Action {} for output {} does not exist.", t.b.asString("id"), action.id(), t.b.asString("output"));
						return;
					}
					
					Tuple<List<Tuple<Destination.Type, String>>, List<Tuple<Action.Type, String>>> valid = nexts.get(t.b.asString("output"));
					if( valid != null ) valid.a.add(Tuple.of((Destination.Type)t.a, t.b.asString("input")));
				});
				
				List<Task<?>> tasks = new LinkedList<>();
				for( Map.Entry<String, Tuple<List<Tuple<Destination.Type, String>>, List<Tuple<Action.Type, String>>>> entry : nexts.entrySet() )
				{
					Message m = outputs.get(entry.getKey());
					List<Tuple<Destination.Type, String>> destinations = entry.getValue().a;
					List<Tuple<Action.Type, String>> actions = entry.getValue().b;
					for( int i = actions.size()-1; i >= 0; i-- )
					{
						Tuple<Action.Type, String> t2 = actions.get(i);
						tasks.add(next(i == 0 && destinations.isEmpty() ? m : m.clone(), t2.a, t2.b));
					}
					for( int i = destinations.size()-1; i >= 0; i-- )
					{
						Tuple<Destination.Type, String> t2 = destinations.get(i);
						tasks.add(next(i == 0 ? m : m.clone(), t2.a, t2.b));
					}
				}
				return Task.all(tasks);
			})
			.or((e) ->
			{
				Manager.of(Logger.class).fine(Action.class, e);
				Manager.of(Logger.class).severe("DISCARD", "MESSAGE DISCARDED");
				// TODO : discard ?
			});
		}
		
		/**
		 * Runs the specified destination
		 * @param message the message to deliver
		 * @param destination the destination to run
		 * @param input the name of the input channel of the destination
		 * @return the task object that completes when the message has been fully processed
		 */
		private Task<Void> next(Message message, Destination.Type destination, String input)
		{
			return Manager.of(Executor.class).normal(() ->
			{
				Manager.of(Monitor.class).count(destination);
				destination.accept(message, input);
			})
			.or((e) ->
			{
				Manager.of(Logger.class).fine(Destination.class, e);
				Manager.of(Logger.class).severe("DISCARD", "MESSAGE DISCARDED");
				// TODO : discard ?
			});
		} 
		
		/**
		 * Hardcoded category to the {@link Queue} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Queue.class); }
	}
	
	protected Class<? extends Queue.Type> defaultTarget() { return Queue.Type.class; }
	protected Supplier<? extends Queue.Type> defaultCreator() { return Queue.Type::new; }
	protected Class<? extends Queue> category() { return Queue.class; }

	@Override
	public Template<? extends Queue.Type> template()
	{
		return super.template()
			.summary("Queue")
			.description("The queue will buffer messages in memory and process them according to the configured parameters.")
			.add(new Parameter("limit")
				.summary("Maximum number of queued messages")
				.description("This parameter defines the maximum number of messages that can be queued. If the limit is reached, "
					+ "the queue will block the publisher until some spots are freed. To disable the limit, set it to a negative value. "
					+ "If the limit is set to 0, then it means there will not be any queuing and the messages will be processed directly as they arrive.")
				.rule(Parameter.Rule.INTEGER)
				.format(Parameter.Format.NUMBER)
				.defaultValue(Data.of(-1)))
			.add(new Parameter("concurrency")
				.summary("Concurrency level")
				.description("The concurrency level defines how many messages can be processed simultaneously. "
					+ "In order to ensure messages are processed sequentially, set this parameter to 1. The value 0 or a negative value means "
					+ "that there is no limit to the number of concurrent processing. In a way, this means that there is no queuing because "
					+ "all messages are sent for processing immediately. The default value is the number of processors on the machine.")
				.rule(Parameter.Rule.DIGIT)
				.format(Parameter.Format.NUMBER)
				.defaultValue(Data.of(Runtime.getRuntime().availableProcessors())))
			.add(new Parameter("priority")
				.summary("High priority")
				.description("Whether or not this queue should execute in high priority mode.")
				.rule(Parameter.Rule.BOOLEAN)
				.format(Parameter.Format.BOOLEAN)
				.defaultValue(Data.of(false)))
			.add(new Relationship("actions")
				.category(Action.class)
				.summary("Linked Actions")
				.description("List of action entities that are directly connected to this queue.")
				.add(new Parameter("input")
					.summary("Input Channel")
					.description("The name of the input channel to which this action is bound.")
					.format(Parameter.Format.TEXT)))
			.add(new Relationship("destinations")
				.category(Destination.class)
				.summary("Linked Destinations")
				.description("List of destination entities that are directly connected to this queue.")
				.add(new Parameter("input")
					.summary("Input Channel")
					.description("The name of the input channel to which this action is bound.")
					.format(Parameter.Format.TEXT)))
			.builder((data, instance) ->
			{
				if( instance instanceof Queue.Type)
				{
					((Queue.Type)instance).concurrency = instance.valueOf("concurrency").asInt();
					((Queue.Type)instance).limit = instance.valueOf("limit").asInt();
				}
				Registry.add(instance);
			})
			.modifier((data, instance) ->
			{
				if( instance instanceof Queue.Type)
				{
					((Queue.Type)instance).concurrency = instance.valueOf("concurrency").asInt();
					((Queue.Type)instance).limit = instance.valueOf("limit").asInt();
				}
			});
	}
}
