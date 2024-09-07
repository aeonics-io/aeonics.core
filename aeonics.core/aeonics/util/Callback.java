package aeonics.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import aeonics.util.Functions.BiConsumer;
import aeonics.util.Functions.Supplier;
import aeonics.manager.Logger;
import aeonics.manager.Manager;

/**
 * This class can be used to set a callback method.
 * <p>The callback will call every registered handlers with the trigger value and a reference to the 
 * target object instance. This way, you do not have to keep a reference to the outer scope from a lambda expression.</p>
 * <pre>
 * 
 * // DO THIS : use the provided target
 * 
 * MyClass instance = new MyClass();
 * instance.callback.then((value, target) -&gt; { target.something(); });
 * 
 * // AVOID THIS : reference the outer scope
 * 
 * MyClass instance = new MyClass();
 * instance.callback.then((value, target) -&gt; { instance.something(); });
 * 
 * </pre>
 * <p>Consider using one of the {@link #once(BiConsumer)} variants for handlers that should execute only once.</p>
 * @param <V> the type of data that triggered this callback
 * @param <T> the type of the target class of this callback
 */
public class Callback<V, T> implements Iterable<BiConsumer<V, T>>
{
	/**
	 * The target object to which this callback is bound
	 */
	private Supplier<T> target;
	
	/**
	 * Creates a new callback for the specified target
	 * @param target the target of this callback
	 */
	public Callback(T target)
	{
		this(() -> target);
	}
	
	/**
	 * Creates a new callback for the specified target supplier.
	 * Upon trigger, the target value is fetched once before returning and cached during the handler propagation.
	 * @param target the getter for the target of this callback
	 */
	public Callback(Supplier<T> target)
	{
		this.target = Objects.requireNonNullElse(target, () -> null);
	}
	
	/**
	 * This class specified that a callback handler should only run once and then be removed from the list of handlers.
	 * @param <U> the type of data that triggered this callback
	 * @param <S> the type of the target class of this callback
	 */
	public static class Once<U, S> implements BiConsumer<U, S>
	{
		private AtomicBoolean accepted = new AtomicBoolean(false);
		private BiConsumer<U, S> handler;
		
		/**
		 * Wraps a handler to run only once.
		 * This constructor is private such that only the Callback class can construct instances
		 * using {@link Callback#once(BiConsumer)} or {@link Callback#once(Runnable)}.
		 * @param t the handler to run
		 */
		private Once(BiConsumer<U, S> t) { handler = t; }
		
		public void accept(U value, S target) throws Exception
		{
			if( !accepted.compareAndSet(false, true) ) return;
			handler.accept(value, target);
		}
	}
	
	/**
	 * Returns a handler that will only run once.
	 * @param <U> the type of data that triggered this callback
	 * @param <S> the type of the target class of this callback
	 * @param handler the handler that should only run once. You may provide a {@link BiConsumer} that will be given the callback target as second argument.
	 * @return a handler that will only run once
	 */
	public static <U, S> Once<U, S> once(BiConsumer<U, S> handler) { Objects.requireNonNull(handler); return new Once<U, S>(handler); }
	
	/**
	 * Returns a handler that will only run once.
	 * @param <U> the type of data that triggered this callback
	 * @param <S> the type of the target class of this callback
	 * @param handler the handler that should only run once
	 * @return a handler that will only run once
	 */
	public static <U, S> Once<U, S> once(aeonics.util.Functions.Runnable handler) { Objects.requireNonNull(handler); return new Once<U, S>((value, target) -> { handler.run(); }); }
	
	/**
	 * The current handler
	 */
	private Queue<BiConsumer<V, T>> handlers = new ConcurrentLinkedQueue<>();
	
	/**
	 * Adds a handler for this callback.
	 * <p>Consider {@link Once} if the handler should only run once.</p>
	 * @param handler the handler
	 */
	public void then(BiConsumer<V, T> handler)
	{
		this.handlers.offer(handler);
	}
	
	/**
	 * Removes the specified handler from this callback.
	 * @param handler the handler
	 */
	public void remove(BiConsumer<V, T> handler)
	{
		this.handlers.remove(handler);
	}
	
	/**
	 * Triggers the handlers with a null value
	 */
	public void trigger() { trigger(null); }
	
	/**
	 * Triggers the handlers with the specified value. All handlers are executed synchronously and sequentially
	 * @param value the value
	 */
	public void trigger(V value)
	{
		if( handlers.size() == 0 ) return;
		
		// perform a shallow copy so that handlers added or removed during execution
		// do not cause troubles
		List<BiConsumer<V, T>> cache = new ArrayList<>(handlers);
		try
		{
			T realTarget = target.get();
			for( BiConsumer<V, T> h : cache )
			{
				if( h == null ) continue;
				
				try
				{
					h.accept(value, realTarget);
				}
				catch(Exception t) { Manager.of(Logger.class).warning(Callback.class, t); }
				finally { if( h instanceof Once ) remove(h); }
			}
		}
		catch(Exception t)
		{
			Manager.of(Logger.class).warning(Callback.class, t);
		}
	}

	/**
	 * Provides an unmodifiable iterator over registered handlers for this callback.
	 * Note that the iterator is not thread safe in case handlers are added or removed
	 * during iteration.
	 */
	public Iterator<BiConsumer<V, T>> iterator()
	{
		return Collections.unmodifiableCollection(handlers).iterator();
	}
}
