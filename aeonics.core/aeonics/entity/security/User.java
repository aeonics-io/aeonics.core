package aeonics.entity.security;

import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.entity.Registry;
import aeonics.manager.Security;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.Json;
import aeonics.util.StringUtils;
import aeonics.util.Snapshotable.SnapshotMode;
import aeonics.util.Tuples.Tuple;

/**
 * This item plays a role in the definition of the {@link Security}.
 * It represents a login to the system, whether or not it is linked to a physical person, a service, or a device is
 * left at the sole discretion of the security {@link Provider}.
 * 
 * <p><b>Important implementation notes:</b></p>
 * <p>The {@link Type#id()} is the unique identifier for a user, it does not carry information but is used to reference users.
 * You should avoid duplicate IDs and those must be stable over time: never change and the same user must always have the same ID.</p>
 * 
 * <p>The {@link Type#login()} is used to perform the authentication, it can be of any form: email, login name or opaque credentials.</p>
 * 
 * <p>The {@link Type#name()} is the friendly name of the user. Usually it is identical to the login but it is not a requirement. 
 * Since the name is free text there may be duplicates, so <b>never</b> rely on this information.</p>
 * 
 * <p>Any other property or profile information is not included by default, you can add them in the custom attributes property, fetch them somehow, or add
 * a related profile entity.</p>
 * <p>Example:</p>
 * <pre>
 * id: 12345-678910
 * login: john.doe@example.com
 * name: Monster2023
 * email: john.doe@example.com (custom attribute)
 * firstname: John (custom attribute)
 * lastname: Doe (custom attribute)
 * </pre>
 */
public class User extends Item<User.Type>
{
	public static final User.Type ANONYMOUS = new User().template().create(Data.map().put("id", "10000000-1100000000000000"))
		.name("ANONYMOUS").internal(true).snapshotMode(SnapshotMode.UPDATE);
	public static final User.Type SYSTEM = new User().template().create(Data.map().put("id", "10000000-1200000000000000"))
		.name("SYSTEM").internal(true).snapshotMode(SnapshotMode.UPDATE);
	
	/**
	 * Superclass for all user entities.
	 */
	public static class Type extends Entity
	{
		/**
		 * Returns the user login
		 * @return the user login
		 */
		public String login() { return valueOf("login").asString(); }
		
		/**
		 * Returns true if this user is active
		 * @return true if this user is active
		 */
		public boolean active() { return valueOf("active").asBool(); }
		
		/**
		 * Hardcoded category to the {@link User} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(User.class); }
		
		/**
		 * Checks if a user is a member of the specified group
		 * @param group the group
		 * @return true if the user is a member of the group
		 */
		public boolean isMemberOf(String group)
		{
			Group.Type g = Registry.of(Group.class).get(group);
			if( g == null ) return false;
			return isMemberOf(g);
		}
		
		/**
		 * Checks if a user is a member of the specified group
		 * @param group the group
		 * @return true if the user is a member of the group
		 */
		public boolean isMemberOf(Group.Type group)
		{
			if( group == null ) return false;
			
			for( Tuple<Entity, Data> g : relations("groups") )
			{
				if( group.equals(g.a) )
					return true;
			}
			
			return false;
		}
		
		/**
		 * Checks if a user has the specified role.
		 * The verification is performed directly or via the user group memberships.
		 * @param role the role
		 * @return true if the user has the role
		 */
		public boolean hasRole(String role)
		{
			Role.Type r = Registry.of(Role.class).get(role);
			if( r == null ) return false;
			return hasRole(r);
		}
		
		/**
		 * Checks if a user has the specified role.
		 * The verification is performed directly or via the user group memberships.
		 * @param role the role
		 * @return true if the user has the role
		 */
		public boolean hasRole(Role.Type role)
		{
			if( role == null ) return false;
			
			for( Tuple<Entity, Data> r : relations("roles") )
			{
				if( role.equals(r.a) )
					return true;
			}
			
			for( Tuple<Entity, Data> g : relations("groups") )
			{
				for( Tuple<Entity, Data> r : g.a.relations("roles") )
				{
					if( role.equals(r.a) )
						return true;
				}
			}
			
			return false;
		}
		
		/**
		 * Returns the user attributes
		 * @return the user attributes
		 */
		public Data attributes()
		{
			return valueOf("attributes");
		}
	}
	
	protected Class<? extends User.Type> defaultTarget() { return User.Type.class; }
	protected Supplier<? extends User.Type> defaultCreator() { return User.Type::new; }
	protected Class<? extends User> category() { return User.class; }

	@Override
	public Template<? extends User.Type> template()
	{
		return super.template()
			.summary("User")
			.description("Users represents a login to the system and can be linked to groups and roles. Users can only login if they are supported by a security provider.")
			.add(new Parameter("active")
				.summary("Active")
				.description("Whether or not this user is active")
				.rule(Parameter.Rule.BOOLEAN)
				.format(Parameter.Format.BOOLEAN)
				.optional(true)
				.defaultValue(true))
			.add(new Parameter("login")
				.summary("Login")
				.description("The login name used for authentication. If the login is not set, the user cannot login.")
				.format(Parameter.Format.TEXT)
				.optional(true))
			.add(new Parameter("attributes")
				.summary("Attributes")
				.description("Additional user attributes in the form of a data map.")
				.rule(Parameter.Rule.JSON_MAP)
				.format(Parameter.Format.JSON)
				.optional(true)
				.defaultValue(Data.map())
				.validator((v) -> v != null && (v.isMap() || Json.decode(v.asString()).isMap())))
			.add(new Relationship("roles")
				.category(Role.class)
				.summary("Roles")
				.description("List of user roles."))
			.add(new Relationship("groups")
				.category(Group.class)
				.summary("Groups")
				.description("List of user groups."))
			;
	}
}
