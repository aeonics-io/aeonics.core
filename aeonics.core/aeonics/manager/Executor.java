package aeonics.manager;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import aeonics.entity.Origin;
import aeonics.template.Factory;
import aeonics.template.Template;
import aeonics.util.Internal;

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
	public Task<Void> priority(aeonics.util.Functions.Runnable task) { return priority(() -> { task.run(); return null; }); }
	
	/**
	 * Create an already resolved priority task.
	 * @param value the resolved value
	 * @return the task object to eventually chain operations
	 */
	public abstract <T> Task<T> priorityResolved(T value);
	
	/**
	 * Create a failed priority task. 
	 * @param error the cause of failure
	 * @return the task object to eventually chain operations
	 */
	public abstract Task<Void> priorityFailed(Throwable error);
	
	/**
	 * Returns a new priority task that completes when all tasks are completed (successfully or not).
	 * @param tasks the list of other tasks
	 * @return the task object to eventually chain operations
	 * @see Task#all(List)
	 */
	public abstract Task<Void> priority(List<Task<?>> tasks);
	
	/**
	 * Schedules the specified task to run in priority. 
	 * There are no guarantees in terms of execution speed although it shall attempt to run before other tasks.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @param <T> the task return type
	 * @return the task object to eventually chain or cancel operations
	 */
	public abstract <T> Task<T> priority(aeonics.util.Functions.Supplier<T> task);
	
	/**
	 * Returns true if the specified thread is a priority thread managed by this executor.
	 * @param thread the thread
	 * @return true if the specified thread is a priority thread managed by this executor
	 */
	public abstract boolean isPriority(Thread thread);
	
	/**
	 * Returns true if the current thread is a priority thread managed by this executor.
	 * @param thread the thread
	 * @return true if the current thread is a priority thread managed by this executor
	 */
	public boolean isPriority() { return isPriority(Thread.currentThread()); };
	
	/**
	 * Schedules the specified task to run.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @return the task object to eventually chain or cancel operations
	 */
	public Task<Void> normal(aeonics.util.Functions.Runnable task) { return normal(() -> { task.run(); return null; }); }
	
	/**
	 * Create an already resolved normal task.
	 * @param value the resolved value
	 * @return the task object to eventually chain operations
	 */
	public abstract <T> Task<T> normalResolved(T value);
	
	/**
	 * Create a failed normal task. 
	 * @param error the cause of failure
	 * @return the task object to eventually chain operations
	 */
	public abstract Task<Void> normalFailed(Throwable error);
	
	/**
	 * Returns a new normal task that completes when all tasks are completed (successfully or not).
	 * @param tasks the list of other tasks
	 * @return the task object to eventually chain operations
	 * @see Task#all(List)
	 */
	public abstract Task<Void> normal(List<Task<?>> tasks);
	
	/**
	 * Schedules the specified task to run.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @param <T> the task return type
	 * @return the task object to eventually chain or cancel operations
	 */
	public abstract <T> Task<T> normal(aeonics.util.Functions.Supplier<T> task);
	
	/**
	 * Returns true if the specified thread is a normal thread managed by this executor.
	 * @param thread the thread
	 * @return true if the specified thread is a normal thread managed by this executor
	 */
	public abstract boolean isNormal(Thread thread);
	
	/**
	 * Returns true if the current thread is a normal thread managed by this executor.
	 * @param thread the thread
	 * @return true if the current thread is a normal thread managed by this executor
	 */
	public boolean isNormal() { return isNormal(Thread.currentThread()); };
	
	/**
	 * Schedules the specified task to run in the background.
	 * This method is usually used for long-lasting tasks with a lower priority.
	 * This method should not block and should not enqueue tasks as all background tasks should run in parallel.
	 * @param task the task to run
	 * @return the task object to eventually chain or cancel operations
	 */
	public Task<Void> background(aeonics.util.Functions.Runnable task) { return background(() -> { task.run(); return null; }); }
	
	/**
	 * Create an already resolved background task.
	 * @param value the resolved value
	 * @return the task object to eventually chain operations
	 */
	public abstract <T> Task<T> backgroundResolved(T value);
	
	/**
	 * Create a failed background task. 
	 * @param error the cause of failure
	 * @return the task object to eventually chain operations
	 */
	public abstract Task<Void> backgroundFailed(Throwable error);
	
	/**
	 * Returns a new background task that completes when all tasks are completed (successfully or not).
	 * @param tasks the list of other tasks
	 * @return the task object to eventually chain operations
	 * @see Task#all(List)
	 */
	public abstract Task<Void> background(List<Task<?>> tasks);
	
	/**
	 * Schedules the specified task to run in the background.
	 * This method is usually used for long-lasting tasks with a lower priority.
	 * This method should not block and should not enqueue tasks as all background tasks should run in parallel.
	 * @param task the task to run
	 * @param <T> the task return type
	 * @return the task object to eventually chain or cancel operations
	 */
	public abstract <T> Task<T> background(aeonics.util.Functions.Supplier<T> task);
	
	/**
	 * Returns true if the specified thread is a background thread managed by this executor.
	 * @param thread the thread
	 * @return true if the specified thread is a background thread managed by this executor
	 */
	public abstract boolean isBackground(Thread thread);
	
	/**
	 * Returns true if the current thread is a background thread managed by this executor.
	 * @param thread the thread
	 * @return true if the current thread is a background thread managed by this executor
	 */
	public boolean isBackground() { return isBackground(Thread.currentThread()); };
	
	/**
	 * Schedules the specified I/O task to run.
	 * This method is usually used for slower I/O tasks that do not consume much CPU and spend most of their time waiting for the I/O.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @return the task object to eventually chain or cancel operations
	 */
	public Task<Void> io(aeonics.util.Functions.Runnable task) { return io(() -> { task.run(); return null; }); }
	
	/**
	 * Create an already resolved io task.
	 * @param value the resolved value
	 * @return the task object to eventually chain operations
	 */
	public abstract <T> Task<T> ioResolved(T value);
	
	/**
	 * Create a failed io task. 
	 * @param error the cause of failure
	 * @return the task object to eventually chain operations
	 */
	public abstract Task<Void> ioFailed(Throwable error);
	
	/**
	 * Returns a new io task that completes when all tasks are completed (successfully or not).
	 * @param tasks the list of other tasks
	 * @return the task object to eventually chain operations
	 * @see Task#all(List)
	 */
	public abstract Task<Void> io(List<Task<?>> tasks);
	
	/**
	 * Schedules the specified I/O task to run.
	 * This method is usually used for slower I/O tasks that do not consume much CPU and spend most of their time waiting for the I/O.
	 * This method may block or not depending on the implementation. It may reject a task or enqueue it depending on specific conditions.
	 * @param task the task to run
	 * @param <T> the task return type
	 * @return the task object to eventually chain or cancel operations
	 */
	public abstract <T> Task<T> io(aeonics.util.Functions.Supplier<T> task);
	
	/**
	 * Returns true if the specified thread is an io thread managed by this executor.
	 * @param thread the thread
	 * @return true if the specified thread is an io thread managed by this executor
	 */
	public abstract boolean isIo(Thread thread);
	
	/**
	 * Returns true if the current thread is an io thread managed by this executor.
	 * @param thread the thread
	 * @return true if the current thread is an io thread managed by this executor
	 */
	public boolean isIo() { return isIo(Thread.currentThread()); };
	
	/**
	 * Protected accessor to {@link Task#completed(Object, java.util.concurrent.Executor)}
	 * @param <T> the return type
	 * @param value the completion value
	 * @param executor the executor in which subsequent tasks should run
	 * @return an already completed task
	 */
	protected static <T> Task<T> completed(T value, java.util.concurrent.Executor executor) { return Task.completed(value, executor); }
	
	/**
	 * Protected accessor to {@link Task#failed(Throwable, java.util.concurrent.Executor)}
	 * @param value the failure cause
	 * @param executor the executor in which subsequent tasks should run
	 * @return an already failed task
	 */
	protected static Task<Void> failed(Throwable value, java.util.concurrent.Executor executor) { return Task.failed(value, executor); }
	
	/**
	 * Protected accessor to {@link Task#all(List)}
	 * @param tasks the list of tasks to group together
	 * @param executor the executor in which subsequent tasks should run
	 * @return a new task that completes when all tasks are completed
	 */
	protected static Task<Void> all(List<Task<?>> tasks, java.util.concurrent.Executor executor) { return Task.all(tasks, executor); }
	
	/**
	 * Protected accessor to {@link Task#sync(aeonics.util.Functions.Supplier, java.util.concurrent.Executor)}
	 * @param <U> the return type of the task
	 * @param task the task to run
	 * @param executor the executor in which the task should run
	 * @return the task object to eventually chain or cancel operations
	 */
	protected static <U> Task<U> sync(aeonics.util.Functions.Supplier<U> task, java.util.concurrent.Executor executor) { return Task.sync(task, executor); }
	
	/**
	 * Protected accessor to {@link Task#async(aeonics.util.Functions.Supplier, java.util.concurrent.Executor)}
	 * @param <U>the return type of the task
	 * @param task the task to run
	 * @param executor the executor in which the task should run
	 * @return the task object to eventually chain or cancel operations
	 */
	protected static <U> Task<U> async(aeonics.util.Functions.Supplier<U> task, java.util.concurrent.Executor executor) { return Task.async(task, executor); }
	
	/**
	 * This class represents a task that is running, is done or will run in the future.
	 * It is a simplified wrapper around {@link CompletableFuture} designed to be used by the Executor implementations.
	 * @param <T> the return type of the task
	 */
	public static final class Task<T>
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
		 * @param executor the executor in which subsequent tasks should run
		 * @return an already completed task
		 */
		protected static <T> Task<T> completed(T value, java.util.concurrent.Executor executor) { return new Task<T>(CompletableFuture.completedFuture(value), executor); }
		
		/**
		 * Returns a completed task that will escape the {@link Executor}'s control.
		 * Use this only as last resort.
		 * @return an already completed task
		 * @hidden
		 */
		@Internal
		public static Task<Void> unknown() { return new Task<Void>(CompletableFuture.completedFuture(null)); }
		
		/**
		 * Returns an already failed task.
		 * @param value the failure cause
		 * @param executor the executor in which subsequent tasks should run
		 * @return an already failed task
		 */
		protected static Task<Void> failed(Throwable value, java.util.concurrent.Executor executor) { return new Task<Void>(CompletableFuture.failedFuture(value), executor); }
		
		/**
		 * Wraps the spcified task to add {@link Logger} and {@link Monitor} support.
		 * @param <T> the return type
		 * @param task the task to wrap
		 * @return the wrapped task with logger and monitor support
		 */
		private static <T> Supplier<T> wrap(aeonics.util.Functions.Supplier<T> task)
		{
			return () ->
			{
				long start = System.nanoTime();
				try { return task.get(); }
				catch(Exception e)
				{
					Manager.of(Logger.class).warning(Executor.class, e);
					throw new RuntimeException(e);
				}
				finally
				{
					long end = System.nanoTime();
					Manager.of(Monitor.class).count(Manager.of(Monitor.class), "ns", (end-start));
				}
			};
		}
		
		/**
		 * Wraps the spcified task to add {@link Logger} and {@link Monitor} support.
		 * @param <T> the return type
		 * @param task the task to wrap
		 * @return the wrapped task with logger and monitor support
		 */
		private static <T, U> Function<T, U> wrap(aeonics.util.Functions.Function<T, U> task)
		{
			return (t) ->
			{
				long start = System.nanoTime();
				try { return task.apply(t); }
				catch(Exception e)
				{
					Manager.of(Logger.class).warning(Executor.class, e);
					throw new RuntimeException(e);
				}
				finally
				{
					long end = System.nanoTime();
					Manager.of(Monitor.class).count(Manager.of(Monitor.class), "ns", (end-start));
				}
			};
		}
		
		/**
		 * Wraps the spcified task to add {@link Logger} and {@link Monitor} support.
		 * @param <T> the return type
		 * @param task the task to wrap
		 * @return the wrapped task with logger and monitor support
		 */
		private static <T, U> BiFunction<T, Throwable, U> wrap(aeonics.util.Functions.BiFunction<T, Throwable, U> task)
		{
			return (t, e) ->
			{
				long start = System.nanoTime();
				try { return task.apply(t, e); }
				catch(Exception x)
				{
					Manager.of(Logger.class).warning(Executor.class, x);
					throw new RuntimeException(x);
				}
				finally
				{
					long end = System.nanoTime();
					Manager.of(Monitor.class).count(Manager.of(Monitor.class), "ns", (end-start));
				}
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
		protected static <U> Task<U> sync(aeonics.util.Functions.Supplier<U> task, java.util.concurrent.Executor executor)
		{
			return new Task<U>(CompletableFuture.supplyAsync(wrap(task), executor));
		}
		
		/**
		 * Schedules the given task to run in the specified executor.
		 * Any {@link #then(Runnable)}, {@link #or(Runnable)} (or variants) methods will be rescheduled asynchronously in the same executor as the task.
		 * @param <U>the return type of the task
		 * @param task the task to run
		 * @param executor the executor in which the task should run
		 * @return the task object to eventually chain or cancel operations
		 */
		protected static <U> Task<U> async(aeonics.util.Functions.Supplier<U> task, java.util.concurrent.Executor executor)
		{
			return new Task<U>(CompletableFuture.supplyAsync(wrap(task), executor), executor);
		}
		
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
		 * Schedules another task to run upon successful completion of this task
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<Void> then(aeonics.util.Functions.Runnable task)
		{
			return then((t) -> { task.run(); return null; });
		}
		
		/**
		 * Schedules another task to run upon successful completion of this task
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<Void> then(aeonics.util.Functions.Consumer<? super T> task)
		{
			return then((t) -> { task.accept(t); return null; });
		}
		
		/**
		 * Schedules another task to run upon successful completion of this task
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> then(aeonics.util.Functions.Supplier<? extends U> task)
		{
			return then((t) -> { return task.get(); });
		}
		
		/**
		 * Schedules another task to run upon successful completion of this task
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> then(aeonics.util.Functions.Function<? super T, ? extends U> task)
		{
			if( executor == null ) return new Task<U>(future.thenApply(wrap(task)));
			return new Task<U>(future.thenApplyAsync(wrap(task), executor), executor);
		}
		
		/**
		 * Schedules another task to run if this task threw an exception.
		 * This stage completes with a null result.
		 * If you wish to interrupt processing, then simply rethrow the exception.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<T> or(aeonics.util.Functions.Runnable task)
		{
			return or((t) -> { task.run(); return null; });
		}
		
		/**
		 * Schedules another task to run if this task threw an exception.
		 * This stage completes with a null result.
		 * If you wish to interrupt processing, then simply rethrow the exception.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<T> or(aeonics.util.Functions.Consumer<Throwable> task)
		{
			return or((t) -> { task.accept(t); return null; });
		}
		
		/**
		 * Schedules another task to run if this task threw an exception. 
		 * This stage completes with the returned value.
		 * If you wish to interrupt processing, then simply rethrow the exception.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<T> or(aeonics.util.Functions.Supplier<? extends T> task)
		{
			return or((t) -> { return task.get(); });
		}
		
		/**
		 * Schedules another task to run if this task threw an exception. 
		 * This stage completes with the returned value.
		 * If you wish to interrupt processing, then simply rethrow the exception.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<T> or(aeonics.util.Functions.Function<Throwable, ? extends T> task)
		{
			if( executor == null ) return new Task<T>(future.exceptionally(wrap(task)));
			
			// CompletableFuture.exceptionallyAsync does not exist in JRE11
			// so use handleAsync instead
						
			return new Task<T>(future.handleAsync(wrap((v, e) ->
			{
				if( e != null ) return task.apply(e);
				else return v;
			}), executor), executor);
		}
		
		/**
		 * Schedules another task to run upon completion of this task regardless of the successful or failure.
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<Void> anyway(aeonics.util.Functions.Runnable task)
		{
			return anyway((t, e) -> { task.run(); return null; });
		}
		
		/**
		 * Schedules another task to run upon completion of this task regardless of the successful or failure.
		 * <p>If the second parameter (the exception) is null, it indicates a successful operation 
		 * and the first parameter (the return value) is filled. That value may be null too.</p>
		 * <p>If the second parameter (the exception) is not null, it indicates a failed operation and 
		 * the first parameter (the return value) is null.</p>
		 * @param task the other task
		 * @return a new task object to eventually chain or cancel operations
		 */
		public Task<Void> anyway(aeonics.util.Functions.BiConsumer<T, Throwable> task)
		{
			return anyway((t, e) -> { task.accept(t, e); return null; });
		}
		
		/**
		 * Schedules another task to run upon completion of this task regardless of the successful or failure.
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> anyway(aeonics.util.Functions.Supplier<? extends U> task)
		{
			return anyway((t, e) -> { return task.get(); });
		}
		
		/**
		 * Schedules another task to run upon completion of this task regardless of the successful or failure.
		 * <p>If the second parameter (the exception) is null, it indicates a successful operation 
		 * and the first parameter (the return value) is filled. That value may be null too.</p>
		 * <p>If the second parameter (the exception) is not null, it indicates a failed operation and 
		 * the first parameter (the return value) is null.</p>
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> anyway(aeonics.util.Functions.BiFunction<T, Throwable, ? extends U> task)
		{
			if( executor == null ) return new Task<U>(future.handle(wrap(task)));
			return new Task<U>(future.handleAsync(wrap(task), executor), executor);
		}
		
		/**
		 * Link the completion of the current task to that of the specified one.
		 * <p>The linked task will only execute in case of successful completion of this one.</p>
		 * <p>If this task completes successfully and the linked task too, then the result is the linked task's value.
		 * This operation is non-blocking and the returned task will be pending until then.</p>
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
		 * <p>The linked task will only execute in case of successful completion of this one.</p>
		 * <p>If this task completes successfully and the linked task too, then the result is the linked task's value.
		 * This operation is non-blocking and the returned task will be pending until then.</p>
		 * Beware that this operation may cause a <b>dead lock</b> in case linking a task that is enqueued and has no chance to execute.
		 * @param task the other task
		 * @param <U> the task return type
		 * @return a new task object to eventually chain or cancel operations
		 */
		public <U> Task<U> link(aeonics.util.Functions.Function<T, Task<U>> task)
		{
			return new Task<U>(future.thenCompose(v -> wrap(task).apply(v).future));
		}
		
		/**
		 * Waits for the result of this task to be available and returns the value.
		 * Beware that this operation may cause a <b>dead lock</b> in case awaiting a task that is enqueued and has no chance to execute.
		 * <p>It is generally bad practice to use this method unless you know exactly what you are doing.</p>
		 * @return the output value of the task
		 * @throws CompletionException if the task itself threw an exception
		 * @throws CancellationException if the task was cancelled using {@link #cancel()}
		 */
		public T await()
		{
			return future.join();
		}
		
		/**
		 * Returns a new task that completes when all tasks are completed (successfully or not).
		 * If you are interested by the result of individual tasks, you should inspect each of them after the returned task completes.
		 * Beware that this operation may cause a <b>dead lock</b> in case providing a task that is enqueued and has no chance to execute.
		 * @param tasks the list of tasks to group together
		 * @param executor the executor in which subsequent tasks should run
		 * @return a new task that completes when all tasks are completed
		 */
		protected static Task<Void> all(List<Task<?>> tasks, java.util.concurrent.Executor executor)
		{
			CompletableFuture<?>[] cfs = new CompletableFuture[tasks.size()];
			for( int i = 0; i < tasks.size(); i++ ) cfs[i] = tasks.get(i).future;
			return new Task<Void>(CompletableFuture.allOf(cfs), executor);
		}
		
		/**
		 * Cancels this task if it is not done yet.
		 * This method applies to this task only and not the parent tasks.
		 */
		public void cancel() { future.cancel(true); }
	}

	/**
	 * Default initial executor template and entity implementation
	 */
	private static final class SynchronousExecutor extends Manager<Executor>
	{
		private static class Implementation extends Executor
		{
			public <T> Task<T> priority(aeonics.util.Functions.Supplier<T> task) { return normal(task); }
			public <T> Task<T> priorityResolved(T value) { return normalResolved(value); }
			public Task<Void> priorityFailed(Throwable error) { return normalFailed(error); }
			public Task<Void> priority(List<Task<?>> tasks) { return normal(tasks); }
			public boolean isPriority(Thread thread) { return false; }
			
			@SuppressWarnings("unchecked")
			public <T> Task<T> normal(aeonics.util.Functions.Supplier<T> task)
			{
				try
				{
					T result = task.get();
					return Task.completed(result, null);
				}
				catch(Throwable t)
				{
					return (Task<T>) Task.failed(t, null);
				}
			}
			public <T> Task<T> normalResolved(T value) { return completed(value, null); }
			public Task<Void> normalFailed(Throwable error) { return failed(error, null); }
			public Task<Void> normal(List<Task<?>> tasks) { return all(tasks, null); }
			public boolean isNormal(Thread thread) { return true; }

			public <T> Task<T> background(aeonics.util.Functions.Supplier<T> task) { return normal(task); }
			public <T> Task<T> backgroundResolved(T value) { return normalResolved(value); }
			public Task<Void> backgroundFailed(Throwable error) { return normalFailed(error); }
			public Task<Void> background(List<Task<?>> tasks) { return normal(tasks); }
			public boolean isBackground(Thread thread) { return false; }
			
			public <T> Task<T> io(aeonics.util.Functions.Supplier<T> task) { return normal(task); }
			public <T> Task<T> ioResolved(T value) { return normalResolved(value); }
			public Task<Void> ioFailed(Throwable error) { return normalFailed(error); }
			public Task<Void> io(List<Task<?>> tasks) { return normal(tasks); }
			public boolean isIo(Thread thread) { return false; }
		}

		@Override
		public Template<? extends Executor> template()
		{
			return new Template<Executor>(target(), type(), category())
				.creator(creator())
				.summary("Synchronous executor")
				.description("Executes all tasks immediately and synchronously in the calling thread.");
		}
		
		protected Class<? extends SynchronousExecutor.Implementation> defaultTarget() { return SynchronousExecutor.Implementation.class; }
		protected Supplier<? extends SynchronousExecutor.Implementation> defaultCreator() { return SynchronousExecutor.Implementation::new; }
	}
	
	/**
	 * Default synchronous executor.
	 * This is used when the actual executor is not yet available
	 * @hidden
	 */
	@Internal
	public static final Executor SYNCHRONOUS = Manager.set(Executor.class, Factory.add(new SynchronousExecutor()).create().name("Synchronous Executor"));
}
