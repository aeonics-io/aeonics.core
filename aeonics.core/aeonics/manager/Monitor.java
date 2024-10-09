package aeonics.manager;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.util.StringUtils;

/**
 * This manager can be used to record monitoring information.
 * <p>
 * It allows to accumulate values over time, based on a four-level deep hyerarchy. 
 * Every time a metric is updated, an individual counter is also incremented. 
 * This means that you have two values available: the accumulated total and the count.
 * </p><p>
 * The hyerarchy is usually composed by {@link Entity#category()} &gt; {@link Entity#getClass()} &gt; {@link Entity#id()} &gt; <code>metric</code>
 * in order to group the different metrics in a structured fashion. 
 * Meanwhile, all hyerarchy levels are plain string and allows for custom values.
 * </p>
 * <p>
 * Implementations may choose to have a time window system or else.
 * Implementations should make sure that it can be used in a thread safe way unless it is designed otherwise voluntarily.
 * </p>
 */
public abstract class Monitor extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Monitor.class; }
	
	public static final String UNSPECIFIED = "unspecified";
	
	/**
	 * Increments the counter of the provided entity by 1.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: "hit"</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: 0</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param entity the target entity
	 */
	public void count(Entity entity) { add(entity.category(), StringUtils.toLowerCase(entity.getClass()), entity.id(), "hit", 0); }
	
	/**
	 * Increments the counter of the provided entity by 1 and the accumulated value by the specified amount.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: "hit"</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: the provided value</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param entity the target entity
	 * @param value the accumulated value
	 */
	public void count(Entity entity, long value) { add(entity.category(), StringUtils.toLowerCase(entity.getClass()), entity.id(), "hit", value); }
	
	/**
	 * Increments the counter of the provided entity by 1 and the accumulated value by the specified amount.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: the specified metric</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: the provided value</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param entity the target entity
	 * @param metric the metric name
	 * @param value the accumulated value
	 */
	public void count(Entity entity, String metric, long value) { add(entity.category(), StringUtils.toLowerCase(entity.getClass()), entity.id(), metric, value); }
	
	/**
	 * Increments the counter of the provied entity by 1.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: "hit"</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: 0</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param <T> the entity class type
	 * @param entity the target entity
	 * @param groupBy a superclass of the entity to group metrics together
	 */
	public <T extends Entity> void count(T entity, Class<? super T> groupBy) { add(entity.category(), StringUtils.toLowerCase(groupBy), entity.id(), "hit", 0); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the specified amount.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: "hit"</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: the provided value</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param <T> the entity class type
	 * @param entity the target entity
	 * @param groupBy a superclass of the entity to group metrics together
	 * @param value the accumulated value
	 */
	public <T extends Entity> void count(T entity, Class<? super T> groupBy, long value) { add(entity.category(), StringUtils.toLowerCase(groupBy), entity.id(), "hit", value); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the specified amount.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: the specified metric</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: the provided value</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param <T> the entity class type
	 * @param entity the target entity
	 * @param groupBy a superclass of the entity to group metrics together
	 * @param metric the metric name
	 * @param value the accumulated value
	 */
	public <T extends Entity> void count(T entity, Class<? super T> groupBy, String metric, long value) { add(entity.category(), StringUtils.toLowerCase(groupBy), entity.id(), metric, value); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed milliseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ms(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: "ms"</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed milliseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ms(T entity) { return ms(entity, StringUtils.toLowerCase(entity.getClass()), "ms"); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed milliseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ms(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: the provided metric</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed milliseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @param metric the target metric
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ms(T entity, String metric) { return ms(entity, StringUtils.toLowerCase(entity.getClass()), metric); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed milliseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ms(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: "ms"</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed milliseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @param groupBy the group by type
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ms(T entity, Class<? super T> groupBy) { return ms(entity, StringUtils.toLowerCase(groupBy), "ms"); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed milliseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ms(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: the provided metric</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed milliseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @param groupBy the group by type
	 * @param metric the target metric
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ms(T entity, Class<? super T> groupBy, String metric) { return ms(entity, StringUtils.toLowerCase(groupBy), "ms"); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed milliseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ms(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by string</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: the provided metric</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed milliseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @param groupBy the group by string
	 * @param metric the target metric
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ms(T entity, String groupBy, String metric)
	{
		return new AutoCloseable() {
			private long start = System.currentTimeMillis();
			public void close() throws Exception {
				add(entity.category(), groupBy, entity.id(), metric, System.currentTimeMillis()-start);
			}
		};
	}
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed nanoseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ns(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: "ns"</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed nanoseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ns(T entity) { return ns(entity, StringUtils.toLowerCase(entity.getClass()), "ns"); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed nanoseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ns(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: the provided metric</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed nanoseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @param metric the target metric
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ns(T entity, String metric) { return ns(entity, StringUtils.toLowerCase(entity.getClass()), metric); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed nanoseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ns(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: "ns"</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed nanoseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @param groupBy the group by type
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ns(T entity, Class<? super T> groupBy) { return ns(entity, StringUtils.toLowerCase(groupBy), "ns"); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed nanoseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ns(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: the provided metric</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed nanoseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @param groupBy the group by type
	 * @param metric the target metric
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ns(T entity, Class<? super T> groupBy, String metric) { return ns(entity, StringUtils.toLowerCase(groupBy), "ns"); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the number of elapsed nanoseconds.
	 * <p>This method should be used in a <code>try...with</code> statement: <code>try( AutoCloseable ms = Monitor.ns(...) ) { ... }</code></p>
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by string</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: the provided metric</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: elapsed nanoseconds</li>
	 * </ul>
	 * @param <T> the entity type
	 * @param entity the target entity
	 * @param groupBy the group by string
	 * @param metric the target metric
	 * @return an auto closeable object to be used in a <code>try...with</code> statement
	 */
	public <T extends Entity> AutoCloseable ns(T entity, String groupBy, String metric)
	{
		return new AutoCloseable() {
			private long start = System.nanoTime();
			public void close() throws Exception {
				add(entity.category(), groupBy, entity.id(), metric, System.nanoTime()-start);
			}
		};
	}
	
	/**
	 * Increments the counter of the provied entity by 1 with the last level being the id of the related entity.
	 * This is useful to track back usage. Example: how many times did a "Fisher" entity interact with a "Lake" entity.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: related entity id</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: 0</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param entity the target entity
	 * @param related the related entity
	 */
	public void countFor(Entity entity, Entity related) { add(entity.category(), StringUtils.toLowerCase(entity.getClass()), entity.id(), related.id(), 0); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the specified amount with the last level being the id of the related entity.
	 * This is useful to track back usage. Example: how many fish did a "Fisher" entity capture in a "Lake" entity.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: related entity id</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: the provided value</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param entity the target entity
	 * @param related the related entity
	 * @param value the accumulated value
	 */
	public void countFor(Entity entity, Entity related, long value) { add(entity.category(), StringUtils.toLowerCase(entity.getClass()), entity.id(), related.id(), value); }
	
	/**
	 * Increments the counter of the provied entity by 1 with the last level being the id of the related entity.
	 * This is useful to track back usage. Example: how many times did a "Fisher" entity interact with a "Lake" entity.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: related entity id</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: 0</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param <T> the entity class type
	 * @param entity the target entity
	 * @param related the related entity
	 * @param groupBy a superclass of the entity to group metrics together
	 */
	public <T extends Entity> void countFor(T entity, Entity related, Class<? super T> groupBy) { add(entity.category(), StringUtils.toLowerCase(groupBy), entity.id(), related.id(), 0); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the specified amount with the last level being the id of the related entity.
	 * This is useful to track back usage. Example: how many fish did a "Fisher" entity capture in a "Lake" entity.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: the group by type</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: related entity id</li>
	 * </ol>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: the provided value</li>
	 * </ul>
	 * @see #add(String, String, String, String, long)
	 * @param <T> the entity class type
	 * @param entity the target entity
	 * @param related the related entity
	 * @param groupBy a superclass of the entity to group metrics together
	 * @param value the accumulated value
	 */
	public <T extends Entity> void countFor(T entity, Entity related, Class<? super T> groupBy, long value) { add(entity.category(), StringUtils.toLowerCase(groupBy), entity.id(), related.id(), value); }
	
	/**
	 * Increments the counter of the provied entity by 1 and the accumulated value by the specified amount.
	 * Levels might be {@link #UNSPECIFIED} to target a higher group by level, but must not be <code>null</code>.
	 * <p><b>Levels should ideally always be alphanumeric lower case to account for case invariant or other remote monitoring systems</b></p>
	 * <ul>
	 * <li>Counter value: +1</li>
	 * <li>Accumulated value: the provided value</li>
	 * </ul>
	 * @param level1 the first hyerarchy level (usually an entity category)
	 * @param level2 the second hyerarchy level (usually the entity class name)
	 * @param level3 the third hyerarchy level (usually the entity id)
	 * @param level4 the fourth hyerarchy level (usually a specific metric name)
	 * @param value the accumulated value
	 */
	public abstract void add(String level1, String level2, String level3, String level4, long value);
	
	/**
	 * Returns all counters and accumulated values.
	 * <ol>
	 * <li>Level 1: null</li>
	 * <li>Level 2: null</li>
	 * <li>Level 3: null</li>
	 * <li>Level 4: null</li>
	 * </ol>
	 * @return all counters and accumulated values
	 */
	public Data report() { return report(null, null, null, null); }
	
	/**
	 * Returns counters and accumulated values for all metrics of the given entity.
	 * <ol>
	 * <li>Level 1: entity category</li>
	 * <li>Level 2: entity class name</li>
	 * <li>Level 3: entity id</li>
	 * <li>Level 4: null</li>
	 * </ol>
	 * @param entity the target entity
	 * @return counters and accumulated values for all metrics of the given entity
	 */
	public Data report(Entity entity) { return report(entity.category(), StringUtils.toLowerCase(entity.getClass()), entity.id(), null); }
	
	/**
	 * Returns the counter and accumulated value of the specified metric.
	 * Levels may be <code>null</code> to specify a wildcard value for that level (meaning all).
	 * <p><b>Levels should ideally always be alphanumeric lower case to account for case invariant or other remote monitoring systems</b></p>
	 * @param level1 the first hyerarchy level (usually an entity category)
	 * @param level2 the second hyerarchy level (usually the entity class name)
	 * @param level3 the third hyerarchy level (usually the entity id)
	 * @param level4 the fourth hyerarchy level (usually a specific metric name)
	 * @return the counter and accumulated value of the specified metric
	 */
	public abstract Data report(String level1, String level2, String level3, String level4);
}
