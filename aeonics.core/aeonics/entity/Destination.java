package aeonics.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.template.Channel;
import aeonics.template.Factory;
import aeonics.template.Item;
import aeonics.util.StringUtils;

/**
 * This class represents the Destination of data. It is the termination point of a data flow.
 * <p>Every time some data is available the {@link Type#accept(Message, String)} method will be called.</p>
 * <p>Be careful that multiple calls to the {@link Type#accept(Message, String)} method may happen in parallel.
 * If your implementation is not thread safe, you should add proper thread safety (example with a <code>synchronized</code> block statement).</p>
 * 
 * <p>There are two recommended ways to create your own item inline (without creating a full class).
 * The first method allows to provide the data processing function and registers automatically the template in
 * the factory and the instance in the registry:</p>
 * <pre>
 * Destination.Type item = new Destination() { } // &lt;-- note the '{ }' to create a new anonymous class
 *     
 *     .template() // &lt;-- create the template and register it in the factory
 *     
 *     // add all your template documentation
 *     .summary("Does something")
 *     
 *     .build() // &lt;-- create an instance of the entity and register it in the registry
 *     
 *     // set the processing function
 *     .process((message, input) -&gt; {}); // &lt;-- the process logic
 * </pre>
 * 
 * <p>If you need more control over the behavior such as private member variables or multiple
 * methods, then you need to declare a custom entity end register it <b>before</b> calling the template method:</p>
 * <pre>
 * public static class MyEntity extends Destination.Type {
 *     private void log(Message message) { System.out.println(message); }
 *     public void accept(Message message, String input) { log(message); }
 * }
 * 
 * Destination.Type item = new Destination() { } // &lt;-- note the '{ }' to create a new anonymous class
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
public abstract class Destination extends Item<Destination.Type>
{
	/**
	 * Superclass template for destinations
	 */
	public static class Template extends aeonics.template.Template<Destination.Type>
	{
		public Template(Class<? extends Destination.Type> target, Class<? extends Destination> type)
		{
			super(target, type, Destination.class);
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
		public Destination.Template input(Channel channel)
		{
			Objects.requireNonNull(channel);
			for( Channel c : inputs ) if( c.name().equals(channel.name()) ) throw new IllegalArgumentException("Duplicate channel");
			inputs.add(channel);
			return this;
		}

		@Override
		public Data export()
		{
			Data i = Data.map();
			for( Channel x : inputs ) i.put(x.name(), x.export());
			
			return super.export()
				.put("inputs", i);
		}
	}
	
	/**
	 * Superclass for all destination entities.
	 */
	public static class Type extends Entity
	{
		/**
		 * The inline process function
		 */
		private BiConsumer<Message, String> process = null;
		
		/**
		 * Sets the inline process function as an alternative to {@link #accept(Message, String)}.
		 * @param <T> this
		 * @param processor the process function
		 * @return this
		 */
		@SuppressWarnings("unchecked")
		public <T extends Type> T process(BiConsumer<Message, String> processor) { this.process = processor; return (T) this; } 
		
		/**
		 * This method is called when data is available. The input channel is provided as a hint on the intended behavior.
		 * 
		 * <p>If you do not specify the processing function using {@link #process(BiConsumer)}, you can override this method 
		 * to define a custom behavior.</p>
		 * 
		 * @param message the input message
		 * @param input the input channel name
		 */
		public void accept(Message message, String input)
		{
			if( process != null )
				process.accept(message, input);
		}
		
		/**
		 * Hardcoded category to the {@link Destination} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Destination.class); }
	}
	
	protected Class<? extends Destination.Type> defaultTarget() { return Destination.Type.class; }
	protected Supplier<? extends Destination.Type> defaultCreator() { return Destination.Type::new; }
	protected Class<? extends Destination> category() { return Destination.class; }

	@Override
	public Destination.Template template()
	{
		Destination.Template t = new Destination.Template(target(), this.getClass())
			.creator(creator())
			.builder((data, instance) -> { Registry.add(instance); });
		return (Destination.Template) Factory.add(t);
	}
}
