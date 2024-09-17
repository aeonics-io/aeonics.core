package aeonics.data;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import aeonics.util.Exportable;
import aeonics.util.Internal;
import aeonics.util.Json;

/**
 * Implementation of Data as a scalar
 * @hidden
 */
@Internal
public class DataObject implements Data
{
	private Object source = null;
	
	public DataObject(Object value) { this.source = value; }
	
	public Iterator<Data> iterator() { return Collections.singleton((Data)this).iterator(); }
	public Iterable<Map.Entry<String, Data>> entrySet() { return Collections.emptySet(); }
	
	public boolean isMap() { return false; }
	public boolean isList() { return false; }
	public boolean isBool() { return source instanceof Boolean; }
	public boolean isNumber() { return source instanceof Number; }
	public boolean isString() { return source instanceof String; }
	public boolean isNull() { return source == null; }
	public boolean is(Class<?> type) { return type.isInstance(source); }
	public boolean isEmpty() { return this.isNull() || (source instanceof String && ((String)source).isBlank()); }
	
	public Data get(String key) { throw new RuntimeException("This element is not a container"); }
	public Data get(int index) { throw new RuntimeException("This element is not a container"); }
	@SuppressWarnings("unchecked")
	public <T> T get() { return (T) source; }
	public boolean containsKey(String key) { return false; }
	
	public void clear() { this.source = null; }
	public int size() { return 1; }
	public Data remove(String key) { throw new RuntimeException("This element is not a container"); }
	public Data remove(int index) { throw new RuntimeException("This element is not a container"); }
	public Data remove(Data item) { throw new RuntimeException("This element is not a container"); }
	public void removeIf(Predicate<Data> check) { throw new RuntimeException("This element is not a container"); }
	
	public Data add(Object value) { throw new RuntimeException("This element is not a container"); }
	public Data put(String key, Object value) { throw new RuntimeException("This element is not a container"); }
	public Data set(int index, Object value) { throw new RuntimeException("This element is not a container"); }
	public Data set(Object value) { if( value instanceof Data ) source = ((Data)value).get(); else source = value; return this; }
	
	public boolean asBool()
	{
		if( source == null ) return false;
		try { return (Boolean) source; }
		catch(NullPointerException npe) { return false; }
		catch(ClassCastException e)
		{
			if( source instanceof String && (((String)source).equalsIgnoreCase("true") || ((String)source).equalsIgnoreCase("yes")) ) return true;
			return asNumber().doubleValue() > 0.0;
		}
	}
	public Number asNumber()
	{
		if( source == null ) return 0;
		try { return (Number) source; }
		catch(ClassCastException e)
		{
			if( source instanceof Boolean ) return ((Boolean)source) ? 1 : 0;
			
			try { return Long.parseLong(source.toString()); } catch(NumberFormatException n) { /* ignore */ }
			try { return Double.parseDouble(source.toString()); } catch(NumberFormatException n) { /* ignore */ }
			
			if( !(source instanceof String) )
				return source.hashCode(); // allow conversion to number to be defined by hashCode()
			return 0;
		}
	}
	public String asString()
	{
		if( source == null ) return "";
		return source.toString();
	}
	@Override
	public String toString() { StringBuilder buffer = new StringBuilder(); toString(buffer); return buffer.toString(); }
	public void toString(StringBuilder buffer)
	{
		if( source == null ) buffer.append("null");
		else if( source instanceof Number )
		{
			// detect if integral number. if yes, print it as a long, else print as double
			if( ((Number)source).doubleValue() == ((Number)source).longValue() ) buffer.append(((Number)source).longValue());
			else buffer.append(((Number)source).doubleValue());
		}
		else if( source instanceof Boolean ) buffer.append((Boolean)source ? "true" : "false");
		else if( source instanceof Exportable ) buffer.append(((Exportable)source).export().toString());
		else
		{
			buffer.append("\"");
			buffer.append(Json.escape(this.asString()));
			buffer.append("\"");
		}
	}
	@Override
	public boolean equals(Object value)
	{
		if( value == this ) return true;
		if( value instanceof Data )
			value = ((Data)value).get();
		
		if( value == this.source ) return true;
		if( value == null || this.source == null ) return false;
		
		if( value instanceof String ) return asString().equals(value);
		if( value instanceof Boolean ) return asBool() == ((Boolean) value);
		if( value instanceof Number ) return asNumber().equals(value);
		
		return value.equals(this.source);
	}
	@Override
	public Data clone() { Data clone = new DataObject(null); cloneTo(clone); return clone; }
	public void cloneTo(Data other)
	{
		if( !(other instanceof DataObject) ) throw new IllegalArgumentException("Clone failed. Incompatible receiving data type.");
		((DataObject)other).source = this.source;
	}
}