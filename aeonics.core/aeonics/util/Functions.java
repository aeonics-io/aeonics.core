package aeonics.util;

import java.util.Objects;

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
		 * @throws Exception if an error happens
		 */
		public void run() throws Exception;
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
		 * @throws Exception if an error happens
		 */
		public R get() throws Exception;
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
		 * @throws Exception if an error happens
		 */
		public boolean test(A a) throws Exception;
		
		/**
	     * Returns a composed predicate that represents a short-circuiting logical
	     * OR of this predicate and another.  When evaluating the composed
	     * predicate, if this predicate is {@code true}, then the {@code other}
	     * predicate is not evaluated.
	     *
	     * <p>Any exceptions thrown during evaluation of either predicate are relayed
	     * to the caller; if evaluation of this predicate throws an exception, the
	     * {@code other} predicate will not be evaluated.
	     *
	     * @param other a predicate that will be logically-ORed with this
	     *              predicate
	     * @return a composed predicate that represents the short-circuiting logical
	     * OR of this predicate and the {@code other} predicate
	     * @throws NullPointerException if other is null
	     */
		default Predicate<A> or(Predicate<? super A> other)
		{
			Objects.requireNonNull(other);
	        return (a) -> test(a) || other.test(a);
		}
		
		/**
	     * Returns a composed predicate that represents a short-circuiting logical
	     * AND of this predicate and another.  When evaluating the composed
	     * predicate, if this predicate is {@code false}, then the {@code other}
	     * predicate is not evaluated.
	     *
	     * <p>Any exceptions thrown during evaluation of either predicate are relayed
	     * to the caller; if evaluation of this predicate throws an exception, the
	     * {@code other} predicate will not be evaluated.
	     *
	     * @param other a predicate that will be logically-ANDed with this
	     *              predicate
	     * @return a composed predicate that represents the short-circuiting logical
	     * AND of this predicate and the {@code other} predicate
	     * @throws NullPointerException if other is null
	     */
	    default Predicate<A> and(Predicate<? super A> other)
	    {
	        Objects.requireNonNull(other);
	        return (a) -> test(a) && other.test(a);
	    }

	    /**
	     * Returns a predicate that represents the logical negation of this
	     * predicate.
	     *
	     * @return a predicate that represents the logical negation of this
	     * predicate
	     */
	    default Predicate<A> negate()
	    {
	        return (a) -> !test(a);
	    }
	    
	    /**
	     * Returns a predicate that is the negation of the supplied predicate.
	     * This is accomplished by returning result of the calling
	     * {@code target.negate()}.
	     *
	     * @param <A>     the type of arguments to the specified predicate
	     * @param target  predicate to negate
	     *
	     * @return a predicate that negates the results of the supplied
	     *         predicate
	     *
	     * @throws NullPointerException if target is null
	     */
	    @SuppressWarnings("unchecked")
	    static <A> Predicate<A> not(Predicate<? super A> target)
	    {
	        Objects.requireNonNull(target);
	        return (Predicate<A>)target.negate();
	    }
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
		 * @throws Exception if an error happens
		 */
		public void accept(A a) throws Exception;
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
		 * @throws Exception if an error happens
		 */
		public R apply(A a) throws Exception;
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
		public default boolean test(A a) throws Exception { return test(a, null); }
		/**
		 * Evaluates this predicate on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @return true if the input arguments match the predicate
		 * @throws Exception if an error happens
		 */
		public boolean test(A a, B b) throws Exception;
		
		/**
	     * Returns a composed predicate that represents a short-circuiting logical
	     * OR of this predicate and another.  When evaluating the composed
	     * predicate, if this predicate is {@code true}, then the {@code other}
	     * predicate is not evaluated.
	     *
	     * <p>Any exceptions thrown during evaluation of either predicate are relayed
	     * to the caller; if evaluation of this predicate throws an exception, the
	     * {@code other} predicate will not be evaluated.
	     *
	     * @param other a predicate that will be logically-ORed with this
	     *              predicate
	     * @return a composed predicate that represents the short-circuiting logical
	     * OR of this predicate and the {@code other} predicate
	     * @throws NullPointerException if other is null
	     */
		default BiPredicate<A, B> or(BiPredicate<? super A, ? super B> other)
		{
			Objects.requireNonNull(other);
	        return (a, b) -> test(a, b) || other.test(a, b);
		}
		
		/**
	     * Returns a composed predicate that represents a short-circuiting logical
	     * AND of this predicate and another.  When evaluating the composed
	     * predicate, if this predicate is {@code false}, then the {@code other}
	     * predicate is not evaluated.
	     *
	     * <p>Any exceptions thrown during evaluation of either predicate are relayed
	     * to the caller; if evaluation of this predicate throws an exception, the
	     * {@code other} predicate will not be evaluated.
	     *
	     * @param other a predicate that will be logically-ANDed with this
	     *              predicate
	     * @return a composed predicate that represents the short-circuiting logical
	     * AND of this predicate and the {@code other} predicate
	     * @throws NullPointerException if other is null
	     */
	    default BiPredicate<A, B> and(BiPredicate<? super A, ? super B> other)
	    {
	        Objects.requireNonNull(other);
	        return (a, b) -> test(a, b) && other.test(a, b);
	    }

	    /**
	     * Returns a predicate that represents the logical negation of this
	     * predicate.
	     *
	     * @return a predicate that represents the logical negation of this
	     * predicate
	     */
	    default BiPredicate<A, B> negate()
	    {
	        return (a, b) -> !test(a, b);
	    }
	    
	    /**
	     * Returns a predicate that is the negation of the supplied predicate.
	     * This is accomplished by returning result of the calling
	     * {@code target.negate()}.
	     *
	     * @param <A>     the type of arguments to the specified predicate
	     * @param <B>     the type of arguments to the specified predicate
	     * @param target  predicate to negate
	     *
	     * @return a predicate that negates the results of the supplied
	     *         predicate
	     *
	     * @throws NullPointerException if target is null
	     */
	    @SuppressWarnings("unchecked")
	    static <A, B> BiPredicate<A, B> not(BiPredicate<? super A, ? super B> target)
	    {
	        Objects.requireNonNull(target);
	        return (BiPredicate<A, B>)target.negate();
	    }
	}
	
	/**
	 * Represents an operation that accepts two input arguments and returns no result.
	 * @param <A> first argument type
	 * @param <B> second argument type
	 */
	@FunctionalInterface
	public static interface BiConsumer<A, B> extends Consumer<A>
	{
		public default void accept(A a) throws Exception { accept(a, null); }
		/**
		 * Performs this operation on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @throws Exception if an error happens
		 */
		public void accept(A a, B b) throws Exception;
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
		public default R apply(A a) throws Exception { return apply(a, null); }
		/**
		 * Applies this function to the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @return the return value
		 * @throws Exception if an error happens
		 */
		public R apply(A a, B b) throws Exception;
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
		public default boolean test(A a, B b) throws Exception { return test(a, b, null); }
		/**
		 * Evaluates this predicate on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @return true if the input arguments match the predicate
		 * @throws Exception if an error happens
		 */
		public boolean test(A a, B b, C c) throws Exception;
		
		/**
	     * Returns a composed predicate that represents a short-circuiting logical
	     * OR of this predicate and another.  When evaluating the composed
	     * predicate, if this predicate is {@code true}, then the {@code other}
	     * predicate is not evaluated.
	     *
	     * <p>Any exceptions thrown during evaluation of either predicate are relayed
	     * to the caller; if evaluation of this predicate throws an exception, the
	     * {@code other} predicate will not be evaluated.
	     *
	     * @param other a predicate that will be logically-ORed with this
	     *              predicate
	     * @return a composed predicate that represents the short-circuiting logical
	     * OR of this predicate and the {@code other} predicate
	     * @throws NullPointerException if other is null
	     */
		default TriPredicate<A, B, C> or(TriPredicate<? super A, ? super B, ? super C> other)
		{
			Objects.requireNonNull(other);
	        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
		}
		
		/**
	     * Returns a composed predicate that represents a short-circuiting logical
	     * AND of this predicate and another.  When evaluating the composed
	     * predicate, if this predicate is {@code false}, then the {@code other}
	     * predicate is not evaluated.
	     *
	     * <p>Any exceptions thrown during evaluation of either predicate are relayed
	     * to the caller; if evaluation of this predicate throws an exception, the
	     * {@code other} predicate will not be evaluated.
	     *
	     * @param other a predicate that will be logically-ANDed with this
	     *              predicate
	     * @return a composed predicate that represents the short-circuiting logical
	     * AND of this predicate and the {@code other} predicate
	     * @throws NullPointerException if other is null
	     */
	    default TriPredicate<A, B, C> and(TriPredicate<? super A, ? super B, ? super C> other)
	    {
	        Objects.requireNonNull(other);
	        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
	    }

	    /**
	     * Returns a predicate that represents the logical negation of this
	     * predicate.
	     *
	     * @return a predicate that represents the logical negation of this
	     * predicate
	     */
	    default TriPredicate<A, B, C> negate()
	    {
	        return (a, b, c) -> !test(a, b, c);
	    }
	    
	    /**
	     * Returns a predicate that is the negation of the supplied predicate.
	     * This is accomplished by returning result of the calling
	     * {@code target.negate()}.
	     *
	     * @param <A>     the type of arguments to the specified predicate
	     * @param <B>     the type of arguments to the specified predicate
	     * @param <C>     the type of arguments to the specified predicate
	     * @param target  predicate to negate
	     *
	     * @return a predicate that negates the results of the supplied
	     *         predicate
	     *
	     * @throws NullPointerException if target is null
	     */
	    @SuppressWarnings("unchecked")
	    static <A, B, C> TriPredicate<A, B, C> not(TriPredicate<? super A, ? super B, ? super C> target)
	    {
	        Objects.requireNonNull(target);
	        return (TriPredicate<A, B, C>)target.negate();
	    }
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
		public default void accept(A a, B b) throws Exception { accept(a, b, null); }
		/**
		 * Performs this operation on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @throws Exception if an error happens
		 */
		public void accept(A a, B b, C c) throws Exception;
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
		public default R apply(A a, B b) throws Exception { return apply(a, b, null); }
		/**
		 * Applies this function to the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @return the return value
		 * @throws Exception if an error happens
		 */
		public R apply(A a, B b, C c) throws Exception;
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
		public default boolean test(A a, B b, C c) throws Exception { return test(a, b, c, null); }
		/**
		 * Evaluates this predicate on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @param d the input argument
		 * @return true if the input arguments match the predicate
		 * @throws Exception if an error happens
		 */
		public boolean test(A a, B b, C c, D d) throws Exception;
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
		public default void accept(A a, B b, C c) throws Exception { accept(a, b, c, null); }
		/**
		 * Performs this operation on the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @param d the input argument
		 * @throws Exception if an error happens
		 */
		public void accept(A a, B b, C c, D d) throws Exception;
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
		public default R apply(A a, B b, C c) throws Exception { return apply(a, b, c, null); }
		/**
		 * Applies this function to the given arguments.
		 * @param a the input argument
		 * @param b the input argument
		 * @param c the input argument
		 * @param d the input argument
		 * @return the return value
		 * @throws Exception if an error happens
		 */
		public R apply(A a, B b, C c, D d) throws Exception;
	}
}
