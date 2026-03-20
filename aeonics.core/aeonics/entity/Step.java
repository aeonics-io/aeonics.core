package aeonics.entity;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import aeonics.manager.Manager;
import aeonics.manager.Monitor;
import aeonics.manager.Network;
import aeonics.manager.Scheduler;
import aeonics.data.Data;
import aeonics.entity.security.User;
import aeonics.manager.Executor;
import aeonics.manager.Executor.Task;
import aeonics.manager.Logger;
import aeonics.template.Channel;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.util.Internal;
import aeonics.util.StringUtils;
import aeonics.util.Functions.BiConsumer;
import aeonics.util.Functions.TriConsumer;
import aeonics.util.Functions.TriFunction;
import aeonics.util.Tuples.Tuple;

/**
 * Represents a step in a processing workflow, which is declined either as 
 * {@link Step.Origin}, {@link Step.Action}, or {@link Step.Destination}.
 * 
 * @see Flow
 */
@SuppressWarnings("unchecked")
public abstract class Step extends Item<Step.Type> 
{
	// ================================
	//
	// GENERIC
	//
	// ================================
	
	public enum ROLE
	{
		ORIGIN,
		ACTION,
		DESTINATION,
		TOPIC,
		QUEUE
	}
	
	/**
	 * Template for all step entities
	 */
	public static class Template extends aeonics.template.Template<Step.Type>
	{
		/**
		 * Creates a new step template
		 * @param target the target entity target type
		 * @param type the item type
		 * @param category the item category
		 */
		public Template(Class<? extends Step.Type> target, Class<? extends Step> type, Class<? extends Step> category)
		{
			super(target, type, category);
			
			add(new Relationship("links")
				.category(Step.class)
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
			
			output(new Channel("error")
				.summary("Error")
				.description("Channel used to redirect messages that could not be processed because of an error."));
			
			output(new Channel("ignore")
				.summary("Ignore")
				.description("Channel used to redirect messages that are ignored voluntarily."));
		}
		
		/**
		 * Whether or not the target entity can have custom input channels
		 */
		private boolean inputEnabled = true;
		
		/**
		 * Sets whether or not the target entity can have custom input channels
		 * @param value the value
		 * @param <T> this type
		 * @return this
		 */
		<T extends Step.Template> T inputEnabled(boolean value) { inputEnabled = value; return (T) this; }
		
		/**
		 * Whether or not the target entity can have custom output channels
		 */
		private boolean outputEnabled = true;
		
		/**
		 * Sets whether or not the target entity can have custom output channels
		 * @param value the value
		 * @param <T> this type
		 * @return this
		 */
		<T extends Step.Template> T outputEnabled(boolean value) { outputEnabled = value; return (T) this; }
		
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
		 * @param <T> this type
		 * @param channel the channel to add
		 * @return this
		 */
		public <T extends Step.Template> T input(Channel channel)
		{
			if( !inputEnabled ) throw new UnsupportedOperationException("This template is not input enabled");
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
		 * @param <T> this type
		 * @param channel the channel to add
		 * @return this
		 */
		public <T extends Step.Template> T output(Channel channel)
		{
			if( !outputEnabled ) throw new UnsupportedOperationException("This template is not output enabled");
			Objects.requireNonNull(channel);
			for( Channel c : outputs ) if( c.name().equals(channel.name()) ) throw new IllegalArgumentException("Duplicate channel");
			outputs.add(channel);
			return (T) this;
		}
		
		/** 
		 * The step icon 
		 */
		private String icon = null;
		
		/**
		 * Returns the name of the icon used to render this step visually.
		 * @return the icon name
		 */
		public String icon() { return icon; }
		
		/**
		 * Sets the name of the icon used to render this step visually.
		 * @param <T> the template type
		 * @param value the icon
		 * @return this
		 */
		public <T extends Step.Template> T icon(String value) { icon = value; return (T) this; }
		
		/** 
		 * The step role in the flow 
		 */
		private ROLE role = null;
		
		/**
		 * Returns the role of this step in the flow.
		 * @return the role of this step in the flow
		 */
		public ROLE role() { return role; }
		
		/**
		 * Sets the role of this step in the flow.
		 * @param <T> the template type
		 * @param value the role
		 * @return this
		 */
		<T extends Step.Template> T role(ROLE value) { role = value; return (T) this; }
		
		@Override
		public Data export()
		{
			Data i = Data.map();
			for( Channel x : inputs ) i.put(x.name(), x.export());
			
			Data o = Data.map();
			for( Channel x : outputs ) o.put(x.name(), x.export());
			
			return super.export()
				.put("icon", icon())
				.put("role", role())
				.put("inputs", i)
				.put("outputs", o);
		}
	}
	
	/**
     * Represents the core processing logic for a step in the workflow.
     * Subclasses should define the specific behavior for message handling and security checks.
     */
	public static abstract class Type extends Entity implements Closeable
	{
		/**
		 * Cleanup relationships when this step is removed from the Registry
		 */
		public void close()
		{
			for( Flow.Type flow : Registry.of(Flow.class) )
				flow.removeRelation("steps", this);
			for( Step.Type step : Registry.of(Step.class) )
				step.removeRelation("links", this);
		}
		
		/** 
         * A security check that will be invoked before processing a message.
         * This is a user-defined logic provided as a {@link TriConsumer}, which
         * takes the current user, the message being processed, and the input channel name.
         * <p>
		 * If access is not granted, this method should throw an exception (e.g., {@link SecurityException}).
		 * If the method completes without throwing an exception, access is considered granted.
		 * </p>
         */
		private TriConsumer<User.Type, Data, String> security = null;
		
		/**
         * Sets a security check to be applied during message processing.
         * <p>
		 * If access is not granted, the check method method should throw an exception (e.g., {@link SecurityException}).
		 * If the check method completes without throwing an exception, access is considered granted.
		 * </p>
         *
         * @param check A {@link TriConsumer} accepting a {@link User}, a {@link Message},
         *              and a {@link String} (input channel name). This check will be
         *              executed before processing of each message.
         * @param <T>   The type of the step (for fluent chaining).
         * @return The current instance of the step for method chaining.
         */
		public <T extends Step.Type> T security(TriConsumer<User.Type, Data, String> check) { this.security = check; return (T) this; }
		
		/**
		 * Performs a security check on the message being processed by calling the registered security callback, if any.
		 * If no callback is registered, this method does nothing.
		 * 
		 * @see #security(TriConsumer)
		 * @param user   The {@link User} initiating the action.
		 * @param message The {@link Message} being processed.
		 * @param input  The name of the input channel associated with the message.
		 * @throws Exception if access if not granted.
		 */
		private void checkSecurity(User.Type user, Data payload, String input) throws Exception
		{
		    if (security != null)
		        security.accept(user, payload, input);
		}
		
		/**
		 * Hardcoded category to the {@link Step} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Step.class); }
		
		/**
		 * Adds a link to this step. This is the same as calling {@link #addRelation(String, Entity, Data)}.
		 * @param <T> this type
		 * @param output the output channel from this entity
		 * @param next the next step
		 * @param input the input channel of the next step
		 * @return this
		 */
		public <T extends Step.Type> T link(String output, Step.Type next, String input)
		{
			return link(output, next, input, Data.map());
		}
		
		/**
		 * Adds a link to this step. This is the same as calling {@link #addRelation(String, Entity, Data)}.
		 * @param <T> this type
		 * @param output the output channel from this entity
		 * @param next the next step
		 * @param input the input channel of the next step
		 * @param parameters other relation parameters
		 * @return this
		 */
		public <T extends Step.Type> T link(String output, Step.Type next, String input, Data parameters)
		{
			// check the channels
			boolean match = false;
			for( Channel c : template().<Step.Template>cast().outputs() )
			{
				if( c.name().equals(output) )
				{
					match = true;
					break;
				}
			}
			if( !match ) throw new IllegalArgumentException("Invalid output name: " + output);
			
			for( Channel c : next.template().<Step.Template>cast().inputs() )
			{
				if( c.name().equals(input) )
				{
					match = true;
					break;
				}
			}
			if( !match ) throw new IllegalArgumentException("Invalid input name: " + input);
			
			// check for duplicate
			for( Tuple<Entity, Data> l : relations("links") )
			{
				if( l.a == next && l.b.asString("output").equals(output) && l.b.asString("input").equals(input) )
					return (T) this;
			}
			
			if( parameters == null || parameters.isNull() ) parameters = Data.map();
			if( !parameters.isMap() ) throw new IllegalArgumentException("Invalid relation parameters: " + parameters);
			
			addRelation("links", next, parameters.put("output", output).put("input", input));
			return (T) this;
		}
		
		/**
		 * Removes a link from this step.
		 * @param <T> this type
		 * @param output the output channel from this entity
		 * @param next the next step
		 * @param input the input channel of the next step
		 * @return this
		 */
		public <T extends Step.Type> T unlink(String output, Step.Type next, String input)
		{
			Iterator<Tuple<Entity, Data>> i = relations("links").iterator();
			while( i.hasNext() )
			{
				Tuple<Entity, Data> l = i.next();
				if( l.a == next && l.b.asString("output").equals(output) && l.b.asString("input").equals(input) )
					i.remove();
			}
			return (T) this;
		}
		
		/**
		 * Performs the actual emit action with drop conditions check first.
		 * @param message the message
		 * @param output the output channel name
		 * @param next the next instance
		 * @param input the input channel name
		 * @return the task completion status
		 */
		private Task<Task<Void>> doEmit(Message message, String output, Step.Type next, String input)
		{
			if( message == null || next == null || output == null || input == null )
				return Manager.of(Executor.class).normalResolved(Manager.of(Executor.class).normalResolved(null));
			
			if( (output.equals("error") && message.metadata().asBool("ignored"))
				|| (output.equals("ignore") && message.metadata().asBool("ignored"))
				)
			{
				Discard.error(message, new Exception("Infinite message routing loop"));
				return Manager.of(Executor.class).normalResolved(Manager.of(Executor.class).normalResolved(null));
			}
			
			if( message.metadata().asInt("ttl") <= 0 )
			{
				Discard.expired(message);
				return Manager.of(Executor.class).normalResolved(Manager.of(Executor.class).normalResolved(null));
			}
			
			if( output.equals("ignore") )
				message.metadata().put("ignored", true);
			else if( output.equals("error") )
				message.metadata().put("errored", true);
			message.metadata().put("ttl", message.metadata().asInt("ttl") - 1);
					
			return Manager.of(Executor.class).normal(() -> 
			{
				return next.accept(message, input);
			});
		}
		
		/**
		 * Emits a message to the specified output channel and routes it to the linked downstream steps.
		 * This method is used by {@link Step.Action} and {@link Step.Origin} steps.
		 *
		 * <p>The method handles special cases for destination steps (which cannot emit to channels other
		 * than "error" or "ignore").</p>
		 *
		 * @param message The {@link Message} to emit to the specified output channel.
		 * @param output  The name of the output channel where the message should be routed.
		 * @return A {@link Task} representing the completion status of emitting the message, including
		 *         all downstream step executions. This means that the returned task completes when all
		 *         next processing paths have completed.
		 */
		Task<Void> emit(Message message, String output)
		{
			// sanity check for destination
			if( this instanceof Step.Destination.Type && !output.equals("error") && !output.equals("ignore") )
			{
				message.metadata().put("error", new IllegalStateException("Invalid output channel for destination step: " + output));
				return emit(message, "error");
			}
			
			// collect the links first
			List<Tuple<Entity, Data>> links = collectLinks(output);
			
			// no match -> ignore
			if( links.size() == 0 )
			{
				if( output.equals("ignore") )
				{
					Discard.error(message, new Exception("Infinite message routing loop"));
					return Manager.of(Executor.class).normalResolved(null);
				}
				else
					return emit(message, "ignore");
			}
			
			// track the status of each
			List<Task<?>> status = new ArrayList<>(links.size());
			boolean clone = links.size() > 1;
			links.forEach(link ->
			{
				Task<Task<Void>> next = doEmit(clone ? message.clone() : message, output, link.a.cast(), output);
				status.add(next.link(n -> n));
			});
			
			// resolve when all have resolved
			return Manager.of(Executor.class).normal(status);
		}
		
		/**
		 * Collects all the relevant links for the specified output channel name
		 * @param output the channel name
		 * @return the list of relevant output links
		 */
		List<Tuple<Entity, Data>> collectLinks(String output)
		{
			List<Tuple<Entity, Data>> links = new ArrayList<>();
			for( Tuple<Entity, Data> link : relations("links") )
			{
				if( link.a == null || !link.b.asString("output").equals(output) )
					continue;
				if( !(link.a instanceof Step.Action.Type) && !(link.a instanceof Step.Destination.Type) )
					continue;
				
				// TODO : include only links in the same flow
				links.add(link);
			}
			
			return links;
		}
		
		/**
		 * Processes a message received on a specific input channel and routes it through the appropriate logic
		 * based on the step type (Action, or Destination).
		 * <p>
		 * This method enforces security checks before processing.
		 * </p>
		 * 
		 * @param message The {@link Message} to be processed.
		 * @param input   The name of the input channel receiving the message.
		 * @return A {@link Task} representing the completion status of forwarding the message, including
		 *         all downstream step executions. This means that the returned task completes when all
		 *         next processing paths have completed.
		 */
		Task<Void> accept(Message message, String input)
		{
			try( Monitor.MonitorTimer x = Manager.of(Monitor.class).ns(this) )
			{
				context(Data.of(message));
			
				// sanity check for origin
				if( this instanceof Step.Origin.Type )
				{
					message.metadata().put("error", new IllegalStateException("Invalid accept operation for origin step"));
					return emit(message, "error");
				}
				
				// check security
				try
				{
					checkSecurity(Registry.of(User.class).get(message.user()), message.content(), input);
				}
				catch(Exception e)
				{
					message.metadata().put("error", e);
					return emit(message, "error");
				}
			
				// simple handling for destination
				if( this instanceof Step.Destination.Type )
					return acceptDestination(message, input);
				
				// handling for action
				if( this instanceof Step.Action.Type )
					return acceptAction(message, input);
			
				// unsupported other step type
				message.metadata().put("error", new IllegalStateException("Invalid step type: " + this.getClass().getName()));
				return emit(message, "error");
			}
		}
		
		/**
		 * Accept method specific for destination entities
		 * @param message the message
		 * @param input the channel name
		 * @return the pending completion task
		 */
		Task<Void> acceptDestination(Message message, String input)
		{
			try
			{
				this.<Step.Destination.Type>cast().process(message, input);
				return Manager.of(Executor.class).normalResolved(null);
			}
			catch(Exception e)
			{
				message.metadata().put("error", e);
				return emit(message, "error");
			}
		}
		
		/**
		 * Accept method specific for action entities
		 * @param message the message
		 * @param input the channel name
		 * @return the pending completion task
		 */
		Task<Void> acceptAction(Message message, String input)
		{
			List<Task<?>> nexts = new ArrayList<>();
			Map<String, List<Tuple<Entity, Data>>> links = aggregateLinks();
		
			// process the output message once per channel
			links.forEach((output, list) ->
			{
				if( output.equals("error") || output.equals("ignore") ) return;
				if( list.size() == 0 ) return;
				
				try
				{
					Message result = this.<Action.Type>cast().process(links.size() > 1 ? message.clone() : message, input, output);
					if( result == null ) return;
					forward(result, output, list, nexts);
				}
				catch(Exception e)
				{
					Message m = links.size() > 1 ? message.clone() : message;
					m.metadata().put("error", e);
					forward(m, output, links.get("error"), nexts);
				}
			});

			// in case ignored
			if( nexts.size() == 0 )
				forward(message, "ignore", links.get("ignore"), nexts);
			// in case ignore ignored
			if( nexts.size() == 0 )
				return emit(message, "ignore");
			
			return Manager.of(Executor.class).normal(nexts);
		}
		
		/**
		 * Groups all links by output channel name
		 * @return a map of all links grouped by output channel name
		 */
		Map<String, List<Tuple<Entity, Data>>> aggregateLinks()
		{
			// aggregate links per output channel
			Map<String, List<Tuple<Entity, Data>>> links = new HashMap<>();
			template().<Step.Template>cast().outputs().forEach(channel ->
			{
				// TODO : include only links in the same flow
				
				List<Tuple<Entity, Data>> list = collectLinks(channel.name());
				if( list != null && list.size() > 0 )
					links.put(channel.name(), list);
			});
			
			return links;
		}
		
		/**
		 * Forwards the message to all links and collects the pending tasks
		 * @param message the message
		 * @param links all links
		 * @param nexts the list of tasks to append to
		 */
		void forward(Message message, String output, List<Tuple<Entity, Data>> links, List<Task<?>> nexts)
		{
			if( links == null ) return;
			
			boolean clone = links.size() > 1;
			links.forEach(t ->
			{
				Task<Task<Void>> next = doEmit(clone ? message.clone() : message, output, t.a.cast(), t.b.asString("input")) ;
				nexts.add(next.link(n -> n));
			});
		}
	}
	
	/**
	 * Hardcoded category to the {@link Step} class
	 */
	protected final Class<? extends Step> category() { return Step.class; }
	
	@Override
	public Template template()
	{
		return new Template(target(), this.getClass(), category())
			.creator(creator())
			.cast();
	}
	
	// ================================
	//
	// ORIGIN
	//
	// ================================
	
	/**
	 * This class represents the Origin of data. It is the starting point of a data flow. 
	 * <p>For each mew message, you should call the {@link Origin.Type#produce(Message, String)} method to inject data in the system, 
	 * which will transfer the message to next processing steps.</p>
	 * 
	 * <p>There are two recommended ways to create your own item inline (without creating a full class).
	 * The first method allows to provide the data collector function and registers automatically the template in
	 * the factory and the instance in the registry:</p>
	 * <pre>
	 * Origin.Type item = new Origin() { } // &lt;-- note the '{ }' to create a new anonymous class
	 *     
	 *     // specify which variant to use
	 *     .target(Origin.Background.class)
	 *     .creator(Origin.Background::new)
	 *     
	 *     .template() // &lt;-- create the template and register it in the factory
	 *     
	 *     // add all your template documentation
	 *     .summary("Does something")
	 *     
	 *     .create() // &lt;-- create an instance of the entity and register it in the registry
	 *     
	 *     // set the processing function
	 *     .&lt;Origin.Background&gt;cast()
	 *     .run(() -&gt; { })
	 * </pre>
	 * 
	 * <p>If you need more control over the behavior such as private member variables or multiple
	 * methods, then you need to declare a custom entity end register it <b>before</b> calling the template method:</p>
	 * <pre>
	 * public static class MyEntity extends Origin.Type {
	 *     private void start() { }
	 *     public void stop() { }
	 * }
	 * 
	 * Origin.Type item = new Origin() { } // &lt;-- note the '{ }' to create a new anonymous class
	 *     
	 *     // register the custom entity before calling the template
	 *     .target(MyEntity.class)
	 *     .creator(MyEntity::new)
	 *     
	 *     .template() // &lt;-- create the template and register it in the factory
	 *     
	 *     // add all your template documentation
	 *     .summary("Does something")
	 *     
	 *     .create(); // &lt;-- create an instance of the entity and register it in the registry
	 * </pre>
	 */
	public static class Origin extends Step
	{
		// ====================
		// BASIC ORIGIN
		// ====================
		
		/**
	     * Represents the type definition for a {@link Step.Origin}, including its
	     * message generation logic. An origin step produces messages and routes
	     * them to the appropriate output channels.
	     */
		public static class Type extends Step.Type implements Closeable
		{
			/**
	         * Produces a message and emits it to the specified output channel. This
	         * method is the entry point for message generation in an origin step.
	         *
	         * @param message The {@link Message} to be emitted to the specified output channel.
	         * @param output  The name of the output channel where the message should be routed.
	         */
			public void produce(Message message, String output)
			{
				this.emit(message, output);
			}
			
			/**
			 * Internal state:
			 *  0: stopped
			 *  1: starting
			 *  2: started
			 *  3: stopping
			 */
			private int state = 0;
			
			/**
			 * Returns whether or not this Origin is in the stopped state.
			 * @return true if this Origin is stopped
			 */
			public boolean stopped() { return state == 0; }
			/**
			 * Signals that this Origin is stopped.
			 * @param value true if this Origin is stopped.
			 */
			@Internal
			public void stopped(boolean value) { if( value ) state = 0; }
			/**
			 * Returns whether or not this Origin is in the starting state.
			 * @return true if this Origin is starting
			 */
			public boolean starting() { return state == 1; }
			/**
			 * Signals that this Origin is starting.
			 * @param value true if this Origin is starting.
			 */
			@Internal
			public void starting(boolean value) { if( value ) state = 1; }
			/**
			 * Returns whether or not this Origin is in the started state.
			 * @return true if this Origin is started
			 */
			public boolean started() { return state == 2; }
			/**
			 * Signals that this Origin is started.
			 * @param value true if this Origin is started.
			 */
			@Internal
			public void started(boolean value) { if( value ) state = 2; }
			/**
			 * Returns whether or not this Origin is in the stopping state.
			 * @return true if this Origin is stopping
			 */
			public boolean stopping() { return state == 3; }
			/**
			 * Signals that this Origin is stopping.
			 * @param value true if this Origin is stopping.
			 */
			@Internal
			public void stopping(boolean value) { if( value ) state = 3; }
			
			/**
			 * The start function
			 */
			private Runnable start = null;
			
			/**
			 * Starts this Origin entity.
			 * The Origin can only be started if it is currently stopped, otherwise it does nothing.
			 * Implementations are responsible to set and maintain the internal state.
			 */
			public void start()
			{
				if( start != null ) start.run();
				else started(true);
			}
			
			/**
			 * Sets the inline start function as an alternative to {@link #start()}.
			 * @param <T> this
			 * @param starter the start function
			 * @return this
			 */
			public <T extends Origin.Type> T start(Runnable starter) { this.start = starter; return (T) this; }
			
			/**
			 * The stop function.
			 */
			private Runnable stop = null;
			
			/**
			 * Stops this Origin entity.
			 * The Origin can only be stopped if it is currently running, otherwise it does nothing.
			 * Implementations are responsible to set and maintain the internal state.
			 */
			public void stop()
			{
				if( stop != null ) stop.run();
				else stopped(true);
			}
			
			/**
			 * Sets the stop function as an alternative to {@link #stop()}.
			 * @param <T> this
			 * @param stopper the stop function
			 * @return this
			 */
			public <T extends Origin.Type> T stop(Runnable stopper) { this.stop = stopper; return (T) this; }
			
			/**
			 * Calls {@link #stop()}
			 */
			public void close() { stop(); }
		}
		
		// ====================
		// BACKGROUND ORIGIN
		// ====================
		
		/**
		 * This class represents a data Origin that runs in the background in order to collect data and inject it in the system.
		 * <p>You should implement the {@link #run()} method and call the {@link #produce(Message, String)} method to inject data in the system.</p>
		 */
		public static class Background extends Origin.Type implements Runnable
		{
			/**
			 * The run function
			 */
			private Runnable run = null;
			
			/**
			 * Sets the run function as an alternative to {@link #run()}.
			 * @param <T> this
			 * @param runner the run function
			 * @return this
			 */
			public <T extends Background> T run(Runnable runner) { this.run = runner; return (T) this; }
			
			/**
			 * Collects data in the background and calls {@link #emit(Message, String)} method to inject data in the system.
			 */
			public void run()
			{
				if( run != null ) run.run();
				else throw new IllegalStateException("Run function is not set");
			}
			
			/**
			 * A reference to the thread in which this Origin is running.
			 */
			private Task<Void> task = null;

			@Override
			public void start()
			{
				synchronized(this)
				{
					if( !stopped() ) return;
					starting(true);
					if( task == null ) task = Manager.of(Executor.class).background(() ->
					{
						started(true);
						while( !stopping() )
						{
							try
							{
								run();
							}
							catch(Exception e)
							{
								Manager.of(Logger.class).severe(getClass(), e);
							}
							
							if( !stopping() )
							{
								// in case there was an error in run() do not loop too fast
								try { Thread.sleep(1000); }
								catch(InterruptedException e) { break; }
							}
						}
						stopped(true);
					});
				}
			}

			@Override
			public void stop()
			{
				synchronized(this)
				{
					if( !started() ) return;
					stopping(true);
					if( task != null ) task.cancel();
				}
			}
		}
		
		// ====================
		// NETWORK SERVER ORIGIN
		// ====================
		
		/**
		 * This class represents a data Origin that listens for network data.
		 * <p>If the connection is interrupted, this endpoint will attempt to reconnect every second (unless it is stopped).</p>
		 * <p>You should implement the {@link #connect()} method and call the {@link #produce(Message, String)} method to inject data in the system.</p>
		 */
		public static class NetworkServer extends Origin.Type
		{
			/**
			 * A reference to the network server.
			 */
			private Network.Server server = null;
			
			/**
			 * Returns the connected network server object.
			 * @return the connected network server object or null if not yet connected
			 */
			protected Network.Server server() { return server; }
			
			/**
			 * The connection supplier
			 */
			private Supplier<Network.Server> connect = null;
			
			/**
			 * Returns a server network connection. 
			 * @return a server network connection
			 * @throws Exception if an error happens while establishing the connection
			 */
			public Network.Server connect() throws Exception
			{
				if( connect != null ) return connect.get();
				else throw new IllegalStateException("Connect function is not set");
			}
			
			/**
			 * Sets the connect function as an alternative to {@link #connect()}.
			 * @param <T> this
			 * @param connecter the connect function
			 * @return this
			 */
			public <T extends NetworkServer> T connect(Supplier<Network.Server> connecter) { this.connect = connecter; return (T) this; }

			@Override
			public void start()
			{
				synchronized(this)
				{
					if( !stopped() ) return;
					starting(true);
					start2();
				}
			}
			
			private void start2()
			{
				synchronized(this)
				{
					if( !starting() ) return;
					try
					{
						server = connect();
						server.onClose().then((x, s) ->
						{
							if( !stopping() && !stopped() )
							{
								Manager.of(Scheduler.class).in((t) -> 
								{
									// restart
									starting(true);
									start2();
								}, 1000);
							}
						});
						started(true);
					}
					catch(Exception e)
					{
						Manager.of(Logger.class).severe(getClass(), e);
						if( !stopping() && !stopped() )
						{
							Manager.of(Scheduler.class).in((t) -> 
							{
								// restart
								starting(true);
								start2();
							}, 1000);
						}
					}
				}
			}

			@Override
			public void stop()
			{
				synchronized(this)
				{
					stopping(true);
					if( server != null )
					{
						try { server.close(); } catch(Exception e) { /* ignore */ }
					}
					stopped(true);
				}
			}
		}
		
		// ====================
		// NETWORK CLIENT ORIGIN
		// ====================
		
		/**
		 * This class represents a data Origin that connects to a remote network endpoint to fetch data.
		 * <p>If the connection is interrupted, this endpoint will attempt to reconnect every second (unless it is stopped).</p>
		 * <p>You should implement the {@link #connect()} method and call the {@link #produce(Message, String)} method to inject data in the system.</p>
		 */
		public static class NetworkClient extends Origin.Type
		{
			/**
			 * A reference to the network connection.
			 */
			private Network.Connection connection = null;
			
			/**
			 * The connection supplier
			 */
			private Supplier<Network.Connection> connect = null;
			
			/**
			 * Returns a server network connection. 
			 * @return a server network connection
			 * @throws Exception if an error happens while establishing the connection
			 */
			public Network.Connection connect() throws Exception
			{
				if( connect != null ) return connect.get();
				else throw new IllegalStateException("Connect function is not set");
			}
			
			/**
			 * Sets the connect function as an alternative to {@link #connect()}.
			 * @param <T> this
			 * @param connecter the connect function
			 * @return this
			 */
			public <T extends NetworkClient> T connect(Supplier<Network.Connection> connecter) { this.connect = connecter; return (T) this; }

			@Override
			public void start()
			{
				synchronized(this)
				{
					if( !stopped() ) return;
					starting(true);
					start2();
				}
			}
			
			private void start2()
			{
				synchronized(this)
				{
					if( !starting() ) return;
					try
					{
						connection = connect();
						connection.onClose().then((x, s) ->
						{
							if( !stopping() && !stopped() )
							{
								Manager.of(Scheduler.class).in((t) -> 
								{
									// restart
									starting(true);
									start2();
								}, 1000);
							}
						});
						started(true);
					}
					catch(Exception e)
					{
						Manager.of(Logger.class).severe(getClass(), e);
						if( !stopping() && !stopped() )
						{
							Manager.of(Scheduler.class).in((t) -> 
							{
								// restart
								starting(true);
								start2();
							}, 1000);
						}
					}
				}
			}

			@Override
			public void stop()
			{
				synchronized(this)
				{
					stopping(true);
					if( connection != null )
					{
						try { connection.close(); } catch(Exception e) { /* ignore */ }
					}
					stopped(true);
				}
			}
		}
		
		protected Class<? extends Origin.Type> defaultTarget() { return Origin.Type.class; }
		protected Supplier<? extends Origin.Type> defaultCreator() { return Origin.Type::new; }
		
		@Override
		public Template template()
		{
			return super.template()
				.inputEnabled(false)
				.outputEnabled(true)
				.role(ROLE.ORIGIN)
				.icon("cloud_upload")
				;
		}
	}
	
	// ================================
	//
	// ACTION
	//
	// ================================
	
	/**
	 * This entity represents an Action taken against data. It is an intermediate step of a data flow.
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
	 *     .create() // &lt;-- create an instance of the entity and register it in the registry
	 *     
	 *     // set the processing function
	 *     .processor((message, input, output) -&gt; null); // &lt;-- the process logic
	 * </pre>
	 * 
	 * <p>If you need more control over the behavior such as private member variables or multiple
	 * methods, then you need to declare a custom entity and register it <b>before</b> calling the template method:</p>
	 * <pre>
	 * public static class MyEntity extends Action.Type {
	 *     private int response = 42;
	 *     private Message respond() { return new Message().content().put("foo", response); }
	 *     public Message process(Message message, String input, String output) { return respond(); }
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
	 *     .summary("Does something")
	 *     
	 *     .create(); // &lt;-- create an instance of the entity and register it in the registry
	 * </pre>
	 */
	public static abstract class Action extends Step
	{
		/**
	     * Represents the type definition for a {@link Step.Action}, including
	     * its processing logic. An action step processes messages received from
	     * input channels and routes the processed result to the appropriate output channels.
	     * 
	     * <p>Implementations should either set a custom processor using {@link #processor(TriFunction)}
	     * or override the {@link #process(Message, String, String)} method to define
	     * the specific transformation logic for the action step.</p>
	     */
		public static class Type extends Step.Type
		{
			/**
	         * A processor function to handle the transformation of incoming messages.
	         * The processor is a user-defined {@link TriFunction} that accepts the
	         * incoming {@link Message}, the input channel name, and the output channel name,
	         * and returns the transformed message.
	         * 
	         * <p>If the returned message is null, then it is considered as ignored.</p>
	         */
			private TriFunction<Message, String, String, Message> processor = null;
			
			/**
	         * Sets the processor function for this action step.
	         *
	         * @param processor A {@link TriFunction} defining the processing logic, which accepts:
	         *                  <ul>
	         *                      <li>The {@link Message} to be processed.</li>
	         *                      <li>The name of the input channel.</li>
	         *                      <li>The name of the output channel.</li>
	         *                  </ul>
	         *                  Returns the transformed {@link Message} or {@code null} if no output is produced and thus the message is ignored.
	         * @param <T>       The type of this action step (for fluent chaining).
	         * @return The current instance of the action type for method chaining.
	         */
			public <T extends Action.Type> T processor(TriFunction<Message, String, String, Message> processor) { this.processor = processor; return (T) this; }
			
			/**
	         * Executes the processing logic for the action step. This method is called when
	         * a message is routed to the action step and invokes the processor
	         * function, if it has been set.
	         *
	         * <p>The method can be called multiple times for the same message if the action
			 * has multiple outputs bound to different downstream steps. However, it is
			 * guaranteed to be called only once per output channel. Unbound output channels do not
			 * trigger the process method.</p>
			 * 
	         * @param message The {@link Message} to be processed.
	         * @param input   The name of the input channel through which the message was received.
	         * @param output  The name of the output channel to which the message will be sent.
	         * @return The processed {@link Message}, or {@code null} if no output is produced and thus the message is ignored.
	         * @throws Exception If an error occurs during processing.
	         */
			protected Message process(Message message, String input, String output) throws Exception
			{
				if( processor != null )
					return processor.apply(message, input, output);
				return null;
			}
		}
		
		protected Class<? extends Action.Type> defaultTarget() { return Action.Type.class; }
		protected Supplier<? extends Action.Type> defaultCreator() { return Action.Type::new; }
		
		@Override
		public Template template()
		{
			return super.template()
				.inputEnabled(true)
				.outputEnabled(true)
				.role(ROLE.ACTION)
				.icon("crop_rotate")
				;
		}
	}
	
	// ================================
	//
	// DESTINATION
	//
	// ================================
	
	/**
	 * This class represents the Destination of data. It is the termination point of a data flow.
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
	 *     .create() // &lt;-- create an instance of the entity and register it in the registry
	 *     
	 *     // set the processing function
	 *     .processor((message, input) -&gt; {}); // &lt;-- the process logic
	 * </pre>
	 * 
	 * <p>If you need more control over the behavior such as private member variables or multiple
	 * methods, then you need to declare a custom entity end register it <b>before</b> calling the template method:</p>
	 * <pre>
	 * public static class MyEntity extends Destination.Type {
	 *     private void log(Message message) { System.out.println(message); }
	 *     public Message process(Message message, String input) { log(message); return null; }
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
	 *     .summary("Does something")
	 *     
	 *     .create(); // &lt;-- create an instance of the entity and register it in the registry
	 * </pre>
	 * <p>Note that the output argument will always be <code>null</code> and the method should always return <code>null</code>.</p>
	 */
	public static abstract class Destination extends Step
	{
		/**
	     * Represents the type definition for a {@link Step.Destination}, including
	     * its processing logic. A destination step processes messages and does not
	     * produce further outputs, except for error handling.
	     * 
	     * <p>Implementations should either set a custom processor using {@link #processor(BiConsumer)}
		 * or override the {@link #process(Message, String)} method to define the
		 * specific behavior of the destination step.</p>
	     */
		public static class Type extends Step.Type
		{
			/**
			 * Represents the type definition for a {@link Step.Destination}, including
			 * its processing logic. A destination step processes messages and does not
			 * produce further outputs, except for error handling.
			 */
			private BiConsumer<Message, String> processor = null;
			
			/**
	         * Sets the processor function for this destination step.
	         *
	         * @param processor A {@link BiConsumer} that defines the processing logic,
	         *                  accepting a {@link Message} and the name of the input channel.
	         * @param <T>       The type of this destination step (for fluent chaining).
	         * @return The current instance of the destination type for method chaining.
	         */
			public <T extends Destination.Type> T processor(BiConsumer<Message, String> processor) { this.processor = processor; return (T) this; }
			
			/**
	         * Executes the processing logic for the destination step. This method is
	         * called when a message is routed to the destination step and invokes the
	         * user-defined processor function, if it has been set.
	         *
	         * @param message The {@link Message} to be processed.
	         * @param input   The name of the input channel through which the message was received.
	         * @throws Exception If an error occurs during processing.
	         */
			protected void process(Message message, String input) throws Exception
			{
				if( processor != null )
					processor.accept(message, input);
			}
		}
		
		protected Class<? extends Destination.Type> defaultTarget() { return Destination.Type.class; }
		protected Supplier<? extends Destination.Type> defaultCreator() { return Destination.Type::new; }
		
		@Override
		public Template template()
		{
			return super.template()
				.inputEnabled(true)
				.outputEnabled(false)
				.role(ROLE.DESTINATION)
				.icon("where_to_vote")
				;
		}
	}
}
