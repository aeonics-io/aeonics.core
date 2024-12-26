package aeonics.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import aeonics.manager.Manager;
import aeonics.data.Data;
import aeonics.manager.Executor;
import aeonics.manager.Executor.Task;
import aeonics.manager.Logger;
import aeonics.template.Channel;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.util.Tuples.Single;
import aeonics.util.Tuples.Tuple;

@SuppressWarnings("unchecked")
abstract class Processor extends Item<Processor.Type> 
{
	static abstract class Type extends Entity
	{
		public Task<Void> accept(Message message, String input)
		{
			try
			{
				List<Task<?>> nexts = new ArrayList<>();
				Map<String, List<Tuple<Entity, Data>>> links = aggregateLinks();
				
				// process the output message once per channel
				Single<Boolean> ignored = Single.of(true);
				links.forEach((output, list) ->
				{
					if( output.equals("error") || output.equals("ignore") ) return;
					if( list.size() == 0 ) return;
					
					try
					{
						Message result = process(links.size() > 1 ? message.clone() : message, input, output);
						if( result == null ) return;
						ignored.a = false;
						forward(result, list, nexts);
					}
					catch(Exception e)
					{
						ignored.a = false;
						Message m = links.size() > 1 ? message.clone() : message;
						m.metadata().put("error", e);
						forward(m, links.get("error"), nexts);
					}
				});
				
				// in case ignored
				if( ignored.a )
					forward(message, links.get("ignore"), nexts);
				
				return Manager.of(Executor.class).normal(nexts);
			}
			catch(Exception e)
			{
				Manager.of(Logger.class).warning(this.getClass(), e);
				return Manager.of(Executor.class).normalResolved(null);
			}
		}
		
		private Map<String, List<Tuple<Entity, Data>>> aggregateLinks()
		{
			// aggregate links per output channel
			Map<String, List<Tuple<Entity, Data>>> links = new HashMap<>();
			relations("link").forEach(t ->
			{
				if( t.a == null ) return;
				
				List<Tuple<Entity, Data>> output = links.computeIfAbsent(t.b.asString("output"), (name) -> new ArrayList<>());
				output.add(t);
			});
			
			return links;
		}
		
		private void forward(Message message, List<Tuple<Entity, Data>> links, List<Task<?>> nexts)
		{
			if( links == null ) return;
			
			boolean clone = links.size() > 1;
			links.forEach(t ->
			{
				Task<Task<Void>> next = Manager.of(Executor.class).normal(() -> 
				{
					return t.a.<Processor.Type>cast().accept(clone ? message.clone() : message, t.b.asString("input"));
				});
				
				nexts.add(next.link(n -> n));
			});
		}
		
		abstract Message process(Message message, String input, String output);
	}
	
	static class Template extends aeonics.template.Template<Processor.Type>
	{
		public Template(Class<? extends Processor.Type> target, Class<? extends Processor> type)
		{
			super(target, type, Processor.class);
			add(new Relationship("links")
				.category(Processor.class)
				.summary("Next flow step")
				.description("The next step to send data after processing.")
				.add(new Parameter("input")
					.summary("Input Channel")
					.description("The name of the input channel of the target step.")
					.format(Parameter.Format.TEXT))
				.add(new Parameter("output")
					.summary("Output Channel")
					.description("The name of the output channel of this step.")
					.format(Parameter.Format.TEXT)));
			icon("crop_rotate");
			
			output(new Channel("error")
				.summary("Error")
				.description("Channel used to redirect messages that could not be processed because of an error."));
			output(new Channel("ignore")
				.summary("Ignore")
				.description("Channel used to redirect messages that are ignored voluntarily."));
		}
		
		/**
		 * List of input channels
		 */
		private List<Channel> inputs = new ArrayList<>();
		
		/**
		 * Returns the list of input channels for this flow item
		 * @return the list of input channels for this flow item
		 */
		public List<Channel> inputs() { return inputs; }
		
		/**
		 * Adds an input channel
		 * @param channel the channel to add
		 * @return this
		 */
		public <T extends Processor.Template> T input(Channel channel)
		{
			Objects.requireNonNull(channel);
			for( Channel c : inputs ) if( c.name().equals(channel.name()) ) throw new IllegalArgumentException("Duplicate channel");
			inputs.add(channel);
			return (T) this;
		}
		
		/**
		 * List of output channels
		 */
		private List<Channel> outputs = new ArrayList<>();
		
		/**
		 * Returns the list of output channels for this flow item
		 * @return the list of output channels for this flow item
		 */
		public List<Channel> outputs() { return outputs; }
		
		/**
		 * Adds an output channel
		 * @param channel the channel to add
		 * @return this
		 */
		public <T extends Processor.Template> T output(Channel channel)
		{
			Objects.requireNonNull(channel);
			for( Channel c : outputs ) if( c.name().equals(channel.name()) ) throw new IllegalArgumentException("Duplicate channel");
			outputs.add(channel);
			return (T) this;
		}
	}
}
