package aeonics.util;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.manager.Snapshot;
import aeonics.template.Factory;
import aeonics.template.Template;

/**
 * Make a class serializable to a JSON-like {@link Data} structure to be used in the snapshot / restore mechanism.
 * Although nothing prevents complex objects to be included in the export, it is recommended to only include scalar / list / map values.
 * 
 * <p>The intent of this interface is to backup an {@link Entity} to stale data and be able to restore it later.
 * To that extent, it is undoubtedly a form of serialization into a human-readable format.</p>
 * 
 * <p>Undernormal circumstances, the shapshot and restore behavior should be orchestrated by the {@link Snapshot} manager.
 * The restore mechanism shall reuse the {@link Factory#build(Data)} method. This means that the shapshot data must
 * comply with the {@link Template#create(Data)} entity creation mechanism.</p>
 * 
 * <p>You <b>may</b> include private data in the snapshot form as it is not intended to be visible by the user.
 * The privacy and security of the snapshot data depends on the implementation of the snapshot manager.</p>
 */
public interface Snapshotable 
{
	/**
	 * Renders this class instance to a simple data structure for snapshot.
	 * It is recommended to call the superclass implementation and append to it if necessary.
	 * @return a snapshot data representation of this class instance
	 */
	public Data snapshot();
}
