package aeonics.util;

import aeonics.data.Data;

/**
 * Make a class serializable to a JSON-like {@link Data} structure.
 * Although nothing prevents complex objects to be included in the export, it is recommended to only include scalar values.
 */
public interface Exportable
{
	/**
	 * Renders this class instance to a simple data structure for serialization.
	 * @return a data representation of this class instance
	 */
	public Data export();
}
