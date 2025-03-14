package aeonics.manager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import aeonics.data.Data;
import aeonics.entity.Message;
import aeonics.entity.Registry;
import aeonics.entity.security.Policy;
import aeonics.entity.security.Provider;
import aeonics.entity.security.Role;
import aeonics.entity.security.Token;
import aeonics.entity.security.User;

/**
 * Manages the global security settings in the system and defines the common
 * behavior of the different security requirements.
 */
public abstract class Security extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Security.class; }
	
	/**
	 * Returns the current active instance of this manager type.
	 * @return the current active instance of this manager type
	 */
	public static Security get() { return Manager.of(Security.class); }
	
	/**
	 * Produces a random meaningless and opaque hash.
	 * The goal of this function is to serve as source of entropy to generate cryptographycally-sufficient long random string values.
	 * <p>This is a good candidate to generate a {@link Token}.</p>
	 * @return a random hash in hex format
	 */
	public abstract String randomHash();
	
	/**
	 * Produces a strong cryptographic hash of the target input data.
	 * The hash is not salted.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input stream to hash
	 * @return the hashed value in hex format
	 */
	public abstract String hash(InputStream value);
	
	/**
	 * Produces a strong cryptographic hash of the input value.
	 * The hash is not salted. Always prefer the {@link #hash(String, String)} version.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to hash
	 * @return the hashed value in hex format
	 */
	public String hash(String value) { return hash(value == null ? null : value.getBytes(StandardCharsets.ISO_8859_1), null); }
	
	/**
	 * Produces a strong cryptographic hash of the input value.
	 * The hash is not salted. Always prefer the {@link #hash(String, String)} version.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to hash
	 * @return the hashed value in hex format
	 */
	public String hash(byte[] value) { return hash(value, null); }
	
	/**
	 * Produces a strong cryptographic hash of the input value.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to hash
	 * @param salt the salt value (may be null but it is not recommended)
	 * @return the hashed value in hex format
	 */
	public String hash(String value, String salt) { return hash(value == null ? null : value.getBytes(StandardCharsets.ISO_8859_1), salt == null ? null : salt.getBytes(StandardCharsets.ISO_8859_1)); }
	
	/**
	 * Produces a strong cryptographic hash of the input value.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to hash
	 * @param salt the salt value (may be null but it is not recommended)
	 * @return the hashed value in hex format
	 */
	public abstract String hash(byte[] value, byte[] salt);
	
	/**
	 * Encrypts the given value with a strong symmetric key encryption method.
	 * Use {@link #decrypt(String, String)} to recover the original value.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to encrypt
	 * @param key the symmetric key
	 * @return the encrypted value in base64 format
	 * @throws SecurityException if any error happens during encryption. For security purpose, the cause of the exception is discarded. 
	 */
	public String encrypt(String value, String key) { return encrypt(value == null ? null : value.getBytes(StandardCharsets.ISO_8859_1), key == null ? null : key.getBytes(StandardCharsets.ISO_8859_1)); }
	
	/**
	 * Encrypts the given value with a strong symmetric key encryption method.
	 * Use {@link #decrypt(String, byte[])} to recover the original value.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input to encrypt
	 * @param key the symmetric key
	 * @return the encrypted value in base64 format
	 * @throws SecurityException if any error happens during encryption. For security purpose, the cause of the exception is discarded.
	 */
	public abstract String encrypt(byte[] value, byte[] key);
	
	/**
	 * Decrypts the given value to recover the original text that was encrypted with {@link #encrypt(String, String)}.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the encrypted text to decrypt in base64 format
	 * @param key the symmetric key
	 * @return the original text
	 * @throws SecurityException if any error happens during decryption, including a key mismatch. For security purpose, the cause of the exception is discarded.
	 */
	public String decrypt(String value, String key) { return new String(decrypt(value, key == null ? null : key.getBytes(StandardCharsets.ISO_8859_1))); }
	
	/**
	 * Decrypts the given value to recover the original value that was encrypted with {@link #encrypt(String, String)}.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the encrypted value to decrypt in base64 format
	 * @param key the symmetric key
	 * @return the original value
	 * @throws SecurityException if any error happens during decryption, including a key mismatch. For security purpose, the cause of the exception is discarded.
	 */
	public abstract byte[] decrypt(String value, byte[] key);
	
	/**
	 * Returns the list of {@link Provider} that may be able to authenticate the specified user.
	 * <p>This method is used to allow the user to choose an authentication provider.</p>
	 * <p>It is not guaranteed that all returned providers can successfully authenticate the user, 
	 * this is more of an indication as per {@link Provider.Type#supports(String)}.</p>
	 * @param user the user login
	 * @return the list of providers that can support authenticating the provided user
	 */
	public List<Provider.Type> providers(String user)
	{
		return Registry.of(Provider.class).get((p) ->
		{
			try
			{
				return p.active() && p.supports(user);
			}
			catch(Exception t)
			{
				Manager.of(Logger.class).warning(Security.class, t);
				return false;
			}
		});
	}
	
	/**
	 * Authenticate a user based on opaque authentication credentials. This is the prefered way to authenticate users.
	 * <p>This method calls {@link Provider.Type#authenticate(Data)} on the selected provider.</p>
	 * <p>This method has the ability to intercept the authentication request and perform auditing, filtering or any other type of logic.</p>
	 * @param provider the target identity provider
	 * @param context the authentication context that shall be used by the provider to authenticate the user
	 * @return the authenticated user instance or {@link User#ANONYMOUS} if no user matches
	 */
	public User.Type authenticate(Provider.Type provider, Data context)
	{
		if( provider == null || !provider.active() ) return User.ANONYMOUS;
		
		try
		{
			User.Type user = provider.authenticate(context);
			if( user == null ) return User.ANONYMOUS;
			return user;
		}
		catch(Exception t)
		{
			Manager.of(Logger.class).warning(Security.class, t);
		}
		return User.ANONYMOUS;
	}
	
	/**
	 * Checks if the user is granted usage of the specified scope with the given context.
	 * The final decision is <pre>!isExplicitlyDenied(user, scope, context) &amp;&amp; isExplicitlyAllowed(user, scope, context)</pre>
	 * The {@link User#SYSTEM} is always granted without checking.
	 * <p>The scope and context are not defined and can be anything, the behavior will depend on applicable {@link Policy}</p>
	 * <p>In general, for the internal routing mechanics, the scope will be "topic" and the context will contain a {@link Message} entity.</p>
	 * @param user the user, must not be null
	 * @param scope the scope of interest, must not be null
	 * @param context the context data to check against rules and policies, may be null
	 * @return true if the user is allowed given the scope and context
	 */
	public boolean granted(User.Type user, String scope, Data context)
	{
		if( user == null ) return false;
		if( user == User.SYSTEM ) return true;
		if( user.hasRole(Role.SUPERADMIN) ) return true;
		
		return !isExplicitlyDenied(user, scope, context) && isExplicitlyAllowed(user, scope, context);
	}
	
	/**
	 * Evaluates if the user is explicitly denied access.
	 * An explicit deny (return true) is different from a non-explicit deny (return false) in that the former says "I am strictly against it" and the latter is more "I do not have any objections".
	 * <p>The scope and context are not defined and can be anything, the behavior will depend on applicable {@link Policy}</p>
	 * <p>In general, the scope will be "topic" and the context will contain a {@link Message} entity.</p>
	 * @param user the user, must not be null
	 * @param scope the scope of interest, must not be null
	 * @param context the context data to check against rules and policies, may be null
	 * @return true if the user is explicitly denied given the scope and context
	 */
	public boolean isExplicitlyDenied(User.Type user, String scope, Data context)
	{
		if( user == null || scope == null ) return false;
		if( user == User.SYSTEM ) return false;
		if( user.hasRole(Role.SUPERADMIN) ) return false;
		
		try
		{
			for( Policy.Type policy : Registry.of(Policy.class) )
			{
				if( !policy.valueOf("scope").equals(scope) ) continue;
				if( policy.isDenied(user, context) )
					return true;
			}
			return false;
		}
		catch(Exception e)
		{
			Manager.of(Logger.class).warning(Security.class, e);
		}
		return false;
	}
	
	/**
	 * Evaluates if the user is explicitly allowed access.
	 * An explicit allow (return true) is different from a non-explicit allow (return false) in that the former says "Yes you can" and the latter is more "I don't know".
	 * <p>The scope and context are not defined and can be anything, the behavior will depend on applicable {@link Policy}</p>
	 * <p>In general, the scope will be "topic" and the context will contain a {@link Message} entity.</p>
	 * @param user the user, must not be null
	 * @param scope the scope of interest, must not be null
	 * @param context the context data to check against rules and policies, may be null
	 * @return true if the user is explicitly allowed given the scope and context
	 */
	public boolean isExplicitlyAllowed(User.Type user, String scope, Data context)
	{
		if( user == null || scope == null ) return false;
		if( user == User.SYSTEM ) return true;
		if( user.hasRole(Role.SUPERADMIN) ) return true;
		
		try
		{
			for( Policy.Type policy : Registry.of(Policy.class) )
			{
				if( !policy.valueOf("scope").equals(scope) ) continue;
				if( policy.isAllowed(user, context) )
					return true;
			}
			return false;
		}
		catch(Exception e)
		{
			Manager.of(Logger.class).warning(Security.class, e);
		}
		return false;
	}
	
	/**
	 * Generates a {@link Token} for the provided user.
	 * @param user the target user, must not be null and must not be {@link User#SYSTEM} or {@link User#ANONYMOUS}
	 * @param validity the validity time in millisecond. A value <code>&lt;= 0</code> means valid forever.
	 * @param exclusive whether or not other tokens for the same user should be revoked (regardless of the scopes)
	 * @param scopes the list of scopes, must not be null or empty
	 * @return a token that can be used to uniquely identify the provided user, or null if the token could not be generated
	 */
	public abstract Token generateToken(User.Type user, long validity, boolean exclusive, String... scopes);
	
	/**
	 * Retreives a token instance based on a an opaque token value.
	 * @param token the token value, must not be null
	 * @param reset whether or not to reset the optional time-based validity of this for this token
	 * @return the matching token instance or null if no token matches
	 */
	public abstract Token authenticate(String token, boolean reset);
	
	/**
	 * Revokes the provided token
	 * If the provided token is null, this method returns without doing anything.
	 * @param token the token to revoke
	 */
	public abstract void revokeToken(Token token);
	
	/**
	 * Revokes all tokens for the provided user
	 * @param user the target user, must not be null and must not be {@link User#SYSTEM} or {@link User#ANONYMOUS}
	 */
	public abstract void clearTokens(User.Type user);
	
	/**
	 * Returns a shadow copy of all tokens for the provided user.
	 * If a token needs to be revoked, use {@link #revokeToken(Token)}.
	 * @param user the target user, must not be null and must not be {@link User#SYSTEM} or {@link User#ANONYMOUS}
	 */
	public abstract Collection<Token> listTokens(User.Type user);
}
