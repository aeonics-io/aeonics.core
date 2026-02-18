package aeonics.manager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
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
	 * Produces a SHA-256 digest of the target input data.
	 * <p>This is <b>not</b> intended for password hashing. Use {@link #hash(String, String)} for passwords.</p>
	 * <p>This is typically used for file integrity comparison (checksums).</p>
	 * @param value the input stream to hash
	 * @return the hash value in hex format
	 */
	public String hash(InputStream value)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			try( DigestInputStream dis = new DigestInputStream(value, md) )
			{
				byte[] buffer = new byte[8192];
				while( dis.read(buffer) != -1 );
			}
			byte[] hash = md.digest();
			char[] hex = new char[hash.length * 2];
			for( int i = 0; i < hash.length; i++ )
			{
				int h = hash[i] & 0xff;
				hex[i * 2] = "0123456789abcdef".charAt(h >>> 4);
				hex[i * 2 + 1] = "0123456789abcdef".charAt(h & 0x0F);
			}
			return new String(hex);
		}
		catch( Exception e )
		{
			throw new RuntimeException("Hash failed");
		}
	}
	
	/**
	 * Produces a SHA-256 digest of the target input value.
	 * <p>This is <b>not</b> intended for password hashing. Use {@link #hash(String, String)} for passwords.</p>
	 * <p>This is typically used for lightweight key obfuscation and spread.</p>
	 * @param value the input text to hash
	 * @return the hashed value in hex format
	 */
	public String hash(String value) { return hash(value == null ? null : value.getBytes(StandardCharsets.ISO_8859_1)); }
	
	/**
	 * Produces a SHA-256 digest of the target input value.
	 * <p>This is <b>not</b> intended for password hashing. Use {@link #hash(String, String)} for passwords.</p>
	 * <p>This is typically used for lightweight key obfuscation and spread.</p>
	 * @param value the input text to hash
	 * @return the hashed value in hex format
	 */
	public String hash(byte[] value)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(value);
			char[] hex = new char[hash.length * 2];
			for( int i = 0; i < hash.length; i++ )
			{
				int h = hash[i] & 0xff;
				hex[i * 2] = "0123456789abcdef".charAt(h >>> 4);
				hex[i * 2 + 1] = "0123456789abcdef".charAt(h & 0x0F);
			}
			return new String(hex);
		}
		catch( Exception e )
		{
			throw new RuntimeException("Hash failed");
		}
	}
	
	/**
	 * Produces a strong cryptographic hash of the input value.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to hash
	 * @param salt the salt value (may be null but it is not recommended)
	 * @return the hashed value in hex format
	 */
	public String hash(String value, String salt) { return hash(value == null ? null : value.getBytes(StandardCharsets.ISO_8859_1), salt == null ? null : salt.getBytes(StandardCharsets.ISO_8859_1), null); }

	/**
	 * Produces a strong cryptographic hash of the input value with an explicit pepper.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to hash
	 * @param salt the salt value (may be null but it is not recommended)
	 * @param pepper the pepper value. If null, the implementation's default pepper is used (if configured).
	 * @return the hashed value in hex format
	 */
	public String hash(String value, String salt, String pepper) { return hash(value == null ? null : value.getBytes(StandardCharsets.ISO_8859_1), salt == null ? null : salt.getBytes(StandardCharsets.ISO_8859_1), pepper == null ? null : pepper.getBytes(StandardCharsets.ISO_8859_1)); }

	/**
	 * Produces a strong cryptographic hash of the input value.
	 * Uses the implementation's default pepper if configured.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to hash
	 * @param salt the salt value (may be null but it is not recommended)
	 * @return the hashed value in hex format
	 */
	public String hash(byte[] value, byte[] salt) { return hash(value, salt, null); }

	/**
	 * Produces a strong cryptographic hash of the input value with an explicit pepper.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input text to hash
	 * @param salt the salt value (may be null but it is not recommended)
	 * @param pepper the pepper value. If null, the implementation's default pepper is used (if configured).
	 * @return the hashed value in hex format
	 */
	public abstract String hash(byte[] value, byte[] salt, byte[] pepper);
	
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
	 * Encrypts the given value with a strong asymmetric key encryption method.
	 * Use {@link #decrypt(String, PrivateKey)} to recover the original value.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input to encrypt
	 * @param key public key
	 * @return the encrypted value in base64 format
	 * @throws SecurityException if any error happens during encryption. For security purpose, the cause of the exception is discarded.
	 */
	public abstract String encrypt(byte[] value, PublicKey key);
	
	/**
	 * Encrypts the given value with a strong asymmetric key encryption method.
	 * Use {@link #decrypt(String, PrivateKey)} to recover the original value.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input to encrypt
	 * @param key public key
	 * @return the encrypted value in base64 format
	 * @throws SecurityException if any error happens during encryption. For security purpose, the cause of the exception is discarded.
	 */
	public String encrypt(byte[] value, Certificate key) { return encrypt(value, key.getPublicKey()); }
	
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
	 * Decrypts the given value to recover the original value that was encrypted with {@link #encrypt(String, PublicKey)}.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the encrypted value to decrypt in base64 format
	 * @param key the private key
	 * @return the original value
	 * @throws SecurityException if any error happens during decryption, including a key mismatch. For security purpose, the cause of the exception is discarded.
	 */
	public abstract byte[] decrypt(String value, PrivateKey key);
	
	/**
	 * Verifies the signature of the given data.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param signature the base64 encoded signature
	 * @param value the input to verify
	 * @param key public key to check the signature
	 * @return true if the signature matches, false otherwise
	 */
	public abstract boolean verify(String signature, byte[] value, PublicKey key);
	
	/**
	 * Verifies the signature of the given data.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param signature the base64 encoded signature
	 * @param value the input to verify
	 * @param key public key to check the signature
	 * @return true if the signature matches, false otherwise
	 */
	public boolean verify(String signature, byte[] value, Certificate key) { return verify(signature, value, key.getPublicKey()); }
	
	/**
	 * Signs the given data.
	 * Use {@link #verify(String, PublicKey)} to check the signature.
	 * The underlying implementation is not enforced but the algorithm shall remain consistent to ensure backward compatibility over time.
	 * @param value the input to sign
	 * @param key private key
	 * @return the signature in base64 format
	 * @throws SecurityException if any error happens
	 */
	public abstract String sign(byte[] value, PrivateKey key);
	
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
	 * Checks whether the given user is currently locked from authentication.
	 * A user may be locked for various reasons, including excessive failed authentication attempts or other security policies.
	 * @param userId the user id to check
	 * @return true if the user is currently locked and authentication should be rejected
	 */
	public abstract boolean isLocked(String userId);

	/**
	 * Records a failed authentication attempt for the given user.
	 * This applies to all authentication mechanisms including passwords, OTP, and other verification methods.
	 * The implementation may apply rate limiting or other security measures based on accumulated failures.
	 * @param userId the user id that failed authentication
	 */
	public abstract void recordFailedAuthentication(String userId);

	/**
	 * Records a successful authentication for the given user.
	 * This applies to all authentication mechanisms and may reset any accumulated failed attempts or other security tracking.
	 * @param userId the user id that successfully authenticated
	 */
	public abstract void recordSuccessfulAuthentication(String userId);

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
	 * Retrieves a token instance based on an opaque token value.
	 * <p><b>Security note:</b> This method performs a direct lookup without rate limiting.
	 * The security of this approach relies entirely on the token entropy: an attacker guessing random values
	 * has a probability of k*N/S per attempt, where k is the number of attempts, N is the number of valid tokens,
	 * and S is the token space size. Implementations must ensure that tokens are generated with sufficient size
	 * and entropy to make brute-force search infeasible. For example, with SHA-256 based tokens (256-bit / 64 hex characters),
	 * even 10^15 attempts against 10^5 valid tokens yields a probability of approximately 10^-57.</p>
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
	
	/**
	 * Attempts to populate the user from its user id from any {@link Provider}.
	 * This is used by the {@link Token} when the user is not already present in the Registry.
	 * After this method returns, it is expected that the user be fully populated and available in the Registry until an eventual cleanup timeout.
	 * @param user_id the user id
	 * @return the populated user, or null if the user could not be populated.
	 */
	public User.Type populate(String user_id)
	{
		for( Provider.Type p : Registry.of(Provider.class) )
		{
			User.Type u = p.populate(user_id);
			if( u != null ) return u;
		}
		return null;
	}
}
