package local;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import aeonics.Boot;
import aeonics.Plugin;
import aeonics.Protocols;
import aeonics.entity.Database;
import aeonics.entity.Origin;
import aeonics.entity.Probe;
import aeonics.entity.Queue;
import aeonics.entity.Registry;
import aeonics.entity.Storage;
import aeonics.entity.Topic;
import aeonics.entity.security.Group;
import aeonics.entity.security.Policy;
import aeonics.entity.security.Provider;
import aeonics.entity.security.Role;
import aeonics.entity.security.Rule;
import aeonics.entity.security.User;
import aeonics.manager.Config;
import aeonics.manager.Lifecycle;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.manager.Network;
import aeonics.template.Factory;
import aeonics.template.Parameter;
import aeonics.manager.Lifecycle.Phase;

public class Main extends Plugin
{
	public String summary() { return "Core System"; }
	public String description() { return "Defines the default behavior of the system."; }
	
	public Main()
	{
		// =============================
		// TEMPORARY CONSOLE LOGGER
		// =============================
		
		String level = System.getProperty("AEONICS_MANAGER_LOGGER_LEVEL");
		if( level == null || level.isBlank() ) level = System.getenv("AEONICS_MANAGER_LOGGER_LEVEL");
		if( level == null || level.isBlank() ) level = "700";
		try { Integer.parseInt(level); } catch(Exception e) { level = "700"; }
		int initialLevel = Integer.parseInt(level);
		System.setProperty("AEONICS_MANAGER_LOGGER_LEVEL", ""+initialLevel);

		try
		{
			// this is necessary to initialize the static properties
			// otherwise they appear as not set for other modules when the 
			// service loader create instances. dont ask why...
			
			Class.forName("aeonics.manager.Logger");
			Class.forName("aeonics.manager.Executor");
			Class.forName("aeonics.manager.Lifecycle");
		}
		catch(Exception e) { throw new RuntimeException(e); }
	}
	
	public void start()
	{
		Protocols.register("storage", new URLStreamHandler()
		{
			protected URLConnection openConnection(URL u) throws IOException 
			{
				Storage.Type s = Registry.of(Storage.class).get(u.getHost());
				if( s == null ) throw new IOException("Storage " + u.getHost() + " not found");
				byte[] content = s.get(u.getFile());
				if( content == null ) throw new IOException("Path " + u.getFile() + " not found on storage " + u.getHost());
				
				return new URLConnection(u)
				{
					public void connect() { connected = true; }
					@Override
					public InputStream getInputStream() { return new ByteArrayInputStream(content); }
				};
			}
		});
		
		Lifecycle.before(Phase.LOAD, this::onBeforeLoad);
		Lifecycle.on(Phase.LOAD, this::onLoad);
		
		Boot.spark(() ->
		{
			// =============================
			// LIFECYCLE BOOT
			// =============================
			
			Lifecycle lifecycle = Manager.of(Lifecycle.class);
			if( lifecycle == null )
			{
				Manager.of(Logger.class).severe(Lifecycle.class, "Missing required implementation");
				return;
			}
			lifecycle.boot();
			
			// when the lifecycle.boot() returns, it means we are shutting down
		});
	}
	
	public void onBeforeLoad()
	{
		// basic entities
		Factory.add(new Database());
		Factory.add(new Origin.Basic());
		Factory.add(new Origin.Scheduled());
		Factory.add(new Probe());
		Factory.add(new Queue());
		Factory.add(new Storage.File());
		Factory.add(new Storage.Memory());
		Factory.add(new Storage.Database());
		Factory.add(new Topic());
		
		// security entities
		Factory.add(new Group());
		Factory.add(new Policy.Allow());
		Factory.add(new Policy.Deny());
		Factory.add(new Policy.TargetedAllow());
		Factory.add(new Policy.TargetedDeny());
		Factory.add(new Provider.Local());
		Factory.add(new Role());
		Factory.add(new Rule.And());
		Factory.add(new Rule.AskProviders());
		Factory.add(new Rule.MatchAll());
		Factory.add(new Rule.MatchAttribute());
		Factory.add(new Rule.MatchContext());
		Factory.add(new Rule.MatchNone());
		Factory.add(new Rule.Or());
		Factory.add(new Rule.Not());
		Factory.add(new Rule.Role());
		Factory.add(new Rule.Xor());
		Factory.add(new User());
	}
	
	public void onLoad()
	{
		Config c = Manager.of(Config.class);
		
		c.declare(Plugin.class, new Parameter("path")
			.summary("Plugins directory")
			.description("The path to the plugins directory. This parameter must be set in the command line.")
			.format(Parameter.Format.TEXT)
			.rule(Parameter.Rule.PATH)
			.optional(false));
		c.declare(Network.class, new Parameter("tls.default.ciphers")
			.summary("Default TLS Ciphers")
			.description("This parameter is a JSON list of default TLS ciphers that should be used to secure network communications. Refer to standard JSSE Cipher Suite Names.")
			.format(Parameter.Format.JSON)
			.rule(Parameter.Rule.JSON_LIST)
			.defaultValue("[TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256]")
			);
		c.declare(Network.class, new Parameter("tls.default.protocols")
			.summary("Default TLS Protocols")
			.description("This parameter is a JSON list of default TLS protocols that should be used to secure network communications. Refer to standard SSLContext Algorithms.")
			.format(Parameter.Format.JSON)
			.rule(Parameter.Rule.JSON_LIST)
			.defaultValue("[TLSv1.3,TLSv1.2]")
			);
	}
}
