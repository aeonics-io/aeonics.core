package aeonics.util;

/**
 * This collection of classes are extensions to the default <code>java.util.function</code> package.
 * All these interfaces allow throwing exceptions.
 */
public class Functions 
{
	private Functions() { /* no instances */ }
	
	/**
	 * Runnable that can throw an exeption
	 */
	@FunctionalInterface
	public static interface Runnable
	{
		/**
		 * Runs.
		 * @throws Throwable if an error happens
		 */
		public void run() throws Throwable;
	}
	
	/**
	 * Supplier that can throw an exeption
	 * @param <R> the return type
	 */
	@FunctionalInterface
	public static interface Supplier<R>
	{
		/**
		 * Gets a result.
		 * @return the return value
		 * @throws Throwable if an error happens
		 */
		public R get() throws Throwable;
	}
	
	// ===================
	// UNI-
	// ===================
	
	/**
	 * Represents a predicate of one argument.
	 * @param <A> first argument type
	 */
	@FunctionalInterface
	public static interface Predicate<A>
	{
		/**
		 * Evaluates this predicate on the given argument.
		 * @param a the input argument
		 * @return true if the input argument matches the predicate
		 * @throws Throwable if an error happens
		 */
		public boolean test(A a) throws Throwable;
	}
	
	/**
	 * Represents an operation that accepts one input argument and returns no result.
	 * @param <A> first argument type
	 */
	@FunctionalInterface
	public static interface Consumer<A>
	{
		/**
		 * Performs this operation on the given argument.
		 * @param a the input argument
		 * @throws Throwable if an error happens
		 */
		public void accept(A a) throws Throwable;
	}
	
	/**
	 * Represents a function that accepts one argument and produces a result.
	 * @param <A> first argument type
	 * @param <R> the return type
	 */
	@FunctionalInterface
	public static interface Function<A, R>
	{
		/**
		 * Applies this function to the given argument.
		 * @param a the input argument
		 * @return the return value
		 * @throws Throwable if an error happens
		 */
		public R apply(A a) throws Throwable;
	}
	
	// ===================
	// BI-
	// ===================
	
	/**
	 * Represents a predicate of two arguments.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 */
	@FunctionalInterface
	public static interface BiPredicate<A, B> extends Predicate<A>
	{
		public default boolean test(A a) throws Throwable { return test(a, null); }
		/**
		 * Evaluates this predicate on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @return true if the input arguments match the predicate
		 * @throws Throwable if an error happens
		 */
		public boolean test(A a, B b) throws Throwable;
	}
	
	/**
	 * Represents an operation that accepts two input arguments and returns no result.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 */
	@FunctionalInterface
	public static interface BiConsumer<A, B> extends Consumer<A>
	{
		public default void accept(A a) throws Throwable { accept(a, null); }
		/**
		 * Performs this operation on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @throws Throwable if an error happens
		 */
		public void accept(A a, B b) throws Throwable;
	}
	
	/**
	 * Represents a function that accepts two arguments and produces a result.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 * @param <R> the return type
	 */
	@FunctionalInterface
	public static interface BiFunction<A, B, R> extends Function<A, R>
	{
		public default R apply(A a) throws Throwable { return apply(a, null); }
		/**
		 * Applies this function to the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @return the return value
		 * @throws Throwable if an error happens
		 */
		public R apply(A a, B b) throws Throwable;
	}
	
	// ===================
	// TRI-
	// ===================
	
	/**
	 * Represents a predicate of three argument.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 * @param <C> thrid argument type
	 */
	@FunctionalInterface
	public static interface TriPredicate<A, B, C> extends BiPredicate<A, B>
	{
		public default boolean test(A a, B b) throws Throwable { return test(a, b, null); }
		/**
		 * Evaluates this predicate on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @return true if the input arguments match the predicate
		 * @throws Throwable if an error happens
		 */
		public boolean test(A a, B b, C c) throws Throwable;
	}
	
	/**
	 * Represents an operation that accepts three input arguments and returns no result.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 * @param <C> third argument type
	 */
	@FunctionalInterface
	public static interface TriConsumer<A, B, C> extends BiConsumer<A, B>
	{
		public default void accept(A a, B b) throws Throwable { accept(a, b, null); }
		/**
		 * Performs this operation on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @throws Throwable if an error happens
		 */
		public void accept(A a, B b, C c) throws Throwable;
	}
	
	/**
	 * Represents a function that accepts three arguments and produces a result.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 * @param <C> third argument type
	 * @param <R> the return type
	 */
	@FunctionalInterface
	public static interface TriFunction<A, B, C, R> extends BiFunction<A, B, R>
	{
		public default R apply(A a, B b) throws Throwable { return apply(a, b, null); }
		/**
		 * Applies this function to the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @return the return value
		 * @throws Throwable if an error happens
		 */
		public R apply(A a, B b, C c) throws Throwable;
	}
	
	// ===================
	// QUADRI-
	// ===================
	
	/**
	 * Represents a predicate of four argument.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 * @param <C> third argument type
	 * @param <D> fourth argument type
	 */
	@FunctionalInterface
	public static interface QuadriPredicate<A, B, C, D> extends TriPredicate<A, B, C>
	{
		public default boolean test(A a, B b, C c) throws Throwable { return test(a, b, c, null); }
		/**
		 * Evaluates this predicate on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @param d the input argument
		 * @return true if the input arguments match the predicate
		 * @throws Throwable if an error happens
		 */
		public boolean test(A a, B b, C c, D d) throws Throwable;
	}
	
	/**
	 * Represents an operation that accepts four input arguments and returns no result.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 * @param <C> third argument type
	 * @param <D> fourth argument type
	 */
	@FunctionalInterface
	public static interface QuadriConsumer<A, B, C, D> extends TriConsumer<A, B, C>
	{
		public default void accept(A a, B b, C c) throws Throwable { accept(a, b, c, null); }
		/**
		 * Performs this operation on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @param d the input argument
		 * @throws Throwable if an error happens
		 */
		public void accept(A a, B b, C c, D d) throws Throwable;
	}
	
	/**
	 * Represents a function that accepts four arguments and produces a result.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 * @param <C> third argument type
	 * @param <D> fourth argument type
	 * @param <R> the return type
	 */
	@FunctionalInterface
	public static interface QuadriFunction<A, B, C, D, R> extends TriFunction<A, B, C, R>
	{
		public default R apply(A a, B b, C c) throws Throwable { return apply(a, b, c, null); }
		/**
		 * Applies this function to the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @param d the input argument
		 * @return the return value
		 * @throws Throwable if an error happens
		 */
		public R apply(A a, B b, C c, D d) throws Throwable;
	}
}
