package aeonics.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import aeonics.Plugin;
import aeonics.entity.Registry;
import aeonics.template.Factory;
import aeonics.template.Template;
import aeonics.util.Callback;
import aeonics.util.Internal;
import aeonics.util.Callback.Once;

/**
 * The lifecycle manager should be considered as a global event bus for different application phases.
 * 
 * <p>Distinct {@link Phase} will occur during the application lifecycle. Plugins or other components may
 * register calback handlers to perform some business logic accordingly, either <b>before</b> the event is triggered, 
 * <b>on</b> (when) the event is triggered, or <b>after</b> the event has been trigered.</p>
 * 
 * <p>Handlers must be instances of the {@link Callback.Once} class so that they will be executed only once.
 * This is to prevent accidential executions in case a phase is triggered multiple times.</p>
 * 
 * <p>A phase is complete when all handlers have completed, therefore it is not considered as an applicaiton "state" but more like an event.
 * A phase could (but should not) be triggered multiple times over the entire application lifecycle. In regular conditions,
 * only one phase will run at a time. Unless you have valid reasons to do so, it is always preferable to use the <code>on</code>
 * methods to avoid unintended side effects.</p>
 * 
 * <p>The lifecycle phases will always happen in this sequence:</p>
 * <ol>
 * <li>{@link Phase#LOAD} : when all plugins have been preloaded using {@link Plugin#start()}, the load phase
 * begins. The load phase should be used to register {@link Factory} items and declare {@link Config} parameters.
 * In this phase, only the {@link Lifecycle} and {@link Logger} managers can be used. The {@link Config} manager does not contain populated values yet
 * but it can be used to define parameters.</li>
 * 
 * <li>{@link Phase#CONFIG}: after the load phase is compelte, the config phase begins. The {@link Config} and {@link Snapshot} managers 
 * are now populated. The config phase should be used to register {@link Registry} items. 
 * The latest snapshot (if available) will be restored in this phase also. 
 * Remaining managers will be set and configured but may not be already available for you to use.</li>
 * 
 * <li>{@link Phase#RUN}: after the config phase is complete, the run phase begins. Everything should be ready and it is time to start the system
 * using the {@link Executor} if needed.</li>
 * 
 * <li>{@link Phase#SHUTDOWN}: after the run phase, the shutdown phase indicates a full system shutdown.
 * In this phase, you should cleanup all resources and prepare for shutdown.</li>
 * </ol>
 */
public abstract class Lifecycle extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Lifecycle.class; }
	
	/**
	 * The different application phases.
	 */
	public enum Phase
	{
		/**
		 * When the system is loading
		 */
		LOAD,
		/**
		 * When the config is available
		 */
		CONFIG,
		/**
		 * When the system is ready
		 */
		RUN,
		/**
		 * When the system is shutting down
		 */
		SHUTDOWN
	}
	
	/**
	 * Initiates the boot sequence.
	 * This method <b>must</b> return only after the {@link Phase#SHUTDOWN} is complete.
	 */
	@Internal
	public abstract void boot();
	
	/**
	 * The before callbacks
	 */
	private static Map<Phase, Callback<Void>> before = new ConcurrentHashMap<>();
	
	/**
	 * Registers a handler to run before other handlers in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public void before(Phase phase, Once<Void> handler) 
	{
		synchronized(phase) { before.computeIfAbsent(phase, (p) -> new Callback<Void>()).then(handler); }
	}
	
	/**
	 * Returns the global callback for the given phase
	 * @param phase the phase
	 * @return the matching callback
	 */
	protected Callback<Void> before(Phase phase) { return before.getOrDefault(phase, new Callback<Void>()); }

	/**
	 * The on callbacks
	 */
	private static Map<Phase, Callback<Void>> on = new ConcurrentHashMap<>();
	
	/**
	 * Registers a handler to run in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public void on(Phase phase, Once<Void> handler) 
	{
		synchronized(phase) { on.computeIfAbsent(phase, (p) -> new Callback<Void>()).then(handler); }
	}
	
	/**
	 * Returns the global callback for the given phase
	 * @param phase the phase
	 * @return the matching callback
	 */
	protected Callback<Void> on(Phase phase) { return on.getOrDefault(phase, new Callback<Void>()); }

	/**
	 * The after callbacks
	 */
	private static Map<Phase, Callback<Void>> after = new ConcurrentHashMap<>();
	
	/**
	 * Registers a handler to run after other handlers in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public void after(Phase phase, Once<Void> handler)
	{
		synchronized(phase) { after.computeIfAbsent(phase, (p) -> new Callback<Void>()).then(handler); }
	}
	
	/**
	 * Returns the global callback for the given phase.
	 * @param phase the phase
	 * @return the matching callback
	 */
	protected Callback<Void> after(Phase phase) { return after.getOrDefault(phase, new Callback<Void>()); }
	
	/**
	 * Default initial lifecycle template and entity implementation
	 */
	private static final class NoopLifecycle extends Manager<Lifecycle>
	{
		private static class Implementation extends Lifecycle
		{
			@Override
			public void boot() { throw new IllegalStateException("Cannot boot on this manager"); }
		}

		@Override
		public Template<? extends Lifecycle> template()
		{
			return new Template<Lifecycle>(target(), type(), category())
				.creator(creator())
				.summary("Noop Lifecycle")
				.description("Does nothing.");
		}
		
		protected Class<? extends NoopLifecycle.Implementation> defaultTarget() { return NoopLifecycle.Implementation.class; }
		protected Supplier<? extends NoopLifecycle.Implementation> defaultCreator() { return NoopLifecycle.Implementation::new; }
	}
	
	/**
	 * Default logger to the console.
	 * This is used when the actual logger is not (yet/longer) available
	 * @hidden
	 */
	@Internal
	public static final Lifecycle NOOP = Manager.set(Lifecycle.class, Factory.add(new NoopLifecycle()).build().name("Noop Lifecycle"));
}
