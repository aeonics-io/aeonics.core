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
import aeonics.entity.Registry;
import aeonics.entity.Storage;
import aeonics.manager.Lifecycle;
import aeonics.manager.Logger;
import aeonics.manager.Manager;

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
}
