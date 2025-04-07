package aeonics.entity;

import java.util.Arrays;
import java.util.stream.Collectors;

import aeonics.data.Data;
import aeonics.entity.Step.Origin;
import aeonics.entity.security.User;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.template.Channel;
import aeonics.util.Snapshotable.SnapshotMode;

/**
 * This class can be used from anywhere in the system to send troubleshooting information to be processed as data.
 */
public class Debug 
{
	private Debug() { /* no instances */ }
	
	public static void register()
	{
		// calling this method will force initialization of all private static members
	}
	
	private static final class _Debug extends Origin.Type
	{
		@Override
		public void produce(Message message, String channel)
		{
			if( message == null ) return;
			if( !started() ) start();
			
			// if the message was already discarded or is debug, ignore
			if( message.metadata().asBool("discarded") || message.metadata().asBool("debug") ) return;
			message.metadata().put("debug", true);
			
			super.produce(message, channel);
		}
	}
	
	private static final _Debug DEBUG = new Origin() { }
		.target(_Debug.class)
		.creator(_Debug::new)
		.template()
		.<Step.Template>cast().icon("bug_report")
		.output(new Channel("data").summary("Debug").description("Debug information sent from the system"))
		.summary("Debug")
		.description("This data origin is used as a common central debug point.")
		.create(Data.map().put("id", "10000000-1400000000000000"))
		.name("Debug")
		.internal(true)
		.snapshotMode(SnapshotMode.UPDATE)
		;
	
	/**
	 * Generate debug data with the specified values and the call stack trace
	 * @param key the message key (see {@link Message#key()})
	 * @param values all the values that should be included
	 */
	public static void debug(String key, Object ...values)
	{
		Message m = new Message(key);
		m.user(User.SYSTEM.id());
		m.content(Data.map().put("stack", getStackTrace()).put("values", values));
		
		DEBUG.produce(m, "data");
	}
	
	/**
	 * Generate debug data with the specified values
	 * @param key the message key (see {@link Message#key()})
	 * @param values all the values that should be included
	 */
	public static void values(String key, Object ...values)
	{
		Message m = new Message(key);
		m.user(User.SYSTEM.id());
		m.content(Data.map().put("values", values));
		
		DEBUG.produce(m, "data");
	}
	
	/**
	 * Generate debug data with the call stack trace
	 * @param key the message key (see {@link Message#key()})
	 */
	public static void stacktrace(String key)
	{
		Message m = new Message(key);
		m.user(User.SYSTEM.id());
		m.content(Data.map().put("stack", getStackTrace()));
		
		DEBUG.produce(m, "data");
	}
	
	private static Data getStackTrace()
	{
		int level = Manager.of(Logger.class).level();
		return Data.of(Arrays.stream(new Exception().fillInStackTrace().getStackTrace())
			.filter((e) ->
			{
				if( e.getClassName().equals(Debug.class.getName()) ) return false;
				
				return (level <= Logger.ALL || !(
					(e.getModuleName() != null && e.getModuleName().startsWith("java."))
					|| e.getClassName().startsWith("java.")
					|| e.getClassName().startsWith("javax.")
					|| e.getClassName().startsWith("jdk.")
					|| e.getClassName().startsWith("sun.")
					|| e.getClassName().startsWith("aeonics.")) );
			})
			.collect(Collectors.toList()));
	}
}
