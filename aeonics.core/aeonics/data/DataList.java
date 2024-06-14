/*
 * Copyright (c) Aeonics srl and/or its respectful owner. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This material is subject to the Aeonics Commercial License agreement.
 */
package aeonics.data;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import aeonics.util.Internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Implementation of Data as a list
 * @hidden
 */
@Internal
public class DataList implements Data
{
	private LinkedList<Data> source = new LinkedList<Data>();
	
	public DataList(Collection<?> list)
	{
		if( list == null || list.isEmpty() ) return;
		
		for( Object item : list )
			source.add(Data.of(item));
	}
	
	public DataList(Set<?> list)
	{
		if( list == null || list.isEmpty() ) return;
		
		for( Object item : list )
			source.add(Data.of(item));
	}
	
	public DataList(Enumeration<?> list)
	{
		if( list == null ) return;
		
		while( list.hasMoreElements() )
			source.add(Data.of(list.nextElement()));
	}
	
	public DataList(Iterable<?> list)
	{
		if( list == null ) return;
		
		for( Object item : list )
			source.add(Data.of(item));
	}
	
	public DataList(Iterator<?> list)
	{
		if( list == null ) return;
		
		while( list.hasNext() )
			source.add(Data.of(list.next()));
	}
	
	public DataList(Object[] list)
	{
		if( list == null ) return;
		
		for( int i = 0; i < list.length; i++ )
			source.add(Data.of(list[i]));
	}
	
	public Iterator<Data> iterator() { return source.iterator(); }
	public Iterable<Map.Entry<String, Data>> entrySet() { return Collections.emptySet(); }
	
	public boolean isMap() { return false; }
	public boolean isList() { return true; }
	public boolean isBool() { return false; }
	public boolean isNumber() { return false; }
	public boolean isString() { return false; }
	public boolean isNull() { return false; }
	public boolean is(Class<?> type) { return false; }
	public boolean isEmpty() { return source.isEmpty(); }
	
	public Data get(String key) { return this.get(Integer.parseInt(key)); }
	public Data get(int index) { if( index < 0 || index >= source.size() ) return Data.of(null); return source.get(index); }
	@SuppressWarnings("unchecked")
	public <T> T get() { return (T) this; }
	public boolean containsKey(String key)
	{
		try
		{
			int i = Integer.parseInt(key);
			return ( i >= 0 && i < source.size() );
		}
		catch(Exception e) { return false; }
	}
	
	public void clear() { source.clear(); }
	public int size() { return source.size(); }
	public Data remove(String key) { return this.remove(Integer.parseInt(key)); }
	public Data remove(int index) { return source.remove(index); }
	public Data remove(Data item) { int index = this.source.indexOf(item); if( index >= 0 ) return source.remove(index); return null; }
	
	public Data add(Object value) { if( value instanceof Data ) source.add((Data)value); else source.add(Data.of(value)); return this; }
	public Data put(String key, Object value) { return set(Integer.parseInt(key), value); }
	public Data set(int index, Object value) { if( value instanceof Data ) source.set(index, (Data)value); else source.set(index, Data.of(value)); return this; }
	public Data set(Object value) { return add(value); }
	
	public boolean asBool() { return !source.isEmpty(); }
	public Number asNumber() { return source.size(); }
	public String asString() { return toString(); }
	
	@Override
	public String toString() { StringBuilder buffer = new StringBuilder(); toString(buffer); return buffer.toString(); }
	public void toString(StringBuilder buffer)
	{
		buffer.append("[");
		boolean first = true;
		for( Data item : source )
		{
			if( !first ) buffer.append(", ");
			first = false;
			
			if( item == null ) buffer.append("null");
			else item.toString(buffer);
		}
		buffer.append("]");
	}
	@Override
	public boolean equals(Object value)
	{
		if( value == this ) return true;
		if( value instanceof DataList )
			value = ((DataList)value).source;
		else if( value instanceof Data )
			value = ((Data)value).get();
		
		if( value == this.source ) return true;
		if( value == null ) return false;
		if( (value instanceof Collection) && ((Collection<?>)value).size() != this.source.size() ) return false;
		if( !(value instanceof Iterable) ) return false;

		Iterator<?> a = ((Iterable<?>)value).iterator();
		Iterator<Data> b = iterator();
		while (a.hasNext() && b.hasNext())
			if( !b.next().equals(a.next()) ) return false;
		if( a.hasNext() || b.hasNext() ) return false;
		return true;
	}
	@Override
	public Data clone() { Data clone = new DataList((Collection<?>) null); cloneTo(clone); return clone; }
	public void cloneTo(Data other)
	{
		if( !(other instanceof DataList) ) throw new IllegalArgumentException("Clone failed. Incompatible receiving data type.");
		
		other.clear();
		for( Data d : this )
			other.add(d.clone());
	}
}