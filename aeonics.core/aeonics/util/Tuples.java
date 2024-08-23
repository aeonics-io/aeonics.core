package aeonics.util;

/**
 * This collection of classes that can group different Objects together.
 * It is useful when you need to return multiple values from a method, or when you need
 * a final variable but still change the value from within a lambda scope.
 */
public class Tuples 
{
	private Tuples() { /* no instances */ }
	
	// ===================
	// ONE-
	// ===================
	
	/**
	 * A wrapper for one value
	 * @param <A> the value type
	 */
	public static class Single<A>
	{
		/**
		 * The wrapped value
		 */
		public volatile A a;
		
		/**
		 * Creates a one-value wrapper
		 * @param a the value
		 */
		public Single(A a) { this.a = a; }
		
		@Override
		public boolean equals(Object obj)
		{
			if( obj instanceof Single )
			{
				Single<?> t = (Single<?>)obj;
				return ((a == null && t.a == null) || a.equals(t.a));
			}
			else return false;
		}
		
		/**
		 * Creates a new one-value wrapper
		 * @param <X> the value type
		 * @param a the value
		 * @return a new one-value wrapper
		 */
		public static <X> Single<X> of(X a) { return new Single<X>(a); }
	}
	
	// ===================
	// TWO-
	// ===================
	
	/**
	 * A wrapper for two values
	 * @param <A> the first value type
	 * @param <B> the second value type
	 */
	public static class Tuple<A, B>
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
		
		@Override
		public boolean equals(Object obj)
		{
			if( obj instanceof Tuple )
			{
				Tuple<?, ?> t = (Tuple<?, ?>) obj;
				return ((a == null && t.a == null) || a.equals(t.a)) && ((b == null && t.b == null) || b.equals(t.b));
			}
			else return false;
		}
		
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
	
	// ===================
	// THREE-
	// ===================
	
	/**
	 * A wrapper for three values
	 * @param <A> the first value type
	 * @param <B> the second value type
	 * @param <C> the third value type
	 */
	public static class Triple<A, B, C>
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
		 * The wrapped value
		 */
		public volatile C c;
		
		/**
		 * Creates a new three-value wrapper
		 * @param a the first value
		 * @param b the second value
		 * @param c the third value
		 */
		public Triple(A a, B b, C c) { this.a = a; this.b = b; this.c = c; }
		
		@Override
		public boolean equals(Object obj)
		{
			if( obj instanceof Triple )
			{
				Triple<?, ?, ?> t = (Triple<?, ?, ?>) obj;
				return ((a == null && t.a == null) || a.equals(t.a)) && ((b == null && t.b == null) || b.equals(t.b)) && ((c == null && t.c == null) || c.equals(t.c));
			}
			else return false;
		}
		
		/**
		 * Creates a new three-value wrapper
		 * @param <X> the first value type
		 * @param <Y> the second value type
		 * @param <Z> the third value type
		 * @param a the first value
		 * @param b the second value
		 * @param c the third value
		 * @return a new three-value wrapper
		 */
		public static <X, Y, Z> Triple<X, Y, Z> of(X a, Y b, Z c) { return new Triple<X, Y, Z>(a, b, c); }
	}
	
	// ===================
	// FOUR-
	// ===================
	
	/**
	 * A wrapper for four values
	 * @param <A> the first value type
	 * @param <B> the second value type
	 * @param <C> the third value type
	 * @param <D> the fourth value type
	 */
	public static class Quadruple<A, B, C, D>
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
		 * The wrapped value
		 */
		public volatile C c;
		
		/**
		 * The wrapped value
		 */
		public volatile D d;
		
		/**
		 * Creates a new four-value wrapper
		 * @param a the first value
		 * @param b the second value
		 * @param c the third value
		 * @param d the fourth value
		 */
		public Quadruple(A a, B b, C c, D d) { this.a = a; this.b = b; this.c = c; this.d = d; }
		
		@Override
		public boolean equals(Object obj)
		{
			if( obj instanceof Quadruple )
			{
				Quadruple<?, ?, ?, ?> t = (Quadruple<?, ?, ?, ?>) obj;
				return ((a == null && t.a == null) || a.equals(t.a)) && ((b == null && t.b == null) || b.equals(t.b)) && ((c == null && t.c == null) || c.equals(t.c)) && ((d == null && t.d == null) || d.equals(t.d));
			}
			else return false;
		}
		
		/**
		 * Creates a new four-value wrapper
		 * @param <W> the first value type
		 * @param <X> the second value type
		 * @param <Y> the third value type
		 * @param <Z> the fourth value type
		 * @param a the first value
		 * @param b the second value
		 * @param c the third value
		 * @param d the fourth value
		 * @return a new four-value wrapper
		 */
		public static <W, X, Y, Z> Quadruple<W, X, Y, Z> of(W a, X b, Y c, Z d) { return new Quadruple<W, X, Y, Z>(a, b, c, d); }
	}
}
