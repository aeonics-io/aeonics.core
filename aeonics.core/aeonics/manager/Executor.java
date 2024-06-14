package aeonics.manager;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import aeonics.entity.Origin;

/**
 * Manages the execution of tasks accross the system.
 * There are three type of execution mode: <ul>
 * <li>Normal: for regular tasks</li>
 * <li>Background: for long lasting background tasks, typically an {@link Origin} entity or another {@link Manager}</li>
 * <li>Priority: for thasks that should execute asap</li></ul>
 */
public abstract class Executor extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Executor.class; }
	
	/**
	 * Schedules the specified task to run in priority. 
	 * There are no guarantees in terms of execution speed although it shall attempt to run before other tasks.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @return the task object to eventually chain or cancel operations
	 */
	public Task<Void> priority(Runnable task) { return priority(() -> { task.run(); return null; }); }
	
	/**
	 * Schedules the specified task to run in priority. 
	 * There are no guarantees in terms of execution speed although it shall attempt to run before other tasks.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @param <T> the task return type
	 * @return the task object to eventually chain or cancel operations
	 */
	public abstract <T> Task<T> priority(Supplier<T> task);
	
	/**
	 * Schedules the specified task to run.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @return the task object to eventually chain or cancel operations
	 */
	public Task<Void> normal(Runnable task) { return normal(() -> { task.run(); return null; }); }
	
	/**
	 * Schedules the specified task to run.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @param <T> the task return type
	 * @return the task object to eventually chain or cancel operations
	 */
	public abstract <T> Task<T> normal(Supplier<T> task);
	
	/**
	 * Schedules the specified task to run in the background.
	 * This method is usually used for long-lasting tasks with a lower priority.
	 * This method should not block and should not enqueue tasks as all background tasks should run in parallel.
	 * @param task the task to run
	 * @return the task object to eventually chain or cancel operations
	 */
	public Task<Void> background(Runnable task) { return background(() -> { task.run(); return null; }); }
	
	/**
	 * Schedules the specified task to run in the background.
	 * This method is usually used for long-lasting tasks with a lower priority.
	 * This method should not block and should not enqueue tasks as all background tasks should run in parallel.
	 * @param task the task to run
	 * @param <T> the task return type
	 * @return the task object to eventually chain or cancel operations
	 */
	public abstract <T> Task<T> background(Supplier<T> task);
	
	/**
	 * Schedules the specified I/O task to run.
	 * This method is usually used for slower I/O tasks that do not consume much CPU and spend most of their time waiting for the I/O.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @return the task object to eventually chain or cancel operations
	 */
	public Task<Void> io(Runnable task) { return io(() -> { task.run(); return null; }); }
	
	/**
	 * Schedules the specified I/O task to run.
	 * This method is usually used for slower I/O tasks that do not consume much CPU and spend most of their time waiting for the I/O.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @param <T> the task return type
	 * @return the task object to eventually chain or cancel operations
	 */
	public abstract <T> Task<T> io(Supplier<T> task);
	
	/**
	 * This class represents a task that is running, is done or will run in the future.
	 * It is a simplified wrapper around {@link CompletableFuture} designed to be used by the Execute implementations.
	 * @param <T> the return type of the task
	 */
	public static class Task<T>
	{
		/**
		 * The internal future
		 */
		private final CompletableFuture<T> future;
		
		/**
		 * A reference to the executor, it may be null if the task runs in synchronous mode
		 */
		private final java.util.concurrent.Executor executor;
		
		/**
		 * Returns an already completed task.
		 * @param <T> the return type
		 * @param value the completion value
		 * @return an already completed task
		 */
		public static <T> Task<T> completed(T value) { return new Task<T>(CompletableFuture.completedFuture(value)); }
		
		/**
		 * Returns an already failed task.
		 * @param value the failure cause
		 * @return an already failed task
		 */
		public static Task<Void> failed(Throwable value) { return new Task<Void>(CompletableFuture.failedFuture(value)); }
		
		/**
		 * Wraps the spcified task to add {@link Logger} and {@link Monitor} support.
		 * @param <T> the return type
		 * @param task the task to wrap
		 * @return the wrapped task with logger and monitor support
		 */
		private static <T> Supplier<T> wrap(Supplier<T> task)
		{
			return () ->
			{
				long start = System.nanoTime();
				T value = null;
				try { value = task.get(); }
				catch(Exception e)
				{
					Manager.of(Logger.class).warning(Executor.class, e);
					throw e;
				}
				finally
				{
					long end = System.nanoTime();
					Manager.of(Monitor.class).count(Manager.of(Monitor.class), "ns", (end-start));
				}
				return value;
			};
		}
		
		/**
		 * Schedules the given task to run in the specified executor.
		 * Any {@link #then(Runnable)}, {@link #or(Runnable)} (or variants) methods will run synchronously in the same thread as the task.
		 * @param <U> the return type of the task
		 * @param task the task to run
		 * @param executor the executor in which the task should run
		 * @return the task object to eventually chain or cancel operations
		 */
		public static <U> Task<U> sync(Supplier<U> task, java.util.concurrent.Executor executor) { return new Task<U>(CompletableFuture.supplyAsync(wrap(task), executor)); }
		
		/**
		 * Schedules the given task to run in the specified executor.
		 * Any {@link #then(Runnable)}, {@link #or(Runnable)} (or variants) methods will be rescheduled asynchronously in the same executor as the task.
		 * @param <U>the return type of the task
		 * @param task the task to run
		 * @param executor the executor in which the task should run
		 * @return the task object to eventually chain or cancel operations
		 */
		public static <U> Task<U> async(Supplier<U> task, java.util.concurrent.Executor executor) { return new Task<U>(CompletableFuture.supplyAsync(wrap(task), executor), executor); }
		
		/**
		 * Creates a synchronous task without executor
		 * @see #sync(Supplier, Executor)
		 * @see #async(Supplier, Executor)
		 * @param future the internal future
		 */
		private Task(CompletableFuture<T> future) { this(future, null); }
		
		/**
		 * Creates an asynchronous task
		 * @see #sync(Supplier, Executor)
		 * @see #async(Supplier, Executor)
		 * @param future the internal future
		 * @param executor the task executor
		 */
		private Task(CompletableFuture<T> future, java.util.concurrent.Executor executor) { this.executor = executor; this.future = future; }
		
		/**
		 * Schedules another task to run upon completion of this task
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<Void> then(Runnable task)
		{
			if( executor == null ) return new Task<Void>(future.thenRun(task));
			return new Task<Void>(future.thenRunAsync(task, executor), executor);
		}
		
		/**
		 * Schedules another task to run upon completion of this task
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<Void> then(Consumer<? super T> task)
		{
			if( executor == null ) return new Task<Void>(future.thenAccept(task));
			return new Task<Void>(future.thenAcceptAsync(task, executor), executor);
		}
		
		/**
		 * Schedules another task to run upon completion of this task
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> then(Supplier<? extends U> task) { return then((t) -> { return task.get(); }); }
		
		/**
		 * Schedules another task to run upon completion of this task
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> then(Function<? super T, ? extends U> task)
		{
			if( executor == null ) return new Task<U>(future.thenApply(task));
			return new Task<U>(future.thenApplyAsync(task, executor), executor);
		}
		
		/**
		 * Schedules another task to run if this task threw an exception. If the new task returns a valid value, that value is used instead of the original task.
		 * If you wish to interrupt processing, then simply rethrow the exception.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<T> or(Runnable task)
		{
			if( executor == null ) return new Task<T>(future.handle((v, e) -> { if( e != null ) { task.run(); return null; } else return v; }));
			return new Task<T>(future.handleAsync((v, e) -> { if( e != null ) { task.run(); return null; } else return v; }, executor), executor);
		}
		
		/**
		 * Schedules another task to run if this task threw an exception. If the new task returns a valid value, that value is used instead of the original task.
		 * If you wish to interrupt processing, then simply rethrow the exception.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<T> or(Consumer<Throwable> task)
		{
			if( executor == null ) return new Task<T>(future.handle((v, e) -> { if( e != null ) { task.accept(e); return null; } else return v; }));
			return new Task<T>(future.handleAsync((v, e) -> { if( e != null ) { task.accept(e); return null; } else return v; }, executor), executor);
		}
		
		/**
		 * Schedules another task to run if this task threw an exception. If the new task returns a valid value, that value is used instead of the original task.
		 * If you wish to interrupt processing, then simply rethrow the exception.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<T> or(Supplier<? extends T> task)
		{
			if( executor == null ) return new Task<T>(future.handle((v, e) -> {if( e != null )  return task.get(); else return v; }));
			return new Task<T>(future.handleAsync((v, e) -> { if( e != null ) return task.get(); else return v; }, executor), executor);
		}
		
		/**
		 * Schedules another task to run if this task threw an exception. If the new task returns a valid value, that value is used instead of the original task.
		 * If you wish to interrupt processing, then simply rethrow the exception.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<T> or(Function<Throwable, ? extends T> task)
		{
			if( executor == null ) return new Task<T>(future.handle((v, e) -> { if( e != null ) return task.apply(e); else return v; }));
			return new Task<T>(future.handleAsync((v, e) -> { 
				if( e != null ) return task.apply(e); else return v; 
				}, executor), executor);
		}
		
		/**
		 * Link the completion of the current task to that of the specified one.
		 * This means that the returnd task will only complete (with the same result or error) when the provided one completes.
		 * This operation is non-blocking and the returned task will be pending until then.
		 * Beware that this operation may cause a <b>dead lock</b> in case linking a task that is enqueued and has no chance to execute.
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> link(Task<U> task)
		{
			return new Task<U>(future.thenCompose(v -> task.future));
		}
		
		/**
		 * Link the completion of the current task to that of the specified one.
		 * This means that the returnd task will only complete (with the same result or error) when the provided one completes.
		 * This operation is non-blocking and the returned task will be pending until then.
		 * Beware that this operation may cause a <b>dead lock</b> in case linking a task that is enqueued and has no chance to execute.
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> link(Function<T, Task<U>> task)
		{
			return new Task<U>(future.thenCompose(v -> task.apply(v).future));
		}
		
		/**
		 * Waits for the result of this task to be available and returns the value.
		 * Beware that this operation may cause a <b>dead lock</b> in case awaiting a task that is enqueued and has no chance to execute.
		 * @return the output value of the task
		 * @throws ExecutionException if the task itself threw an exception
		 * @throws CancellationException if the task was cancelled using {@link #cancel()}
		 */
		public T await() throws Exception { return future.get(); }
		
		/**
		 * Waits for all tasks to complete either successfully or with a failure.
		 * If you are interested by the result of individual tasks, you should inspect each of them after the returned task completes.
		 * Beware that this operation may cause a <b>dead lock</b> in case providing a task that is enqueued and has no chance to execute.
		 * @param tasks the list of tasks to group together
		 * @return a new task that completes when all tasks are completed
		 */
		public static Task<Void> all(List<Task<?>> tasks)
		{
			CompletableFuture<?>[] cfs = new CompletableFuture[tasks.size()];
			for( int i = 0; i < tasks.size(); i++ ) cfs[i] = tasks.get(i).future;
			return new Task<Void>(CompletableFuture.allOf(cfs));
		}
		
		/**
		 * Cancels this task if it is not done yet.
		 * This method applies to this task only and not the parent tasks.
		 */
		public void cancel() { future.cancel(true); }
	}
}
