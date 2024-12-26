package aeonics.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import aeonics.data.Data;
import aeonics.manager.Config;
import aeonics.manager.Manager;
import aeonics.manager.Snapshot;
import aeonics.template.Factory;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.Callback;
import aeonics.util.CheckCaller;
import aeonics.util.Exportable;
import aeonics.util.Internal;
import aeonics.util.Json;
import aeonics.util.Snapshotable;
import aeonics.util.StringUtils;
import aeonics.util.Tuples.Tuple;

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
public class Entity implements Exportable, Snapshotable
{
	/**
	 * Default constructor that checks if the entity has been created using a {@link Template}.
	 * @throws IllegalCallerException if the constructor has not been called through a Template
	 */
	public Entity()
	{
		CheckCaller.require(Template.class, "create");
	}
	
	/**
	 * Performs an unsafe cast to the specified subtype.
	 * <p>This method is useful when using the chained method flow with templates:</p>
	 * <pre>
	 * new Item().template().create() // &lt;-- returns a generic Entity
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
	 * Fetches the template associated with this entity.
	 * @return the template associated with this entity
	 * @see Factory#of(Entity)
	 */
	public Template<Entity> template()
	{
		return Factory.of(this);
	}
	
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
	 * Returns the value of the specified parameter without any contextual information.
	 * @param parameter the parameter name
	 * @return the parameter value or an empty data if the parameter value is not set or the parameter does not exist for this entity.
	 */
	public Data valueOf(String parameter)
	{
		return valueOf(parameter, null);
	}
	
	/**
	 * Returns the value of the specified parameter with the specified contextual information.
	 * @param parameter the parameter name
	 * @param context the context used for binding
	 * @return the parameter value or an empty data if the parameter value is not set or the parameter does not exist for this entity.
	 */
	public Data valueOf(String parameter, Data context)
	{
		Tuple<Data, Parameter> t = parameters().get(parameter);
		if( t == null ) return Data.empty();
		if( t.b.format().equals(Parameter.Format.JSON) && t.a.isString() )
			t.a = Json.decode(t.a.asString());
		if( !t.b.bindable() ) return t.a;
		return t.b.resolve(t.a, context);
	}
	
	/**
	 * Sets the value of the specified parameter.
	 * <p><b>Caution:</b> using this method manually will bypass the {@link Template#update(Data, Entity)} and will ignore the potential onUpdate handlers.</p>
	 * @param parameter the parameter name
	 * @param value the new value
	 * @param <T> this entity type
	 * @return this
	 * @see Parameter#validate(Data)
	 * @throws IllegalArgumentException if the provided value does not pass parameter validation
	 */
	@SuppressWarnings("unchecked")
	public <T extends Entity> T parameter(String parameter, Object value)
	{
		Data data = Data.of(value);
		
		Tuple<Data, Parameter> t = parameters().get(parameter);
		if( t == null )
		{
			t = Tuple.of(data, new Parameter(parameter));
			parameters().put(parameter, t);
		}
		if( t.b.validate(data) )
			t.a = data;
		else
			throw new IllegalArgumentException("Parameter validation failed");
		
		return (T) this;
	}
	
	/**
	 * The list of entity relationships.
	 */
	private Map<String, Tuple<List<Data>, Relationship>> relationships = new ConcurrentHashMap<String, Tuple<List<Data>, Relationship>>();
	
	/**
	 * Returns the list of relationships of this entity instance.
	 * @return the list of relationships
	 */
	public Set<String> relationships() { return Collections.unmodifiableSet(relationships.keySet()); }
	
	/**
	 * Defines a new relationship for this entity.
	 * @param value the relationship
	 * @throws IllegalArgumentException if the relationship already exists
	 */
	public void defineRelation(Relationship value)
	{
		if( relationships.putIfAbsent(value.name(), Tuple.of(new LinkedList<Data>(), value)) != null )
			throw new IllegalArgumentException("Duplicate relationship");
	}
	
	/**
	 * Removes an existing relationship of this entity.
	 * @param value the relationship name
	 * @hidden
	 */
	@Internal
	public void removeRelation(String value)
	{
		relationships.remove(value);
	}
	
	/**
	 * Returns the number of relationship types that this entity contains.
	 * @return the number of relationship types that this entity contains
	 */
	public int countRelations()
	{
		return relationships.size();
	}
	
	/**
	 * Returns the number of active relations for the target relationship type.
	 * @param name the name of the relationship
	 * @return the number of active relations for the target relationship type.
	 */
	public int countRelations(String name)
	{
		Tuple<List<Data>, Relationship> r = relationships.get(name);
		if( r == null ) return 0;
		return r.a.size();
	}
	
	/**
	 * Fetches all related entities from the registry along with the relation properties.
	 * The returned iteratory may include null entity component.
	 * @param <R> The related entity type
	 * @param name the name of the relationship
	 * @return an iterable list of all related entities and the relation properties
	 */
	public <R extends Entity> Iterable<Tuple<R, Data>> relations(String name)
	{
		Tuple<List<Data>, Relationship> r = relationships.get(name);
		if( r == null ) return Collections.emptyList();
		
		Iterator<Data> i = r.a.iterator();
		return () ->
		{		
			return new Iterator<Tuple<R, Data>>()
			{
				public boolean hasNext() { return i.hasNext(); }
				@SuppressWarnings({ "unchecked" })
				public Tuple<R, Data> next()
				{
					Data data = i.next();
					return Tuple.of((R) Registry.of(r.b.category()).get(data.asString("id")), data);
				}
				@Override
				public void remove() { i.remove(); }
			};
		};
	}
	
	/**
	 * Fetches the first relation entity.
	 * @param <R> The related entity type
	 * @param name the name of the relationship
	 * @return the first entity found for that relation, or null if none are found
	 */
	public <R extends Entity> R firstRelation(String name)
	{
		Tuple<List<Data>, Relationship> r = relationships.get(name);
		if( r == null ) return null;
		
		Registry<?> registry = Registry.of(r.b.category());
		for( Data data : r.a )
		{
			Entity e = registry.get(data.asString("id"));
			if( e != null ) return e.cast();
		}
		return null;
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
	 * Adds a relation to the provided entity without any parameter or entity existence checks
	 * @param relationship the relationship name
	 * @param entity the related entity id
	 * @param parameters the relationship parameters as a data object
	 * @return this for chaining
	 * @throws IllegalArgumentException if the relationship does not exist
	 * @hidden
	 */
	@Internal
	public Entity addUncheckedRelation(String relationship, String entity, Data parameters)
	{
		Tuple<List<Data>, Relationship> r = relationships.get(relationship);
		if( r == null ) throw new IllegalArgumentException("Invalid relationship name");
		
		r.a.add(parameters.put("id", entity));
		return this;
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
		if( !r.b.category().equals(entity.category()) ) throw new IllegalArgumentException("Entity category mismatch");
		if( r.b.max() > 0 && r.b.max() <= r.a.size() ) throw new RuntimeException("Maximum number of relations reached");
		
		if( parameters == null || parameters.isNull() ) parameters = Data.map();
		parameters.put("id", entity.id());
		
		for( Parameter p : r.b.parameters().values() )
		{
			Data value = parameters.get(p.name());
			if( !p.validate(value) )
				throw new IllegalArgumentException("Invalid value for parameter " + p.name());
		}
		
		r.a.add(parameters);
		return this;
	}
	
	/**
	 * Returns whether or not this entity is related to the specified one
	 * @param relationship the relationship name
	 * @param entity the related entity
	 * @return true if this entity is related to the target one, false otherwise
	 * @throws IllegalArgumentException if the relationship does not exist
	 * @throws RuntimeException if the minimum number of relations is reached
	 */
	public boolean hasRelation(String relationship, Entity entity)
	{
		if( entity == null ) return false;
		
		Tuple<List<Data>, Relationship> r = relationships.get(relationship);
		if( r == null ) throw new IllegalArgumentException("Invalid relationship name");
		
		for( int i = 0; i < r.a.size(); i++ )
		{
			if( r.a.get(i).asString("id").equals(entity.id()) )
			{
				return true;
			}
		}
		return false;
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
		if( entity == null ) return;
		
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
	 * Clears all related entities for the specified relationship
	 * @param relationship the relationship name
	 */
	public void clearRelation(String relationship)
	{
		Tuple<List<Data>, Relationship> r = relationships.get(relationship);
		if( r != null ) r.a.clear();
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
	
	/**
	 * The onRemove event callback
	 */
	private Callback<Void, Entity> onRemove = new Callback<>(this);
	
	/**
	 * Event callback called when the entity is removed from the registry.
	 * This is supposed to happen only once.
	 * You should {@link Callback#then(aeonics.util.Functions.BiConsumer)} this event handler to subscribe to events.
	 * @return the onRemove event handler
	 */
	public Callback<Void, Entity> onRemove() { return onRemove; }
	
	/**
	 * The onUpdate event callback
	 */
	private Callback<Data, Entity> onUpdate = new Callback<>(this);
	
	/**
	 * Event callback called when the entity has been updated by the template.
	 * You should {@link Callback#then(aeonics.util.Functions.BiConsumer)} this event handler to subscribe to events.
	 * @return the onUpdate event handler
	 */
	public Callback<Data, Entity> onUpdate() { return onUpdate; }
	
	/**
	 * The onCreate event callback
	 */
	private Callback<Data, Entity> onCreate = new Callback<>(this);
	
	/**
	 * Event callback called when the entity has been created by the template.
	 * This is supposed to happen only once.
	 * You should {@link Callback#then(aeonics.util.Functions.BiConsumer)} this event handler to subscribe to events.
	 * @return the onCreate event handler
	 */
	public Callback<Data, Entity> onCreate() { return onCreate; }
	
	/**
	 * The default entity export implementation includes informational metadata fields
	 * as well as all declared {@link #parameters()} and all declared {@link #relationships()}.
	 * 
	 * <p>If there are potentially private or confidential data returned by the default implementation,
	 * you should override it and modify the result before returning it.</p> 
	 * 
	 * <p>Note that you may provide your own custom implementation although it may introduce inconsistencies with
	 * the frontent application in case of unexpected format or missing information. Therefore, it is always
	 * prefeable to call <code>super.export()</code> and manipulate the result instead.</p>
	 */
	public Data export()
	{
		Data d = Data.map()
			.put("id", id())
			.put("name", name())
			.put("internal", internal())
			.put("category", category())
			.put("type", type())
			.put("class", getClass().getName())
			.put("plugin", getClass().getModule().getName());
		
		Data p = Data.map();
		for( Tuple<Data, Parameter> t : parameters.values() )
			p.put(t.b.name(), t.a == null ? t.b.defaultValue() : t.a);
		d.put("parameters", p);
		
		Data r = Data.map();
		for( Tuple<List<Data>, Relationship> t : relationships.values() )
		{
			Data l = Data.list();
			for( Data x : t.a ) l.add(x);
			r.put(t.b.name(), l);
		}
		d.put("relationships", r);
		
		return d;
	}
	
	/**
	 * The default entity snapshot implementation includes the required metadata fields
	 * to be used by the {@link Template} to restore it. It also includes all declared
	 * {@link #parameters()} and all declared {@link #relationships()}.
	 * 
	 * <p>If this behavior is sufficient, you do not need to override this method.
	 * If there is additionnal data to be included in (or removed from) the output, then
	 * you should override this method, call the <code>super.snapshot()</code> and work on
	 * the returned data.</p>
	 * 
	 * <p>You may provide your own custom implementation although it may introduce inconsistencies with
	 * the rest of the system.</p>
	 * 
	 * <p>In order to safeguard the potentially private or confidential data returned by this method out of necessity,
	 * a check on the caller is performed to allow only the current {@link Snapshot} implementation.</p>
	 */
	public Data snapshot()
	{
		CheckCaller.require(Manager.of(Snapshot.class).getClass(), null);
		
		Data d = Data.map()
			.put("id", id())
			.put("name", name())
			.put("internal", internal())
			.put("category", category())
			.put("type", type())
			.put("class", getClass().getName())
			.put("plugin", getClass().getModule().getName());
		
		Data p = Data.map();
		for( Tuple<Data, Parameter> t : parameters.values() )
			p.put(t.b.name(), t.a == null ? t.b.defaultValue() : t.a);
		d.put("parameters", p);
		
		Data r = Data.map();
		for( Tuple<List<Data>, Relationship> t : relationships.values() )
		{
			Data l = Data.list();
			for( Data x : t.a ) l.add(x);
			r.put(t.b.name(), l);
		}
		d.put("relationships", r);
		
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
