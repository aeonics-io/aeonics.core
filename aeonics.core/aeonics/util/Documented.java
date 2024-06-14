package aeonics.util;

import aeonics.data.Data;

/**
 * Provides basic documentation about a class instance so that it can be exposed to the end user.
 * Documented classes must also be {@link Exportable} to provide a representation of the instance to the user.
 */
public interface Documented extends Exportable
{
	/**
	 * Returns the friendly name of this instance. Ideally this should be unique.
	 * @return the instance name
	 */
	public String name();
	
	/**
	 * Returns the short summary about this class instance
	 * @return the summary text
	 */
	public String summary();
	
	/**
	 * Returns a longer description of this class instance.
	 * Per convention, the description may contain markdown to organize the content in a structured way for the user.
	 * <p>See: <a href="https://en.wikipedia.org/wiki/Markdown">https://en.wikipedia.org/wiki/Markdown</a> and 
	 * <a href="https://www.markdownguide.org/">https://www.markdownguide.org/</a></p>  
	 * @return the description markdown text
	 */
	public String description();
	
	public default Data export()
	{
		return Data.map()
			.put("name", name())
			.put("summary", summary())
			.put("description", description())
			.put("__class", getClass().getName())
			.put("__plugin", getClass().getModule().getName());
	}
}
