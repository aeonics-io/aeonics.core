package aeonics.util;

import aeonics.data.Data;

/**
 * Make a class representable in a JSON-like {@link Data} structure.
 * Although nothing prevents complex objects to be included in the export, it is recommended to only include scalar / list / map values.
 * 
 * <p>The intent of this interface is to present instances of classes, objects or data to the user.
 * This means that the {@link #export()} method should not include private or confidential data.
 * Be careful that sometimes, a superclass could include such information by default, it is thus necessary
 * to override the parent implementation with your own.</p>
 */
public interface Exportable
{
	/**
	 * Renders this class instance to a simple data structure for rendering client-side.
	 * @return a public data representation of this class instance
	 */
	public Data export();
}
