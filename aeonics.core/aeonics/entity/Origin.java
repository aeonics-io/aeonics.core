package aeonics.entity;

import java.io.Closeable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import aeonics.util.Functions.Consumer;
import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.manager.Executor;
import aeonics.manager.Lifecycle;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.manager.Monitor;
import aeonics.manager.Network;
import aeonics.manager.Scheduler;
import aeonics.manager.Scheduler.Cron;
import aeonics.manager.Executor.Task;
import aeonics.template.Channel;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.util.Internal;
import aeonics.util.StringUtils;
import aeonics.util.Tuples.Tuple;

/**
 * This class represents the Origin of data. It is the starting point of a data flow. 
 * <p>For each mew message, you should call the {@link Type#emit(Message, String)} method to inject data in the system, 
 * which will transfer the message according to the related topics.</p>
 * <p>This class offers an incomplete {@link Template} that defines the relationship with {@link Topic}. 
 * You should thus extend this template in your implementation.</p>
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
 *     .summary("Do something")
 *     
 *     .create(); // &lt;-- create an instance of the entity and register it in the registry
 * </pre>
 */
public abstract class Origin extends Item<Origin.Type>
{
	/**
	 * Superclass template for actions
	 */
	public static class Template extends aeonics.template.Template<Origin.Type>
	{
		public Template(Class<? extends Origin.Type> target, Class<? extends Origin> type)
		{
			super(target, type, Origin.class);
			add(new Relationship("topics")
				.category(Topic.class)
				.summary("Topics")
				.description("The list of output channel to topic link. Multiple topics can be bound to the same channel and one topic can be bound to mulriple channels.")
				.add(new Parameter("channel")
					.summary("Channel")
					.description("The name of the output channel to which this topic is bound.")
					.format(Parameter.Format.TEXT)
				)
			);
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
		public Origin.Template output(Channel channel)
		{
			Objects.requireNonNull(channel);
			for( Channel c : outputs ) if( c.name().equals(channel.name()) ) throw new IllegalArgumentException("Duplicate channel");
			outputs.add(channel);
			return this;
		}

		@Override
		public Data export()
		{
			Data o = Data.map();
			for( Channel x : outputs ) o.put(x.name(), x.export());
			
			return super.export()
				.put("outputs", o);
		}
	}
	
	/**
	 * Superclass for all origin entities.
	 * The {@link #emit(Message, String)} method will publish the message in the topic specified in the entity relationships.
	 */
	public static class Type extends Entity implements Closeable
	{
		/**
		 * This method will call the {@link Topic.Type#publish(Message)} method of all topics that are bound to the specified channel.
		 * @param message the message to publish
		 * @param channel the name of the output channel
		 */
		public void emit(Message message, String channel)
		{
			if( message == null ) return;
			Objects.requireNonNull(channel, "The output channel name is invalid.");
			
			if( !started() )
			{
				Discard.ignore(message, "Origin " + id() + " not started");
				return;
			}
			
			Manager.of(Monitor.class).count(this);
			
			boolean shouldClone = false;
			for( Tuple<Entity, Data> relation : this.relations("topics") )
			{
				if( channel.equals(relation.b.asString("channel")) )
				{
					Topic.Type t = relation.a.cast();
					if( t != null )
					{
						try { t.publish(shouldClone ? message.clone() : message); shouldClone = true; }
						catch(Exception e)
						{
							Discard.error(message, e);
						}
					}
					else
					{
						Discard.ignore(message, "Unknown topic " + relation.b);
					}
				}
			}
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
		}
		
		/**
		 * Sets the inline start function as an alternative to {@link #start()}.
		 * @param <T> this
		 * @param starter the start function
		 * @return this
		 */
		@SuppressWarnings("unchecked")
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
		}
		
		/**
		 * Sets the stop function as an alternative to {@link #stop()}.
		 * @param <T> this
		 * @param stopper the stop function
		 * @return this
		 */
		@SuppressWarnings("unchecked")
		public <T extends Origin.Type> T stop(Runnable stopper) { this.stop = stopper; return (T) this; }
		
		/**
		 * Calls {@link #stop()}
		 */
		public void close() { stop(); }
		
		/**
		 * Hardcoded category to the {@link Origin} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Origin.class); }
	}
	
	protected Class<? extends Origin.Type> defaultTarget() { return Origin.Type.class; }
	protected Supplier<? extends Origin.Type> defaultCreator() { return Origin.Type::new; }
	protected Class<? extends Origin> category() { return Origin.class; }

	@Override
	public Origin.Template template()
	{
		Origin.Template t = new Origin.Template(target(), this.getClass())
			.creator(creator());
		return t;
	}
	
	// =========================================
	//
	// BASIC ORIGIN
	//
	// =========================================
	
	/**
	 * This class represents a basic data Origin that has no predefined behavior.
	 * <p>As is, the only way to emit data is to call the {@link Origin.Type#emit(Message, String)} method manually.</p>
	 */
	public static class Basic extends Origin
	{
		public static class Type extends Origin.Type
		{
			@Override
			public void start() { started(true); }
			
			@Override
			public void stop() { stopped(true); }
		}
		
		protected Class<? extends Basic.Type> defaultTarget() { return Basic.Type.class; }
		protected Supplier<? extends Basic.Type> defaultCreator() { return Basic.Type::new; }
		
		@Override
		public Origin.Template template()
		{
			return super.template()
				.summary("Basic Origin")
				.description("This data origin does not emit any data unless targetted directly by other entities.")
				;
		}
	}
	
	
	// =========================================
	//
	// SCHEDULED ORIGIN
	//
	// =========================================
	
	/**
	 * This class represents a data Origin that is activated at regular interval.
	 * <p>You should provide the {@link Scheduled.Type#task(Consumer)} and set a recurrence rule.</p>
	 */
	public static class Scheduled extends Origin
	{
		public static class Type extends Origin.Type
		{
			private Consumer<ZonedDateTime> task = null;
			private Scheduler.Cron.Type cron = null;
			
			private void runTask(ZonedDateTime value)
			{
				if( task != null )
				{
					try
					{
						task.accept(value);
					}
					catch(Exception e)
					{
						Manager.of(Logger.class).warning(Scheduled.class, e);
					}
				}
			}
			
			/**
			 * Sets the task that shall run at the specified interval
			 * @param value the current time
			 * @return this
			 */
			public Type task(Consumer<ZonedDateTime> value)
			{
				this.task = value;
				return this;
			}
			
			@Override
			public void start()
			{
				if( cron != null )
				{
					Registry.add(cron);
					Manager.of(Scheduler.class).refresh();
					started(true);
				}
			}
			
			@Override
			public void stop()
			{
				if( cron != null )
				{
					Registry.of(Scheduler.Cron.class).remove(cron.id());
					stopped(true);
				}
			}
		}
		
		protected Class<? extends Scheduled.Type> defaultTarget() { return Scheduled.Type.class; }
		protected Supplier<? extends Scheduled.Type> defaultCreator() { return Scheduled.Type::new; }
		
		@Override
		public Origin.Template template()
		{
			return super.template()
				.summary("Scheduled origin")
				.description("This data origin is triggered automatically at regular interval.")
				.add(new Parameter("rule")
					.summary("Recurring rule")
					.description("The recurrence is defined by a RFC-5545 RRULE and DTSART string.")
					.format(Parameter.Format.TEXT))
				.onCreate((data, instance) -> 
				{
					((Origin.Scheduled.Type)instance).cron = new Cron() { }
						.template()
						.create()
						.task((time) -> { ((Origin.Scheduled.Type)instance).runTask(time); })
						.start(ZonedDateTime.now().withNano(0))
						.rule(data.asString("rule"));
						
					if( Manager.of(Lifecycle.class).phase() == Lifecycle.Phase.RUN )
						((Origin.Scheduled.Type)instance).start();
				})
				.onUpdate((data, instance) -> 
				{
					if( data.containsKey("rule") )
					{
						((Origin.Scheduled.Type)instance).cron.rule(data.asString("rule"));
						Manager.of(Scheduler.class).refresh();
					}
				});
		}
	}
	
	// =========================================
	//
	// BACKGROUND ORIGIN
	//
	// =========================================
	
	/**
	 * This class represents a data Origin that runs in the background in order to collect data and inject it in the system.
	 * <p>You should implement the {@link #run()} method and call the {@link #emit(Message, String)} method to inject data in the system.</p>
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
		@SuppressWarnings("unchecked")
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
	
	// =========================================
	//
	// NETWORK SERVER ORIGIN
	//
	// =========================================
	
	/**
	 * This class represents a data Origin that listens for network data.
	 * <p>If the connection is interrupted, this endpoint will attempt to reconnect every second (unless it is stopped).</p>
	 * <p>You should implement the {@link #connect()} method and call the {@link #emit(Message, String)} method to inject data in the system.</p>
	 */
	public static class NetworkServer extends Origin.Type
	{
		/**
		 * A reference to the network server.
		 */
		private Network.Server server = null;
		
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
		@SuppressWarnings("unchecked")
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
	
	// =========================================
	//
	// NETWORK CLIENT ORIGIN
	//
	// =========================================
	
	/**
	 * This class represents a data Origin that connects to a remote network endpoint to fetch data.
	 * <p>If the connection is interrupted, this endpoint will attempt to reconnect every second (unless it is stopped).</p>
	 * <p>You should implement the {@link #connect()} method and call the {@link #emit(Message, String)} method to inject data in the system.</p>
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
		@SuppressWarnings("unchecked")
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
}
