package aeonics.entity.security;

import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.manager.Security;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.StringUtils;
import aeonics.util.Tuples.Tuple;

/**
 * This item plays a role in the definition of the {@link Security}.
 * It allows to define the {@link Rule} applied to allow or deny access.
 * 
 * <p>There are four main policies:</p>
 * <ul>
 * <li>{@link Allow} that applies to everyone</li>
 * <li>{@link Deny} that applies to everyone</li>
 * <li>{@link TargetedAllow} that applies to target users, roles or groups.</li>
 * <li>{@link TargetedDeny} that applies to target users, roles or groups.</li>
 * </ul>
 * Other custom policies may be implemented to combine {@link Type#isAllowed(User.Type, Data)} and {@link Type#isDenied(User.Type, Data)}.
 */
public abstract class Policy extends Item<Policy.Type>
{
	// =========================================
	//
	// BASE POLICY
	//
	// =========================================
	
	/**
	 * Superclass for all policy entities.
	 */
	public abstract static class Type extends Entity
	{
		/**
		 * Evaluates the related rule to know if the access is denied
		 * @param user the concerned user, must not be null but may be {@link User#ANONYMOUS} or {@link User#SYSTEM}
		 * @param context the context data to feed to the rule, may be null
		 * @return true if the rule matches, false otherwise
		 */
		public abstract boolean isDenied(User.Type user, Data context);
		
		/**
		 * Evaluates the related rule to know if the access is allowed
		 * @param user the concerned user, must not be null but may be {@link User#ANONYMOUS} or {@link User#SYSTEM}
		 * @param context the context data to feed to the rule, may be null
		 * @return true if the rule matches, false otherwise
		 */
		public abstract boolean isAllowed(User.Type user, Data context);
		
		/**
		 * Hardcoded category to the {@link Policy} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Policy.class); }
	}

	@Override
	public Template<? extends Policy.Type> template()
	{
		return super.template()
			.add(new Parameter("scope")
				.summary("Scope")
				.description("The scope to which this policy applies. The scope will be used as a filter to select applicable policies.")
				.format(Parameter.Format.TEXT))
			.add(new Relationship("rule")
				.category(Rule.class)
				.summary("Rule")
				.description("The security rule to apply to this policy. The rule can be a logical combination of other rules.")
				.max(1))
			;
	}
	
	protected Class<? extends Policy> category() { return Policy.class; }
	
	// =========================================
	//
	// NON-TARGETED ALLOW / DENY
	//
	// =========================================
	
	/**
	 * A policy that validates the rule to allow access. The rule applies to everyone.
	 */
	public static class Allow extends Policy
	{
		public static class Type extends Policy.Type
		{
			public boolean isDenied(User.Type user, Data context) { return false; }

			public boolean isAllowed(User.Type user, Data context)
			{
				if( user == User.SYSTEM ) return true;
				
				try
				{
					for( Tuple<Entity, Data> rules : relations("rule") )
					{
						Rule.Type rule = rules.a.cast();
						if( rule != null )
							return rule.test(user, context);
					}
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).warning(Rule.class, e);
				}
				return false;
			}
		}
		
		protected Class<? extends Allow.Type> defaultTarget() { return Allow.Type.class; }
		protected Supplier<? extends Allow.Type> defaultCreator() { return Allow.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Allow.Type> template()
		{
			return (Template<Allow.Type>) super.template()
				.summary("Allow Everyone")
				.description("A policy that evaluates a security rule for everyone in order to allow access.");
		}
	}
	
	/**
	 * A policy that validates the rule to deny access. The rule applies to everyone.
	 */
	public static class Deny extends Policy
	{
		public static class Type extends Policy.Type
		{
			public boolean isAllowed(User.Type user, Data context) { return false; }

			public boolean isDenied(User.Type user, Data context)
			{
				if( user == User.SYSTEM ) return false;
				
				try
				{
					for( Tuple<Entity, Data> rules : relations("rule") )
					{
						Rule.Type rule = rules.a.cast();
						if( rule != null )
							return rule.test(user, context);
					}
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).warning(Rule.class, e);
				}
				return false;
			}
		}
		
		protected Class<? extends Deny.Type> defaultTarget() { return Deny.Type.class; }
		protected Supplier<? extends Deny.Type> defaultCreator() { return Deny.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Deny.Type> template()
		{
			return (Template<Deny.Type>) super.template()
				.summary("Deny Everyone")
				.description("A policy that evaluates a security rule for everyone in order to deny access.");
		}
	}
	
	// =========================================
	//
	// TARGETED ALLOW / DENY
	//
	// =========================================
	
	/**
	 * Base policy that can apply to multiple targets: {@link User}, {@link Role}, or {@link Group}.
	 * <p>Implementations are encouraged to call {@link Type#appliesTo(User.Type)} first in order to determine 
	 * if the specified user is a target of the policy.</p>
	 */
	public abstract static class TargetedPolicy extends Policy
	{
		/**
		 * Superclass for all targeted policy entities.
		 */
		public abstract static class Type extends Policy.Type
		{
			/**
			 * Determines if this policy applies to the provided user.
			 * <p>That is:</p><ul>
			 * <li>if the user is directly related to this policy,</li>
			 * <li>or if the user has a role that is related to this policy,</li> 
			 * <li>or if the user is a member of a group that is directly related to this policy,</li>
			 * <li>or if the user is a member of a group that has a role that is related to this policy.</li>
			 * </ul>
			 * @param user the user to check
			 * @return true if this policy applies to the provided user
			 */
			public boolean appliesTo(User.Type user)
			{
				if( user == null ) return false;
				
				// 1. USER IS REFERENCED
				String id = user.id();
				for( Tuple<Entity, Data> users : relations("users") )
					if( users.a != null && id.equals(users.a.id()) )
						return true;
				
				// 2. USER->ROLE IS REFERENCED
				for( Tuple<Entity, Data> roles : relations("roles") )
				{
					if( roles.a == null ) continue;
					id = roles.a.id();
					for( Tuple<Entity, Data> userroles : user.relations("roles") )
						if( userroles.a != null && id.equals(userroles.a.id()) )
							return true;
				}
				
				// 3. USER->GROUP IS REFERENCED
				for( Tuple<Entity, Data> groups : relations("groups") )
				{
					if( groups.a == null ) continue;
					id = groups.a.id();
					for( Tuple<Entity, Data> usergroups : user.relations("groups") )
						if( usergroups.a != null && id.equals(usergroups.a.id()) )
							return true;
					
					// 4. USER->GROUP->ROLE IS REFERENCED
					for( Tuple<Entity, Data> roles : relations("roles") )
					{
						if( roles.a == null ) continue;
						id = roles.a.id();
						for( Tuple<Entity, Data> grouproles : groups.a.relations("roles") )
							if( grouproles.a != null && id.equals(grouproles.a.id()) )
								return true;
					}
				}
				
				return false;
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends TargetedPolicy.Type> template()
		{
			return (Template<TargetedPolicy.Type>) super.template()
				.type(TargetedPolicy.class)
				.add(new Relationship("users")
					.category(User.class)
					.summary("Users")
					.description("The list of users for which this policy applies explicitly. Normally a policy applies to a role, but exceptions can be added for individual users."))
				.add(new Relationship("groups")
					.category(Group.class)
					.summary("Groups")
					.description("The list of groups for which this policy applies explicitly. Normally a policy applies to a role, but exceptions can be added for individual groups of users."))
				.add(new Relationship("roles")
					.category(Role.class)
					.summary("Roles")
					.description("The list of roles for which this policy applies."))
				;
		}
	}
	
	/**
	 * A policy that validates the rule to allow access for specific target users, roles or groups.
	 * This policy should be used in a deny-by-default approach.
	 */
	public static class TargetedAllow extends TargetedPolicy
	{
		public static class Type extends TargetedPolicy.Type
		{
			public boolean isDenied(User.Type user, Data context) { return false; }

			public boolean isAllowed(User.Type user, Data context)
			{
				if( user == User.SYSTEM ) return true;
				if( !appliesTo(user) ) return false;
				
				try
				{
					for( Tuple<Entity, Data> rules : relations("rule") )
					{
						Rule.Type rule = rules.a.cast();
						if( rule != null )
							return rule.test(user, context);
					}
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).warning(Rule.class, e);
				}
				return false;
			}
		}
		
		protected Class<? extends TargetedAllow.Type> defaultTarget() { return TargetedAllow.Type.class; }
		protected Supplier<? extends TargetedAllow.Type> defaultCreator() { return TargetedAllow.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends TargetedAllow.Type> template()
		{
			return (Template<TargetedAllow.Type>) super.template()
				.summary("Allow Target")
				.description("A policy that evaluates a security rule for specific targets (users, roles or groups) in order to allow access.");
		}
	}
	
	/**
	 * A policy that validates the rule to deny access for specific target users, roles or groups.
	 * This policy should be used in an allow-by-default approach.
	 */
	public static class TargetedDeny extends TargetedPolicy
	{
		public static class Type extends TargetedPolicy.Type
		{
			public boolean isAllowed(User.Type user, Data context) { return false; }

			public boolean isDenied(User.Type user, Data context)
			{
				if( user == User.SYSTEM ) return false;
				if( !appliesTo(user) ) return false;
				
				try
				{
					for( Tuple<Entity, Data> rules : relations("rule") )
					{
						Rule.Type rule = rules.a.cast();
						if( rule != null )
							return rule.test(user, context);
					}
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).warning(Rule.class, e);
				}
				return false;
			}
		}
		
		protected Class<? extends TargetedDeny.Type> defaultTarget() { return TargetedDeny.Type.class; }
		protected Supplier<? extends TargetedDeny.Type> defaultCreator() { return TargetedDeny.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends TargetedDeny.Type> template()
		{
			return (Template<TargetedDeny.Type>) super.template()
				.summary("Deny Target")
				.description("A policy that evaluates a security rule for specific targets (users, roles or groups) in order to deny access.");
		}
	}
}
