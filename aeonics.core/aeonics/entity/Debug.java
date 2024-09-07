package aeonics.entity;

import java.util.Arrays;
import java.util.stream.Collectors;

import aeonics.data.Data;
import aeonics.entity.security.User;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.template.Channel;

/**
 * This class can be used from anywhere in the system to send troubleshooting information to be processed as data.
 */
public class Debug 
{
	private Debug() { /* no instances */ }
	
	private static final class _Debug extends Origin.Basic.Type
	{
		@Override
		public void emit(Message message, String channel)
		{
			if( message == null ) return;
			
			// if the message was already discarded or is debug, ignore
			if( message.metadata().asBool("discarded") || message.metadata().asBool("debug") ) return;
			message.metadata().put("debug", true);
			
			super.emit(message, channel);
		}
	}
	
	private static final Origin.Type DEBUG = new Origin.Basic() { }
		.target(_Debug.class)
		.creator(_Debug::new)
		.template()
		.summary("Debug")
		.description("This data origin is used as a common central debug point. It publishes in the internal 'debug' topic.")
		.<Origin.Template>cast()
		.output(new Channel("data")
			.summary("Data")
			.description("All messages are published through this channel"))
		.create(Data.map().put("id", "10000000-1400000000000000"))
		.addRelation("topics", new Topic()
			.template()
			.create(Data.map().put("id", "10000000-1500000000000000"))
			.name("debug")
			.internal(true), Data.map().put("channel", "data"))
		.name("Debug")
		.internal(true);
	
	/**
	 * Generate debug data. The call stack trace will be included in the message
	 * @param key the message key (see {@link Message#key()})
	 * @param values all the values that should be included
	 */
	public static void debug(String key, Object ...values)
	{
		Message m = new Message(key);
		m.user(User.SYSTEM.id());
		m.content(Data.map().put("stack", getStackTrace()).put("values", values));
		
		DEBUG.emit(m, "data");
	}
	
	private static Data getStackTrace()
	{
		int level = Manager.of(Logger.class).level();
		return Data.of(Arrays.stream(new Exception().fillInStackTrace().getStackTrace())
			.filter((e) ->
			{
				return (level <= Logger.ALL || ((e.getModuleName() == null || !e.getModuleName().startsWith("java.")) 
					|| !e.getClassName().startsWith("java.") 
					|| !e.getClassName().startsWith("javax.") 
					|| !e.getClassName().startsWith("jdk.") 
					|| !e.getClassName().startsWith("sun.") 
					|| !e.getClassName().startsWith("aeonics.")) );
			})
			.collect(Collectors.toList()));
	}
}
