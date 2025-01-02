package aeonics.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.entity.Step.Action;
import aeonics.entity.security.User;
import aeonics.manager.Manager;
import aeonics.manager.Security;
import aeonics.template.Channel;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.util.StringUtils;
import aeonics.util.Tuples.Tuple;

/**
 * This class represents a publishing topic for messages.
 * A {@link Queue} can subscribe to a topic by providing a binding key. 
 * The mechanism to subscribe to a topic depends on the implementation.
 */
public class Topic extends Action
{
	public static class Type extends Action.Type
	{
		public Type()
		{
			super();
			
			security((user, message, channel) ->
			{
				if( !Manager.of(Security.class).granted(user == null ? User.ANONYMOUS : user, "topic", Data.map().put("message", message).put("topic", id())) )
					throw new SecurityException("Access denied");
			});
		}
		
		/**
		 * Override the link collection to include only matching subscriptions
		 */
		@Override
		List<Tuple<Entity, Data>> collectLinks(String output)
		{
			if( !output.equals("subscribe") ) return super.collectLinks(output);
			
			List<Tuple<Entity, Data>> links = new ArrayList<>();
			for( Tuple<Entity, Data> link : relations("links") )
			{
				if( link.a == null || !link.b.asString("output").equals(output) )
					continue;
				if( !(link.a instanceof Step.Action.Type) && !(link.a instanceof Step.Destination.Type) )
					continue;			
				if( StringUtils.simplePathMatches(link.b.asString("binding"), ((Message)context().get()).key()) )
					links.add(link);
			}

			return links;
		}
		
		@Override
		protected Message process(Message message, String input, String output) throws Exception
		{
			// mark this message as it passed through this topic
			message.metadata().put("topic", id());
			return message;
		}
		
		/**
		 * Publishes a message on this topic.
		 * The message will be delivered to all links with a matching subscription.
		 * @param message the message to deliver
		 */
		public void publish(Message message)
		{
			if( message == null ) return;
			accept(message, "publish");
		}
	}
	
	protected Class<? extends Topic.Type> defaultTarget() { return Topic.Type.class; }
	protected Supplier<? extends Topic.Type> defaultCreator() { return Topic.Type::new; }

	@Override
	public Template template()
	{
		return super.template()
			.input(new Channel("publish")
				.summary("Publish")
				.description("Publish a message on this topic."))
			.output(new Channel("subscribe")
				.summary("Subscribe")
				.description("Subscriptions on this topic."))
			.icon("alt_route")
			.summary("Topic")
			.description("Dispatches messages to subscribed entities.")
			
			// override links parameter
			.add(new Relationship("links")
				.category(Step.class)
				.summary("Next flow step")
				.description("The next step to send data after processing.")
				.add(new Parameter("input")
					.summary("Input Channel")
					.description("The name of the input channel of the target step.")
					.format(Parameter.Format.TEXT))
				.add(new Parameter("binding")
					.summary("Subscription binding key")
					.description("The subscription key allows to filter the messages that the queue will receive.")
					.format(Parameter.Format.TEXT)
					.optional(true)
					.defaultValue("#")))
			
			.cast();
	}
}
