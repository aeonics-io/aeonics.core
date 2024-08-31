package aeonics.manager;

import java.lang.ref.WeakReference;

import aeonics.util.Callback;

/**
 * This manager is keeping track of elements that expire or should be triggered after a specified amount of time.
 * An element can only be triggered once and then it is removed from the watch list.
 * In behavior, this is similar to {@link Scheduler#in(java.util.function.Consumer, long)} but the intent is not the same.
 */
public abstract class Timeout extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Timeout.class; }
	
	/**
	 * This class englobes a target object to track and defines a {@link #delay()}.
	 * The target object is kept as a weak reference.
	 * 
	 * Once the delay is reached then the {@link #onExpire} callback will be triggered with the target.
	 * If the target is not valid anymore, then the callback is never called and this tracker instance is subject to removal from the timeout watch list.
	 * 
	 * If the delay is <code>&lt; 0</code> then this tracker is considered cancelled and is subject to removal from the timeout watch list.
	 * @see #delay()
	 * @param <T> the type of target element
	 */
	public abstract static class Tracker<T>
	{
		/**
		 * Creates a target instance with the specified element.
		 * The target element may be null in which case it is the responsibility of the caller to perform the specified logic upon {@link #onExpire()}.
		 * @param target the target element
		 */
		protected Tracker(T target) { this.target = new WeakReference<T>(target); }
		
		/**
		 * Target element
		 */
		private WeakReference<T> target;
		/**
		 * Returns the target element or null if it was garbage collected meanwhile
		 * @return the target element
		 */
		public T target() { return target.get(); }
		
		/**
		 * Returns the delay in milliseconds after which this element expires.
		 * This method may be called multiple times, pushing back the timeout value by the specified amount every time.
		 * <p>If the element has expired, this method <b>must return 0</b>. The {@link #onExpire()} handler will be called, then this tracker instance is subject to removal from the timeout watch list.</p>
		 * <p>If the returned delay is <b>negative</b>, it is considered cancelled and not expired, this tracker instance is then subject to removal from the timeout watch list without ever triggering the {@link #onExpire()} handler.</p>
		 * <p>If the value is <b>positive</b>, then it is not yet expired and will be checked again later.</p>
		 * @return the delay in milliseconds
		 */
		public abstract long delay();
		
		/**
		 * The onExpire callback
		 */
		private Callback<T, Tracker<T>> onExpire = new Callback<>(this);
		/**
		 * Gets the callback object that will be called when the element has expired.
		 * @see #delay()
		 * @return the onExpire callback
		 */
		public Callback<T, Tracker<T>> onExpire() { return onExpire; }
	}
	
	/**
	 * Adds the specified tracker to the watch list.
	 * @param <T> the tracked element type
	 * @param tracker the tracker
	 */
	public abstract <T> void watch(Tracker<T> tracker);
	
	/**
	 * Removes the specified tracker from the watch list.
	 * @param <T> the tracked element type
	 * @param tracker the tracker
	 */
	public abstract <T> void remove(Tracker<T> tracker);
	
	/**
	 * This method will re-inspect all {@link Tracker} in the watch list to determine if
	 * some elements have expired.
	 * You should not need to call this method unless in some specific circumstances.
	 */
	public abstract void refresh();
}
