package aeonics.entity.security;

import java.util.function.BiPredicate;
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
import aeonics.util.Tuples.Tuple;

/**
 * This item plays a role in the definition of the {@link Security}.
 * It represents a rule to grant or deny access based on some condition.
 */
public abstract class Rule extends Item<Rule.Type>
{
	/**
	 * Superclass for all role entities.
	 */
	public abstract static class Type extends Entity implements BiPredicate<User.Type, Data>
	{
		/**
		 * Hardcoded category to the {@link Rule} class
		 */
		public final String category() { return StringUtils.toLowerCase(Rule.class); }
	}
	
	protected Class<? extends Rule> category() { return Rule.class; }
	
	// =========================================
	//
	// BASIC RULES
	//
	// =========================================
	
	public static class And extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				for( Tuple<Entity, Data> rule : relations("rules") )
				{
					if( !((Rule.Type)rule.a).test(user, data) )
						return false;
				}
				return true;
			}
		}
		
		protected Class<? extends And.Type> defaultTarget() { return And.Type.class; }
		protected Supplier<? extends And.Type> defaultCreator() { return And.Type::new; }
		
		@SuppressWarnings("unchecked")
		public Template<? extends And.Type> template()
		{
			return (Template<And.Type>) super.template()
				.summary("AND")
				.description("Match multiple rules with the AND conditional logic. That is, the rule matches only if all related rules also match.")
				.add(new Relationship("rules")
					.category(Rule.class)
					.summary("Rules")
					.description("Other rules to match"));
		}
	}
	
	public static class Not extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				Rule.Type rule = firstRelation("rule");
				if( rule != null )
					return !rule.test(user, data);
				return true;
			}
		}
		
		protected Class<? extends Not.Type> defaultTarget() { return Not.Type.class; }
		protected Supplier<? extends Not.Type> defaultCreator() { return Not.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Not.Type> template()
		{
			return (Template<Not.Type>) super.template()
				.summary("NOT")
				.description("Inverts the related rule.")
				.add(new Relationship("rule")
					.category(Rule.class)
					.summary("Rule")
					.description("Other rules to invert")
					.max(1));
		}
	}
	
	public static class Or extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				for( Tuple<Entity, Data> rule : relations("rules") )
				{
					if( ((Rule.Type)rule.a).test(user, data) )
						return true;
				}
				return false;
			}
		}
		
		protected Class<? extends Or.Type> defaultTarget() { return Or.Type.class; }
		protected Supplier<? extends Or.Type> defaultCreator() { return Or.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Or.Type> template()
		{
			return (Template<Or.Type>) super.template()
				.summary("OR")
				.description("Match multiple rules with the OR conditional logic. That is, the rule matches if at least one of the related rules also match.")
				.add(new Relationship("rules")
					.category(Rule.class)
					.summary("Rules")
					.description("Other rules to match"));
		}
	}
	
	public static class Xor extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				boolean match = false;
				for( Tuple<Entity, Data> rule : relations("rules") )
				{
					if( ((Rule.Type)rule.a).test(user, data) )
					{
						if( match ) return false;
						match = true;
					}
				}
				return match;
			}
		}
		
		protected Class<? extends Xor.Type> defaultTarget() { return Xor.Type.class; }
		protected Supplier<? extends Xor.Type> defaultCreator() { return Xor.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Xor.Type> template()
		{
			return (Template<Xor.Type>) super.template()
				.summary("XOR")
				.description("Match multiple rules with the XOR conditional logic. That is, the rule matches if exactly-and-only one of the related rules also match.")
				.add(new Relationship("rules")
					.category(Rule.class)
					.summary("Rules")
					.description("Other rules to match"));
		}
	}
	
	public static class MatchAll extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				return true;
			}
		}
		
		protected Class<? extends MatchAll.Type> defaultTarget() { return MatchAll.Type.class; }
		protected Supplier<? extends MatchAll.Type> defaultCreator() { return MatchAll.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends MatchAll.Type> template()
		{
			return (Template<MatchAll.Type>) super.template()
				.summary("Match all")
				.description("A security rule that matches everything without any conditions.");
		}
	}
	
	public static class MatchNone extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				return false;
			}
		}
		
		protected Class<? extends MatchNone.Type> defaultTarget() { return MatchNone.Type.class; }
		protected Supplier<? extends MatchNone.Type> defaultCreator() { return MatchNone.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends MatchNone.Type> template()
		{
			return (Template<MatchNone.Type>) super.template()
				.summary("Match none")
				.description("A security rule that does not match anything no matter what.");
		}
	}
	
	public static class MatchAttribute extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				if( user == null ) return false;
				
				String attribute = valueOf("attribute").asString();
				Data value = valueOf("value");
				Data attributes = user.valueOf("attributes");
				
				// user own attributes
				if( attributes.isMap() && attributes.equals(attribute, value) ) return true;
				
				// user role attributes
				for( Tuple<Entity, Data> roles : user.relations("roles") )
				{
					if( roles.a == null ) continue;
					attributes = roles.a.valueOf("attributes");
					if( attributes.isMap() && attributes.equals(attribute, value) ) return true;
				}
				
				// group attributes
				for( Tuple<Entity, Data> groups : user.relations("groups") )
				{
					if( groups.a == null ) continue;
					Group.Type group = groups.a.cast();
					attributes = group.valueOf("attributes");
					if( attributes.isMap() && attributes.equals(attribute, value) ) return true;
					
					// group role attributes
					for( Tuple<Entity, Data> roles : group.relations("roles") )
					{
						if( roles.a == null ) continue;
						attributes = roles.a.valueOf("attributes");
						if( attributes.isMap() && attributes.equals(attribute, value) ) return true;
					}
				}
				
				return false;
			}
		}
		
		protected Class<? extends MatchAttribute.Type> defaultTarget() { return MatchAttribute.Type.class; }
		protected Supplier<? extends MatchAttribute.Type> defaultCreator() { return MatchAttribute.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends MatchAttribute.Type> template()
		{
			return (Template<MatchAttribute.Type>) super.template()
				.summary("Match user attribute")
				.description("A security rule that checks if a user attribute is equal to the specified value. The attribute may be inherited by one of the user groups or roles.")
				.add(new Parameter("attribute")
					.summary("Attribute name")
					.description("The name of the user attribute to match.")
					.format(Parameter.Format.TEXT))
				.add(new Parameter("value")
					.summary("Expected value")
					.description("The expected value of the user attribute to match")
					.format(Parameter.Format.TEXT))
				;
		}
	}
	
	public static class MatchContext extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				if( data == null || !data.isMap() ) return false;
				
				String property = valueOf("property").asString();
				Data value = valueOf("value");
				
				if( valueOf("wildcard").asBool() )
					return StringUtils.simplePathMatches(value.asString(), data.getNested(property).asString());
				else
					return data.getNested(property).equals(value);
			}
		}
		
		protected Class<? extends MatchContext.Type> defaultTarget() { return MatchContext.Type.class; }
		protected Supplier<? extends MatchContext.Type> defaultCreator() { return MatchContext.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends MatchContext.Type> template()
		{
			return (Template<MatchContext.Type>) super.template()
				.summary("Match context")
				.description("A security rule that checks the current context data to verify if a property is equal to the specified value.")
				.add(new Parameter("property")
					.summary("Property name")
					.description("The name of the context property to match.")
					.format(Parameter.Format.TEXT))
				.add(new Parameter("value")
					.summary("Expected value")
					.description("The expected value of the context property to match")
					.format(Parameter.Format.TEXT))
				.add(new Parameter("wildcard")
					.summary("Wildcard pattern")
					.description("Whether or not the value should be applied using the wildcard path logic.")
					.rule(Parameter.Rule.BOOLEAN)
					.format(Parameter.Format.BOOLEAN))
				;
		}
	}
	
	public static class AskProviders extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				try
				{
					for( Provider.Type provider : Registry.of(Provider.class) )
						if( provider.supports(user.id()) && !provider.check(this, user, data) )
							return false;
					return true;
				}
				catch(Exception e)
				{
					return false;
				}
			}
		}
		
		protected Class<? extends AskProviders.Type> defaultTarget() { return AskProviders.Type.class; }
		protected Supplier<? extends AskProviders.Type> defaultCreator() { return AskProviders.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends AskProviders.Type> template()
		{
			return (Template<AskProviders.Type>) super.template()
				.summary("Ask providers")
				.description("Delegates the decision to all identity providers that support the user. The rule will pass only if all applicable providers allow it.")
				;
		}
	}
	
	public static class Group extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				try
				{
					String pattern = valueOf("group").asString();
					for( Tuple<Entity, Data> group : user.relations("groups") )
					{
						if( group.a.id().equals(pattern) ||
							StringUtils.simplePathMatches(pattern, group.a.name()) )
							return true;
					}
					
					return false;
				}
				catch(Exception e)
				{
					return false;
				}
			}
		}
		
		protected Class<? extends Group.Type> defaultTarget() { return Group.Type.class; }
		protected Supplier<? extends Group.Type> defaultCreator() { return Group.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Group.Type> template()
		{
			return (Template<Group.Type>) super.template()
				.summary("User group")
				.description("Checks if the user is a member of the specified group.")
				.add(new Parameter("group")
					.summary("Group name or id")
					.description("The group name or id to match. This property can include a wildcard pattern in case of the group name.")
					.format(Parameter.Format.TEXT)
					);
		}
	}
	
	public static class Role extends Rule
	{
		public static class Type extends Rule.Type
		{
			public boolean test(User.Type user, Data data)
			{
				try
				{
					String pattern = valueOf("role").asString();
					for( Tuple<Entity, Data> role : user.relations("roles") )
					{
						if( role.a.id().equals(pattern) ||
							StringUtils.simplePathMatches(pattern, role.a.name()) )
							return true;
					}
					
					// inherited roles via group membership 
					for( Tuple<Entity, Data> group : user.relations("groups") )
					{
						for( Tuple<Entity, Data> role : group.a.relations("roles") )
						{
							if( role.a.id().equals(pattern) ||
								StringUtils.simplePathMatches(pattern, role.a.name()) )
								return true;
						}
					}
					
					return false;
				}
				catch(Exception e)
				{
					return false;
				}
			}
		}
		
		protected Class<? extends Role.Type> defaultTarget() { return Role.Type.class; }
		protected Supplier<? extends Role.Type> defaultCreator() { return Role.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Role.Type> template()
		{
			return (Template<Role.Type>) super.template()
				.summary("User role")
				.description("Checks if the user has the specified role.")
				.add(new Parameter("role")
					.summary("Role name or id")
					.description("The role name or id to match. This property can include a wildcard pattern in case of the role name.")
					.format(Parameter.Format.TEXT)
					);
		}
	}
}
