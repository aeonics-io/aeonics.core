package aeonics.entity;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

import aeonics.entity.Step.Origin;
import aeonics.manager.Lifecycle;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.manager.Scheduler;
import aeonics.manager.Scheduler.Cron;
import aeonics.template.Parameter;
import aeonics.util.Functions.Consumer;

/**
 * This class represents an origin step that is activated at regular interval.
 * <p>You should provide the {@link Scheduled.Type#task(Consumer)} and set a recurrence rule.</p>
 */
public class Scheduled extends Origin
{
	public static class Type extends Origin.Type
	{
		public Type()
		{
			super();
			snapshotMode(SnapshotMode.UPDATE);
		}
		
		private Consumer<ZonedDateTime> task = null;
		private Scheduler.Cron.Type cron = null;
		
		@Override
		public void produce(Message message, String output)
		{
			super.produce(message, output);
		}
		
		private void runTask(ZonedDateTime value)
		{
			if( task != null )
			{
				try
				{
					task.accept(value);
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).warning(Scheduled.class, e);
				}
			}
		}
		
		/**
		 * Sets the task that shall run at the specified interval
		 * @param value the current time
		 * @return this
		 */
		public Type task(Consumer<ZonedDateTime> value)
		{
			this.task = value;
			return this;
		}
		
		@Override
		public void start()
		{
			if( cron != null )
			{
				if( !Registry.of(Scheduler.Cron.class).contains(cron.id()) )
					Registry.add(cron);
				Manager.of(Scheduler.class).refresh();
				started(true);
			}
		}
		
		@Override
		public void stop()
		{
			if( cron != null )
			{
				Registry.of(Scheduler.Cron.class).remove(cron.id());
				stopped(true);
			}
		}
	}
	
	protected Class<? extends Scheduled.Type> defaultTarget() { return Scheduled.Type.class; }
	protected Supplier<? extends Scheduled.Type> defaultCreator() { return Scheduled.Type::new; }
	
	@Override
	public Origin.Template template()
	{
		return super.template()
			.icon("alarm")
			.summary("Scheduled origin")
			.description("This data origin is triggered automatically at regular interval.")
			.add(new Parameter("rule")
				.summary("Recurring rule")
				.description("The recurrence is defined by a RFC-5545 RRULE and DTSART string.")
				.format(Parameter.Format.TEXT))
			.onCreate((data, instance) -> 
			{
				((Scheduled.Type)instance).cron = new Cron() { }
					.template()
					.create()
					.task((time) -> { ((Scheduled.Type)instance).runTask(time); })
					.start(ZonedDateTime.now().withNano(0))
					.rule(data.asString("rule"));
					
				if( Manager.of(Lifecycle.class).phase() == Lifecycle.Phase.RUN )
					((Scheduled.Type)instance).start();
			})
			.onUpdate((data, instance) -> 
			{
				if( data.containsKey("rule") )
				{
					((Scheduled.Type)instance).cron.rule(data.asString("rule"));
					if( Manager.of(Lifecycle.class).phase() == Lifecycle.Phase.RUN )
						Manager.of(Scheduler.class).refresh();
				}
			});
	}
}
