package aeonics.entity.security;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import aeonics.data.Data;
import aeonics.entity.Registry;
import aeonics.manager.Manager;
import aeonics.manager.Security;
import aeonics.util.Exportable;

/**
 * Tokens play a role in the definition of the {@link Security}.
 * They can be used to represent a time-based access for a list of {@link Policy} scopes.
 * <p>Entities should only use tokens to perform security checks in order to identify whether or not a user is allowed
 * to perform an action. This ensures that the current access (i.e. scope) is effectively granted.</p> 
 */
public class Token implements Exportable
{
	/**
	 * Creates a new token.
	 * The start of validity is now and the token is valid for 1 hour.
	 * The token value is a {@link Security#randomHash()}.
	 * @param user the linked user, must not be null
	 * @param scope all applicable scopes
	 */
	public Token(User.Type user, String... scope)
	{
		Objects.requireNonNull(user);
		if( user == User.ANONYMOUS || user == User.SYSTEM ) throw new IllegalArgumentException("Invalid user");
		this.user = user.id();
		if( scope != null ) for( String s : scope ) this.scopes.add(s);
		this.value = Manager.of(Security.class).randomHash();
	}
	
	/**
	 * Creates a new token.
	 * The start of validity is now.
	 * The token value is a {@link Security#randomHash()}.
	 * @param user the linked user, must not be null
	 * @param validity the validity period in ms. A value &lt;0 meand unlimited.
	 * @param scope all applicable scopes
	 */
	public Token(User.Type user, long validity, String... scope)
	{
		Objects.requireNonNull(user);
		if( user == User.ANONYMOUS || user == User.SYSTEM ) throw new IllegalArgumentException("Invalid user");
		this.user = user.id();
		if( scope != null ) for( String s : scope ) this.scopes.add(s);
		this.validity = validity;
		this.value = Manager.of(Security.class).randomHash();
	}
	
	/**
	 * Creates a new token.
	 * The token value is a {@link Security#randomHash()}.
	 * @param user the linked user, must not be null
	 * @param validity the validity period in ms. A value &lt;0 meand unlimited.
	 * @param epoch the start of validity as a timestamp in ms
	 * @param scope all applicable scopes
	 */
	public Token(User.Type user, long validity, long epoch, String... scope)
	{
		Objects.requireNonNull(user);
		if( user == User.ANONYMOUS || user == User.SYSTEM ) throw new IllegalArgumentException("Invalid user");
		this.user = user.id();
		if( scope != null ) for( String s : scope ) this.scopes.add(s);
		this.validity = validity; 
		this.epoch = epoch; 
		this.value = Manager.of(Security.class).randomHash();
	}
	
	/**
	 * Creates a new token.
	 * @param user the linked user, must not be null
	 * @param validity the validity period in ms. A value &lt;0 meand unlimited.
	 * @param epoch the start of validity as a timestamp in ms
	 * @param value the opaque token value
	 * @param scope all applicable scopes
	 */
	public Token(User.Type user, long validity, long epoch, String value, String... scope)
	{
		Objects.requireNonNull(user);
		if( user == User.ANONYMOUS || user == User.SYSTEM ) throw new IllegalArgumentException("Invalid user");
		this.user = user.id();
		if( scope != null ) for( String s : scope ) this.scopes.add(s);
		this.validity = validity; 
		this.epoch = epoch; 
		this.value = value;
	}
	
	/**
	 * Restores a token from its {@link #export()} form.
	 * @param data the exported data
	 */
	public Token(Data data)
	{
		if( data == null || !data.isMap() ) throw new IllegalArgumentException("Invalid token data: " + data);
		
		this.user = data.asString("user");
		if( user.equals(User.ANONYMOUS.id()) || user.equals(User.SYSTEM.id()) ) throw new IllegalArgumentException("Invalid user");
		
		this.validity = data.asLong("validity"); 
		this.epoch = data.asLong("epoch");
		this.value = data.asString("value");
		for( Data s : data.get("scopes") )
			this.scopes.add(s.asString());
	}
	
	/**
	 * the public cryptographically secure and universally unique token value
	 */
	protected String value = null;
	
	/**
	 * Returns the public cryptographically secure and universally unique value of this token. 
	 * The value itself is considered meaningless and opaque.
	 * @return the public cryptographically secure and universally unique value of this token
	 */
	public String value() { return value; }
	
	/**
	 * The linked user id
	 */
	protected String user = null;
	
	/**
	 * Returns the {@link User} linked to this token.
	 * @return the {@link Provider} linked to this token, it may be null if the user does not exist in the registry
	 */
	public User.Type user()
	{
		User.Type u = Registry.of(User.class).get(user);
		if( u != null ) return u;
		else return Manager.of(Security.class).populate(user);
	}
	
	/**
	 * Returns true if the {@link User} linked to this token matches the provided identifier.
	 * @param user the user to check
	 * @return true if the {@link User} linked to this token matches the provided identifier
	 */
	public boolean isFor(String user) { return user != null && user.equals(this.user); }
	
	/**
	 * The start of validity point in time
	 */
	protected long epoch = System.currentTimeMillis();
	
	/**
	 * The validity from {@link #epoch} in ms.
	 * A validity &lt;= 0 means unlimited.
	 */
	protected long validity = 3_600_000; // 1h
	
	/**
	 * Returns the start of validity of this token as a timestamp in ms
	 * @return the start of validity of this token as a timestamp in ms
	 */
	public long notBefore() { return epoch; }
	
	/**
	 * Returns the end of validity of this token as a timestamp in ms
	 * @return the end of validity of this token as a timestamp in ms
	 */
	public long notAfter() { return validity <= 0 ? Long.MAX_VALUE : epoch + validity; }
	
	/**
	 * Returns whether or not this token is valid at the current moment in time
	 * @return true if this token is valid at the current moment in time
	 */
	public boolean isValid()
	{
		if( validity <= 0 ) return true;
		return (epoch + validity) > System.currentTimeMillis();
	}
	
	/**
	 * The validity from {@link #epoch} in ms.
	 * A validity &lt;= 0 means unlimited.
	 * @return the token validity 
	 */
	public long validity()
	{
		return validity;
	}
	
	/**
	 * Resets the start of validity of this token to now
	 */
	public void reset() { epoch = System.currentTimeMillis(); }
	
	/**
	 * List of scopes of this token
	 */
	protected Set<String> scopes = new HashSet<>();
	
	/**
	 * Returns the token scopes
	 * @return the token scopes
	 */
	public Set<String> scopes()
	{
		return scopes;
	}
	
	/**
	 * Returns whether or not this token applies to the specified scope
	 * @param scope the scope to check
	 * @return true if this token applies to the specified scope
	 */
	public boolean inScope(String scope)
	{
		return scopes.contains(scope);
	}
	
	/**
	 * Adds the specified scope to this token
	 * @param scope the scope to add
	 */
	public void addScope(String scope)
	{
		scopes.add(scope);
	}
	
	/**
	 * Removes the specified scope from this token
	 * @param scope the scope to remove
	 */
	public void removeScope(String scope)
	{
		scopes.remove(scope);
	}

	public Data export()
	{
		Data scopes = Data.list();
		for( String s : this.scopes ) scopes.add(s);
		
		return Data.map()
			.put("user", user)
			.put("value", value)
			.put("epoch", epoch)
			.put("validity", validity)
			.put("scopes", scopes);
	}
}
