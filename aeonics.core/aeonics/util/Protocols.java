package aeonics.util;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class can be used to register multiple protocol schemes usable via the {@link java.net.URL} class. 
 * This is because an application can only call {@link java.net.URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)} once which is 
 * not compatible with a modular system that needs to register multiple protocols.
 */
public class Protocols implements URLStreamHandlerFactory
{
	private static Map<String, URLStreamHandler> handlers = new ConcurrentHashMap<>();
	
	public URLStreamHandler createURLStreamHandler(String protocol)
	{
		return handlers.get(protocol);
	}
	
	/**
	 * Registers a new protocol scheme
	 * @param protocol the protocol name (i.e.: for "http://" the protocol is "http")
	 * @param handler the URLStreamHandler to handle the protocol
	 */
	public static void register(String protocol, URLStreamHandler handler)
	{
		handlers.put(protocol, handler);
	}
	
	/**
	 * Unregisters a protocol scheme
	 * @param protocol the protocol name (i.e.: for "http://" the protocol is "http")
	 */
	public static void unregister(String protocol)
	{
		handlers.remove(protocol);
	}
}
