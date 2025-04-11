package aeonics.entity.security;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.manager.Config;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.manager.Security;
import aeonics.manager.Vault;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.Json;
import aeonics.util.Tuples.Tuple;

/**
 * This item complements the {@link Provider} by managing other means of authentication.
 * It SHOULD ideally be honored by the different authentication techniques, but may also
 * be used to provide a double-confirmation for specific actions.
 */
public abstract class Multifactor extends Item<Multifactor.Type>
{
	// =========================================
	//
	// BASE MFA
	//
	// =========================================
	
	/**
	 * This base entity defines the minimum requirements for MFA providers.
	 */
	public abstract static class Type extends Entity
	{
		/**
		 * private key to store private data
		 */
		private String key = null;
		
		/**
		 * Enroll a user with this MultifactorAuthentication provider.
		 * @param user the user to enroll
		 * @param context any implementation specific data
		 * @throws RuntimeException unchecked exception in case of constraint violation or any other failure
		 */
		public abstract void enroll(User.Type user, Data context);
		
		/**
		 * Checks if the specified user is enrolled with this MultifactorAuthentication provider.
		 * @param user the user
		 * @return true if the user is enrolled
		 */
		public boolean enrolled(User.Type user) { return !privateData(user).isEmpty(); }
		
		/**
		 * Perform the MFA check.
		 * @param user the target user
		 * @param context any implementation specific data
		 * @return true if the multifactor authentication check succeeds, false otherwise
		 */
		public abstract boolean check(User.Type user, Data context);
		
		/**
		 * Generates valid settings for pre-enrollment. This method may not be necessary in all implementations.
		 * <p>There must be no side effect from using this method. No information should be retained.</p>
		 * @param user the user to generate enrollment data for
		 * @param context any implementation specific data
		 * @return any implementation specific data
		 * @throws RuntimeException unchecked exception in case of constraint violation or any other failure
		 */
		public abstract Data generate(User.Type user, Data context);
		
		/**
		 * Blank check using enrollment context and the MFA check.
		 * This method can be used to validate the enroll data and that the user is able to successfully use it.
		 * <p>There must be no side effect from using this method. No information should be retained.</p>
		 * @param enroll the implementation specific entollment data
		 * @param test the implementation specific test data
		 * @return true if the multifactor authentication blank check succeeds, false otherwise
		 */
		public abstract boolean blank(User.Type user, Data enroll, Data test);
		
		/**
		 * Forgets about a user. This operation is the opposite of {@link #enroll(aeonics.entity.security.User.Type, Data)}.
		 * @param user the user to forget
		 */
		public void forget(User.Type user) { privateData(user, null); }
		
		/**
		 * Determines if this MFA applies to the specified user, in other words, if the user MUST enroll.
		 * The strict enforcement of the enrollment requirement and practical means to do it depend on the implementation.
		 * @param user the target user
		 * @return true if the MFA applies to the user.
		 */
		public boolean appliesTo(User.Type user)
		{
			if( user == User.ANONYMOUS || user == User.SYSTEM ) return false;
			
			for( Tuple<Entity, Data> role : relations("roles") )
				if( role != null && role.a != null && user.hasRole(role.a.<Role.Type>cast()) )
					return true;
			
			for( Tuple<Entity, Data> group : relations("groups") )
				if( group != null && group.a != null && user.isMemberOf(group.a.<Group.Type>cast()) )
					return true;
			
			return false;
		}
		
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
		
		@Override
		public Data snapshot()
		{
			return super.snapshot().put("key", key);
		}
	}
	
	@Override
	public Template<? extends Multifactor.Type> template()
	{
		return super.template()
			.add(new Relationship("roles")
				.category(Role.class)
				.summary("Roles")
				.description("The user roles that this multifactor method applies to."))
			.add(new Relationship("groups")
				.category(Group.class)
				.summary("Groups")
				.description("The user groups that this multifactor method applies to."))
			.onCreate((data, instance) -> 
			{
				if( data.containsKey("key") ) ((Multifactor.Type)instance).key = data.asString("key");
				else ((Multifactor.Type)instance).key = Manager.of(Security.class).randomHash();
			})
			;
	}
	
	protected Class<? extends Multifactor> category() { return Multifactor.class; }
	
	// =========================================
	//
	// TOTP
	//
	// =========================================
	
	public static class TOTP extends Multifactor
	{
		// https://github.com/google/google-authenticator/wiki/Key-Uri-Format
    	// otpauth://totp/ACME%20Co:john.doe@email.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA1&digits=6&period=30
		
		public static class Type extends Multifactor.Type
		{
			/**
			 * @param context expects a map with the "secret" key generated by {@link #generate(User.Type, Data)}
			 */
			public void enroll(User.Type user, Data context)
			{
				if( user == null || user == User.ANONYMOUS || user == User.SYSTEM )
					throw new IllegalArgumentException("Invalid user");
				if( enrolled(user) )
					throw new RuntimeException("Already enrolled");
				if( context == null || !context.isMap() || context.isEmpty("secret") )
					throw new IllegalArgumentException("Invalid enrollment data");
				
				try
				{
					Data info = Data.map()
						.put("secret", context.asString("secret"))
						.put("period", Manager.of(Config.class).get(type(), "otpperiod"))
						.put("digits", Manager.of(Config.class).get(type(), "otpdigits"))
						.put("algorithm", Manager.of(Config.class).get(type(), "otpalgorithm"));
					
					privateData(user, info);
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).info(Multifactor.class, e);
					throw new RuntimeException("TOTP generation failed");
				}
			}
			
			/**
			 * @param context ignored
			 */
			public Data generate(User.Type user, Data context)
			{
				if( user == null || user == User.ANONYMOUS || user == User.SYSTEM )
					throw new IllegalArgumentException("Invalid user");
				
				Data info = Data.map()
					.put("secret", base32Encode(generateSecret()))
					.put("period", Manager.of(Config.class).get(type(), "otpperiod"))
					.put("digits", Manager.of(Config.class).get(type(), "otpdigits"))
					.put("algorithm", Manager.of(Config.class).get(type(), "otpalgorithm"));
System.out.println(type() + ":" + "otpissuer = " + Manager.of(Config.class).get(type(), "otpissuer").asString());
				return info
					.put("url", "otpauth://totp/" 
						+ URLEncoder.encode(Manager.of(Config.class).get(type(), "otpissuer").asString(), StandardCharsets.UTF_8).replace("+", "%20") + ":" 
						+ URLEncoder.encode(user.name(), StandardCharsets.UTF_8).replace("+", "%20") + "?" +
						"secret=" + info.get("secret") +
						"&issuer=" + URLEncoder.encode(Manager.of(Config.class).get(type(), "otpissuer").asString(), StandardCharsets.UTF_8).replace("+", "%20") +
						"&algorithm=" + info.asString("algorithm") +
						"&digits=" + info.asString("digits") +
						"&period=" + info.asString("period"));
			}
			
			/**
			 * @param enroll expects the "secret" key set
			 * @param test expects the "otp" key set
			 */
			public boolean blank(User.Type user, Data enroll, Data test)
			{
				if( user == null || user == User.ANONYMOUS || user == User.SYSTEM )
					throw new IllegalArgumentException("Invalid user");
				if( enroll == null || !enroll.isMap() || enroll.isEmpty("secret") )
					throw new IllegalArgumentException("Invalid enrollment data");
				if( test == null || !test.isMap() || test.isEmpty("otp") )
					throw new IllegalArgumentException("Invalid test data");
				
				try
				{
					Data info = Data.map()
						.put("secret", enroll.asString("secret"))
						.put("period", Manager.of(Config.class).get(type(), "otpperiod"))
						.put("digits", Manager.of(Config.class).get(type(), "otpdigits"))
						.put("algorithm", Manager.of(Config.class).get(type(), "otpalgorithm"));
					
					String otp = test.asString("otp");
					long now = System.currentTimeMillis();
					byte[] secret = base32Decode(enroll.asString("secret"));
					
					return checkAt(info.asString("algorithm"), info.asInt("period"), info.asInt("digits"), secret, otp, now)
						|| checkAt(info.asString("algorithm"), info.asInt("period"), info.asInt("digits"), secret, otp, now - (info.asLong("period") * 1000L));
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).fine(Multifactor.class, e);
					return false;
				}
			}

			/**
			 * @param context Data map with the "otp" property set to the value to check
			 */
			public boolean check(User.Type user, Data context)
			{
				if( user == User.ANONYMOUS || user == User.SYSTEM ) return false;
				if( context == null || !context.isMap() || context.isEmpty("otp") ) return false;
				
				synchronized(user)
				{
					try
					{
						Data info = privateData(user);
						if( info == null || info.isEmpty() || info.asString("latest").equals(context.asString("otp")) ) return false;
						
						String otp = context.asString("otp");
						long now = System.currentTimeMillis();
						byte[] secret = base32Decode(info.asString("secret"));
						
						if( !checkAt(info.asString("algorithm"), info.asInt("period"), info.asInt("digits"), secret, otp, now) && 
							!checkAt(info.asString("algorithm"), info.asInt("period"), info.asInt("digits"), secret, otp, now - (info.asLong("period") * 1000L)) ) return false;
						
						// from here, code did match.
						// keep the last successful match to prevent replay attacks
						info.put("latest", otp);
						privateData(user, info);
						
						return true;
					}
					catch(Exception e)
					{
						Manager.of(Logger.class).warning(Multifactor.class, e);
						return false;
					}
				}
			}
			
			private boolean checkAt(String algorithm, int period, int digits, byte[] secret, String otp, long time) throws Exception
			{
				int current = Integer.parseInt(otp);
				
				long timeslice = time / 1000L / period;
		        
		        Mac mac = Mac.getInstance("Hmac" + algorithm);
		        mac.init(new SecretKeySpec(secret, "Hmac" + algorithm));
		        byte[] hmac = mac.doFinal(ByteBuffer.allocate(8).putLong(timeslice).array());
		        
		        int offset = hmac[hmac.length - 1] & 0x0f;
		        int binary = ((hmac[offset] & 0x7f) << 24) |
		                      ((hmac[offset + 1] & 0xff) << 16) |
		                      ((hmac[offset + 2] & 0xff) << 8) |
		                      (hmac[offset + 3] & 0xff);
		        int totp = binary % (int) Math.pow(10, digits);
		        
		        return current == totp;
			}
		
			private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
			private static String base32Encode(byte[] data)
			{
				StringBuilder result = new StringBuilder();
		        int bitCount = 0;
		        int currentByte = 0;
		        
		        for (byte b : data)
		        {
		            currentByte <<= 8;
		            currentByte |= b & 0xFF;
		            bitCount += 8;
		            
		            while (bitCount >= 5) {
		                int index = (currentByte >>> (bitCount - 5)) & 0x1F;
		                result.append(BASE32_ALPHABET[index]);
		                bitCount -= 5;
		            }
		        }
		        
		        if (bitCount > 0)
		        {
		            currentByte <<= (5 - bitCount);
		            int index = currentByte & 0x1F;
		            result.append(BASE32_ALPHABET[index]);
		        }
		        
		        return result.toString();
			}

			private static final int[] REVERSE_BASE32 = new int[] { 26, 27, 28 , 29 , 30, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 };
			private static byte[] base32Decode(String value)
			{
				byte[] result = new byte[(value.length() * 5) / 8];
				int resultIndex = 0;
				int currentByte = 0;
				int bitCount = 0;
				
				for( char c : value.toCharArray() )
				{
					int i = c - 50;
					if( i < 0 || i >= REVERSE_BASE32.length ) throw new IllegalArgumentException("Invalid character in Base32 string: " + c);
					i = REVERSE_BASE32[i];
				
					currentByte <<= 5;
					currentByte |= i & 0x1F;
					bitCount += 5;

				    if (bitCount >= 8)
				    {
						result[resultIndex++] = (byte) ((currentByte >> (bitCount - 8)) & 0xFF);
						bitCount -= 8;
				    }
				}
				
				return result;
			}
			
			private static byte[] generateSecret()
			{
				byte[] hash = Manager.of(Security.class).randomHash().getBytes();
				byte[] secret = new byte[6];
				
				for( int i = 0; i < 6; i++ ) secret[i] = hash[i];
				for( int i = 6; i < hash.length; i++ ) secret[i%6] ^= hash[i];
				
				return secret;
			}
		}
		
		protected Class<? extends TOTP.Type> defaultTarget() { return TOTP.Type.class; }
		protected Supplier<? extends TOTP.Type> defaultCreator() { return TOTP.Type::new; }
		
		public Template<? extends Multifactor.Type> template()
		{
			return super.template()
				.summary("TOTP")
				.description("Time-based One Time Password implementation.")
				.config(new Parameter("otpperiod")
					.summary("OTP time window")
					.description("The OTP time window in seconds.")
					.rule(Parameter.Rule.DIGIT)
					.format(Parameter.Format.NUMBER)
					.defaultValue(30))
				.config(new Parameter("otpdigits")
					.summary("OTP number of digits")
					.description("The number of OTP code digits.")
					.rule(Parameter.Rule.DIGIT)
					.format(Parameter.Format.NUMBER)
					.defaultValue(6))
				.config(new Parameter("otpalgorithm")
					.summary("OTP algorithm")
					.description("The name of the hash algorithm to use in OTP.")
					.values("SHA1")
					.format(Parameter.Format.SELECT)
					.defaultValue("SHA1"))
				.config(new Parameter("otpissuer")
					.summary("OTP issuer name")
					.description("The name of the OTP issuer to be displayed by MFA apps.")
					.format(Parameter.Format.TEXT)
					.defaultValue("Aeonics"))
				;
		}
	}
	
	// =========================================
	//
	// WebAuthN
	//
	// =========================================
}
