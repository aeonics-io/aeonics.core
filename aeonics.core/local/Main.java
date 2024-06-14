package local;

import aeonics.Boot;
import aeonics.Plugin;
import aeonics.manager.Lifecycle;
import aeonics.manager.Logger;
import aeonics.manager.Manager;

public class Main extends Plugin
{
	public void start()
	{
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
		
		// =============================
		// TEMPORARY CONSOLE LOGGER
		// =============================
		
		String level = System.getProperty("AEONICS_MANAGER_LOGGER_LEVEL");
		if( level == null || level.isBlank() ) level = System.getenv("AEONICS_MANAGER_LOGGER_LEVEL");
		try { Integer.parseInt(level); } catch(Exception e) { level = "700"; }
		int initialLevel = Integer.parseInt(level);
		System.setProperty("AEONICS_MANAGER_LOGGER_LEVEL", ""+initialLevel);

		Logger.CONSOLE.level(initialLevel);
		Manager.set(Logger.class, Logger.CONSOLE);
	}
}
