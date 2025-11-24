package aeonics.util;

import java.util.Collection;

import aeonics.util.Functions.Predicate;

public class ListUtils
{
	public static <T> boolean contains(Collection<T> list, Predicate<T> check)
	{
		return find(list, check) != null;
	}
	
	public static <T> T find(Collection<T> list, Predicate<T> check)
	{
		if( list == null || check == null ) return null;
		for( T t : list )
		{
			try { if( check.test(t) ) return t; }
			catch(Exception e) { /* ignore */ }
		}
		return null;
	}
}
