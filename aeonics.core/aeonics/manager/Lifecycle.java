package aeonics.manager;

import aeonics.Plugin;
import aeonics.entity.Registry;
import aeonics.template.Factory;
import aeonics.util.Callback;
import aeonics.util.Internal;

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
	 * Registers a handler to run before other handlers in the specified phase.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public abstract void before(Phase phase, Callback.Once<Void> handler);
	
	/**
	 * Registers a handler to run in the specified phase.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public abstract void on(Phase phase, Callback.Once<Void> handler);
	
	/**
	 * Registers a handler to run after other handlers in the specified phase.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public abstract void after(Phase phase, Callback.Once<Void> handler);
}
