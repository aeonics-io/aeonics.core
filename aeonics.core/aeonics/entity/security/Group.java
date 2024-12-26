package aeonics.entity.security;

import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.manager.Security;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.Json;
import aeonics.util.StringUtils;

/**
 * This item plays a role in the definition of the {@link Security}.
 * It represents a group of {@link User}. All users inherit the {@link Role} of the group(s) they are member of.
 */
public class Group extends Item<Group.Type>
{
	public static final Group.Type ADMINISTRATORS = new Group().template().create(Data.map().put("id", "10000000-1800000000000000"))
		.name("Administrators").internal(true).addRelation("roles", Role.SUPERADMIN).cast();
	public static final Group.Type USERS = new Group().template().create(Data.map().put("id", "10000000-1900000000000000"))
		.name("Users").internal(true);
	
	/**
	 * Superclass for all group entities.
	 */
	public static class Type extends Entity
	{
		/**
		 * Hardcoded category to the {@link Group} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Group.class); }
	}
	
	protected Class<? extends Group.Type> defaultTarget() { return Group.Type.class; }
	protected Supplier<? extends Group.Type> defaultCreator() { return Group.Type::new; }
	protected Class<? extends Group> category() { return Group.class; }

	@Override
	public Template<? extends Group.Type> template()
	{
		return super.template()
			.summary("User Group")
			.description("A group represents a set of users that share common attributes and/or roles. It is useful if you have predefined security policies and you do not want to defined them on a per-user level.")
			.add(new Parameter("attributes")
				.summary("Attributes")
				.description("Additional group attributes that all users of this group shall inherit.")
				.defaultValue(Data.map())
				.format(Parameter.Format.JSON)
				.rule(Parameter.Rule.JSON_MAP)
				.optional(true)
				.validator((v) -> v != null && (v.isMap() || Json.decode(v.asString()).isMap())))
			.add(new Relationship("roles")
				.category(Role.class)
				.summary("Roles")
				.description("List of roles that apply to all users in this group."))
			.icon("group");
	}
}
