package aeonics.entity;

import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.StringUtils;
import aeonics.util.Tuples.Tuple;

/**
 * This entity represents a data flow. 
 * It is a high-level representation that encapsulates the interconnected elements of a data pipeline,
 * defining the logical movement, transformation, and organization of data from source to destination.
 * 
 * This entity does not play an active role in the data processing, it is mainly used to render data flows visually.
 */
public class Flow extends Item<Flow.Type>
{
	/**
	 * Superclass template for flows
	 */
	public static class Type extends Entity
	{
		/**
		 * Hardcoded category to the {@link Flow} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Flow.class); }
		
		/**
		 * Adds a step to this flow. This is equivalent to {@link #addRelation(String, Entity)}
		 * @param <T> this type
		 * @param step the step
		 * @param x the x location
		 * @param y the y location
		 * @return this
		 */
		@SuppressWarnings("unchecked")
		public <T extends Type> T step(Step.Type step, int x, int y)
		{
			// check if already exists
			for( Tuple<Entity, Data> s : relations("steps") )
			{
				if( s.a == step )
				{
					s.b.put("x", x).put("y", y);
					return (T) this;
				}
			}
			
			addRelation("steps", step, Data.map().put("x", x).put("y", y));
			return (T) this;
		}
	}

	protected Class<? extends Type> defaultTarget() { return Flow.Type.class; }
	protected Supplier<? extends Type> defaultCreator() { return Flow.Type::new; }
	protected Class<? extends Item<? super Type>> category() { return Flow.class; }
	
	@SuppressWarnings("unchecked")
	@Override
	public Template<? extends Flow.Type> template()
	{
		return (Template<Flow.Type>) super.template()
			.summary("Data flow")
			.description("A data flow is a high-level representation used to regroup interconnected elements of a data pipeline.")
			.add(new Parameter("size")
				.summary("Size")
				.description("The grid size of the data flow.")
				.format(Parameter.Format.NUMBER)
				.rule(Parameter.Rule.INTEGER)
				.defaultValue(5))
			.add(new Parameter("notes")
				.summary("Notes")
				.description("Notes about this data flow.")
				.format(Parameter.Format.TEXT)
				.optional(true))
			.add(new Relationship("steps")
				.summary("Steps")
				.description("The different steps that are part of this data flow.")
				.category(Step.class)
				.add(new Parameter("x").summary("X").description("The X position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
				.add(new Parameter("y").summary("Y").description("The Y position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
			)
			;
	}
}
