package aeonics.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.entity.Registry;
import aeonics.manager.Config;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.util.Documented;
import aeonics.util.Internal;
import aeonics.util.StringUtils;
import aeonics.util.Tuple;

/**
 * A Template is used to document and create an {@link Entity} from user input.
 * It contains the definition of the expected initialization parameters and other entity-related information.
 * <p>You must provide a {@link #creator(Supplier)} to provide new instances.</p>
 * @param <T> the entity type
 * @see Parameter
 * @see Relationship
 */
@SuppressWarnings("unchecked")
public class Template<T extends Entity> implements Documented
{
	/**
	 * The custom instance creator
	 */
	private Supplier<? extends T> creator = null;
	
	/**
	 * Returns the custom instance creator
	 * @return the custom instance creator
	 */
	private Supplier<? extends T> creator() { return creator; }
	
	/**
	 * Sets the custom instance creator that privides new instance of the target entity.
	 * The returned objects must be instances of the {@link #target()} entity type.
	 * <p>This method should not attempt to initialize the instance, if specific initialization is required, you
	 * should provide a {@link #builder(BiConsumer)} and a {@link #modifier(BiConsumer)}.</p>
	 * @param <U> the template type
	 * @param creator the custom initializer
	 * @return this
	 */
	public <U extends Template<T>> U creator(Supplier<? extends T> creator) { this.creator = creator; return (U) this; }
	
	/**
	 * The list of parameters
	 */
	private Map<String, Parameter> parameters = new HashMap<>();
	
	/**
	 * Adds a parameter that the target entity expects.
	 * @param <U> the template type
	 * @param parameter the parameter definition
	 * @return this
	 */
	public <U extends Template<T>> U add(Parameter parameter) { parameters.put(parameter.name(), parameter); return (U) this; }
	
	/**
	 * Removes a parameter that the target entity expects.
	 * @param <U> the template type
	 * @param parameter the parameter name
	 * @return this
	 */
	public <U extends Template<T>> U removeParameter(String parameter) { parameters.remove(parameter); return (U) this; }
	
	/**
	 * The list of related configuration parameters that the target entity needs to watch 
	 */
	private List<String> configs = new ArrayList<>();
	
	/**
	 * Creates and registers the specified configuration parameter.
	 * <p>The configuration parameter registration will be bound to the {@link #type()} of the target entity.</p>
	 * @param <U> the template type
	 * @param config the parameter definition
	 * @return this
	 */
	public <U extends Template<T>> U config(Parameter config)
	{
		Manager.of(Config.class).declare(type(), config);
		return config(type(), config.name());
	}
	
	/**
	 * Creates and registers the specified configuration parameter.
	 * <p>The configuration parameter registration will be bound to the specified type regardless of the target entity type.</p>
	 * @param <U> the template type
	 * @param type the parameter category
	 * @param config the parameter definition
	 * @return this
	 */
	public <U extends Template<T>> U config(Class<?> type, Parameter config)
	{
		Manager.of(Config.class).declare(type, config);
		return config(type, config.name());
	}
	
	/**
	 * Registers the specified existing configuration parameter.
	 * @param <U> the template type
	 * @param type the parameter category
	 * @param name the parameter name
	 * @return this
	 */
	public <U extends Template<T>> U config(Class<?> type, String name)
	{
		this.configs.add(Config.implodeName(type, name));
		return (U) this;
	}
	
	/**
	 * Registers the specified existing configuration parameter.
	 * @param <U> the template type
	 * @param type the parameter category
	 * @param name the parameter name
	 * @return this
	 */
	public <U extends Template<T>> U config(String type, String name)
	{
		this.configs.add(Config.implodeName(type, name));
		return (U) this;
	}
	
	/**
	 * Registers the specified existing configuration parameter.
	 * @param <U> the template type
	 * @param name the parameter name
	 * @return this
	 */
	public <U extends Template<T>> U config(String name)
	{
		this.configs.add(Config.implodeName(Config.explodeName(name)));
		return (U) this;
	}
	
	/**
	 * The list of relationships
	 */
	private Map<String, Relationship> relationships = new HashMap<>();
	
	/**
	 * Adds a relationship for the target entity
	 * @param <U> the template type
	 * @param relationship the relationship definition
	 * @return this
	 */
	public <U extends Template<T>> U add(Relationship relationship) { relationships.put(relationship.name(), relationship); return (U) this; }
	
	/**
	 * Removes a relationship for the target entity
	 * @param <U> the template type
	 * @param relationship the relationship name
	 * @return this
	 */
	public <U extends Template<T>> U removeRelationship(String relationship) { relationships.remove(relationship); return (U) this; }
	
	/**
	 * The target entity implementation
	 */
	private Class<? extends T> target;
	
	/**
	 * Returns the target entity implementation
	 * @return the target entity implementation
	 */
	public Class<? extends T> target() { return target; }
	
	/**
	 * Sets the target entity implementation
	 * @param <U> the template type
	 * @param value the target entity implementation
	 * @return this
	 */
	public <U extends Template<T>> U target(Class<? extends T> value) { target = value; return (U) this; }
	
	/**
	 * The target entity category
	 */
	private String category;
	
	/**
	 * Returns the target entity category
	 * The entity category is always lower case.
	 * @return the target entity category
	 */
	public String category() { return category; }
	
	/**
	 * Sets the target entity category
	 * The entity category is always lower case.
	 * @param <U> the template type
	 * @param value the target entity category
	 * @return this
	 */
	public <U extends Template<T>> U category(String value) { category = StringUtils.toLowerCase(value); return (U) this; }
	
	/**
	 * Sets the target entity category
	 * The entity category is always lower case.
	 * @param <U> the template type
	 * @param value the target entity category
	 * @return this
	 */
	public <U extends Template<T>> U category(Class<? extends Item<? super T>> value) { category = StringUtils.toLowerCase(value); return (U) this; }
	
	/**
	 * The target entity type
	 */
	private Class<? extends Item<? super T>> type = null;
	
	/**
	 * Returns the target entity type
	 * The entity type is always lower case.
	 * @return the target entity type
	 */
	public Class<? extends Item<? super T>> type() { return type; }
	
	/**
	 * Sets the target entity type
	 * The entity type is always lower case.
	 * @param <U> the template type
	 * @param value the target entity type
	 * @return this
	 */
	public <U extends Template<T>> U type(Class<? extends Item<? super T>> value) { type = value; return (U) this; }
	
	/**
	 * Creates a new template for the specified target entity in the specified category.
	 * A default {@link Parameter} `name` is automatically added.
	 * <p>You should only use this constructor if the target is a substitute for the specified type.</p>
	 * @param target the target entity type. It is the entity instance to create. It must match the {@link #creator(Supplier)} and be the same as (or a sybtype of) the type parameter.
	 * @param type the entity supertype. It is the desired entity type as registered in the Factory. See {@link Factory#get(String)}.
	 * @param category the entity category. It is the entity category as registered in the Factory and Registry. See {@link Factory#of(String)} and {@link Registry#of(String)}.
	 */
	public Template(Class<? extends T> target, Class<? extends Item<? super T>> type, Class<? extends Item<? super T>> category)
	{
		this.target = Objects.requireNonNull(target);
		this.type = type;
		this.category = StringUtils.toLowerCase(category);
	}
	
	/**
	 * Returns the target entity name which is the {@link StringUtils#toLowerCase(Class)} of the target class.
	 * @see #target()
	 * @return the target entity name
	 */
	public String name() { return StringUtils.toLowerCase(target()); }
	
	/**
	 * The target entity summary
	 */
	private String summary = "";
	
	/**
	 * Returns the target entity summary
	 * @return the target entity summary
	 */
	public String summary() { return summary; }
	
	/**
	 * Sets the target entity summary
	 * @param <U> the template type
	 * @param value the summary
	 * @return this
	 */
	public <U extends Template<T>> U summary(String value) { summary = value; return (U) this; }
	
	/**
	 * The target entity description
	 */
	private String description = "";
	
	/**
	 * Returns the target entity description
	 * @return the target entity description
	 */
	public String description() { return description; }
	
	/**
	 * Sets the target entity description
	 * @param <U> the template type
	 * @param value the description
	 * @return this
	 */
	public <U extends Template<T>> U description(String value) { description = value; return (U) this; }
	
	/**
	 * Whether or not parameter validation is enforced
	 */
	private boolean enforceParameterValidation = true;
	
	/**
	 * Returns whether or not parameter validation is enforced
	 * @return whether or not parameter validation is enforced
	 */
	public boolean enforceParameterValidation() { return enforceParameterValidation; }
	
	/**
	 * Sets whether or not parameter validation is enforced
	 * @param <U> the template type
	 * @param value whether or not parameter validation is enforced
	 * @return this
	 */
	public <U extends Template<T>> U enforceParameterValidation(boolean value) { enforceParameterValidation = value; return (U) this; }
	
	/**
	 * The optional custom initializer
	 */
	private BiConsumer<Data, T> builder = null;
	
	/**
	 * Sets an optional custom initializer that will be given the newly created entity instance and the user input. 
	 * @param <U> the template type
	 * @param builder the custom initializer
	 * @return this
	 */
	public <U extends Template<T>> U builder(BiConsumer<Data, T> builder) { this.builder = builder; return (U) this; }
	
	/**
	 * Creates a new entity instance and sets the basic properties and relationships.
	 * If a custom builder is set, it is then called.
	 * @return an instance of the target entity
	 * @throws RuntimeException if an error happens during initialization
	 * @hidden
	 */
	@Internal
	public T build() { return build(null); }
	
	/**
	 * Creates a new entity instance and sets the basic properties and relationships.
	 * If a custom builder is set, it is then called.
	 * @param data the user input data
	 * @return an instance of the target entity
	 * @throws RuntimeException if an error happens during initialization
	 * @hidden
	 */
	@Internal
	public T build(Data data)
	{
		if( data == null ) data = Data.map();
		
		boolean internal = false;
		if( data.containsKey("__internal") ) internal = data.asBool("__internal");
		
		if( data.containsKey("__category") && !category().equals(data.asString("__category")) )
			throw new RuntimeException("Entity category mismatch");
		
		if( data.containsKey("__type") && !StringUtils.toLowerCase(type()).equals(data.asString("__type")) )
			throw new RuntimeException("Entity type mismatch");
		
		Supplier<? extends T> creator = creator();
		if( creator == null )
			throw new RuntimeException("No creator defined for this template");
		T instance = creator.get();
		if( !target().isInstance(instance) )
			throw new RuntimeException("Entity instance does not match the target type");
		instance.initialize(category(), StringUtils.toLowerCase(type()), data.asString("__id"), internal);
		if( !instance.category().equals(category()) )
			throw new RuntimeException("Entity category mismatch: " + instance.category() + " <> " + category());
		if( !instance.type().equals(StringUtils.toLowerCase(type())) )
			throw new RuntimeException("Entity type mismatch: " + instance.type() + " <> " + type());
		
		if( !data.containsKey("__name") )
			instance.name(StringUtils.toLowerCase(target()) + "-" + instance.id());
		else
			instance.name(data.asString("__name"));
				
		for( Parameter p : parameters.values() )
		{
			Data value = data.get(p.name());
			if( enforceParameterValidation() && !p.validate(value) )
				throw new RuntimeException("Invalid value for parameter " + p.name());
			
			instance.parameters().put(p.name(), Tuple.of(value, p));
		}
		
		for( Relationship r : relationships.values() )
		{
			instance.defineRelation(r);
			
			Data rels = data.containsKey(r.name()) ? data.get(r.name()) : Data.list();
			if( !rels.isList() ) rels = Data.list().add(rels);
			
			if( (r.min() > 0 && rels.size() < r.min()) || (r.max() > 0 && rels.size() > r.max()) )
				throw new RuntimeException("Invalid count for relationship " + r.name());
			
			for( Data link : rels )
			{
				for( Parameter p : r.parameters().values() )
				{
					Data value = link.get(p.name());
					if( enforceParameterValidation() && !p.validate(value) )
						throw new RuntimeException("Invalid value for parameter " + p.name() + " of relationship " + r.name());
				}
				instance.addUncheckedRelation(r.name(), link.asString("id"), link);
			}
		}
		
		if( Manager.of(Config.class) != null )
		{
			Config configManager = Manager.of(Config.class);
			for( String config : configs )
				configManager.watch(config, instance::config);
		}
		
		if( builder != null )
		{
			try { builder.accept(data, instance); }
			catch(Exception e)
			{
				e.printStackTrace();
				Manager.of(Logger.class).warning(Template.class, e);
				throw e;
			}
		}
		
		instance.onCreate().trigger(null);
		return instance;
	}
	
	/**
	 * The optional custom modifier
	 */
	private BiConsumer<Data, T> modifier = null;
	
	/**
	 * Sets an optional custom modifier that can update an existing instance given user input. 
	 * @param <U> the template type
	 * @param modifier the custom modifier
	 * @return this
	 */
	public <U extends Template<T>> U modifier(BiConsumer<Data, T> modifier) { this.modifier = modifier; return (U) this; }
	
	/**
	 * Updates an existing entity given new parameters. By default, all parameter values and relationships are updated.
	 * Although, the entity might need to be notified about the change if necessary.
	 * If a custom modifier is set, it is called after updating the parameters and relationships.
	 * If the entity cannot be updated, override this method and throw an exception instead.
	 * @param data the new user input data
	 * @param instance the existing instance to modify
	 * @return the modified instance
	 */
	public T modify(Data data, T instance)
	{
		if( data == null || !data.isMap() || data.isEmpty() || instance == null ) return instance;
		
		for( Parameter p : parameters.values() )
		{
			if( !data.containsKey(p.name()) ) continue;
			
			Data value = data.get(p.name());
			if( enforceParameterValidation() && !p.validate(value) )
				throw new RuntimeException("Invalid value for parameter " + p.name());
			
			Tuple<Data, Parameter> t = instance.parameters().get(p.name());
			if( t == null ) instance.parameters().put(p.name(), Tuple.of(value, p));
			else t.a = value;
		}
		
		for( Relationship r : relationships.values() )
		{
			if( !data.containsKey(r.name()) ) continue;
			
			instance.clearRelation(r.name());
			Data rels = data.get(r.name());
			if( !rels.isList() ) rels = Data.list().add(rels);
			
			if( (r.min() > 0 && rels.size() < r.min()) || (r.max() > 0 && rels.size() > r.max()) )
				throw new RuntimeException("Invalid count for relationship " + r.name());
			
			for( Data link : rels )
			{
				for( Parameter p : r.parameters().values() )
				{
					Data value = link.get(p.name());
					if( enforceParameterValidation() && !p.validate(value) )
						throw new RuntimeException("Invalid value for parameter " + p.name() + " of relationship " + r.name());
				}
				instance.addUncheckedRelation(r.name(), link.asString("id"), link);
			}
		}
		if( modifier != null ) modifier.accept(data, instance);
		
		instance.onUpdate().trigger(null);
		return instance;
	}

	@Override
	public Data export()
	{
		Data p = Data.map();
		for( Parameter x : parameters.values() ) p.put(x.name(), x.export());
		
		Data r = Data.map();
		for( Relationship x : relationships.values() ) r.put(x.name(), x.export());
		
		Data c = Data.map();
		for( String x : configs )
		{
			Tuple<String, String> name = Config.explodeName(x);
			Parameter definition = Manager.of(Config.class).definition(name.a, name.b); 
			c.put(x, definition == null ? null : definition.export());
		}
		
		return Documented.super.export()
			.put("__type_plugin", type().getModule().getName())
			.put("__target_plugin", target().getModule().getName())
			.put("parameters", p)
			.put("relations", r)
			.put("configs", c)
			.put("category", category())
			.put("type", StringUtils.toLowerCase(type()))
			.put("target", StringUtils.toLowerCase(target()))
			.put("enforceParameterValidation", enforceParameterValidation());
	}
}
