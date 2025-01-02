package aeonics.util;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.manager.Snapshot;
import aeonics.template.Factory;
import aeonics.template.Template;

/**
 * Make an {@link Entity} serializable to a JSON-like {@link Data} structure to be used in the snapshot/restore mechanism.
 * This mechanism allows an {@link Entity} to be backed up as a stale data structure and restored at a later time.
 *
 * <p>Although complex objects can technically be included in the snapshot, it is recommended to use simple scalar, list, or map values
 * to maintain portability and ensure compatibility with various snapshot consumers.</p>
 *
 * <p>The privacy and security of the snapshot data rely on the implementation of the {@link Snapshot} manager, which controls access to snapshots.
 * Sensitive private data may be included in snapshots, so careful consideration should be given to data encryption or restricted access in sensitive contexts.</p>
 *
 * <p>The snapshot and restore behavior should typically be orchestrated by the {@link Snapshot} manager using either
 * {@link Factory#create(Data)} for full restoration or {@link Template#update(Data, Entity)} for incremental updates.</p>
 */
public interface Snapshotable 
{
	/**
	 * Serializes this class instance into a simple data structure for use in snapshots.
	 * If subclassing, ensure that data from the superclass is included in the resulting {@link Data}.
	 *
	 * @return a snapshot data representation of this class instance
	 */
	public Data snapshot();
	
	/**
     * Returns the intended snapshot mode suitable for the target entity.
     *
     * @return the desired snapshot mode
     */
	public SnapshotMode snapshotMode();
	
	/**
	 * Defines the behavior of entities during snapshot and restore operations.
	 */
	public enum SnapshotMode
	{
	    /**
	     * The entity will be fully serialized and restored using {@link Template#create(Data)}.
	     * Suitable for entities that need complete reinitialization during restore.
	     */
	    FULL,

	    /**
	     * The entity will be excluded from snapshots entirely and will not be restored.
	     * Suitable for transient or runtime-only entities.
	     */
	    NONE,

	    /**
	     * The entity will be serialized and updated using {@link Template#update(Data, Entity)}.
	     * Suitable for entities that should retain their existing identity but need updated state.
	     */
	    UPDATE
	}
}
