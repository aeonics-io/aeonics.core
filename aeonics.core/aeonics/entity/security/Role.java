package aeonics.entity.security;

import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.manager.Security;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Template;
import aeonics.util.Json;
import aeonics.util.StringUtils;

/**
 * This item plays a role in the definition of the {@link Security}.
 * It represents a role (or grant) that is given to a {@link Group} or to a {@link User}.
 * 
 * <p>There is no further meaning to the role than a label. It is a logical way to segment user access policies further down.</p>
 */
public class Role extends Item<Role.Type>
{
	public static final Role.Type SUPERADMIN = new Role().template().create(Data.map().put("id", "10000000-1300000000000000")).name("SUPERADMIN").internal(true);
	
	/**
	 * Superclass for all role entities.
	 */
	public static class Type extends Entity
	{
		/**
		 * Hardcoded category to the {@link Role} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Role.class); }
	}
	
	protected Class<? extends Role.Type> defaultTarget() { return Role.Type.class; }
	protected Supplier<? extends Role.Type> defaultCreator() { return Role.Type::new; }
	protected Class<? extends Role> category() { return Role.class; }

	@Override
	public Template<? extends Role.Type> template()
	{
		return super.template()
			.summary("Role")
			.description("A role represents a high level set of security policies that apply to individual users or to a group of users.")
			.add(new Parameter("attributes")
				.summary("Attributes")
				.description("Additional attributes common to all users with this role.")
				.rule(Parameter.Rule.JSON_MAP)
				.format(Parameter.Format.JSON)
				.optional(true)
				.defaultValue(Data.map())
				.validator((v) -> v != null && (v.isMap() || Json.decode(v.asString()).isMap())))
			;
	}
}
