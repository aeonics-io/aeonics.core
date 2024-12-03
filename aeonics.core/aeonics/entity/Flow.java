package aeonics.entity;

import java.util.function.Supplier;

import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.StringUtils;

/**
 * This entity represents a data flow. 
 * It is a high-level representation that encapsulates the interconnected elements of a data pipeline: 
 * {@link Origin}, {@link Action}, {@link Destination}, {@link Topic}, and {@link Queue}, 
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
			.add(new Relationship("origins")
				.summary("Origins")
				.description("The origin entities that are part of this data flow.")
				.category(Origin.class)
				.add(new Parameter("x").summary("X").description("The X position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
				.add(new Parameter("y").summary("Y").description("The Y position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
			)
			.add(new Relationship("actions")
				.summary("Actions")
				.description("The action entities that are part of this data flow.")
				.category(Action.class)
				.add(new Parameter("x").summary("X").description("The X position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
				.add(new Parameter("y").summary("Y").description("The Y position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
			)
			.add(new Relationship("destinations")
					.summary("Destinations")
					.description("The destination entities that are part of this data flow.")
					.category(Destination.class)
					.add(new Parameter("x").summary("X").description("The X position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
					.add(new Parameter("y").summary("Y").description("The Y position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
			)
			.add(new Relationship("queues")
					.summary("Queues")
					.description("The queue entities that are part of this data flow.")
					.category(Queue.class)
					.add(new Parameter("x").summary("X").description("The X position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
					.add(new Parameter("y").summary("Y").description("The Y position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
			)
			.add(new Relationship("topics")
					.summary("Topics")
					.description("The topic entities that are part of this data flow.")
					.category(Topic.class)
					.add(new Parameter("x").summary("X").description("The X position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
					.add(new Parameter("y").summary("Y").description("The Y position in the data flow grid").format(Parameter.Format.NUMBER).rule(Parameter.Rule.INTEGER).defaultValue(0))
			)
			;
	}
}
