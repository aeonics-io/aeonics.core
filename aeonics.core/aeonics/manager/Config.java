package aeonics.manager;

import java.util.Map;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.template.Parameter;
import aeonics.util.Tuples.Tuple;
import aeonics.util.Functions.BiConsumer;

/**
 * Manages common configuration parameters for all entity instances.
 * The configuration parameters are typically grouped by entity type.
 * Entities have the possibility to {@link #watch(String, String, BiConsumer)} a specified value and
 * be notified when the value changes.
 * 
 * <p>The official parameter name is based on a category linked to a class type and a final friendly name.
 * The resulting string can be obrained from {@link #implodeName(Class, String)}.
 * The other way around, the couple of category/name can be retreived using {@link #explodeName(String)}.</p> 
 */
public abstract class Config extends Manager.Type
{
	/**
	 * Standard config name sanitization.
	 * Replaces all non alphanumeric characters with a "." (dot) character and converts to lower case, and skips all whitespace characters.
	 * @param name the input name
	 * @return the sanitized name
	 */
	public static final String sanitize(String name)
	{
		StringBuilder sb = new StringBuilder(name.length());
		for( int i = 0; i < name.length(); i++ )
		{
			char c = name.charAt(i);
			if (Character.isWhitespace(c))
				continue;
			else if( c >= 'a' && c <= 'z' || c >= '0' && c <= '9' )
				sb.append(c);
			else if( c >= 'A' && c <= 'Z' )
				sb.append((char)(c + 32)); // ASCII lowercase conversion
			else
				sb.append('.');
		}
		return sb.toString();
	}
	
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Config.class; }
	
	/**
	 * Returns the current active instance of this manager type.
	 * @return the current active instance of this manager type
	 */
	public static Config get() { return Manager.of(Config.class); }
	
	/**
	 * Returns the normalized imploded parameter name based on the type and friendly name.
	 * @see #explodeName(String)
	 * @see #sanitize(String)
	 * @param type the parameter type
	 * @param name the parameter friendly name
	 * @return the normalized parameter name
	 */
	public static String implodeName(Class<?> type, String name) { return implodeName(sanitize(type.getName()), name); }
	
	/**
	 * Returns the normalized imploded parameter name based on the type and friendly name.
	 * @see #explodeName(String)
	 * @see #sanitize(String)
	 * @param type the parameter type 
	 * @param name the parameter friendly name
	 * @return the normalized parameter name
	 */
	public static String implodeName(String type, String name) { return sanitize(type) + ":" + sanitize(name); }
	
	/**
	 * Returns the normalized imploded parameter name based on the normalized exploded name.
	 * @see #implodeName(Class, String)
	 * @param name the parameter friendly name
	 * @return the normalized parameter name
	 */
	public static String implodeName(Tuple<String, String> name) { return name.a + ":" + name.b; }
	
	/**
	 * Returns the normalized exploded parameter name based on the imploded name.
	 * @see #implodeName(Class, String)
	 * @param name the imploded parameter name
	 * @return the normalized parameter name
	 */
	public static Tuple<String, String> explodeName(String name)
	{
		int delimiter = name.indexOf(':');
		
		// there was no ':' so split based on the last '.' instead
		if( delimiter < 0 ) delimiter = name.lastIndexOf('.');
		
		// there was no '.'
		if( delimiter < 0 )
			return Tuple.of("system", sanitize(name));
		else
			return Tuple.of(sanitize(name.substring(0, delimiter)), sanitize(name.substring(delimiter+1)));
	}
	
	/**
	 * Declares a new common configuration parameter for the target entity type.
	 * Redeclaring an existing parameter does not change the current value, only the definition.
	 * @param type the entity type (will be sanitized).
	 * @param parameter the parameter definition
	 * @see #sanitize(String)
	 */
	public abstract void declare(String type, Parameter parameter);
	
	/**
	 * Declares a new common configuration parameter for the target entity type.
	 * Redeclaring an existing parameter does not change the current value, only the definition.
	 * @param type the entity type
	 * @param parameter the parameter definition
	 */
	public void declare(Class<?> type, Parameter parameter)
	{
		declare(type.getName(), parameter);
	}
	
	/**
	 * Returns the parameter definition for the target entity type.
	 * @param type the entity type (will be sanitized)
	 * @param parameter the parameter name (will be sanitized)
	 * @return the parameter definition or null if the parameter is not found
	 * @see #sanitize(String)
	 */
	public abstract Parameter definition(String type, String parameter);
	
	/**
	 * Returns the parameter definition for the target entity type.
	 * @param type the entity type (will be sanitized)
	 * @param parameter the parameter name (will be sanitized)
	 * @return the parameter definition or null if the parameter is not found
	 * @see #sanitize(String)
	 */
	public Parameter definition(Class<?> type, String parameter)
	{
		return definition(type.getName(), parameter);
	}
	
	/**
	 * Returns the parameter definition for the specified concatenated key.
	 * The key will be sanitized and split by '.': <code>Entity.Type.NAME</code> will be converted to the configuration 
	 * parameter <code>entity type &gt; name</code>
	 * @param key the parameter name (will be sanitized)
	 * @return the parameter definition or null if the parameter is not found
	 * @see #sanitize(String)
	 */
	public Parameter definition(String key)
	{
		if( key == null || key.isBlank() ) return null;
		
		Tuple<String, String> name = explodeName(key);
		
		return definition(name.a, name.b);
	}
	
	/**
	 * Fetches the configuration parameter value for the specified entity type and parameter name.
	 * @param type the entity type (will be sanitized)
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return the configuration parameter value or {@link Data#empty()} if there is no value
	 * @see #sanitize(String)
	 */
	public abstract Data get(String type, String name);
	
	/**
	 * Fetches the configuration parameter value for the specified entity type and parameter name.
	 * @param type the entity type
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return the configuration parameter value or {@link Data#empty()} if there is no value
	 * @see #sanitize(String)
	 */
	public Data get(Class<?> type, String name)
	{
		return get(type.getName(), name);
	}
	
	/**
	 * Fetches the configuration parameter value for the specified entity type and parameter name.
	 * @param entity the entity instance. Its {@link Entity#type()} method will be used.
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return the configuration parameter value or {@link Data#empty()} if there is no value
	 * @see #sanitize(String)
	 */
	public Data get(Entity entity, String name)
	{
		return get(entity.type(), name);
	}
	
	/**
	 * Fetches the configuration parameter value for the specified concatenated key.
	 * The key will be sanitized and split by '.': <code>Entity.Type.NAME</code> will be converted to the configuration 
	 * parameter <code>entity type &gt; name</code>
	 * @param key the parameter name (will be sanitized)
	 * @return the configuration parameter value or {@link Data#empty()} if there is no value
	 * @see #sanitize(String)
	 */
	public Data get(String key)
	{
		if( key == null || key.isBlank() ) return null;
		
		Tuple<String, String> name = explodeName(key);
		
		return get(name.a, name.b);
	}
	
	/**
	 * Checks if the configuration parameter value for the specified entity type and parameter name is set.
	 * @param type the entity type (will be sanitized)
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return true if the configuration parameter is set, regardless of its value
	 * @see #sanitize(String)
	 */
	public abstract boolean contains(String type, String name);
	
	/**
	 * Checks if the configuration parameter value for the specified entity type and parameter name is set.
	 * @param type the entity type
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return true if the configuration parameter is set, regardless of its value
	 * @see #sanitize(String)
	 */
	public boolean contains(Class<?> type, String name)
	{
		return contains(type.getName(), name);
	}
	
	/**
	 * Checks if the configuration parameter value for the specified entity type and parameter name is set.
	 * @param entity the entity instance. Its {@link Entity#type()} method will be used.
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return true if the configuration parameter is set, regardless of its value
	 * @see #sanitize(String)
	 */
	public boolean contains(Entity entity, String name)
	{
		return contains(entity.type(), name);
	}
	
	/**
	 * Checks if the configuration parameter value for the specified concatenated key is set.
	 * The key will be sanitized and split by '.': <code>Entity.Type.NAME</code> will be converted to the configuration 
	 * parameter <code>entity type &gt; name</code>
	 * @param key the parameter name (will be sanitized)
	 * @return true if the configuration parameter is set, regardless of its value
	 * @see #sanitize(String)
	 */
	public boolean contains(String key)
	{
		if( key == null || key.isBlank() ) return false;
		
		Tuple<String, String> name = explodeName(key);
		
		return contains(name.a, name.b);
	}
	
	/**
	 * Sets the value of the specified configuration parameter. 
	 * @param type the entity type (will be sanitized)
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @param value the new parameter value
	 * @return the previous value associated with that parameter if any
	 * @throws IllegalArgumentException if the value does not match the parameter requirements
	 * @see #sanitize(String)
	 */
	public abstract Data set(String type, String name, Object value);
	
	/**
	 * Sets the value of the specified configuration parameter. 
	 * @param type the entity type
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @param value the new parameter value
	 * @return the previous value associated with that parameter if any
	 * @throws IllegalArgumentException if the value does not match the parameter requirements
	 * @see #sanitize(String)
	 */
	public Data set(Class<?> type, String name, Object value)
	{
		return set(type.getName(), name, value);
	}
	
	/**
	 * Sets the value of the specified configuration parameter. 
	 * @param entity the entity instance. Its {@link Entity#type()} method will be used.
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @param value the new parameter value
	 * @return the previous value associated with that parameter if any
	 * @throws IllegalArgumentException if the value does not match the parameter requirements
	 * @see #sanitize(String)
	 */
	public Data set(Entity entity, String name, Object value)
	{
		return set(entity.type(), name, value);
	}
	
	/**
	 * Sets the value of the specified concatenated configuration parameter.
	 * The key will be sanitized and split by '.': <code>Entity.Type.NAME</code> will be converted 
	 * to the configuration parameter <code>entity type &gt; name</code>
	 * @param key the parameter name (will be sanitized)
	 * @param value the new parameter value
	 * @return the previous value associated with that parameter if any, or null if the key is invalid
	 * @see #sanitize(String)
	 */
	public Data set(String key, Object value)
	{
		if( key == null || key.isBlank() ) return null;
		
		Tuple<String, String> name = explodeName(key);
		
		return set(name.a, name.b, value);
	}
	
	/**
	 * Removes the value of the specified configuration parameter. 
	 * @param type the entity type (will be sanitized)
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return the previous value associated with that parameter if any
	 * @see #sanitize(String)
	 */
	public abstract Data remove(String type, String name);
	
	/**
	 * Removes the value of the specified configuration parameter. 
	 * @param type the entity type
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return the previous value associated with that parameter if any
	 * @see #sanitize(String)
	 */
	public Data remove(Class<?> type, String name)
	{
		return remove(type.getName(), name);
	}
	
	/**
	 * Removes the value of the specified configuration parameter. 
	 * @param entity the entity instance. Its {@link Entity#type()} method will be used.
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @return the previous value associated with that parameter if any
	 * @see #sanitize(String)
	 */
	public Data remove(Entity entity, String name)
	{
		return remove(entity.type(), name);
	}
	
	/**
	 * Removes the value of the specified concatenated configuration parameter.
	 * The key will be sanitized an split by '.': <code>Entity.Type.NAME</code> will be converted 
	 * to the configuration parameter <code>entity type &gt; name</code>
	 * @param key the parameter name (will be sanitized)
	 * @return the previous value associated with that parameter if any
	 * @see #sanitize(String)
	 */
	public Data remove(String key)
	{
		if( key == null || key.isBlank() ) return null;
		
		Tuple<String, String> name = explodeName(key);
		
		return remove(name.a, name.b);
	}
	
	/**
	 * Adds a callback that will be triggered when the config value changes.
	 * The key will be sanitized and split by '.': <code>Entity.Type.NAME</code> will be converted 
	 * to the configuration parameter <code>entity type &gt; name</code>
	 * @param key the parameter name (will be sanitized)
	 * @param callback the callback function, it will receive a {@link Tuple} with the full config key and the value
	 * @see #sanitize(String)
	 */
	public void watch(String key, BiConsumer<String, Data> callback)
	{
		if( key == null || key.isBlank() ) return;
		
		Tuple<String, String> name = explodeName(key);
		
		watch(name.a, name.b, callback);
	}
	
	/**
	 * Adds a callback that will be triggered when the config value changes.
	 * @param type the entity type
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @param callback the callback function, it will receive a {@link Tuple} with the full config key and the value
	 * @see #sanitize(String)
	 */
	public void watch(Class<?> type, String name, BiConsumer<String, Data> callback)
	{
		watch(type.getName(), name, callback);
	}
	
	/**
	 * Adds a callback that will be triggered when the config value changes.
	 * @param entity the entity instance. Its {@link Entity#type()} method will be used.
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @param callback the callback function, it will receive a {@link Tuple} with the full config key and the value
	 * @see #sanitize(String)
	 */
	public void watch(Entity entity, String name, BiConsumer<String, Data> callback)
	{
		watch(entity.type(), name, callback);
	}
	
	/**
	 * Adds a callback that will be triggered when the config value changes.
	 * @param type the entity type (will be sanitized)
	 * @param name the name of the configuration parameter (will be sanitized)
	 * @param callback the callback function, it will receive a {@link Tuple} with the full config key and the value
	 * @see #sanitize(String)
	 */
	public abstract void watch(String type, String name, BiConsumer<String, Data> callback);
	
	/**
	 * Returns all the values for all the parameters of the given entity type.
	 * @param type the entity type (will be sanitized)
	 * @return all matching parameters and their value
	 * @see #sanitize(String)
	 */
	public abstract Map<String, Data> all(String type);
	
	/**
	 * Returns all the values for all the parameters of the given entity type.
	 * @param type the entity type
	 * @return all matching parameters and their value
	 */
	public Map<String, Data> all(Class<?> type)
	{
		return all(type.getName());
	}
	
	/**
	 * Returns all the values for all the parameters of the given entity type.
	 * @param entity the entity instance. Its {@link Entity#type()} method will be used.
	 * @return all matching parameters and their value
	 */
	public Map<String, Data> all(Entity entity)
	{
		return all(entity.type());
	}
	
	/**
	 * Returns all the values for all the parameters of all entity types.
	 * @return all parameters and their value grouped by entity type
	 */
	public abstract Map<String, Map<String, Data>> all();
}
