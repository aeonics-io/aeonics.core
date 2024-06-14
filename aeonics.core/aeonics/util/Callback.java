package aeonics.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import aeonics.manager.Executor.Task;
import aeonics.util.Functions.Consumer;
import aeonics.manager.Logger;
import aeonics.manager.Executor;
import aeonics.manager.Manager;

/**
 * This class can be used to set a callback method.
 * The target class would return the Callback object and the calling node would set a handler.
 * In this way, the object reference remains in the scope of the caller.
 * <p>Note that this object will keep strong references to the handlers and therefore could cause a 
 * <b>memory leak</b> if the handler is an anonymous method that is never removed. You should
 * consider removing the handler properly in order to allow the GC to reclaim no-longer used objects.</p>
 * <pre>
 * // in this case, the anonymous handler will hold a reference MyClass
 * // and it will never be GC'd.
 * 
 * MyClass i = new MyClass();
 * callback.then((value) -&gt; { i.something(); });
 * i = null;
 * 
 * // consider removing the handler manually
 * 
 * MyClass i = new MyClass();
 * Consumer&lt;?&gt; handler = (value) -&gt; { i.something(); };
 * callback.then(handler);
 * i = null;
 * callback.remove(handler);
 * 
 * </pre>
 * @param <T> the value type
 */
public class Callback<T>
{
	/**
	 * This class specified that a callback handler should only run once and then be removed from the list of handlers.
	 * @param <U> the value type
	 */
	public static class Once<U> implements Consumer<U>
	{
		private AtomicBoolean accepted = new AtomicBoolean(false);
		private Consumer<U> target;
		
		/**
		 * Wraps a handler to run only once
		 * @param t the handler to run
		 */
		public Once(Consumer<U> t) { target = t; }
		
		public void accept(U value) throws Throwable
		{
			if( !accepted.compareAndSet(false, true) ) return;
			target.accept(value);
		}
	}
	
	/**
	 * Returns a handler that will only run once.
	 * @param <U> the callback value type
	 * @param handler the handler that should only run once
	 * @return a handler that will only run once
	 */
	public static <U> Once<U> once(Consumer<U> handler) { Objects.requireNonNull(handler); return new Once<U>(handler); }
	
	/**
	 * Returns a handler that will only run once.
	 * @param <U> the callback value type
	 * @param handler the handler that should only run once
	 * @return a handler that will only run once
	 */
	public static <U> Once<U> once(Runnable handler) { Objects.requireNonNull(handler); return new Once<U>((value) -> { handler.run(); }); }
	
	/**
	 * The current handler
	 */
	private Queue<Consumer<T>> handlers = new ConcurrentLinkedQueue<>();
	
	/**
	 * Adds a handler for this callback.
	 * <p>Consider {@link Once} if the handler should only run once.</p>
	 * @param handler the handler
	 */
	public void then(Consumer<T> handler)
	{
		this.handlers.offer(handler);
	}
	
	/**
	 * Removes the specified handler from this callback.
	 * @param handler the handler
	 */
	public void remove(Consumer<T> handler)
	{
		this.handlers.remove(handler);
	}
	
	/**
	 * Triggers the handlers with the specified value, asynchronously but sequentially
	 * @param value the value
	 * @return the task object that completes when all handlers are complete. You can {@link Task#await()} or {@link Task#link(Task)} it if needed.
	 */
	public Task<Void> trigger(T value)
	{
		// perform a shallow copy so that handlers added or removed during execution
		// do not cause troubles
		List<Consumer<T>> cache = new ArrayList<>(handlers);
		
		return Manager.of(Executor.class).normal(() ->
		{
			for( Consumer<T> h : cache )
			{
				if( h == null ) continue;
				
				try { h.accept(value); } catch(Throwable t) { Manager.of(Logger.class).warning(Callback.class, t); }
				finally { if( h instanceof Once ) remove(h); }
			}
		});
	}
}
