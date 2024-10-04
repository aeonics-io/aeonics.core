package aeonics.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import aeonics.util.Internal;
import aeonics.util.Json;

/**
 * Implementation of Data as a map
 * @hidden
 */
@Internal
public class DataMap implements Data
{
	private HashMap<String, Data> source = new HashMap<String, Data>();
	
	public DataMap(Map<?, ?> map)
	{
		if( map == null || map.isEmpty() ) return;
		
		for( Map.Entry<?,?> entry : map.entrySet() )
			source.put(entry.getKey() == null ? null : entry.getKey().toString(), Data.of(entry.getValue()));
	}
	
	public Iterator<Data> iterator() { return source.values().iterator(); }
	public Iterable<Map.Entry<String, Data>> entrySet() { return source.entrySet(); }
	
	public boolean isMap() { return true; }
	public boolean isList() { return false; }
	public boolean isBool() { return false; }
	public boolean isNumber() { return false; }
	public boolean isString() { return false; }
	public boolean isNull() { return false; }
	public boolean is(Class<?> type) { return false; }
	public boolean isEmpty() { return source.isEmpty(); }
	
	public Data get(String key) { Data a = source.get(key); return (a==null?Data.of(null):a); }
	public Data get(int index) { return this.get("" + index); }
	@SuppressWarnings("unchecked")
	public <T> T get() { return (T) this; }
	public boolean containsKey(String key) { return source.containsKey(key); }
	
	public void clear() { source.clear(); }
	public int size() { return source.size(); }
	public Data remove(String key) { return source.remove(key); }
	public Data remove(int index) { return this.remove("" + index); }
	public Data remove(Data item) { source.values().remove(item); return item; }
	public void removeIf(Predicate<Data> check) { this.source.values().removeIf(check); }
	
	public Data add(Object value) { return put("", value); }
	public Data add(Object ...value)
	{
		for( int i = 0; i < value.length; i++ )
		{
			String key = "" + value[i];
			i++;
			put(key, i < value.length ? value[i] : null);
		}
		return this;
	}
	public Data put(String key, Object value) { if( value instanceof Data ) source.put(key, (Data)value); else source.put(key, Data.of(value)); return this; }
	public Data set(int index, Object value) { return put("" + index, value); }
	public Data set(Object value) { return put("", value); }
	
	public boolean asBool() { return !source.isEmpty(); }
	public Number asNumber() { return source.size(); }
	public String asString() { return toString(); }
	@Override
	public String toString() { StringBuilder buffer = new StringBuilder(); toString(buffer); return buffer.toString(); }
	public void toString(StringBuilder buffer)
	{
		buffer.append("{");
		boolean first = true;
		for( Map.Entry<String, Data> entry : source.entrySet() )
		{
			if( !first ) buffer.append(", ");
			first = false;
			
			buffer.append("\"");
			if( entry.getKey() != null ) buffer.append(Json.escape(entry.getKey()));
			buffer.append("\": ");
			if( entry.getValue() == null ) buffer.append("null");
			else entry.getValue().toString(buffer);
		}
		buffer.append("}");
	}
	@Override
	public boolean equals(Object value)
	{
		if( value == this ) return true;
		if( value instanceof DataMap )
			value = ((DataMap)value).source;
		else if( value instanceof Data )
			value = ((Data)value).get();
		
		if( value == this.source ) return true;
		if( value == null ) return false;
		
		if( !(value instanceof Map) ) return false;
		Map<?, ?> v = ((Map<?,?>)value);
		
		if( this.source.size() != v.size() ) return false;
		for( Map.Entry<String, Data> entry : this.source.entrySet() )
		{
			if( !v.containsKey(entry.getKey()) ) return false;
			if( !entry.getValue().equals(v.get(entry.getKey())) ) return false;
		}
		return true;
	}
	@Override
	public Data clone() { Data clone = new DataMap(null); cloneTo(clone); return clone; }
	public void cloneTo(Data other)
	{
		if( !(other instanceof DataMap) ) throw new IllegalArgumentException("Clone failed. Incompatible receiving data type.");
		
		other.clear();
		for( Map.Entry<String, Data> entry : this.entrySet() )
			other.put(entry.getKey(), entry.getValue().clone());
	}
}