package aeonics.entity.security;

import java.math.BigDecimal;
import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.entity.Registry;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.manager.Security;
import aeonics.manager.Vault;
import aeonics.template.Factory;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Template;
import aeonics.util.Json;
import aeonics.util.StringUtils;

/**
 * This item plays a role in the definition of the {@link Security}.
 * It represents a specific authentication provider that manages the login of users.
 * <p>Optionally, the identity provider may evaluate a specific security {@link Rule} 
 * using {@link Provider.Type#check(Rule.Type, User.Type, Data)}.</p>
 * <p>Each identity provider may also store private secure data about users if needed. 
 * That data is only accessible to the instance that stored it in the first place.</p>
 */
public abstract class Provider extends Item<Provider.Type>
{
	/**
	 * Superclass for all provider entities.
	 */
	public abstract static class Type extends Entity
	{
		/**
		 * private key to store private data
		 */
		private String key = null;
		
		/**
		 * Fetch private data related to the provided user.
		 * <p>If there is no data associated with this user, this method returns an empty data object.</p>
		 * @param user the target user
		 * @return the private data related to the provided user
		 */
		protected final Data privateData(User.Type user)
		{
			try
			{
				if( user == null ) return Data.map();
				Data data = Manager.of(Vault.class).get(Manager.of(Security.class).hash(id() + "." + user.id()), key);
				if( data.isEmpty() ) return data;
				return Json.decode(data.asString());
			}
			catch(Exception e)
			{
				return Data.map();
			}
		}
		
		/**
		 * Sets or removes private data related to the provided user.
		 * If the provided data is <code>null</code> then the private data is removed entirely.
		 * The input data <b>must</b> be a map object.
		 * @param user the target user
		 * @param data the private data related to the provided user as a map object, or null to remove the related data
		 */
		protected final void privateData(User.Type user, Data data)
		{
			if( user == null ) throw new IllegalArgumentException("Invalid user");
			if( data != null && !data.isMap() ) throw new IllegalArgumentException("Invalid data");
			
			try
			{
				if( data == null ) Manager.of(Vault.class).remove(Manager.of(Security.class).hash(id() + "." + user.id()), key);
				else Manager.of(Vault.class).set(Manager.of(Security.class).hash(id() + "." + user.id()), data, key);
			}
			catch(Exception e)
			{
				throw new RuntimeException("Operation failed");
			}
		}
		
		/**
		 * Returns true if this provider supports authenticating the specified user login.
		 * <p>This method should be a quick indication and not an extensive check, so it may happen that this method returns true inadvertently.</p>
		 * @param user the user login
		 * @return true if this provider supports authenticating the specified user login
		 */
		public abstract boolean supports(String user);
		
		/**
		 * Authenticates a user based on the provided opaque credentials.
		 * @param context the authentication credentials
		 * @return the matching user instance or null if no user matches
		 */
		public abstract User.Type authenticate(Data context);
		
		/**
		 * Enable this provider for the specified existing user.
		 * The user may be null in which case a new user should be created.
		 * It is a good idea to synchronize this method to avoid side effects.
		 * @param context the authentication credentials
		 * @param existing the existing user to bind or null in case of a new user
		 * @return null in case the operation failed, or the user (existing or new one) in case of success
		 */
		public abstract User.Type join(Data context, User.Type existing);
		
		/**
		 * Disables this provider for the specified user.
		 * This method should clear all data as if the user never joined this provider.
		 * If the user is null or not joined, then ignore it.
		 * It is a good idea to synchronize this method to avoid side effects.
		 * @param user the user to unbind
		 */
		public abstract void leave(User.Type user);
		
		/**
		 * Checks if the user passes the specified rule.
		 * <p>This method may be called by a specific {@link Rule} that wants the explicit decision from the providers
		 * (i.e. {@link Rule.AskProviders}).</p>
		 * @param rule the rule to evaluate, must not be null
		 * @param user the user, must not be null
		 * @param context the context data to check against matching policies, may be null
		 * @return true if the user is explicitly denied
		 */
		public boolean check(Rule.Type rule, User.Type user, Data context) { return true; }
		
		/**
		 * Returns true if this provider is active
		 * @return true if this provider is active
		 */
		public boolean active() { return valueOf("active").asBool(); }
		
		/**
		 * Hardcoded category to the {@link Provider} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Provider.class); }

		@Override
		public Data export()
		{
			return super.export().put("__key", key);
		}
	}

	@Override
	public Template<? extends Provider.Type> template()
	{
		return super.template()
			.add(new Parameter("active")
				.summary("Active")
				.description("Whether or not this provider is active")
				.defaultValue(Data.of(true)))
			.builder((data, instance) -> 
			{
				if( instance instanceof Provider.Type )
				{
					if( data.containsKey("__key") ) ((Provider.Type)instance).key = data.asString("__key");
					else ((Provider.Type)instance).key = Manager.of(Security.class).randomHash();
				}
				Registry.add(instance);
			});
	}
	
	protected Class<? extends Provider> category() { return Provider.class; }
	
	// =========================================
	//
	// REMOTE PROVIDER
	//
	// =========================================
	
	public static abstract class Remote extends Provider.Type
	{
		public abstract String loginPageRedirectUrl();

		@Override
		public Data export()
		{
			return super.export()
				.put("login_redirect", loginPageRedirectUrl());
		}
	}
	
	// =========================================
	//
	// LOCAL USERNAME / PASSWORD
	//
	// =========================================
		
	public static class Local extends Provider
	{
		public static class Type extends Provider.Type
		{
			public boolean supports(String user)
			{
				User.Type u = Registry.of(User.class).get(user);
				if( user == null ) return false;
				Data priv = privateData(u);
				return !priv.isEmpty();
			}
			
			public User.Type authenticate(Data context)
			{
				if( context == null || !context.isMap() || !context.containsKey("username") || !context.containsKey("password") ) return null;
				User.Type user = Registry.of(User.class).get(context.asString("username"));

				if( user == null ) return null;
				Data priv = privateData(user);

				if( priv.isEmpty() ) return null;
				
				String hash = Manager.of(Security.class).hash(context.asString("password"), priv.asString("salt"));
				if( hash.equals(priv.asString("password")) ) return user;
				else return null;
			}
			
			public synchronized User.Type join(Data context, User.Type existing)
			{
				if( context == null || !context.isMap() || !context.containsKey("username") || !context.containsKey("password") ) return null;
				long complexity = complexity(context.asString("password"));
				if( complexity < valueOf("complexity").asLong() )
				{
					Manager.of(Logger.class).warning(this.getClass(), "Password complexity requirement not met");
					return null;
				}

				// check for clash
				if( existing == null )
				{
					User.Type user = Registry.of(User.class).get(context.asString("username"));
					if( user != null ) existing = user;
				}
				
				// create user if needed
				if( existing == null )
					existing = Factory.of(User.class).get(User.class).build().name(context.asString("username"));

				// already joined
				Data priv = privateData(existing);
				if( !priv.isEmpty() ) return existing;
				priv = Data.map().put("salt", Manager.of(Security.class).randomHash());
				
				// hash the pass
				String hash = Manager.of(Security.class).hash(context.asString("password"), priv.asString("salt"));
				privateData(existing, priv.put("password", hash));
				
				return existing;
			}
			
			public synchronized void leave(User.Type user)
			{
				if( user == null ) return;
				privateData(user, null);
			}
			
			private long complexity(String password)
			{
				boolean lower = false;
				boolean upper = false;
				boolean digit = false;
				boolean symbol = false;
				for( char c : password.toCharArray() )
				{
					if( c >= 'a' && c <= 'z' ) lower = true;
					else if( c >= 'A' && c <= 'Z' ) upper = true;
					else if( c >= '0' && c <= '9' ) digit = true;
					else if( !symbol ) symbol = true;
				}
				int base = 0 
					+ (lower ? 26 : 0)
					+ (upper ? 26 : 0)
					+ (digit ? 10 : 0)
					+ (symbol ? 8 : 0);
				
				BigDecimal complexity = BigDecimal.valueOf(base).pow(password.length());
				
				try { return complexity.longValueExact(); }
				catch(Exception e) { return Long.MAX_VALUE; }
			}
		}
		
		protected Class<? extends Local.Type> defaultTarget() { return Local.Type.class; }
		protected Supplier<? extends Local.Type> defaultCreator() { return Local.Type::new; }

		@Override
		public Template<? extends Provider.Type> template()
		{
			return super.template()
				.summary("Local identity provider")
				.description("This identity provider uses a password authentication scheme. The context data should contain the 'username' and 'password' properties.")
				.add(new Parameter("complexity")
					.summary("Password policy complexity")
					.description("Enforce the password complexity to be above the specified threshold of possible combinations.")
					.defaultValue(Data.of(10_000_000_000_000_000L))
					)
				;
		}
	}
}
