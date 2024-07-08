package aeonics.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.template.Channel;
import aeonics.template.Factory;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.util.StringUtils;

/**
 * This entity represents an Action taken against data. It is an intermediate step of a data flow.
 * <p>Every time some data is available the {@link Type#accept(Message, String, Set)} method will be called.</p>
 * <p>Be careful that multiple calls to the {@link Type#accept(Message, String, Set)} method may happen in parallel.
 * If your implementation is not thread safe, you should add proper thread safety (example with a <code>synchronized</code> block statement).</p>
 * 
 * <p>There are two recommended ways to create your own action inline (without creating a full class).
 * The first method allows to provide the data processing function and registers automatically the template in
 * the factory and the instance in the registry:</p>
 * <pre>
 * Action.Type action = new Action() { } // &lt;-- note the '{ }' to create a new anonymous class
 *     
 *     .template() // &lt;-- create the template and register it in the factory
 *     
 *     // add all your template documentation
 *     .summary("Does something")
 *     
 *     .build() // &lt;-- create an instance of the entity and register it in the registry
 *     
 *     // set the processing function
 *     .process((message, input, outputs) -&gt; null); // &lt;-- the process logic
 * </pre>
 * 
 * <p>If you need more control over the behavior such as private member variables or multiple
 * methods, then you need to declare a custom entity end register it <b>before</b> calling the template method:</p>
 * <pre>
 * public static class MyEntity extends Action.Type {
 *     private int response = 42;
 *     private Map&lt;String, Message&gt; respond() { return new HashMap&lt;&gt;("response", response); }
 *     public Map&lt;String, Message&gt; accept(Message message, String input, Set&lt;String&gt; outputs) { return respond(); }
 * }
 * 
 * Action.Type action = new Action() { } // &lt;-- note the '{ }' to create a new anonymous class
 *     
 *     // register the custom entity before calling the template
 *     .target(MyEntity.class)
 *     .creator(MyEntity::new)
 *     
 *     .template() // &lt;-- create the template and register it in the factory
 *     
 *     // add all your template documentation
 *     .summary("Do something")
 *     
 *     .build(); // &lt;-- create an instance of the entity and register it in the registry
 * </pre>
 */
public abstract class Action extends Item<Action.Type>
{
	/**
	 * Superclass template for actions
	 */
	public static class Template extends aeonics.template.Template<Action.Type>
	{
		public Template(Class<? extends Action.Type> target, Class<? extends Action> type)
		{
			super(target, type, Action.class);
			add(new Relationship("actions")
				.category(Action.class)
				.summary("Linked Actions")
				.description("List of action entities that are directly connected to this queue.")
				.add(new Parameter("input")
					.summary("Input Channel")
					.description("The name of the input channel of the target action.")
					.format(Parameter.Format.TEXT))
				.add(new Parameter("output")
					.summary("Output Channel")
					.description("The name of the input channel to which this action is bound.")
					.format(Parameter.Format.TEXT)));
			add(new Relationship("destinations")
				.category(Destination.class)
				.summary("Linked Destinations")
				.description("List of destination entities that are directly connected to this queue.")
				.add(new Parameter("input")
					.summary("Input Channel")
					.description("The name of the input channel of the target destination.")
					.format(Parameter.Format.TEXT))
				.add(new Parameter("output")
					.summary("Output Channel")
					.description("The name of the input channel to which this action is bound.")
					.format(Parameter.Format.TEXT)));
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
		public Action.Template input(Channel channel)
		{
			Objects.requireNonNull(channel);
			for( Channel c : inputs ) if( c.name().equals(channel.name()) ) throw new IllegalArgumentException("Duplicate channel");
			inputs.add(channel);
			return this;
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
		public Action.Template output(Channel channel)
		{
			Objects.requireNonNull(channel);
			for( Channel c : outputs ) if( c.name().equals(channel.name()) ) throw new IllegalArgumentException("Duplicate channel");
			outputs.add(channel);
			return this;
		}
		
		@Override
		public Data export()
		{
			Data i = Data.map();
			for( Channel x : inputs ) i.put(x.name(), x.export());
			
			Data o = Data.map();
			for( Channel x : outputs ) o.put(x.name(), x.export());
			
			return super.export()
				.put("inputs", i)
				.put("outputs", o);
		}
	}
	
	/**
	 * Superclass for all action entities.
	 */
	@SuppressWarnings("unchecked")
	public static class Type extends Entity
	{
		/**
		 * Processing function interface
		 */
		public static interface Process
		{
			/**
			 * This method is called when data is available. The input channel is provided as a hint on the intended behavior,
			 * and the list of meaningful output channels is also provided as a hint.
			 * @param message the input data
			 * @param input the input channel name
			 * @param outputs the list of meaningful output channels
			 * @return a map with all output channels bound to a message. Any channel may be included in the response, with or without messgage (null).
			 * Although, for sobriety, it is best if only requested output channels that did produce a message be included in the result.
			 */
			public Map<String, Message> accept(Message message, String input, Set<String> outputs);
		}
		
		/**
		 * This method is called when data is available. The input channel is provided as a hint on the intended behavior,
		 * and the list of meaningful output channels is also provided as a hint.
		 * 
		 * <p>If you do not specify the processing function using {@link #process(Process)}, you can override this method 
		 * to define a custom behavior.</p>
		 * 
		 * @param message the input data
		 * @param input the input channel name
		 * @param outputs the list of meaningful output channels
		 * @return a map with all output channels bound to a message. Any channel may be included in the response, with or without messgage (null).
		 * Although, for sobriety, it is best if only requested output channels that did produce a message be included in the result.
		 */
		public Map<String, Message> accept(Message message, String input, Set<String> outputs)
		{
			if( processor != null ) return processor.accept(message, input, outputs);
			else return null;
		}
		
		/**
		 * Process function
		 */
		private Process processor = null;
		
		/**
		 * Sets the process function as an alternative to {@link #accept(Message, String, Set)}.
		 * @param <T> this
		 * @param processor the process function
		 * @return this
		 */
		public <T extends Action.Type> T process(Process processor) { this.processor = processor; return (T) this; }
		
		/**
		 * Hardcoded category to the {@link Action} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Action.class); }
	}
	
	protected Class<? extends Action.Type> defaultTarget() { return Action.Type.class; }
	protected Supplier<? extends Action.Type> defaultCreator() { return Action.Type::new; }
	protected Class<? extends Action> category() { return Action.class; }

	@Override
	public Action.Template template()
	{
		Action.Template t = new Action.Template(target(), this.getClass())
			.creator(creator())
			.builder((data, instance) -> { Registry.add(instance); });
		return (Action.Template) Factory.add(t);
	}
}
