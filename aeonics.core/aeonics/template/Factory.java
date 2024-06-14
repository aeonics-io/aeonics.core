package aeonics.template;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.util.StringUtils;

/**
 * The factory contains the list of all {@link Template} that can be used to create an {@link Entity} based on user input.
 * The templates are organized by {@link Template#category()}.
 * @param <T> the entity category
 */
@SuppressWarnings("unchecked")
public class Factory<T extends Entity> implements Iterable<Template<T>>
{
	/**
	 * Holds the different factory categories.
	 */
	private static Map<String, Factory<? extends Entity>> factories = new ConcurrentHashMap<>();
	
	/**
	 * Fetches the factory of the given entity category.
	 * This is the same as calling <code>of(StringUtils.toLowerCase(category))</code>
	 * @param <U> The entity category
	 * @param category The entity category
	 * @return The factory containing all {@link Template} for the specified category
	 */
	public static <U extends Entity> Factory<U> of(Class<? extends Item<? super U>> category) { return of(StringUtils.toLowerCase(category)); }
	
	/**
	 * Fetches the factory of the given entity category.
	 * @param <U> The entity category
	 * @param type The entity category (should be or will be converted to lower case)
	 * @return The factory containing all {@link Template} for the specified category
	 */
	public static <U extends Entity> Factory<U> of(String type)
	{
		type = StringUtils.toLowerCase(type);
		factories.computeIfAbsent(type, (t) -> new Factory<U>(t));
		return (Factory<U>) factories.get(type);
	}
	
	/**
	 * Fetches the template that can be used to create or modify the specified entity.
	 * @param <U> The entity category
	 * @param instance The entity category (should be or will be converted to lower case)
	 * @return The factory containing all {@link Template} for the specified category
	 */
	public static <U extends Entity> Template<U> of(Entity instance)
	{
		Factory<?> f = of(instance.category());
		return (Template<U>) f.get(instance.type());
	}
	
	/**
	 * Checks if the factory of the given entity category exists.
	 * @param type The entity category (should be or will be converted to lower case)
	 * @return true if the factory of the given entity category exists
	 */
	public static boolean has(String type)
	{
		type = StringUtils.toLowerCase(type);
		return factories.containsKey(type);
	}
	
	/**
	 * Adds the specified template to its matching factory {@link Template#category()}.
	 * @param <U> the entity category
	 * @param template the template
	 * @return the template
	 */
	public static <U extends Entity> Template<U> add(Template<U> template)
	{
		if( template == null ) return null;
		return Factory.of(template.category()).put(template);
	}
	
	/**
	 * Adds the specified template provider to its matching factory {@link Template#category()}.
	 * @see Template
	 * @param <U> the entity category
	 * @param templated the template provider
	 * @return the template
	 */
	public static <U extends Entity> Template<? extends U> add(Item<U> templated)
	{
		if( templated == null ) return null;
		return add((Template<U>) templated.template());
	}
	
	/**
	 * Builds an instance of the exported entity.
	 * @param <U> The entity type
	 * @param data the exported entity data
	 * @return the entity
	 * @see Entity#export()
	 * @throws RuntimeException if a matching template cannot be found
	 * @throws IllegalArgumentException if the provided data is invalid or corrupted
	 */
	public static <U extends Entity> U build(Data data)
	{
		if( data == null || data.isEmpty("__type") || data.isEmpty("__category") )
			throw new IllegalArgumentException("Invalid input data");
		Template<U> template = Factory.of(data.asString("__category")).get(data.asString("__type"));
		if( template == null )
			throw new RuntimeException("No template found for " + data.asString("__category") + "." + data.asString("__type"));
		return template.build(data);
	}
	
	/**
	 * Returns an iterator over all factories
	 * @return a factory iterator
	 */
	public static Iterable<Factory<?>> all()
	{
		return factories.values();
	}
	
	// ===============================
	
	/**
	 * private constructor
	 * @param category the factory category
	 */
	private Factory(String category) { this.category = StringUtils.toLowerCase(category); }
	
	/**
	 * This factory's category
	 */
	private String category = null;
	/**
	 * Returns the factory category.
	 * @return the factory category
	 */
	public String category() { return category; }
	
	/**
	 * Holds all the templates in this factory category.
	 */
	private ConcurrentHashMap<String, Template<T>> templates = new ConcurrentHashMap<>();
	
	/**
	 * Fetches the template for the specified entity item type.
	 * This is the same as calling <code>get(StringUtils.toLowerCase(type))</code>
	 * @param <U> the entity type
	 * @param <V> the entity item type
	 * @param type the entity item type
	 * @return the matching template or null if no such template is found
	 */
	public <U extends T, V extends Item<? super T>> Template<U> get(Class<V> type) { return get(StringUtils.toLowerCase(type)); }
	
	/**
	 * Fetches the template for the specified entity type.
	 * @param <U> the entity type
	 * @param type the entity type (should be or will be converted to lower case)
	 * @return the matching template or null if no such template is found
	 */
	public <U extends T> Template<U> get(String type)
	{
		return (Template<U>) templates.get(StringUtils.toLowerCase(type));
	}
	
	/**
	 * Returns true if a template is registered for the specified entity type.
	 * This is the same as calling <code>contains(StringUtils.toLowerCase(type))</code>
	 * @param <V> the entity item type
	 * @param type the entity item type
	 * @return true if a template is registered for the specified entity type
	 */
	public <V extends Item<? super T>> boolean contains(Class<V> type) { return contains(StringUtils.toLowerCase(type)); }
	
	/**
	 * Returns true if a template is registered for the specified entity type.
	 * @param type the entity type (should be or will be converted to lower case)
	 * @return true if a template is registered for the specified entity type
	 */
	public boolean contains(String type)
	{
		return templates.containsKey(StringUtils.toLowerCase(type));
	}
	
	/**
	 * Adds the specified template into this factory. The {@link Template#category()} should match this factory's category.
	 * @param <U> the entity type
	 * @param template the template
	 * @return the template
	 * @throws IllegalArgumentException if the template is null or if the category does not match
	 */
	public <U extends T> Template<U> put(Template<U> template)
	{
		if( template == null ) throw new IllegalArgumentException("Cannot register a null template");
		if( !template.category().equals(category()) ) throw new IllegalArgumentException("Template category mismatch");
		templates.put(StringUtils.toLowerCase(template.type()), (Template<T>) template);
		return template;
	}
	
	/**
	 * Removes the template for the specified entity item type.
	 * This is the same as calling <code>remove(StringUtils.toLowerCase(type))</code>
	 * @param <U> the entity type
	 * @param <V> the entity item type
	 * @param type the entity item type
	 * @return the removed template, or null if there was no template for the specified entity type
	 */
	public <U extends T, V extends Item<? super T>> Template<U> remove(Class<V> type) { return remove(StringUtils.toLowerCase(type)); }
	
	/**
	 * Removes the template for the specified entity type.
	 * The entity type should always be lower case.
	 * @param <U> the entity type
	 * @param type the entity type (should be or will be converted to lower case)
	 * @return the removed template, or null if there was no template for the specified entity type
	 */
	public <U extends T> Template<U> remove(String type)
	{
		return (Template<U>) templates.remove(StringUtils.toLowerCase(type));
	}
	
	/**
	 * Removes all templates from this factory.
	 */
	public void clear() { templates.clear(); }
	
	/**
	 * Returns an iterator over all templates registered in this factory
	 * @return a template iterator
	 */
	public Iterator<Template<T>> iterator()
	{
		return templates.values().iterator();
	}
}
