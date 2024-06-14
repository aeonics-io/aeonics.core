/*
 * Copyright (c) Aeonics srl and/or its respectful owner. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This material is subject to the Aeonics Commercial License agreement.
 */
package aeonics.util;

/**
 * A wrapper for two values
 * @param <A> the first value type
 * @param <B> the second value type
 */
public class Tuple<A, B>
{
	/**
	 * The wrapped value
	 */
	public volatile A a;
	/**
	 * The wrapped value
	 */
	public volatile B b;
	/**
	 * Creates a new two-value wrapper
	 * @param a the first value
	 * @param b the second value
	 */
	public Tuple(A a, B b) { this.a = a; this.b = b; }
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) { if( obj instanceof Tuple ) return equals((Tuple<A, B>)obj); else return false; }
	/**
	 * Returns whether or not both values match these ones
	 * @param t the wrapped values
	 * @return true if the wrapped values match these ones
	 */
	public boolean equals(Tuple<A, B> t) { return ((a == null && t.a == null) || a.equals(t.a)) && ((b == null && t.b == null) || b.equals(t.b)); }
	@Override
	public int hashCode() { return (a == null ? 0 : a.hashCode()) ^ (b == null ? 0 : b.hashCode()); }
	/**
	 * Creates a new two-value wrapper
	 * @param <X> the first value type
	 * @param <Y> the second value type
	 * @param a the first value
	 * @param b the second value
	 * @return a new two-value wrapper
	 */
	public static <X, Y> Tuple<X, Y> of(X a, Y b) { return new Tuple<X, Y>(a, b); }
}
