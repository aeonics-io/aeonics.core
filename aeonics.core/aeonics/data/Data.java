package aeonics.data;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import aeonics.util.Internal;

/**
 * Inert representation of mutable data.
 * 
 * <p>This class has the ability to wrap either an object, or a list of objects, or a key/value map.
 * It can perform basic type coersion with late evaluation principle.</p>
 * 
 * <p>By default, implementations will try to be as resilient as possible (i.e. convert a string key to an integer for a list) but it may eventually give up and
 * throw a RuntimeException.</p>
 */
public interface Data extends Iterable<Data>
{
	// ==========================
	// STATIC METHODS
	// ==========================
	
	/**
	 * Wraps the specified object in a data structure
	 * @param item the object to wrap
	 * @return a data representation
	 */
	public static Data of(Object item)
	{
		if( item instanceof Data )
			return (Data) item;
		if( item instanceof Throwable )
			return new DataObject(item);
		if( item instanceof Map )
			return new DataMap((Map<?,?>)item);
		if( item instanceof Collection )
			return new DataList((Collection<?>)item);
		if( item instanceof Set )
			return new DataList((Set<?>)item);
		if( item instanceof Enumeration )
			return new DataList((Enumeration<?>)item);
		if( item instanceof Iterable )
			return new DataList((Iterable<?>)item);
		if( item instanceof Iterator )
			return new DataList((Iterator<?>)item);
		if( item != null && item.getClass().isArray() && !item.getClass().getComponentType().isPrimitive() )
			return new DataList((Object[])item);
		return new DataObject(item);
	}
	
	/**
	 * Returns a null object data
	 * @return a null object data
	 */
	public static Data empty() { return new DataObject(null); }
	
	/**
	 * Returns an empty data map
	 * @return an empty data map
	 */
	public static Data map() { return new DataMap(null); }
	
	/**
	 * Returns an empty data list 
	 * @return an empty data list 
	 */
	public static Data list() { return new DataList((Collection<?>)null); }
	
	// ==========================
	// ITERABLE METHODS
	// ==========================
	
	/**
	 * Returns an iterator over the elements of this list of data, or over the values of the map.
	 * If this data instance is a regular object, then this method returns an iterator over itself.
	 * @return an iterator over the elements
	 */
	public Iterator<Data> iterator();
	
	/**
	 * Returns an entry set of this data map.
	 * If this data instance is not a map, an empty set is returned.
	 * @return an entry set of this data
	 */
	public Iterable<Map.Entry<String, Data>> entrySet();
	
	// ==========================
	// IS * METHODS
	// ==========================
	
	/**
	 * Returns true if this data instance is a map
	 * @return true if this data instance is a map
	 */
	public boolean isMap();
	
	/**
	 * Returns true if this data instance is a list
	 * @return true if this data instance is a list
	 */
	public boolean isList();
	
	/**
	 * Returns true if this data instance is a boolean object
	 * @return true if this data instance is a boolean object
	 */
	public boolean isBool();
	
	/**
	 * Returns true if this data instance is a number type object
	 * @return true if this data instance is a number type object
	 */
	public boolean isNumber();
	
	/**
	 * Returns true if this data instance is a string object
	 * @return true if this data instance is a string object
	 */
	public boolean isString();
	
	/**
	 * Returns true of this data instance is a null object
	 * @return true of this data instance is a null object
	 */
	public boolean isNull();
	
	/**
	 * Returns true of tis data instance is of the specified type
	 * @param type the object type
	 * @return true of tis data instance is of the specified type
	 */
	public boolean is(Class<?> type);
	
	/**
	 * Returns true if this data instance is null, blank, an empty list or an empty map
	 * @return true if this data instance is null, blank, an empty list or an empty map
	 */
	public boolean isEmpty();
	
	/**
	 * Returns true if the mapped value is a map itself
	 * @param key the key to check
	 * @return true if the mapped value is a map itself
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isMap(String key)    { return isMap() && get(key).isMap(); }
	
	/**
	 * Returns true if the mapped value is a list
	 * @param key the key to check
	 * @return true if the mapped value is a list
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isList(String key)   { return isMap() && get(key).isList(); }
	
	/**
	 * Returns true if the mapped value is a boolean object
	 * @param key the key to check
	 * @return true if the mapped value is a boolean object
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isBool(String key)   { return isMap() && get(key).isBool(); }
	
	/**
	 * Returns true if the mapped value is a number type
	 * @param key the key to check
	 * @return true if the mapped value is a number type
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isNumber(String key) { return isMap() && get(key).isNumber(); }
	
	/**
	 * Returns true if the mapped value is a string object
	 * @param key the key to check
	 * @return true if the mapped value is a string object
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isString(String key) { return isMap() && get(key).isString(); }
	
	/**
	 * Returns true if the mapped value is a null object
	 * @param key the key to check
	 * @return true if the mapped value is a null object
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isNull(String key)   { return isMap() && get(key).isNull(); }
	
	/**
	 * Returns true if the mapped value is a a null object, blank or empty
	 * @param key the key to check
	 * @return true if the mapped value is a null object, blank or empty
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isEmpty(String key)  { return isMap() && get(key).isEmpty(); }
	
	/**
	 * Returns true if the mapped value is an object of the specified type
	 * @param key the key to check
	 * @param type the expected class type
	 * @return true if the mapped value is an object of the specified type
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean is(String key, Class<?> type) { return isMap() && get(key).is(type); }
	
	/**
	 * Returns true if the specified element is a map
	 * @param index the element index
	 * @return true if the specified element is a map
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isMap(int index)     { return isList() && get(index).isMap(); }
	
	/**
	 * Returns true if the specified element is a list itself
	 * @param index the element index
	 * @return true if the specified element is a list itself
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isList(int index)    { return isList() && get(index).isList(); }
	
	/**
	 * Returns true if the specified element is a boolean object
	 * @param index the element index
	 * @return true if the specified element is a boolean object
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isBool(int index)    { return isList() && get(index).isBool(); }
	
	/**
	 * Returns true if the specified element is a number type
	 * @param index the element index
	 * @return true if the specified element is a number type
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isNumber(int index)  { return isList() && get(index).isNumber(); }
	
	/**
	 * Returns true if the specified element is a string object
	 * @param index the element index
	 * @return true if the specified element is a string object
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isString(int index)  { return isList() && get(index).isString(); }
	
	/**
	 * Returns true if the specified element is a null object
	 * @param index the element index
	 * @return true if the specified element is a null object
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isNull(int index)    { return isList() && get(index).isNull(); }
	
	/**
	 * Returns true if the specified element is a null object, blank or empty
	 * @param index the element index
	 * @return true if the specified element is a null object, blank or empty
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean isEmpty(int index)   { return isList() && get(index).isEmpty(); }
	
	/**
	 * Returns true if the specified element is an object of the specified type
	 * @param index the element index
	 * @param type the expected class type
	 * @return true if the specified element is an object of the specified type
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean is(int index, Class<?> type) { return isList() && get(index).is(type); }
	
	// ==========================
	// AS * METHODS
	// ==========================
	
	/**
	 * Coerces this data to a boolean.
	 * Case invariant "true" or "yes", numeric value 0 and boolean value true all return true.
	 * If map or list, returns true if not empty.
	 * @return this data as a boolean
	 */
	public boolean asBool();
	
	/**
	 * Coerces this data to a string.
	 * Null is converted to an empty string, otherwise the toString() representation.
	 * If map or list, returns the JSON representation.
	 * @return this data as a string
	 */
	public String asString();
	
	/**
	 * Coerces this data to a number.
	 * Native numeric value, boolean as 0 or 1, null as 0, string parsed to a long or double, or the object hashCode() .
	 * If map or list, returns the number of elements.
	 * @return this data as a number
	 */
	public Number asNumber();
	
	/**
	 * Coerces this data to an integer as per {@link Number#intValue()}
	 * @see #asNumber()
	 * @return this data as an integer
	 */
	public default int asInt() { return asNumber().intValue(); }
	
	/**
	 * Coerces this data to a long as per {@link Number#longValue()}
	 * @see #asNumber()
	 * @return this data as a long
	 */
	public default long asLong() { return asNumber().longValue(); }
	
	/**
	 * Coerces this data to a double as per {@link Number#doubleValue()}
	 * @see #asNumber()
	 * @return this data as a double
	 */
	public default double asDouble() { return asNumber().doubleValue(); }
	
	/**
	 * Coerces the mapped value to a boolean.
	 * Case invariant "true" or "yes", numeric value 0 and boolean value true all return true.
	 * If map or list, returns true if not empty.
	 * @param key the key to check
	 * @return the mapped value as a boolean
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean asBool(String key)  { Data a = get(key); if( a == null ) return false; else return a.asBool(); }
	
	/**
	 * Coerces the mapped value to a string.
	 * Null is converted to an empty string, otherwise the toString() representation.
	 * If map or list, returns the JSON representation.
	 * @param key the key to check
	 * @return the mapped value as a string
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default String asString(String key) { Data a = get(key); if( a == null ) return ""; else return a.asString(); }
	
	/**
	 * Coerces the mapped value to a number.
	 * Native numeric value, boolean as 0 or 1, null as 0, string parsed to a long or double, or the object hashCode() .
	 * If map or list, returns the number of elements.
	 * @param key the key to check
	 * @return the mapped value as a number
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default Number asNumber(String key) { Data a = get(key); if( a == null ) return 0; else return a.asNumber(); }
	
	/**
	 * Coerces the mapped value to an integer as per {@link Number#intValue()}
	 * @see #asNumber(String)
	 * @param key the key to check
	 * @return the mapped value as an integer
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default int asInt(String key)       { return asNumber(key).intValue(); }
	
	/**
	 * Coerces the mapped value to a long as per {@link Number#longValue()}
	 * @see #asNumber(String)
	 * @param key the key to check
	 * @return the mapped value as a long
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default long asLong(String key)     { return asNumber(key).longValue(); }
	
	/**
	 * Coerces the mapped value to a double as per {@link Number#doubleValue()}
	 * @see #asNumber(String)
	 * @param key the key to check
	 * @return the mapped value as a double
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default double asDouble(String key) { return asNumber(key).doubleValue(); }
	
	/**
	 * Coerces the specified element to a boolean.
	 * Case invariant "true" or "yes", numeric value 0 and boolean value true all return true.
	 * If map or list, returns true if not empty.
	 * @param index the element index
	 * @return the specified element as a boolean
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default boolean asBool(int index)   { Data a = get(index); if( a == null ) return false; else return a.asBool(); }
	
	/**
	 * Coerces the specified element to a string.
	 * Null is converted to an empty string, otherwise the toString() representation.
	 * If map or list, returns the JSON representation.
	 * @param index the element index
	 * @return the specified element as a string
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default String asString(int index)  { Data a = get(index); if( a == null ) return ""; else return a.asString(); }
	
	/**
	 * Coerces the specified element to a number.
	 * Native numeric value, boolean as 0 or 1, null as 0, string parsed to a long or double, or the object hashCode() .
	 * If map or list, returns the number of elements.
	 * @param index the element index
	 * @return the specified element as a number
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default Number asNumber(int index)  { Data a = get(index); if( a == null ) return 0; else return a.asNumber(); }
	
	/**
	 * Coerces the specified element to an integer as per {@link Number#intValue()}
	 * @see #asNumber(int)
	 * @param index the element index
	 * @return the specified element as an integer
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default int asInt(int index)        { return asNumber(index).intValue(); }
	
	/**
	 * Coerces the specified element to a long as per {@link Number#longValue()}
	 * @see #asNumber(int)
	 * @param index the element index
	 * @return the specified element as a long
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default long asLong(int index)      { return asNumber(index).longValue(); }
	
	/**
	 * Coerces the specified element to a double as per {@link Number#doubleValue()}
	 * @see #asNumber(int)
	 * @param index the element index
	 * @return the specified element as a double
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public default double asDouble(int index)  { return asNumber(index).doubleValue(); }
	
	// ==========================
	// EQUALS FOR MAP/LIST
	// ==========================
	
	/**
	 * Returns true if the mapped value equals the specified object as per <code>get(key).equals(other);</code>
	 * @param key the key to check
	 * @param other the value to compare
	 * @return true if the mapped value equals the specified object
	 */
	public default boolean equals(String key, Object other) { Data a = get(key); if( a == null ) return other == null; else return a.equals(other); }
	
	/**
	 * Returns true if the specified element equals the specified object as per <code>get(index).equals(other);</code>
	 * @param index the element index
	 * @param other the value to compare
	 * @return true if the specified element equals the specified object
	 */
	public default boolean equals(int index, Object other) { Data a = get(index); if( a == null ) return other == null; else return a.equals(other); }
	
	// ==========================
	// OTHER MAP/LIST METHODS
	// ==========================
	
	/**
	 * Returns the mapped value
	 * @param key the element key
	 * @return the mapped value
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public Data get(String key);
	
	/**
	 * Returns the specified element
	 * @param index the element index
	 * @return the specified element
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public Data get(int index);
	
	/**
	 * Unwraps the real object value and performs an implicit cast to the receiving type
	 * @param <T> the receiving type
	 * @return the real object value
	 */
	public <T> T get();
	
	/**
	 * Returns true if an object can be fetched with the specified key
	 * @see #get(String)
	 * @param key the element key
	 * @return true if an object can be fetched with the specified key
	 */
	public boolean containsKey(String key);
	
	/**
	 * Unwraps the real object value and performs an implicit cast to the receiving type.
	 * If the real object value is null, the provided fallback value is returned instead 
	 * @param <T> the receiving type
	 * @param fallback the fallback value in case the real object value is null
	 * @return the real object value or the fallback value in case of null
	 */
	public default <T> T getOr(T fallback) { T value = get(); return (value == null ? fallback : value); }
	
	/**
	 * Removes all elements in case of a map or list, or sets the value to null in case of an object
	 */
	public void clear();
	
	/**
	 * Returns the number of element in case of map or list, or 1 for an object
	 * @return the number of element in case of map or list, or 1 for an object
	 */
	public int size();
	
	/**
	 * Removes the mapped element and returns it
	 * @param key the element key
	 * @return the previously mapped element, or null if there was no mapping for the key
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public Data remove(String key);
	
	/**
	 * Removes the specified element and returns it. The remaining elements are shifted and the size is reduced by 1.
	 * @param index the element index
	 * @return the removed element
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public Data remove(int index);
	
	/**
	 * Removes the value if it exists and returns it.
	 * @param item the value to remove
	 * @return the removed value
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public Data remove(Data item);
	
	/**
	 * Adds the provided value to this map or list.
	 * If this data instance is a map, the value will be mapped to an empty key
	 * @param value the value to add
	 * @return <b>this</b> for chaining
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public Data add(Object value);
	
	/**
	 * Adds the provided value to this map or list.
	 * @param key the element key
	 * @param value the value to add
	 * @return <b>this</b> for chaining
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public Data put(String key, Object value);
	
	/**
	 * Sets the value at the specified index
	 * @param index the element index
	 * @param value the value to set
	 * @return <b>this</b> for chaining
	 * @throws RuntimeException if this data instance is not a map or a list
	 */
	public Data set(int index, Object value);
	
	/**
	 * Swaps the underlying value entirely.
	 * If this data instance is a list, <code>add(value)</code> is used instead.
	 * If this data instance is a map, <code>put("", value)</code> is used instead.
	 * @param value the new value
	 * @return <b>this</b> for chaining
	 */
	public Data set(Object value);
	
	/**
	 * Internal toString builder for map or list to create a JSON representation
	 * @param buffer the buffer to append to
	 * @hidden
	 */
	@Internal
	public void toString(StringBuilder buffer);
	
	/**
	 * Deep clone of this data instance.
	 * In case of a map or list, all contained elements are also deep cloned.
	 * In case of a non-simple (String, int,...) value object, then the object is referenced and not duplicated.
	 * @return a new deep copy of this data instance
	 */
	public Data clone();
	
	/**
	 * Clears and deep clones this data instance into the target data object.
	 * @param other the target data object to copy to
	 * @throws IllegalArgumentException if the destination data object is not the same type as this instance (map, list or object)
	 */
	public void cloneTo(Data other);
	
	// ==========================
	// NESTED METHODS
	// ==========================
	
	static final Pattern nestedSplitter = Pattern.compile("[\\./\\\\]");
	
	/**
	 * Returns the nested value matching the specified key.
	 * The key should be a dot, forward slash or back slash ('./\') separated list of sub-keys to fetch.
	 * <p>The key <code>foo.bar.key</code> will fetch the <code>foo</code> object, 
	 * then fetch the <code>bar</code> object from it,
	 * then return the <code>key</code> mapped value.</p>
	 * 
	 * <p>If this data object is not a list or a map, or if the key does not map to a value, then an empty data is returned.</p>
	 * @param key the nested key
	 * @return the nested value associated with the key or an empty data object if not found
	 * @see #get(String)
	 */
	public default Data getNested(String key)
	{
		if( key == null || key.isEmpty() ) return this;
		
		String[] parts = nestedSplitter.split(key);
		Data root = this;
		for( String p : parts )
		{
			if( root.containsKey(p) )
				root = root.get(p);
			else
				return Data.empty();
		}
		
		return root;
	}
	
	/**
	 * Returns true if this data object contains the specified nested key.
	 * The key should be a dot, forward slash or back slash ('./\') separated list of sub-keys to fetch.
	 * <p>The key <code>foo.bar.key</code> will fetch the <code>foo</code> object, 
	 * then fetch the <code>bar</code> object from it,
	 * then return if the <code>key</code> exists.</p>
	 * 
	 * <p>If this data object is not a list or a map, or if the key does not map to a value, then this method returns false.</p>
	 * @param key the nested key
	 * @return true if this data object contains the specified nested key
	 * @see #containsKey(String)
	 */
	public default boolean containsKeyNested(String key)
	{
		if( key == null || (!isMap() && !isList()) ) return false;
		
		String[] parts = nestedSplitter.split(key);
		Data root = this;
		for( String p : parts )
		{
			if( root.containsKey(p) )
				root = root.get(p);
			else
				return false;
		}
		
		return true;
	}
	
	/**
	 * Adds the provided nested value ot this map or list.
	 * The key should be a dot, forward slash or back slash ('./\') separated list of sub-keys to fetch.
	 * <p>The key <code>foo.bar.key</code> will fetch the <code>foo</code> object, 
	 * then fetch the <code>bar</code> object from it,
	 * then set the value for the <code>key</code>.</p>
	 * 
	 * <p>Missing intermediate objects are created automatically.</p>
	 * @param key the nested key
	 * @param value the value to add
	 * @return <b>this</b> top level object for chaining
	 * @see #put(String, Object)
	 */
	public default Data putNested(String key, Object value)
	{
		if( key == null ) key = "";
		
		String[] parts = nestedSplitter.split(key);
		Data root = this;
		int i = 0;
		for( ; i < parts.length-1; i++ )
		{
			if( root.containsKey(parts[i]) )
				root = root.get(parts[i]);
			else
				break;
		}
		
		for( ; i < parts.length-1; i++ )
		{
			if( !root.containsKey(parts[i]) )
			{
				Data missing = Data.map();
				root.put(parts[i], missing);
				root = missing;
			}
		}
		root.put(parts[i], value);
		
		return this;
	}
	
	/**
	 * Removes the nested element and returns it.
	 * The key should be a dot, forward slash or back slash ('./\') separated list of sub-keys to fetch.
	 * <p>The key <code>foo.bar.key</code> will fetch the <code>foo</code> object, 
	 * then fetch the <code>bar</code> object from it,
	 * then remove the value mapped to the <code>key</code>.</p>
	 * @param key the nested key
	 * @return the previously mapped element, or null if there was no mapping for the nested key
	 * @see #remove(String)
	 */
	public default Data removeNested(String key)
	{
		if( key == null ) key = "";
		
		String[] parts = nestedSplitter.split(key);
		Data root = this;
		int i = 0;
		for( ; i < parts.length-1; i++ )
		{
			if( root.containsKey(parts[i]) )
				root = root.get(parts[i]);
			else
				return null;
		}
		
		if( root.containsKey(parts[i]) )
			return root.remove(parts[i]);
		else
			return null;
	}
}
