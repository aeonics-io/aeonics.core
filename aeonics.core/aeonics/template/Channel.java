package aeonics.template;

import aeonics.entity.Step.Action;
import aeonics.entity.Step.Destination;
import aeonics.entity.Step.Origin;
import aeonics.util.Documented;

/**
 * This class represents a channel for an {@link Origin}, {@link Action} or {@link Destination} entity. 
 * It provides a hint about possible variants of the action.
 * It is used by the corresponding {@link Template} to create and populate new instances of the action.
 */
@SuppressWarnings("unchecked")
public class Channel implements Documented
{
	/**
	 * Creates a new channel
	 * @param name the channel name
	 * @throws IllegalArgumentException if the name is null or blank.
	 */
	public Channel(String name)
	{
		if( name == null || name.isBlank() ) throw new IllegalArgumentException("Channel name is mandatory");
		this.name = name;
	}
	
	/**
	 * The channel name
	 */
	private String name;
	/**
	 * Returns the channel name
	 * @return the channel name
	 */
	public String name() { return name; }
	
	/**
	 * The channel summary
	 */
	private String summary = "";
	/**
	 * Returns the channel summary
	 * @return the channel summary
	 */
	public String summary() { return summary; }
	/**
	 * Sets the channel summary
	 * @param <C> this channel type
	 * @param value the summary
	 * @return this
	 */
	public <C extends Channel> C summary(String value) { summary = value; return (C)this; }
	
	/**
	 * The channel description
	 */
	private String description = "";
	/**
	 * Returns the channel description
	 * @return the channel description
	 */
	public String description() { return description; }
	/**
	 * Sets the channel description
	 * @param <C> this channel type
	 * @param value the description
	 * @return this
	 */
	public <C extends Channel> C description(String value) { description = value; return (C)this; }
}
