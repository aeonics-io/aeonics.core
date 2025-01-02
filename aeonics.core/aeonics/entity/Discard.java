package aeonics.entity;

import aeonics.data.Data;
import aeonics.entity.Step.Origin;
import aeonics.entity.security.User;
import aeonics.template.Channel;
import aeonics.util.Snapshotable.SnapshotMode;

/**
 * This class is a specialized form of {@link Origin} entity.
 * It is used by the internal data processing mechanics as a last resort to process otherwise discarded messages.
 * <p>You do not have direct access to the entity, you should use the static methods available in this class instead.</p>
 * @see #expired(Message)
 * @see #error(Message, Throwable)
 * @see #ignore(Message, String)
 * @see #drop(Message, String)
 * @see #policy(Message, String)
 */
public class Discard 
{
	private Discard() { /* no instances */ }
	
	public static void register()
	{
		// calling this method will force initialization of all private static members
	}
	
	/**
	 * The discard cause
	 */
	public enum Cause
	{
		/**
		 * The message was discarded because it is no longer valid 
		 */
		EXPIRED,
		/**
		 * The message was discarded because of an error
		 */
		ERROR,
		/**
		 * The message was discarded because it did not match any handler
		 */
		IGNORE,
		/**
		 * The message was discarded because it was explicitly dropped
		 */
		DROP,
		/**
		 * The message was discarded because of queue policy
		 */
		POLICY
	}
	
	private static final class _Discard extends Origin.Type
	{
		@Override
		public void produce(Message message, String channel)
		{
			if( message == null ) return;
			if( !started() ) start();
			
			// if the message was already discarded or is debug, ignore
			if( message.metadata().asBool("discarded") || message.metadata().asBool("debug") ) return;
			message.metadata().put("discarded", true);
			
			super.produce(message, channel);
		}
	}
	
	private static final _Discard DISCARD = new Origin() { }
		.target(_Discard.class)
		.creator(_Discard::new)
		.template()
		.<Step.Template>cast().icon("delete")
		.output(new Channel("data").summary("Data").description("Discarded data that has been ignored by other entities"))
		.summary("Discard")
		.description("This data origin is used as a last resort for all discarded messages. It publishes in the internal 'discard' topic.")
		.create(Data.map().put("id", "10000000-1600000000000000"))
		.name("Discard")
		.internal(true)
		.snapshotMode(SnapshotMode.UPDATE);
	
	/**
	 * Handle an expired message.
	 * An expired message can be because of a time validity or any other reason that make the message stale.
	 * @param message the message
	 */
	public static void expired(Message message)
	{
		discard(message, Cause.EXPIRED, null);
	}
	
	/**
	 * Handle an error during processing.
	 * A message can be in error because of a technical error (an exception), a business logic error (corrupted data),
	 * or any other dependency error (third party unavailable). 
	 * @param message the message
	 * @param cause the error
	 */
	public static void error(Message message, Throwable cause)
	{
		discard(message, Cause.ERROR, cause);
	}
	
	/**
	 * Handle an ignored message.
	 * A message is ignored when there is no handler or no binding to process it. It may also
	 * be a logical choice to ignore the message based on its content.
	 * @param message the message
	 * @param reason the reason
	 */
	public static void ignore(Message message, String reason)
	{
		discard(message, Cause.IGNORE, reason);
	}
	
	/**
	 * Handle an explicitly dropped message.
	 * A message can be dropped explicitly for any reason. It could be because it is expired, ignored or else.
	 * @param message the message
	 * @param reason the reason
	 */
	public static void drop(Message message, String reason)
	{
		discard(message, Cause.DROP, reason);
	}
	
	/**
	 * Handle a policy restriction.
	 * A queue or other internal system that imposes restrictions (delay, rate, queue size) may reject a
	 * message that does not meet the policy settings.
	 * @param message the message
	 * @param reason the reason
	 */
	public static void policy(Message message, String reason)
	{
		discard(message, Cause.POLICY, reason);
	}
	
	private static void discard(Message message, Cause cause, Object reason)
	{
		if( message == null ) return;
		
		message.key(cause + "/" + message.key());
		message.metadata().put("discard_user", message.user());
		message.metadata().put("discard_cause", cause);
		message.metadata().put("discard_reason", reason);
		message.metadata().put("discard_topic", message.metadata().asString("topic"));
		message.user(User.SYSTEM.id());

		DISCARD.produce(message, "data");
	}
}
