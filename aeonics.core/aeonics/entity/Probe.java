package aeonics.entity;

import aeonics.data.Data;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.template.Item;
import aeonics.template.Template;
import aeonics.util.Snapshotable;
import aeonics.util.Functions.Supplier;

/**
 * A probe represents any type of information that can be fetched at any point in time.
 * Usually this is used for system metrics, counters or current state.
 * 
 * <p>There is no requrement in terms of data structure or allowed values.</p>
 * <p>Unless overriden, a probe is always internal ({@link Entity#internal()}).</p>
 */
public class Probe extends Item<Probe.Type>
{
	public static class Type extends Entity
	{
		private Supplier<Data> source;
		
		/**
		 * Use this method to set the probe data supplier
		 * @param <T> this type
		 * @param source the probe data supplier
		 * @return this
		 */
		@SuppressWarnings("unchecked")
		public <T extends Type> T source(Supplier<Data> source) { this.source = source; return (T) this; }
		
		/**
		 * Fetches the data for this probe
		 * @return the probe data
		 */
		public synchronized Data report()
		{
			if( source == null ) return Data.map();
			try
			{
				Data data = source.get();
				if( data == null ) return Data.map();
				return data;
			}
			catch(Exception e)
			{
				Manager.of(Logger.class).info(Probe.class, e);
				return Data.map();
			}
		}
		
		/**
		 * A probe is always internal
		 */
		public boolean internal() { return true; }
		
		/**
		 * Probes are not included in snapshots
		 */
		public Snapshotable.SnapshotMode snapshotMode() { return SnapshotMode.NONE; }
	}
	
	protected Class<? extends Probe.Type> defaultTarget() { return Probe.Type.class; }
	protected java.util.function.Supplier<? extends Probe.Type> defaultCreator() { return Probe.Type::new; }
	protected Class<? extends Probe> category() { return Probe.class; }

	@SuppressWarnings("unchecked")
	@Override
	public Template<? extends Probe.Type> template()
	{
		return (Template<Probe.Type>) super.template()
			.summary("Probe")
			.description("This entity provides probe data.")
			;
	}
}
