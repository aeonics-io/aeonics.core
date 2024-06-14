package aeonics.entity;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import aeonics.template.Relationship;

/**
 * This entity represents a logical Flow to process data. It has its own input and output channels but it also contains other {@link Queue}, 
 * {@link Action} and {@link Destination} entities that participate to the logical flow.
 * 
 */
public class Flow extends Action
{
	public static class Type extends Action.Type
	{
		@Override
		public Map<String, Message> accept(Message message, String input, Set<String> outputs)
		{
			// TODO : implement flow as action
			throw new RuntimeException("Not implemented");
		}
	}

	@Override
	protected Class<? extends Flow.Type> defaultTarget() { return Flow.Type.class; }
	@Override
	protected Supplier<? extends Flow.Type> defaultCreator() { return Flow.Type::new; }
	@Override
	protected Class<? extends Flow> category() { return Flow.class; }

	@Override
	public Action.Template template()
	{
		return super.template()
			.summary("Flow")
			.description("Flows regroup Queues, Actions and Destinations into a logical runtime context. "
				+ "Meanwhile, Flows can also be embeded in another flow as an Action")
			.add(new Relationship("queues")
				.summary("Queues")
				.description("The list of queues that are considered to be part of this flow."))
			; 
	}
}
