package aeonics.entity;

import java.io.Closeable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import aeonics.data.Data;
import aeonics.template.Item;
import aeonics.util.Callback;
import aeonics.util.Exportable;
import aeonics.util.StringUtils;

/**
 * The registry contains the list of all {@link Entity} that have been populated based on user input.
 * The registry is organised by {@link Entity#category()}.
 * @param <T> the entity category
 */
@SuppressWarnings("unchecked")
public class Registry<T extends Entity> implements Iterable<T>, Exportable
{
	/**
	 * Holds the different registry categories.
	 */
	private static Map<String, Registry<? extends Entity>> registries = new ConcurrentHashMap<>();
	
	/**
	 * Fetches the registry of the given entity category.
	 * This is the same as calling <code>of(StringUtils.toLowerCase(category))</code>
	 * @param <U> The entity category
	 * @param category The entity category
	 * @return The registry containing all {@link Entity} for the specified category
	 */
	public static <U extends Entity> Registry<U> of(Class<? extends Item<U>> category) { return of(StringUtils.toLowerCase(category)); }
	
	/**
	 * Fetches the registry of the given entity category.
	 * @param <U> The entity category
	 * @param type The entity category (should be or will be converted to lower case)
	 * @return The registry containing all {@link Entity} for the specified category
	 */
	public static <U extends Entity> Registry<U> of(String type)
	{
		type = StringUtils.toLowerCase(type);
		registries.computeIfAbsent(type, (t) -> { return new Registry<>(t); });
		return (Registry<U>) registries.get(type);
	}
	
	/**
	 * Checks if the registry of the given entity category exists.
	 * @param type The entity category (should be or will be converted to lower case)
	 * @return true if the registry of the given entity category exists
	 */
	public static boolean has(String type)
	{
		type = StringUtils.toLowerCase(type);
		return registries.containsKey(type);
	}
	
	/**
	 * Adds the specified entity to its matching registry {@link Entity#category()}.
	 * @param <U> the entity category
	 * @param entity the entity
	 * @return the entity
	 */
	public static <U extends Entity> U add(U entity)
	{
		if( entity == null ) return null;
		return Registry.of(entity.category()).put(entity);
	}
	
	/**
	 * Returns an iterator over all registries
	 * @return a registry iterator
	 */
	public static Iterable<Registry<? extends Entity>> all()
	{
		return Collections.unmodifiableCollection(registries.values());
	}
	
	// ===============================
	
	/**
	 * The onAdd event callback
	 */
	private Callback<Entity> onAdd = new Callback<>();
	
	/**
	 * Event callback called every time an entity is added to this registry.
	 * You should {@link Callback#then(aeonics.util.Functions.Consumer)} this event handler to subscribe to events.
	 * @return the onAdd event handler
	 */
	public Callback<Entity> onAdd() { return onAdd; }
	
	/**
	 * The onRemove event callback
	 */
	private Callback<Entity> onRemove = new Callback<>();
	
	/**
	 * Event callback called every time an entity is removed from this registry.
	 * You should {@link Callback#then(aeonics.util.Functions.Consumer)} this event handler to subscribe to events.
	 * @return the onRemove event handler
	 */
	public Callback<Entity> onRemove() { return onRemove; }
	
	/**
	 * private constructor
	 * @param category the registry category
	 */
	private Registry(String category) { this.category = StringUtils.toLowerCase(category); }
	
	/**
	 * This registry's category
	 */
	private String category = null;
	/**
	 * Returns the registry category.
	 * @return the registry category
	 */
	public String category() { return category; }
	
	/**
	 * Holds all the entities in this registry category.
	 */
	private Map<String, T> entities = new ConcurrentHashMap<>();
	
	/**
	 * Checks whether the specified entity exists in this registry.
	 * The id is checked first and is a very cheap lookup. If nothing matches, all entities are checked to find the first `name` property that matches.
	 * @param id the entity id
	 * @return true if the entity is found, false otherwise
	 */
	public boolean contains(String id)
	{
		if( id == null || id.isBlank() ) return false;
		if( entities.containsKey(id) ) return true;
		
		// search by name : much slower
		for( T e : entities.values() )
			if( e != null && id.equals(e.name()) )
				return true;
		return false;
	}
	
	/**
	 * Fetches the entity based on its id or `name` property.
	 * The id is checked first and is a very cheap lookup. If nothing matches, all entities are checked to find the first `name` property that matches.
	 * @param <U> the entity type
	 * @param id the entity id
	 * @return the matching entity or null if no such entity is found
	 */
	public <U extends T> U get(String id)
	{
		if( id == null || id.isBlank() ) return null;
		T entity = entities.get(id);
		if( entity != null ) return (U) entity;
		
		// search by name : much slower
		for( T e : entities.values() )
			if( e != null && id.equals(e.name()) )
				return (U) e;
		return null;
	}
	
	/**
	 * Fetches the first entity that matches the specified criteria.
	 * @param <U> the entity type
	 * @param comparator the matching criteria
	 * @return the matching entity or null if no such entity is found
	 */
	public <U extends T> U get(Predicate<T> comparator)
	{
		if( comparator == null ) return null;
		for( T entity : entities.values() )
			if( entity != null && comparator.test(entity) )
				return (U) entity;
		return null;
	}
	
	/**
	 * Adds the specified entity into this registry. The {@link Entity#category()} should match this registry's category.
	 * @param <U> the entity type
	 * @param entity the entity
	 * @return the entity
	 * @throws IllegalArgumentException if the entity is null or if the category does not match
	 */
	public <U extends T> U put(U entity)
	{
		if( entity == null ) throw new IllegalArgumentException("Cannot register a null entity");
		if( !entity.category().equals(category()) ) throw new IllegalArgumentException("Entity category mismatch");
		entities.put(entity.id(), entity);
		onAdd().trigger(entity);
		return entity;
	}
	
	/**
	 * Removes the specified entity.
	 * The id is checked first and is a very cheap lookup. If nothing matches, all entities are checked to find the first `name` property that matches.
	 * If the entity is {@link Closeable}, it is closed.
	 * @param <U> the entity type
	 * @param id the entity id
	 * @return the removed entity or null if no entity was found
	 */
	public <U extends T> U remove(String id)
	{
		if( id == null || id.isBlank() ) return null;
		T entity = entities.remove(id);
		if( entity != null )
		{
			onRemove().trigger(entity);
			return closeIfCloseable((U) entity);
		}
		
		// search by name : much slower
		for( T e : entities.values() )
		{
			if( e != null && id.equals(e.name()) )
			{
				entities.remove(e.id());
				onRemove().trigger(e);
				return closeIfCloseable((U) e);
			}
		}
		return null;
	}
	
	/**
	 * Removes the first entity matching the specified criteria.
	 * If the entity is {@link Closeable}, it is closed.
	 * @param <U> the entity type
	 * @param comparator the matching criteria
	 * @return the removed entity or null if no entity was found
	 */
	public <U extends T> U remove(Predicate<T> comparator)
	{
		if( comparator == null ) return null;
		for( T entity : entities.values() )
		{
			if( entity != null && comparator.test(entity) )
			{
				entities.remove(entity.id());
				onRemove().trigger(entity);
				return closeIfCloseable((U) entity);
			}
		}
		return null;
	}
	
	/**
	 * Removes all entities from this registry.
	 * If the entity is {@link Closeable}, it is closed. 
	 */
	public void clear() { clear((e) -> true); }
	
	/**
	 * Removes all entities from this registry matching the specified criteria.
	 * If the entity is {@link Closeable}, it is closed.
	 * @param comparator the matching criteria
	 */
	public void clear(Predicate<T> comparator)
	{
		if( comparator == null ) return;
		Iterator<T> values = entities.values().iterator();
		while( values.hasNext() )
		{
			T entity = values.next();
			if( entity == null ) continue;
			if( comparator.test(entity) )
			{
				values.remove();
				onRemove().trigger(entity);
				closeIfCloseable(entity);
			}
		}
	}
	
	/**
	 * Closes a {@link Closeable} entity
	 * @param <U> the entity type
	 * @param entity the entity
	 * @return the entity
	 */
	private <U extends T> U closeIfCloseable(U entity)
	{
		if( entity instanceof Closeable )
		{
			try { ((Closeable) entity).close(); }
			catch(Exception e) { /* ignore */ }
		}
		return entity;
	}
	
	/**
	 * Returns an iterator over all entities registered in this registry
	 * @return an entity iterator
	 */
	public Iterator<T> iterator()
	{
		return entities.values().iterator();
	}
	
	public Data export()
	{
		Data d = Data.list();
		for( Entity e : entities.values() )
			d.add(e.export());
		return d;
	}
}
