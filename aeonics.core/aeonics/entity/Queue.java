package aeonics.entity;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import aeonics.entity.Step.Action;
import aeonics.manager.Executor;
import aeonics.manager.Executor.Task;
import aeonics.manager.Manager;
import aeonics.template.Channel;
import aeonics.template.Parameter;
import aeonics.util.Tuples.Tuple;

/**
 * This class represents a queue that can subscribe to one or more topics.
 * <p>The queue will deliver downstream messages based on the different parameters and policy.</p>
 */
public class Queue extends Action
{
	/**
	 * This is the default entity to manage the orchestration of the different messages.
	 * While all steps are naturally queued by the {@link Executor}, this instance
	 * adds another intermediate queuing level based on the level of concurrency.
	 * 
	 * Incoming messages are processed entirely before starting the next (depending on the concurrency).
	 */
	public static class Type extends Action.Type
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
		private Deque<Tuple<Message, Task<Void>>> queue = new ConcurrentLinkedDeque<>();
		
		/**
		 * Override the default accept action to queue messages
		 */
		Task<Void> acceptAction(Message message, String input)
		{
			if( concurrency <= 0 )
				return super.acceptAction(message, input);
			
			Tuple<Message, Task<Void>> job = Tuple.of(message, Manager.of(Executor.class).normalPending());
			synchronized(this)
			{
				if( limit > 0 && queue.size() >= limit )
				{
					message.metadata().put("error", new IllegalStateException("Queue size exceeded"));
					return emit(message, "error");
				}
				queue.offer(job);
			}
			
			while( checkNext() ) ;
			return job.b;
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
			
			Tuple<Message, Task<Void>> job = queue.poll();
			if( job == null )
			{
				parallel.decrementAndGet();
				return false;
			}
			
			emit(job.a, "data").anyway(() ->
			{
				parallel.decrementAndGet();
				job.b.complete(null);
				while( checkNext() ) ;
			});
			
			return true;
		}
	}
	
	protected Class<? extends Queue.Type> defaultTarget() { return Queue.Type.class; }
	protected Supplier<? extends Queue.Type> defaultCreator() { return Queue.Type::new; }

	@Override
	public Template template()
	{
		return super.template()
			.icon("stacks")
			.input(new Channel("data")
				.summary("Data")
				.description("Enqueue a message."))
			.output(new Channel("data")
				.summary("Data")
				.description("Dequeued messages."))
			.summary("Queue")
			.description("The queue will buffer messages in memory and process them according to the configured parameters.")
			.add(new Parameter("limit")
				.summary("Maximum number of queued messages")
				.description("This parameter defines the maximum number of messages that can be queued. If the limit is reached, "
					+ "additional messages will be rejected. To disable the limit, set it to a negative value. "
					+ "If the limit is set to 0, then it means there will not be any queuing and the messages will be processed directly as they arrive.")
				.rule(Parameter.Rule.INTEGER)
				.format(Parameter.Format.NUMBER)
				.defaultValue(-1))
			.add(new Parameter("concurrency")
				.summary("Concurrency level")
				.description("The concurrency level defines how many messages can be processed simultaneously. "
					+ "In order to ensure messages are processed sequentially, set this parameter to 1. The value 0 or a negative value means "
					+ "that there is no limit to the number of concurrent processing. In a way, this means that there is no queuing because "
					+ "all messages are sent for processing immediately. The default value is the number of processors on the machine.")
				.rule(Parameter.Rule.DIGIT)
				.format(Parameter.Format.NUMBER)
				.defaultValue(Runtime.getRuntime().availableProcessors()))
			.add(new Parameter("priority")
				.summary("High priority")
				.description("Whether or not this queue should execute in high priority mode.")
				.rule(Parameter.Rule.BOOLEAN)
				.format(Parameter.Format.BOOLEAN)
				.defaultValue(false))
			.onCreate((data, instance) ->
			{
				((Queue.Type)instance).concurrency = instance.valueOf("concurrency").asInt();
				((Queue.Type)instance).limit = instance.valueOf("limit").asInt();
			})
			.onUpdate((data, instance) ->
			{
				((Queue.Type)instance).concurrency = instance.valueOf("concurrency").asInt();
				((Queue.Type)instance).limit = instance.valueOf("limit").asInt();
			});
	}
}
