package aeonics.manager;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import aeonics.entity.Entity;
import aeonics.template.Item;
import aeonics.util.StringUtils;

/**
 * This class acts as a central registry for entities that must have only one authoritative instance.
 * It is used typically for application-wide cross cutting concerns.
 * <p>
 * As an entity, it represents a class that can only exists once in the system.
 * All Manager instances must extend this class and should be registered using {@link #set(Class, Type)}.
 * </p>
 * <p>
 * All Manager Type instances are considered internal and will never be affected by a snapshot or restore operation.
 * This means that managers must not have own instance parameters. If needed, use config parameters instead.
 * </p>
 * @param <T> The manager entity type
 */
public abstract class Manager<T extends Manager.Type> extends Item<T>
{
	/**
	 * The internal instance registry
	 */
	private static ConcurrentHashMap<String, Manager.Type> instances = new ConcurrentHashMap<String, Manager.Type>();
	
	/**
	 * Returns the instance of the specified manageable type
	 * @param <T> the manageable type
	 * @param type the manager type
	 * @return the instance of the specified entity type, or null if none was registered
	 */
	public static <T extends Manager.Type> T of(Class<T> type) { return type.cast(instances.get(StringUtils.toLowerCase(type))); }
	
	/**
	 * Sets the instance of the specified manager type. The instance must not be null.
	 * @param <T> the manageable instance type
	 * @param type the manageable type
	 * @param instance the instance
	 * @return the instance that has been set
	 * @throws RuntimeException if another instance is already registered for this entity type
	 * @throws IllegalArgumentException if the instance is not an instance of the specified entity type
	 */
	public static <T extends Manager.Type> T set(Class<T> type, T instance)
	{
		if( !type.isInstance(instance) ) throw new IllegalArgumentException("Invalid instance type");
		synchronized(instances)
		{
			String t = StringUtils.toLowerCase(type);
			if( instances.containsKey(t) ) throw new RuntimeException("Instance is already set");
			instances.put(t, instance);
			
			Objects.requireNonNullElse(Manager.of(Logger.class), Logger.CONSOLE)
				.config(Manager.class, "Manager of {} has been set to {} from module {}", t, instance.getClass().getName(), instance.getClass().getModule().getName());
			return instance;
		}
	}
	
	/**
	 * Removes the instance of a manageable type
	 * @param <T> the manageable type
	 * @param type the manageable type
	 */
	public static <T extends Manager.Type> void remove(Class<T> type)
	{
		synchronized(instances)
		{
			String t = StringUtils.toLowerCase(type);
			Manager.Type previous = instances.remove(t);
			if( previous != null )
				Objects.requireNonNullElse(Manager.of(Logger.class), Logger.CONSOLE)
					.config(Manager.class, "Manager of {} has been removed", t);
		}
	}
	
	/**
	 * Replaces the instance of the specified entity type. The instance must not be null.
	 * @param <T> the manageable instance type
	 * @param type the entity type
	 * @param instance the instance
	 * @return the instance that has been set
	 * @throws IllegalArgumentException if the instance is not an instance of the specified entity type
	 */
	public static <T extends Manager.Type> T replace(Class<T> type, T instance)
	{
		if( !type.isInstance(instance) ) throw new IllegalArgumentException("Invalid instance type");
		synchronized(instances)
		{
			String t = StringUtils.toLowerCase(type);
			Manager.Type previous = instances.put(t, instance);
			if( previous != null )
				Objects.requireNonNullElse(Manager.of(Logger.class), Logger.CONSOLE)
					.config(Manager.class, "Manager of {} has been replaced by {} from module {}", t, instance.getClass().getName(), instance.getClass().getModule().getName());
			else
				Objects.requireNonNullElse(Manager.of(Logger.class), Logger.CONSOLE)
					.config(Manager.class, "Manager of {} has been set to {} from module {}", t, instance.getClass().getName(), instance.getClass().getModule().getName());
			return instance;
		}
	}
	
	/**
	 * Returns an iterator over all managers
	 * @return a manager iterator
	 */
	public static Iterable<Manager.Type> all()
	{
		return Collections.unmodifiableCollection(instances.values());
	}
	
	/**
	 * Basic manager entity superclass
	 */
	public abstract static class Type extends Entity
	{
		/**
		 * Hardcoded interal value to <code>true</code>
		 */
		@Override
		public boolean internal() { return true; }
		
		/**
		 * Hardcoded category to the {@link Manager} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Manager.class); }
		
		/**
		 * Returns the type of manager that shall be used in {@link Manager#of(Class)} and to which this manager belongs.
		 * @return the type of manager
		 */
		public abstract Class<? extends Manager.Type> manager();
	}
	
	@SuppressWarnings("unchecked")
	protected Class<? extends Manager<T>> category() { return (Class<? extends Manager<T>>) (Class<?>) Manager.class; }
}
