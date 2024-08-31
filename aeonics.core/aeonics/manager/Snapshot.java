package aeonics.manager;

import java.util.Collection;

import aeonics.data.Data;
import aeonics.manager.Executor.Task;
import aeonics.util.Callback;
import aeonics.util.Functions.Consumer;

/**
 * Manages restore points of the system.
 * This manager is responsible to create snapshots and to restore them.
 * Entities or Plugins that want to react to such events may add a callback handler to be notified when such events happen.
 */
public abstract class Snapshot extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Snapshot.class; }
	
	/**
	 * The snapshot create callback
	 */
	protected static Callback<Data, Snapshot> createCallback = new Callback<>(() -> Manager.of(Snapshot.class));
	
	/**
	 * Adds an event handler to react to the current snapshot phase.
	 * Implementations may store elements in the provided data object that will be persisted at the end of the snapshot phase.
	 * For isolation purposes, a new empty data object will be provided per plugin (java module scope).
	 * @param handler the snapshot handler
	 */
	public static void onSnapshot(Consumer<Data> handler) { createCallback.then((data, self) -> handler.accept(data)); }
	
	/**
	 * The snapshot restore callback
	 */
	protected static Callback<Data, Snapshot> restoreCallback = new Callback<>(() -> Manager.of(Snapshot.class));
	
	/**
	 * Adds an event handler to react to the current restore phase.
	 * Implementations may fetch elements from the provided data object.
	 * For isolation purposes, each plugin (java module scope) will be given its own data object.
	 * @param handler the restore handler
	 */
	public static void onRestore(Consumer<Data> handler) { restoreCallback.then((data, self) -> handler.accept(data)); }
	
	/**
	 * Creates a new snapshot using the specified suffix while the prefix is usually the date of the snapshot.
	 * 
	 * <p>Snapshots are always performed asynchronously and handlers are called sequentially.
	 * When all {@link #onSnapshot(Consumer)} handlers have executed, the returned task will complete with the final snapshot name.</p>
	 * 
	 * @param suffix the snapshot suffix (will be sanitized to contain 0 to 30 alpha numerical characters a-zA-Z0-9)
	 * @return the asynchronous snapshot task
	 */
	public abstract Task<String> create(String suffix);
	
	/**
	 * Checks if the specified snapshot exists and triggers the restore operation.
	 * 
	 * <p>Snapshots are always performed asynchronously and handlers are called sequentially.
	 * When all {@link #onRestore(Consumer)} handlers have executed, the returned task will complete.</p>
	 * 
	 * @param snapshot the snapshot name
	 * @return the asynchronous restore task
	 * @throws IllegalArgumentException if the specified snapshot does not exist
	 */
	public abstract Task<Void> restore(String snapshot);
	
	/**
	 * Lists all available snapshots
	 * @return the list of snapshots
	 */
	public abstract Collection<String> list();
	
	/**
	 * Permanently removes a snapshot
	 * @param snapshot the snapshot to remove
	 */
	public abstract void remove(String snapshot);
	
	/**
	 * Checks if a snapshot exists
	 * @param snapshot the snapshot to check
	 * @return true if the snapshot exists, false otherwise
	 */
	public abstract boolean exists(String snapshot);
	
	/**
	 * Gets the name of the latest snapshot or <code>null</code> if there are no snapshots.
	 * @return the the latest snapshot name
	 */
	public abstract String latest();
}
