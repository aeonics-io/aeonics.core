package aeonics.entity;

import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.entity.security.User;
import aeonics.manager.Manager;
import aeonics.manager.Monitor;
import aeonics.manager.Security;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.StringUtils;
import aeonics.util.Tuples.Tuple;

/**
 * This class represents a publishing topic for messages.
 * A {@link Queue} can subscribe to a topic by providing a binding key. 
 * The mechanism to subscribe to a topic depends on the implementation.
 */
public class Topic extends Item<Topic.Type>
{
	public static class Type extends Entity
	{
		/**
		 * Publishes a message on this topic.
		 * The message will be delivered to all queues with a matching subscription.
		 * @param message the message to deliver
		 */
		public void publish(Message message)
		{
			if( message == null ) return;
			
			User.Type user = Registry.of(User.class).get(message.user());
			if( !Manager.of(Security.class).granted(user == null ? User.ANONYMOUS : user, "topic", Data.map().put("message", message).put("topic", id())) )
				throw new SecurityException("Access denied");
			
			Manager.of(Monitor.class).count(this);

			// mark this message as it passed through this topic
			message.metadata().put("topic", id());
			
			String key = message.key();
			boolean shouldClone = false;
			for( Tuple<Entity, Data> relation : this.relations("queues") )
			{
				if( StringUtils.simplePathMatches(relation.b.asString("binding"), key) )
				{
					Queue.Type q = relation.a.cast();
					if( q != null )
					{
						try
						{ q.accept(shouldClone ? message.clone() : message); shouldClone = true; }
						catch(Throwable e)
						{
							Discard.error(message, e);
						}
					}
				}
				else
				{
					Discard.ignore(message, "No matching queue process the message");
				}
			}
		}
		
		/**
		 * Hardcoded category to the {@link Topic} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Topic.class); }
	}
	
	protected Class<? extends Topic.Type> defaultTarget() { return Topic.Type.class; }
	protected Supplier<? extends Topic.Type> defaultCreator() { return Topic.Type::new; }
	protected Class<? extends Topic> category() { return Topic.class; }

	@Override
	public Template<? extends Topic.Type> template()
	{
		return super.template()
			.summary("Topic")
			.description("Dispatches messages to subscribed queues.")
			.add(new Relationship("queues")
				.category(Queue.class)
				.summary("Queues")
				.description("The list of subscriptions.")
				.add(new Parameter("binding")
					.summary("Subscription binding key")
					.description("The subscription key allows to filter the messages that the queue will receive.")
					.format(Parameter.Format.TEXT)
					.optional(true)
					.defaultValue("#")));
	}
}
