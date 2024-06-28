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
import aeonics.util.StringUtils;
import aeonics.util.Tuple;

/**
 * This item plays a role in the definition of the {@link Security}.
 * It represents a login to the system, whether or not it is linked to a physical person, a service, or a device is
 * left at the sole discretion of the security {@link Provider}.
 * 
 * <p><b>Important implementation notes:</b> the {@link Type#id()} is the unique identifier for a user. It should be the login 
 * (or somewhat identifier) used to connect. On the other hand, the "name" property shall be the human printable user name. 
 * It may be identical to the login but may differ in case of remote identity provisioning.</p>
 * <p>Example:</p>
 * <ul>
 * <li>id: john.doe@example.com (built-in id() as login)</li>
 * <li>name: Monster2023 (default "name" property)</li>
 * <li>email: john.doe@example.com (custom attribute)</li>
 * <li>firstname: John (custom attribute)</li>
 * <li>lastname: Doe (custom attribute)</li>
 * </ul>
 */
public class User extends Item<User.Type>
{
	public static final User.Type ANONYMOUS = new User().template().build(Data.map().put("__internal", true).put("__id", "10000000-1100000000000000")).name("ANONYMOUS");
	public static final User.Type SYSTEM = new User().template().build(Data.map().put("__internal", true).put("__id", "10000000-1200000000000000")).name("SYSTEM");
	
	/**
	 * Superclass for all user entities.
	 */
	public static class Type extends Entity
	{
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
	}
	
	protected Class<? extends User.Type> defaultTarget() { return User.Type.class; }
	protected Supplier<? extends User.Type> defaultCreator() { return User.Type::new; }
	protected Class<? extends User> category() { return User.class; }

	@Override
	public Template<? extends User.Type> template()
	{
		return super.template()
			.add(new Parameter("active")
				.summary("Active")
				.description("Whether or not this user is active")
				.rule(Parameter.Rule.BOOLEAN)
				.format(Parameter.Format.BOOLEAN)
				.optional(true)
				.defaultValue(Data.of(true)))
			.add(new Parameter("attributes")
				.summary("Attributes")
				.description("Additional user attributes in the form of a data map.")
				.rule(Parameter.Rule.JSON_MAP)
				.format(Parameter.Format.JSON)
				.optional(true)
				.defaultValue(Data.map()))
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
