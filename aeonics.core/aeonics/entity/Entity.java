package aeonics.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import aeonics.data.Data;
import aeonics.manager.Config;
import aeonics.template.Factory;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.Exportable;
import aeonics.util.Internal;
import aeonics.util.StringUtils;
import aeonics.util.Tuple;

/**
 * An Entity is the basic building block that composes the system. It is the definition of a class that can be instanciated at runtime
 * from a {@link Template}.
 * 
 * <p>An entity has a {@link #category()} which allows to group entities together. It also has a {@link #type()} which is the intended
 * item type regardless of the actual implementation. There should not be more than one class definition for each category-type couple.</p>
 * 
 * <p>Most of the time, the entity category will be enforced using a <code>final</code> override. The type will less likely be enforced to
 * allow subclasses to extend an existing type.</p>
 */
public class Entity implements Exportable
{
	/**
	 * Default constructor that checks if the entity has been created using a {@link Template}.
	 * @throws IllegalCallerException if the constructor has not been called through a Template
	 */
	public Entity()
	{
		StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		Optional<StackWalker.StackFrame> valid = walker.walk(frames -> 
			frames.filter(frame ->
				frame.getMethodName().equals("build") 
				&& frame.getDeclaringClass().isAssignableFrom(Template.class)
			).findFirst()
		);
		if( valid.isEmpty() )
			throw new IllegalCallerException("An entity can only be constructed from a template.");
	}
	
	/**
	 * Performs an unsafe cast to the specified subtype.
	 * <p>This method is useful when using the chained method flow with templates:</p>
	 * <pre>
	 * new Item().template().build() // &lt;-- returns a generic Entity
	 *     .&lt;MySubclass&gt;cast() // &lt;-- unsafe cast
	 *     .foo();
	 * </pre>
	 * @param <T> the return type
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T cast() { return (T) this; }
	
	/**
	 * Initializes internal properties.
	 * @param category the entity item category (should be or will be converted to lower case)
	 * @param type the entity item type (should be or will be converted to lower case)
	 * @param id the entity id (if null or blank, one will be generated)
	 * @param internal whether or not this entity is internal
	 * @hidden
	 */
	@Internal
	public final void initialize(String category, String type, String id, boolean internal)
	{
		if( this.id != null ) throw new IllegalStateException("Entity " + id() + " is already initialized");
		
		this.category = StringUtils.toLowerCase(category);
		this.type = StringUtils.toLowerCase(type);
		this.internal = internal;
		this.id = (id == null || id.isBlank() ? generateId() : id);
	}
	
	/**
	 * The entity id.
	 */
	private String id;
	
	/**
	 * Returns the unique entity id. The id is initialized when the entity is created.
	 * @return the entity id
	 */
	public String id() { return id; }
	
	/**
	 * Sets the unique entity id.
	 * @param <T> this
	 * @param value the entity id
	 * @return this
	 * @hidden
	 */
	@SuppressWarnings("unchecked")
	@Internal
	public <T extends Entity> T id(String value) { id = value; return (T) this; }
	
	/**
	 * The entity category.
	 */
	private String category;
	
	/**
	 * Returns the category of this entity. This property should be the {@link StringUtils#toLowerCase(Class)} of the entity's category type.
	 * It is used to register instances in the proper registry category.
	 * The category should always be lower case.
	 * @see Registry#of(String)
	 * @return the entity category
	 */
	public String category() { return category; }
	
	/**
	 * The entity type.
	 */
	private String type;
	
	/**
	 * Returns the type of this entity. This property should be the {@link StringUtils#toLowerCase(Class)} of the entity's supertype.
	 * It is used by the snapshot system to find the proper factory to build instances of this class.
	 * The type should always be lower case.
	 * @see Factory#get(Class)
	 * @return the entity category
	 */
	public String type() { return type; }
	
	/**
	 * Whether or not this entity is internal to the system.
	 */
	private boolean internal = false;
	
	/**
	 * Returns whether or not this entity is considered internal to the system. Internal entities are not serialized in case of a snapshot.
	 * @return whether or not this entity is internal
	 */
	public boolean internal() { return internal; }
	
	/**
	 * Sets whether or not this entity is considered internal to the system. Internal entities are not serialized in case of a snapshot.
	 * @param <T> this
	 * @param value whether or not this entity is internal
	 * @return this
	 * @hidden
	 */
	@SuppressWarnings("unchecked")
	@Internal
	public <T extends Entity> T internal(boolean value) { internal = value; return (T) this; }
	
	/**
	 * The entity name
	 */
	private String name = null;
	
	/**
	 * Returns this entity name
	 * @return this entity name
	 */
	public String name() { return name; }
	
	/**
	 * Sets this entity name.
	 * @param <T> this
	 * @param value this entity name
	 * @return this
	 */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T name(String value) { name = value; return (T) this; }
	
	/**
	 * Checks if two entities are equal.
	 * <p>Entities are considered equal if their {@link #category} and {@link #id()} match.</p>
	 * @param other the entity to compare
	 * @return true if the entities are equal
	 */
	@Override
	public boolean equals(Object other)
	{
		if( !(other instanceof Entity) ) return false;
		if( other == this ) return true;
		return ((Entity)other).category().equals(category()) && ((Entity)other).id().equals(id());
	}

	@Override
	public int hashCode() { return id().hashCode(); }
	
	/**
	 * The list of parameters and their raw value.
	 */
	private Map<String, Tuple<Data, Parameter>> parameters = new HashMap<String, Tuple<Data, Parameter>>();
	
	/**
	 * Returns the list of parameters of this entity instance.
	 * @return the list of parameters
	 * @hidden
	 */
	@Internal
	public Map<String, Tuple<Data, Parameter>> parameters() { return parameters; }
	
	/**
	 * The entity thread dependent context.
	 */
	private ThreadLocal<Data> context = null;
	
	/**
	 * Sets the current execution context surrounding the action at a given time.
	 * @param value the current context, set to null to release the context
	 * @hidden
	 */
	@Internal
	public void context(Data value)
	{
		if( value == null )
		{
			if( context != null ) context.remove();
			return;
		}
		
		if( context == null )
		{
			synchronized(this) { context = new ThreadLocal<>(); }
		}
		context.set(value);
	}
	
	/**
	 * Returns the value of the specified parameter.
	 * @param parameter the parameter name
	 * @return the parameter value or an empty data if the parameter value is not set or the parameter does not exist for this entity.
	 */
	public Data valueOf(String parameter)
	{
		Tuple<Data, Parameter> t = parameters().get(parameter);
		if( t == null ) return Data.empty();
		if( !t.b.bindable() ) return t.a;
		return t.b.resolve(t.a, null);
	}
	
	/**
	 * Sets the value of the specified parameter.
	 * <p><b>Caution:</b> using this method manually will bypass the {@link Template#modify(Data, Entity)} and will ignore the potential modifier.</p>
	 * @param parameter the parameter name
	 * @param value the new value
	 * @see Parameter#validate(Data)
	 * @throws IllegalArgumentException if the provided value does not pass parameter validation
	 */
	public void valueOf(String parameter, Data value)
	{
		Tuple<Data, Parameter> t = parameters().get(parameter);
		if( t == null )
		{
			t = Tuple.of(value, new Parameter(parameter));
			parameters().put(parameter, t);
		}
		if( t.b.validate(value) )
			t.a = value;
		else
			throw new IllegalArgumentException("Parameter validation failed");
	}
	
	/**
	 * The list of entity relationships.
	 */
	private Map<String, Tuple<List<Data>, Relationship>> relationships = new HashMap<String, Tuple<List<Data>, Relationship>>();
	
	/**
	 * Returns the list of relationships of this entity instance.
	 * @return the list of relationships
	 * @hidden
	 */
	@Internal
	public Map<String, Tuple<List<Data>, Relationship>> relationships() { return relationships; }
	
	/**
	 * Fetches all related entities from the registry along with the relation properties.
	 * @param <R> The related entity type
	 * @param name the name of the relationship
	 * @return an iterable list of all related entities and the relation properties
	 */
	public <R extends Entity> Iterable<Tuple<R, Data>> relations(String name)
	{
		Tuple<List<Data>, Relationship> r = relationships.get(name);
		Iterator<Data> i = r.a.iterator();
		return () ->
		{		
			return new Iterator<Tuple<R, Data>>()
			{
				public boolean hasNext() { return i.hasNext(); }
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public Tuple<R, Data> next()
				{
					Data data = i.next();
					return Tuple.of((R) Registry.of((Class) r.b.category()).get(data.asString("id")), data);
				}
				@Override
				public void remove() { i.remove(); }
			};
		};
	}
	
	/**
	 * Adds a relation to the provided entity
	 * @param relationship the relationship name
	 * @param entity the related entity
	 * @return this for chaining
	 * @throws IllegalArgumentException if the relationship does not exist, or the provided entity does not match the relation type, or the relationship requires some parameters
	 * @throws RuntimeException if the maximum number of relations is reached
	 */
	public Entity addRelation(String relationship, Entity entity) { return addRelation(relationship, entity, null); }
	
	/**
	 * Adds a relation to the provided entity
	 * @param relationship the relationship name
	 * @param entity the related entity name or id
	 * @return this for chaining
	 * @throws IllegalArgumentException if the relationship does not exist, or the provided entity does not match the relation type, or the relationship requires some parameters
	 * @throws RuntimeException if the maximum number of relations is reached
	 */
	public Entity addRelation(String relationship, String entity)
	{
		Tuple<List<Data>, Relationship> r = relationships.get(relationship);
		if( r == null ) throw new IllegalArgumentException("Invalid relationship name");
		return addRelation(relationship, Registry.of(StringUtils.toLowerCase(r.b.category())).get(entity), null);
	}
	
	/**
	 * Adds a relation to the provided entity
	 * @param relationship the relationship name
	 * @param entity the related entity
	 * @param parameters the relationship parameters as a data object
	 * @return this for chaining
	 * @throws IllegalArgumentException if the relationship does not exist, or the provided entity does not match the relation type, or the parameters do not pass validation
	 * @throws RuntimeException if the maximum number of relations is reached
	 */
	public Entity addRelation(String relationship, Entity entity, Data parameters)
	{
		if( entity == null ) throw new IllegalArgumentException("Invalid entity");
		Tuple<List<Data>, Relationship> r = relationships.get(relationship);
		if( r == null ) throw new IllegalArgumentException("Invalid relationship name");
		if( !StringUtils.toLowerCase(r.b.category()).equals(entity.category()) ) throw new IllegalArgumentException("Entity category mismatch");
		if( r.b.max() > 0 && r.b.max() <= r.a.size() ) throw new RuntimeException("Maximum number of relations reached");
		
		if( parameters == null || parameters.isNull() ) parameters = Data.map();
		parameters.put("id", entity.id());
		
		for( Parameter p : r.b.parameters().values() )
		{
			Data value = parameters.get(p.name());
			if( !p.validate(value) )
				throw new IllegalArgumentException("Invalid value for parameter " + p.name());
		}
		
		for( Data e : r.a )
		{
			if( e.asString("id").equals(entity.id()) )
			{
				// overwrite the existing value
				for( Map.Entry<String, Data> entry : parameters.entrySet() )
					e.put(entry.getKey(), entry.getValue());
				return this;
			}
		}
		
		r.a.add(parameters);
		return this;
	}
	
	/**
	 * Removes a relation with the specified entity
	 * @param relationship the relationship name
	 * @param entity the related entity
	 * @throws IllegalArgumentException if the relationship does not exist
	 * @throws RuntimeException if the minimum number of relations is reached
	 */
	public void removeRelation(String relationship, Entity entity)
	{
		Tuple<List<Data>, Relationship> r = relationships.get(relationship);
		if( r == null ) throw new IllegalArgumentException("Invalid relationship name");
		if( r.b.min() > 0 &&  r.b.min() >= r.a.size() ) throw new RuntimeException("Minimum number of relations reached");
		
		for( int i = 0; i < r.a.size(); i++ )
		{
			if( r.a.get(i).asString("id").equals(entity.id()) )
			{
				r.a.remove(i);
				return;
			}
		}
	}
	
	/**
	 * This method is a callback function that will be called by the {@link Config} manager when the configured value changes.
	 * The registration is usually performed by the {@link Template} that creates new instance of entities, but it can also
	 * be done manually using the {@link Config#watch(String, aeonics.entity.security.Functions.BiConsumer)} method.
	 * The default implementation of this method does nothing and shall be overridden to add support for dynamic changes.
	 * @param key the config key
	 * @param value the new value
	 */
	public void config(String key, Data value) { /* can be overridden */ }
	
	public Data export()
	{
		Data d = Data.map()
			.put("__id", id())
			.put("__name", name())
			.put("__internal", internal())
			.put("__category", category())
			.put("__type", type())
			.put("__class", getClass().getName())
			.put("__plugin", getClass().getModule().getName());
		
		for( Tuple<Data, Parameter> t : parameters.values() )
			d.put(t.b.name(), t.a == null ? t.b.defaultValue() : t.a);
		for( Tuple<List<Data>, Relationship> t : relationships.values() )
		{
			Data l = Data.list();
			for( Data x : t.a ) l.add(x);
			d.put(t.b.name(), l);
		}
		
		return d;
	}
	
	/**
	 * Source of entropy
	 */
	private static final Random random = new Random();
	
	/**
	 * Random id generator for entities
	 * @return the newly generated id
	 */
	private static String generateId()
	{
		int r = random.nextInt()>>>1;
		long t = System.nanoTime()>>>1;
		StringBuilder b = new StringBuilder("00000000-0000000000000000");
		String h = Integer.toHexString(r);
		for( int i = 0; i < h.length(); i++ ) b.setCharAt(7-i, h.charAt(h.length()-1-i));
		h = Long.toHexString(t);
		for( int i = 0; i < h.length(); i++ ) b.setCharAt(9+i, h.charAt(h.length()-1-i));
		return b.toString();
	}
}
