package aeonics.template;

import java.util.function.Supplier;

import aeonics.entity.Entity;
import aeonics.entity.Registry;

/**
 * Represents a cohesive unit combining an {@link Entity} and its associated {@link Template}.
 * 
 * <p>The Item interface acts as a blueprint for defining entities and their creation templates.
 * It encapsulates the relationship and context between an entity and its builder, ensuring
 * a structured and consistent approach to entity creation.</p>
 *
 * <p>Implementing this interface typically involves defining static inner classes for both the
 * entity and its template. These inner classes should be either public, for allowing external
 * access and subclassing, or private, to encapsulate the entity's construction within the Item
 * itself. This structure promotes a clean separation between the entity's definition and
 * its construction logic, while also providing flexibility in how entities and their templates
 * are extended and used.</p>
 *
 * <p>Best Practices:</p><ul>
 * <li>Implement the entity and template as static inner classes within the implementing class of Item.</li>
 * <li>Use public inner classes when you intend to allow subclassing.</li>
 * <li>Use private inner classes to restrict subclassing.</li>
 * <li>The template returned by {@link #template()} should create entities of the type returned by {@link #target()}.</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>
 * public class MyItem implements Item&lt;MyEntity&gt; {
 *     public static class MyEntity implements Entity { }
 *     
 *     protected Class&lt;? extends MyEntity&gt; defaultTarget() { return MyEntity.class; }
 *     protected Supplier&lt;? extends MyEntity&gt; defaultCreator() { return MyEntity::new; }
 *     protected Class&lt;? extends MyItem&gt; category() { return MyItem.class; }
 * }
 * </pre>
 * 
 * <p>Normally, the template should be immutable and should only be called once when registering in the {@link Factory}.
 * Although, it allows also to provide a partial template that should be complemented by subclasses if need be.</p>
 * 
 * <p>Example with a final template:</p>
 * <pre>
 * private static Template&lt;MyEntity&gt; template = new Template&lt;MyEntity&gt;();
 * public Template&lt;MyEntity&gt; template() { return template; }
 * </pre>
 * In this case, the template is constructed only once.
 * 
 * <p>Example with a partial template:</p>
 * <pre>
 * public Template&lt;MyEntity&gt; template() { return new MyTemplate(); }
 * </pre>
 * In this case, a new instance of the template is provided every time, this allows subclasses to complement it as such:
 * <pre>
 * public Template&lt;MyEntity&gt; template() { return super.template().summary("This is the sub-template"); }
 * </pre>
 * 
 * <p>The static method {@link #from(Class, Class, Supplier)} allows to declare an Item with a default template based on the entity class.
 * This is a way to provide an Item from within an Entity definition (reversed pattern).</p>
 * <pre>
 * public class MyEntity extends Entity {
 *     public static Item&lt;MyEntity&gt; item() {
 *         return Item.from(MyItemCategory.class, MyEntity.class, MyEntity::new);
 *     }
 * }
 * </pre>
 *
 * @param <T> the supertype of entity this Item extends
 */
@SuppressWarnings("unchecked")
public abstract class Item<T extends Entity>
{
	/**
	 * Returns the template to build the target entity.
	 * <p>This method should ultimately be used to provide the final entity template.
	 * Although, it may also provide a partial template that subclassed may complement.</p>
	 * @return the matching entity template
	 */
	public Template<? extends T> template()
	{
		Template<? extends T> t = new Template<T>(target(), type(), category())
			.creator(creator())
			.builder((data, instance) -> { Registry.add(instance); });
		return Factory.add(t);
	}
	
	/**
	 * The target entity type
	 */
	private Class<? extends T> target = null;
	
	/**
	 * Returns the target entity type
	 * @return the target entity type
	 */
	public Class<? extends T> target() { return target == null ? defaultTarget() : target; }
	
	/**
	 * Sets the final target entity type that shall be returned by the template.
	 * @param type the target entity type
	 * @return this
	 */
	public Item<T> target(Class<? extends T> type) { this.target = type; return this; }
	
	/**
	 * Returns the default target entity type.
	 * This method should be implemented by subclasses to specify the target entity type.
	 * @return the default target entity type
	 */
	protected abstract Class<? extends T> defaultTarget();
	
	/**
	 * The entity supertype
	 */
	private Class<? extends Item<? super T>> type = null;
	
	/**
	 * Returns the entity supertype
	 * @return the entity supertype
	 */
	public Class<? extends Item<? super T>> type() { return type == null ? defaultType() : type; }
	
	/**
	 * Sets the entity supertype that shall be returned by the template.
	 * @param type the entity supertype
	 * @return this
	 */
	public Item<T> type(Class<? extends Item<? super T>> type) { this.type = type; return this; }
	
	/**
	 * Returns the default entity supertype.
	 * By default, this method returns the current class.
	 * This method may be implemented by subclasses to specify the entity supertype.
	 * @return the default entity supertype
	 */
	protected Class<? extends Item<? super T>> defaultType() { return (Class<? extends Item<? super T>>) this.getClass(); }
	
	/**
	 * The target entity creator
	 */
	private Supplier<? extends T> creator = null;
	
	/**
	 * Returns the target entity creator
	 * @return the target entity creator
	 */
	public Supplier<? extends T> creator() { return creator == null ? defaultCreator() : creator; }
	
	/**
	 * Sets the final entity creator type that shall be used by the template.
	 * @param creator the entity creator
	 * @return this
	 */
	public Item<T> creator(Supplier<? extends T> creator) { this.creator = creator; return this; }
	
	/**
	 * Returns the default target entity creator.
	 * This method should be implemented by subclasses to specify the entity creator.
	 * @return the default target entity creator
	 */
	protected abstract Supplier<? extends T> defaultCreator();
	
	/**
	 * Returns the target entity category.
	 * This method should be implemented by subclasses to specify the entity category.
	 * @return the target entity category
	 */
	protected abstract Class<? extends Item<? super T>> category();
	
	/**
	 * Returns an anonymous inline item based on the target entity implementation.
	 * @param <X> the entity target type
	 * @param category the entity category. It is the entity category as registered in the Factory and Registry. See {@link Factory#of(String)} and {@link Registry#of(String)}.
	 * @param target the target entity type. It is the entity instance to create and it will be used as the entity type. See {@link Factory#get(String)}.
	 * @param creator the custom instance creator that provides new instance of the target entity.
	 * @return a new anonymous item instance
	 */
	public static <X extends Entity> Item<X> from(Class<? extends Item<? super X>> category, Class<X> target, Supplier<X> creator)
	{
		return new Item<X>()
		{
			protected Class<? extends X> defaultTarget() { return target; }
			protected Supplier<? extends X> defaultCreator() { return creator; }
			protected Class<? extends Item<? super X>> category() { return category; }
		};
	}
}
