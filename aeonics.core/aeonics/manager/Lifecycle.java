package aeonics.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import aeonics.Plugin;
import aeonics.entity.Origin;
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
 * 		<li>{@link Phase#BOOT}: this is the initial state and it does not raise any events.</li>
 * 		<li>{@link Phase#LOAD}: when all plugins have been preloaded using {@link Plugin#start()}, the load phase begins.
 * 			<ol>
 * 				<li><b>before:</b> in this stage, you may perform some of your own initialization logic but it should not rely on any
 * 					other plugin, managers, registry or factory.</li>
 * 				<li><b>during:</b> the {@link Lifecycle}, {@link Executor} and {@link Config} managers are available but not populated yet. At this stage,
 * 					you can register {@link Factory} items and declare {@link Config} parameters.</li>
 * 				<li><b>after:</b> this step is the last moment to register an initial {@link Snapshot#onRestore(aeonics.util.Functions.Consumer)} 
 * 					handler. All remaining managers are available but not fully populated yet.</li>
 * 			</ol>
 * 		</li>
 * 		<li>{@link Phase#CONFIG}: when managers and entity types are registered, the initial configuration loading begins.
 * 			<ol>
 * 				<li><b>before:</b> in this stage, the initial snapshot restore happens. It means that the default config and registry is
 * 					being populated now and will only be available in the next stage. Although, you may perform some restore and initialization,
 * 					or defer it for the next phase by keeping a reference to the restore data.</li>
 * 				<li><b>during:</b> The config manager is now fully populated thanks to the snapshot restore operation. The registry is also
 * 					populated with restored entities and you can also register your own entities.</li>
 * 				<li><b>after:</b> if you need to act on existing registry items, it may be a convenient time to do so.</li>
 * 			</ol>
 * 		</li>
 * 		<li>{@link Phase#RUN}: everything is setup and populated, so the system is entering the run phase.
 * 			<ol>
 * 				<li><b>before:</b> in this stage, the {@link Logger} manager is started instead of simple stdout. In case there is no security provider
 * 					defined from the previous stage, a default one is created with basic permissive security rules. 
 * 					All the managers that have active components are starting in this phase.</li>
 * 				<li><b>during:</b> in this stage, you can start your own entities, use the network and executor managers.
 * 					All {@link Origin} entities will be started automatically in this phase.</li>
 * 				<li><b>after:</b> this is the last stage for the normal startup sequence. </li>
 * 			</ol>
 * 		</li>
 * 		<li>{@link Phase#SHUTDOWN}: when the system is requested to stop, the shutdown phase begins.
 * 			<ol>
 * 				<li><b>before:</b> you may handle some business logic before other elements apply their shutdown sequence.</li>
 * 				<li><b>during:</b> all executor tasks are being terminated, the network is closing and everything gets ready for shutdown.</li>
 * 				<li><b>after:</b> in this stage, you shall not use any managers, registry or factory anymore.
 * 					The logger is falling back to stdout and all remaining {@link Origin} entities are stopped.</li>
 * 			</ol>
 * 		</li>
 * </ol>
 */
public abstract class Lifecycle extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Lifecycle.class; }
	
	/**
	 * Returns the current active instance of this manager type.
	 * @return the current active instance of this manager type
	 */
	public static Lifecycle get() { return Manager.of(Lifecycle.class); }
	
	/**
	 * The different application phases.
	 */
	public enum Phase
	{
		/**
		 * When the system is booting and has not started loading yet
		 */
		BOOT,
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
	 * Returns the current lifecycle phase
	 * @return the current lifecycle phase
	 */
	public abstract Phase phase();
	
	/**
	 * Initiates the boot sequence.
	 * This method <b>must</b> return only after the {@link Phase#SHUTDOWN} is complete.
	 */
	@Internal
	public abstract void boot();
	
	/**
	 * The before callbacks
	 */
	private static Map<Phase, Callback<Phase, Lifecycle>> before = new ConcurrentHashMap<>();
	
	/**
	 * Registers a handler to run before other handlers in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public static void before(Phase phase, Once<Phase, Lifecycle> handler) 
	{
		synchronized(phase) { before.computeIfAbsent(phase, (p) -> new Callback<Phase, Lifecycle>(() -> Manager.of(Lifecycle.class))).then(handler); }
	}
	
	/**
	 * Registers a handler to run before other handlers in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public static void before(Phase phase, aeonics.util.Functions.Runnable handler) 
	{
		before(phase, Callback.once(handler));
	}
	
	/**
	 * Returns the global callback for the given phase
	 * @param phase the phase
	 * @return the matching callback
	 */
	protected static Callback<Phase, Lifecycle> before(Phase phase) { return before.getOrDefault(phase, new Callback<Phase, Lifecycle>(() -> Manager.of(Lifecycle.class))); }

	/**
	 * The on callbacks
	 */
	private static Map<Phase, Callback<Phase, Lifecycle>> on = new ConcurrentHashMap<>();
	
	/**
	 * Registers a handler to run in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public static void on(Phase phase, Once<Phase, Lifecycle> handler) 
	{
		synchronized(phase) { on.computeIfAbsent(phase, (p) -> new Callback<Phase, Lifecycle>(() -> Manager.of(Lifecycle.class))).then(handler); }
	}
	
	/**
	 * Registers a handler to run in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public static void on(Phase phase, aeonics.util.Functions.Runnable handler) 
	{
		on(phase, Callback.once(handler));
	}
	
	/**
	 * Returns the global callback for the given phase
	 * @param phase the phase
	 * @return the matching callback
	 */
	protected static Callback<Phase, Lifecycle> on(Phase phase) { return on.getOrDefault(phase, new Callback<Phase, Lifecycle>(() -> Manager.of(Lifecycle.class))); }

	/**
	 * The after callbacks
	 */
	private static Map<Phase, Callback<Phase, Lifecycle>> after = new ConcurrentHashMap<>();
	
	/**
	 * Registers a handler to run after other handlers in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public static void after(Phase phase, Once<Phase, Lifecycle> handler)
	{
		synchronized(phase) { after.computeIfAbsent(phase, (p) -> new Callback<Phase, Lifecycle>(() -> Manager.of(Lifecycle.class))).then(handler); }
	}
	
	/**
	 * Registers a handler to run after other handlers in the specified phase.
	 * All handlers are global and shared for all instances of the Lifecycle manager.
	 * @param phase the application phase
	 * @param handler the handler to run
	 */
	public static void after(Phase phase, aeonics.util.Functions.Runnable handler)
	{
		after(phase, Callback.once(handler));
	}
	
	/**
	 * Returns the global callback for the given phase.
	 * @param phase the phase
	 * @return the matching callback
	 */
	protected static Callback<Phase, Lifecycle> after(Phase phase) { return after.getOrDefault(phase, new Callback<Phase, Lifecycle>(() -> Manager.of(Lifecycle.class))); }
	
	/**
	 * Default initial lifecycle template and entity implementation
	 */
	private static final class NoopLifecycle extends Manager<Lifecycle>
	{
		private static class Implementation extends Lifecycle
		{
			@Override
			public void boot() { throw new IllegalStateException("This Lifecycle implementation is a shim. You should use a real implementation instead."); }
			public Phase phase() { return Phase.BOOT; }
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
	 * Default inert lifecycle.
	 * This is used when the actual lifecycle implementation is not yet available
	 * @hidden
	 */
	@Internal
	public static final Lifecycle NOOP = Manager.set(Lifecycle.class, Factory.add(new NoopLifecycle()).create().name("Noop Lifecycle"));
}
