package aeonics.manager;

import aeonics.data.Data;
import aeonics.entity.Entity;

/**
 * The Vault is meant to store sensitive information in a secure manner, typically encrypted.
 * It is possible to store and retrieve data with a (possibly empty) symmetric key or by providing an owning entity.
 * 
 * <p>In order to be encrypted and decrypted, the {@link Data#asString()} form will be used. Therefore, it is important to use flat string data.</p>
 * 
 * <p>It is recommended but not mandatory that implementations rely on {@link Security#encrypt(String, String)} to encrypt the values.</p>
 * 
 * <p><b>Security notice related to the owning entity verification:</b> 
 * The effectiveness of the implemented security model depends on the integrity and security
 * of the system managing entity creation and ID assignment. This model is recommended for environments
 * where access to the entity management system is controlled and entities are well-managed. Users
 * should be aware that this method cannot protect against all types of identity spoofing or tampering
 * if underlying assumptions about entity management are violated.</p>
 * <p>Implementations are encouraged to enforce access to the owning-entity methods only from a direct call
 * of the entity class i.e. based on the stack trace.</p>
 */
public abstract class Vault extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Vault.class; }
	
	/**
	 * Returns the data associated with the specified name.
	 * This is the same as calling {@link #get(String, String)} with a <code>null</code> key.
	 * @param name the value to retrieve 
	 * @return the secured data or null if there is no data associated with that name
	 * @throws SecurityException if the value requires a non null key or if any other underlying security exception happens
	 */
	public Data get(String name) throws SecurityException { return get(name, (String)null); }
	
	/**
	 * Returns the data associated with the specified name.
	 * The value is returned if the key matches.
	 * @param name the value to retrieve 
	 * @param key the key
	 * @return the secured data or null if there is no data associated with that name
	 * @throws SecurityException if the key does not match or if any other underlying security exception happens
	 */
	public abstract Data get(String name, String key) throws SecurityException;
	
	/**
	 * Stores the value securely but not enforced by a specific key.
	 * <p>This is the same as calling {@link #set(String, Data, String)} with a <code>null</code> key.</p>
	 * <p>The name should be unique because it will be shared globally.</p>
	 * @param name the value name
	 * @param value the value data, it will be transformed {@link Data#asString()} in order to be encrypted
	 * @throws SecurityException if you try to override an existing value with a non-null key or if any other underlying security exception happens
	 */
	public void set(String name, Data value) throws SecurityException { set(name, value, (String)null); }
	
	/**
	 * Stores the value securely and enforced by a specific key (unless null).
	 * <p>The name should be unique because it will be shared globally.</p>
	 * @param name the value name
	 * @param value the value data, it will be transformed {@link Data#asString()} in order to be encrypted
	 * @param key the key
	 * @throws SecurityException if you try to override an existing value with a different key or if any other underlying security exception happens
	 */
	public abstract void set(String name, Data value, String key) throws SecurityException;
	
	/**
	 * Removes the data associated with the specified name.
	 * This is the same as calling {@link #remove(String, String)} with a <code>null</code> key.
	 * @param name the value to retrieve 
	 * @throws SecurityException if the value requires a non null key or if any other underlying security exception happens
	 */
	public void remove(String name) throws SecurityException { remove(name, (String)null); }
	
	/**
	 * Removes the data associated with the specified name.
	 * The value can be removed if the key matches.
	 * @param name the value to retrieve 
	 * @param key the key
	 * @throws SecurityException if the key does not match or if any other underlying security exception happens
	 */
	public abstract void remove(String name, String key) throws SecurityException;
	
	// ============================
	//
	// OWNING ENTITY
	//
	// ============================
	
	/**
	 * Returns the data associated with the specified name for the specified owning entity.
	 * @param name the value to retrieve 
	 * @param owner the owning entity
	 * @return the secured data or null if there is no data associated with that name for that entity
	 * @throws SecurityException if any underlying security exception happens
	 */
	public abstract Data get(String name, Entity owner) throws SecurityException;
	
	/**
	 * Stores the value securely in a way that only the owning entity has access to it.
	 * <p>The name is scoped to the owning instance, so multiple instances can store values with the same name.</p>
	 * @param name the value name
	 * @param value the value data, it will be transformed {@link Data#asString()} in order to be encrypted
	 * @param owner the owning entity
	 * @throws SecurityException if you try to override an existing value with a different key or if any other underlying security exception happens
	 */
	public abstract void set(String name, Data value, Entity owner) throws SecurityException;
	
	/**
	 * Removes the data associated with the specified name for the specified entity.
	 * The value can be removed if the owning entity instance matches.
	 * @param name the value to retrieve 
	 * @param owner the owning entity
	 * @throws SecurityException if any underlying security exception happens
	 */
	public abstract void remove(String name, Entity owner) throws SecurityException;
}
