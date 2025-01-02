package aeonics.entity;

import java.lang.ref.WeakReference;
import java.util.Objects;

import aeonics.data.Data;
import aeonics.entity.security.User;
import aeonics.manager.Network;
import aeonics.util.Exportable;

/**
 * A Message is the common data container that convey information across the system.
 * <p>Each message has several components:</p><ul>
 * <li>{@link #key()}: the binding key that is used by the {@link Queue} to subscribe to a {@link Topic} and target this message.</li>
 * <li>{@link #user()}: the id of the related user. That is, if the message is authenticated for a particular user.</li>
 * <li>{@link #content()}: the actual content of the message, it can be any structured or flat value. By default it is a data map.</li>
 * <li>{@link #connection()}: the optional linked connection. This is usually set in a request/response flow.</li>
 * <li>{@link #metadata()}: any metadata related to this message in the form of key/value.</li>
 * </ul>
 */
public class Message implements Exportable
{
	/**
	 * The message binding key
	 */
	private String key = "";
	
	/**
	 * Returns the message binding key.
	 * The key is never null, but it can be empty.
	 * @return the message binding key
	 */
	public String key() { return key; }
	
	/**
	 * Sets the message binding key
	 * @param value the message binding key
	 * @return this
	 */
	public Message key(String value) { key = Objects.requireNonNullElse(value, ""); return this; }
	
	/**
	 * The id of the user related to this message
	 */
	private String user = null;
	
	/**
	 * Returns the id of the user related to this message.
	 * <p>The id may be null if this message does not relate to any user.</p>
	 * <p>The {@link User} instance should be fetched from the {@link Registry} if needed.</p>
	 * @return the id of the user related to this message
	 */
	public String user() { return user; }
	
	/**
	 * Sets the id of the user related to this message
	 * @param value the id of the user related to this message
	 * @return this
	 */
	public Message user(String value) { user = value; return this; }
	
	/**
	 * The message content
	 */
	private Data content = Data.map();
	
	/**
	 * Returns the content of the message.
	 * It may be any type of data, binary, key-value, list,...
	 * The data is never null but it can be empty.
	 * @return the content of the message
	 */
	public Data content() { return content; }
	
	/**
	 * Sets the content of the message
	 * @param value the content of the message
	 * @return this
	 */
	public Message content(Data value) { content = Objects.requireNonNullElseGet(value, Data::empty); return this; }
	
	/**
	 * The message metadata
	 */
	private Data metadata = Data.map();
	
	/**
	 * Returns the metadata of the message in the form of key/value
	 * @return the metadata of the message
	 */
	public Data metadata() { return metadata; }
	
	/**
	 * The linked connection
	 */
	private WeakReference<Network.Connection> connection = null;
	
	/**
	 * Returns the linked connection, or null if there is none.
	 * The linked connection is typically used to send a response to this message request.
	 * @return the linked connection, or null if there is none
	 */
	public Network.Connection connection() { return connection == null ? null : connection.get(); }
	
	/**
	 * Sets the linked connection
	 * @param value the linked connection
	 * @return this
	 */
	public Message connection(Network.Connection value) { connection = new WeakReference<Network.Connection>(value); return this; }
	
	/**
	 * Creates a new message with the provided binding key.
	 * The default metadata ttl is fixed to 20 hops.
	 * @param key the message binding key
	 */
	public Message(String key)
	{
		key(key);
		metadata().put("ttl", 20);
	}
	
	/**
	 * Private copy constructor
	 * @param copy the instance to copy
	 */
	private Message(Message copy)
	{
		this(copy.key());
		user(copy.user());
		connection(connection());
		content(copy.content().clone());
		copy.metadata().cloneTo(metadata());
	}
	
	/**
	 * Private import constructor
	 * @param export the exported message
	 */
	private Message(Data export)
	{
		this(export.asString("key"));
		user(export.isNull("user") ? null : export.asString("user"));
		content(export.get("content"));
		export.get("metadata").cloneTo(metadata());
	}
	
	/**
	 * Returns a message instance from its exported data representation
	 * @param data the exported message representation 
	 * @return a message instance
	 */
	public static Message of(Data data) { return new Message(data); }
	
	/**
	 * Returns a new deep copy of this message
	 * @return a new deep copy of this message
	 */
	@Override
	public Message clone() { return new Message(this); }
	
	/**
	 * Exports this message to a data representation.
	 * <p>Since the active connection cannot be represented as data, it will be omited.</p>
	 * @return a data representation of this message
	 */
	public Data export()
	{
		return Data.map()
			.put("key", key())
			.put("user", user())
			.put("content", content())
			.put("metadata", metadata());
	}

	@Override
	public String toString() { return export().toString(); }
}
