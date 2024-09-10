package aeonics.template;

import java.util.HashMap;
import java.util.Map;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.util.Documented;
import aeonics.util.StringUtils;

/**
 * This class defines the type of relationship an {@link Entity} may have with other entities.
 * It is used by the corresponding {@link Template} to create and populate new instances of the entity.
 */
@SuppressWarnings("unchecked")
public class Relationship implements Documented
{
	/**
	 * Create a new relationship
	 * @param name the relationship name
	 * @throws IllegalArgumentException if the name is null or blank
	 */
	public Relationship(String name)
	{
		if( name == null || name.isBlank() ) throw new IllegalArgumentException("Relationship name is mandatory");
		this.name = name;
		
		add(new Parameter("id")
			.summary("The related entity id.")
			.description("The related entity id. The related entity will be fetched from the Registry when needed.")
			.optional(false)
			.format(Parameter.Format.TEXT));
	}
	
	/**
	 * The relationship name
	 */
	private String name;
	/**
	 * Returns the relationship name
	 * @return the relationship name
	 */
	public String name() { return name; }
	
	/**
	 * The relationshup summary
	 */
	private String summary = "";
	/**
	 * Returns the relationship summary
	 * @return the relationship summary
	 */
	public String summary() { return summary; }
	/**
	 * Sets the relationship summary
	 * @param <R> the relationship type
	 * @param value the summary
	 * @return this
	 */
	public <R extends Relationship> R summary(String value) { summary = value; return (R)this; }
	
	/**
	 * The relationship description
	 */
	private String description = "";
	/**
	 * Returns the relationship description
	 * @return the relationship description
	 */
	public String description() { return description; }
	/**
	 * Sets the relationship description
	 * @param <R> the relationship type
	 * @param value the description
	 * @return this
	 */
	public <R extends Relationship> R description(String value) { description = value; return (R)this; }
	
	/**
	 * The target entity category to relate to
	 */
	private String category = null;
	/**
	 * Returns the target entity category to relate to
	 * @return the target entity type to relate to
	 */
	public String category() { return category; }
	/**
	 * Sets the target entity category to relate to
	 * @param <R> the relationship type
	 * @param value the target entity category to relate to
	 * @return this
	 */
	public <R extends Relationship> R category(Class<? extends Item<? extends Entity>> value) { category = StringUtils.toLowerCase(value); return (R)this; }
	
	/**
	 * Sets the target entity category to relate to
	 * @param <R> the relationship type
	 * @param value the target entity category to relate to
	 * @return this
	 */
	public <R extends Relationship> R category(String value) { category = value; return (R)this; }
	
	/**
	 * The minimum number of relations
	 */
	private int min = 0;
	/**
	 * Returns the minimum number of relations
	 * @return the minimum number of relations
	 */
	public int min() { return min; }
	/**
	 * Sets the minimum number of relations
	 * @param <R> the relationship type
	 * @param value the minimum number of relations
	 * @return this
	 */
	public <R extends Relationship> R min(int value) { min = value; return (R)this; }
	
	/**
	 * The maximum number of relations
	 */
	private int max = -1;
	/**
	 * Returns the maximum number of relations.
	 * If negative, it means that the maximum is not set.
	 * @return the maximum number of relations
	 */
	public int max() { return max; }
	/**
	 * Sets the maximum number of relations.
	 * @param <R> the relationship type
	 * @param value the maximum number of relations (-1 if undefined)
	 * @return this
	 */
	public <R extends Relationship> R max(int value) { max = value; return (R)this; }
	
	/**
	 * Whether or not the order of multiple values should be perserved
	 */
	private boolean ordered = false;
	/**
	 * Returns whether or not the order of multiple values should be perserved
	 * @return whether or not the order of multiple values should be perserved
	 */
	public boolean ordered() { return ordered; }
	/**
	 * Sets whether or not the order of multiple values should be perserved
	 * @param <R> the relationship type
	 * @param value whether or not the order of multiple values should be perserved
	 * @return this
	 */
	public <R extends Relationship> R ordered(boolean value) { ordered = value; return (R)this; }
	
	/**
	 * The list of parameters
	 */
	private Map<String, Parameter> parameters = new HashMap<>();
	/**
	 * Returns the list of parameters for this relationship
	 * @return the list of parameters for this relationship
	 */
	public Map<String, Parameter> parameters() { return parameters; }
	/**
	 * Adds a parameter linked to this relationship
	 * @param parameter the parameter definition
	 * @param <R> the relationship type
	 * @return this
	 */
	public <R extends Relationship> R add(Parameter parameter) { parameters.put(parameter.name(), parameter); return (R)this; }

	@Override
	public Data export()
	{
		Data p = Data.map();
		for( Parameter x : parameters().values() ) p.put(x.name(), x.export());
		
		return Documented.super.export()
			.put("min", min())
			.put("max", max())
			.put("ordered", ordered())
			.put("category", StringUtils.toLowerCase(category()))
			.put("parameters", p);
	}
}
