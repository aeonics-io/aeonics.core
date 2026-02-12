package aeonics.manager;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.Consumer;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.entity.Registry;
import aeonics.template.Item;
import aeonics.util.Internal;
import aeonics.util.Snapshotable;
import aeonics.util.StringUtils;

/**
 * Manages execution of scheduled tasks. The scheduler will inspect the {@link Registry} for all {@link Cron} and will run them accordingly.
 * <p>It is not expected that the scheduler have a precision smaller than 1 second.</p>
 */
public abstract class Scheduler extends Manager.Type
{
	/**
	 * A named scheduled task
	 */
	public static class Task implements Consumer<ZonedDateTime>
	{
		private Consumer<ZonedDateTime> c = null;
		private Runnable r = null;
		
		private String name = "";
		public String name() { return name; }
		public Task name(String value) { this.name = value; return this; }
		
		private Task() { }
		
		public void accept(ZonedDateTime time)
		{
			if( c != null ) c.accept(time);
			else if( r != null ) r.run();
		}
		
		public static Task of(String name, Runnable task) { Task t = new Task(); t.name(name); t.r = task; return t; }
		public static Task of(String name, Consumer<ZonedDateTime> task) { Task t = new Task(); t.name(name); t.c = task; return t; }
		public static Task of(Runnable task) { Task t = new Task(); t.name(""); t.r = task; return t; }
		public static Task of(Consumer<ZonedDateTime> task) { Task t = new Task(); t.name(""); t.c = task; return t; }
	}
	
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Scheduler.class; }
	
	/**
	 * Returns the current active instance of this manager type.
	 * @return the current active instance of this manager type
	 */
	public static Scheduler get() { return Manager.of(Scheduler.class); }
	
	/**
	 * A task that should be executed at regular interval. The recurrence is defined by a RFC-5545 compliant "RRULE" and "DTSART".
	 */
	public abstract static class Cron extends Item<Cron.Type>
	{
		@SuppressWarnings("unchecked")
		public static class Type extends Entity implements Consumer<ZonedDateTime>
		{
			/**
			 * Cron tasks are internal by default
			 */
			@Override
			public boolean internal() { return true; }
			
			/**
			 * Grab the task name (user defined) instead of the Cron object name, if applicable
			 */
			@Override
			public String name()
			{
				if( this.task instanceof Task )
					return ((Task)this.task).name();
				else
					return super.name();
			}
			
			/**
			 * Cron tasks are not included in snapshots by default
			 */
			@Override
			public Snapshotable.SnapshotMode snapshotMode() { return SnapshotMode.NONE; }
			
			/**
			 * This method will be called by the scheduler when the tasks should run.
			 */
			@Internal
			public void accept(ZonedDateTime time) { if( task != null ) task.accept(time); }
			
			/**
			 * The task to execute
			 */
			private Consumer<ZonedDateTime> task = null;
			
			/**
			 * Returns the task to execute
			 * @return the task to execute
			 */
			public Consumer<ZonedDateTime> task() { return task; }
			
			/**
			 * Sets the task to execute
			 * @param <R> this
			 * @param task the task to execute
			 * @return this for chaining
			 */
			public <R extends Type> R task(Consumer<ZonedDateTime> task)
			{
				this.task = task;
				return (R)this;
			}
			
			/**
			 * Start time for this scheduled task
			 */
			private ZonedDateTime start = null;
			
			/**
			 * Returns the origin point in time where the {@link #rule()} applies. This is the equivalent of the "DTSTART" component of RFC-5545.
			 * @return the origin point in time (may be null if not set)
			 */
			public ZonedDateTime start() { return start; }
			
			/**
			 * Sets the origin point in time where the {@link #rule()} applies. This is the equivalent of the "DTSTART" component of RFC-5545.
			 * @param <R> this
			 * @param start the origin point in time
			 * @return this for chaining
			 */
			public <R extends Type> R start(ZonedDateTime start) { this.start = start; reset(); return (R)this; }
			
			/**
			 * The scheduling rule
			 */
			private String rrule = null;
			
			/**
			 * Returns the scheduling rule as defined by the "RRULE" component of RFC-5545.
			 * @return the scheduling rule (may be null if not set)
			 */
			public String rule() { return rrule; }
			
			/**
			 * Sets the scheduling rule as defined by the "RRULE" component of RFC-5545.
			 * @param <R> this
			 * @param rrule the scheduling rule
			 * @return this for chaining
			 */
			public <R extends Type> R rule(String rrule) { this.rrule = rrule; reset(); return (R)this; }
			
			/**
			 * Hardcoded value of the entity category to the {@link Cron} class
			 */
			@Override
			public final String category() { return StringUtils.toLowerCase(Cron.class); }
			
			/**
			 * Resets and recomputes occurences of this task based on the {@link #rule()}.
			 * This will potentially reset the {@link #next(boolean)} occurence.
			 * @see #next(boolean)
			 */
			public void reset()
			{
				String rr = rule();
				Consumer<ZonedDateTime> t = task();
				ZonedDateTime from = start();
				if( rr == null || t == null || from == null || rr.isBlank() )
				{
					next = null;
					iterator = null;
					return;
				}
				
				synchronized(this)
				{
					if( rr.isBlank() ) return;
					
					RRULE rrule = new RRULE(rule(), start());
					ZonedDateTime now = ZonedDateTime.now().withNano(0).plusSeconds(rrule.freq == RRULE.Freq.SECONDLY ? 0 : 5);
					iterator = rrule.iterator(from.isAfter(now) ? from : now);
					next = iterator.next();
				}
			}
			
			/**
			 * Returns the closest next occurence of this task as defined by {@link #rule()}. 
			 * <p>The next occurence might be in the past, meaning it is past due and should run asap.</p>
			 * <p>The next occurence may also be null which indicates that there are no further occurences at this time.</p>
			 * @param future if true, the next occurence should be computed in the future from now. If false, it should not move ahead to the next future occurence and may return a previousely computed occurence
			 * @return the next occurence of this task or null if there are no next occurences
			 */
			public ZonedDateTime next(boolean future)
			{
				if( iterator == null ) return null;
				if( future )
				{
					ZonedDateTime now = ZonedDateTime.now().withNano(0);
					do { next = iterator.next(); }
					while( next != null && next.isBefore(now) );
				}
				return next;
			}
			
			/**
			 * The occurences iterator
			 */
			private Iterator<ZonedDateTime> iterator = null;
			
			/**
			 * The next occurence
			 */
			private ZonedDateTime next = null;
		}
	
		protected Class<? extends Cron.Type> defaultTarget() { return Cron.Type.class; }
		protected java.util.function.Supplier<? extends Cron.Type> defaultCreator() { return Cron.Type::new; }
		protected Class<? extends Cron> category() { return Cron.class; }
	}
	
	/**
	 * Schedules a task to run once at the specified time.
	 * If the time is past due, the task runs immediately.
	 * @param task the task to run, it will receive the current time when called
	 * @param time the time at which the task should run
	 */
	public abstract void at(Consumer<ZonedDateTime> task, ZonedDateTime time);
	
	/**
	 * Schedules a task to run once after the specified delay has elapsed.
	 * @param task the task to run, it will receive the current time when called
	 * @param delay the number of milliseconds of delay from the time of calling this method
	 */
	public void in(Consumer<ZonedDateTime> task, long delay)
	{
		at(task, ZonedDateTime.now().withNano(0).plus(delay, ChronoUnit.MILLIS));
	}
	
	/**
	 * Schedules a task to run at a specified time interval.
	 * @param task the task to run, it will receive the current time when called
	 * @param step the time interval
	 * @param unit the time unit
	 * @return the cron task
	 */
	public Scheduler.Cron.Type every(Consumer<ZonedDateTime> task, long step, final ChronoUnit unit)
	{
		return every(task, step, unit, ZonedDateTime.now().withNano(0));
	}
	
	/**
	 * Schedules a task to run at a specified time interval starting a the specified moment.
	 * @param task the task to run, it will receive the current time when called
	 * @param step the time interval
	 * @param unit the time unit
	 * @param from the starting point in time
	 * @return the cron task
	 */
	public Scheduler.Cron.Type every(Consumer<ZonedDateTime> task, long step, final ChronoUnit unit, ZonedDateTime from)
	{
		final String rrule;
		long interval = step;
		String freq = "";
		switch(unit)
		{
			case NANOS: 
				interval /= 1000;
				// fallthrough
			case MICROS: 
				interval /= 1000;
				// fallthrough
			case MILLIS:
				interval /= 1000;
				// fallthrough
			case SECONDS:
				freq = RRULE.Freq.SECONDLY.toString();
				break;
			case MINUTES:
				freq = RRULE.Freq.MINUTELY.toString();
				break;
			case HOURS:
				freq = RRULE.Freq.HOURLY.toString();
				break;
			case HALF_DAYS:
				interval /= 2;
				// fallthrough
			case DAYS:
				freq = RRULE.Freq.DAILY.toString();
				break;
			case WEEKS:
				freq = RRULE.Freq.WEEKLY.toString();
				break;
			case MONTHS:
				freq = RRULE.Freq.MONTHLY.toString();
				break;
			case FOREVER:
				interval *= 1000;
				// fallthrough
			case ERAS:
				interval *= 1000;
				// fallthrough
			case MILLENNIA:
				interval *= 10;
				// fallthrough
			case CENTURIES:
				interval *= 10;
				// fallthrough
			case DECADES:
				interval *= 10;
				// fallthrough
			case YEARS:
				freq = RRULE.Freq.YEARLY.toString();
				break;
			default: throw new IllegalArgumentException("Invalid time unit in RRULE");
		}
		rrule = "RRULE:FREQ=" + freq + ";INTERVAL=" + interval;
		
		Scheduler.Cron.Type c = new Scheduler.Cron() {}
			.template()
			.summary("Runs a task at regular interval")
			.description("This task runs every " + step + " " + unit.toString() + " starting from " + (from == null ? ZonedDateTime.now().withNano(0) : from))
			.create()
			.task(task)
			.start(from == null ? ZonedDateTime.now().withNano(0) : from)
			.rule(rrule);
		
		refresh();
		return c;
	}
	
	/**
	 * This method will re-inspect all {@link Cron} in the {@link Registry} to determine if
	 * some tasks need to run.
	 * You should call this method every time a Cron task is added in the Registry so that the scheduler
	 * recomputes the next action date.
	 */
	public abstract void refresh();
	
	/**
	 * RRULE parser and iterator
	 * @hidden
	 */
	@Internal
	public static class RRULE
	{
		// TODO : support negative BYMONTHDAY that indicate the number of days starting from the end of the month.
		
		private enum Part
		{ 
			FREQ("FREQ"), UNTIL("UNTIL"), COUNT("COUNT"), INTERVAL("INTERVAL"), BYSECOND("BYSECOND"), BYMINUTE("BYMINUTE"), BYHOUR("BYHOUR"), 
			BYDAY("BYDAY"), BYMONTHDAY("BYMONTHDAY"), BYYEARDAY("BYYEARDAY"), BYWEEKNO("BYWEEKNO"), BYMONTH("BYMONTH"), BYSETPOS("BYSETPOS"), 
			WKST("WKST");
			private String name;
			Part(String name) { this.name = name; }
			@Override
			public String toString() { return name; }
		}
		
		private enum Freq
		{
			SECONDLY("SECONDLY"), MINUTELY("MINUTELY"), HOURLY("HOURLY"), DAILY("DAILY"), WEEKLY("WEEKLY"), MONTHLY("MONTHLY"), YEARLY("YEARLY");
			private String name;
			Freq(String name) { this.name = name; }
			@Override
			public String toString() { return name; }
		}
		
		private enum Weekday
		{
			SU("SU"), MO("MO"), TU("TU"), WE("WE"), TH("TH"), FR("FR"), SA("SA");
			private String name;
			Weekday(String name) { this.name = name; }
			@Override
			public String toString() { return name; }
		}
		
		private static Map<Weekday, Integer> weekDayMap = new EnumMap<Weekday, Integer>(Weekday.class);
		static
		{
			weekDayMap.put(Weekday.SU, DayOfWeek.SUNDAY.getValue());
			weekDayMap.put(Weekday.MO, DayOfWeek.MONDAY.getValue());
			weekDayMap.put(Weekday.TU, DayOfWeek.TUESDAY.getValue());
			weekDayMap.put(Weekday.WE, DayOfWeek.WEDNESDAY.getValue());
			weekDayMap.put(Weekday.TH, DayOfWeek.THURSDAY.getValue());
			weekDayMap.put(Weekday.FR, DayOfWeek.FRIDAY.getValue());
			weekDayMap.put(Weekday.SA, DayOfWeek.SATURDAY.getValue());
		}
		
		private static Map<Part, Boolean> allowsMultipleValues = new EnumMap<Part, Boolean>(Part.class);
		static
		{
			allowsMultipleValues.put(Part.FREQ, false);
			allowsMultipleValues.put(Part.UNTIL, false);
			allowsMultipleValues.put(Part.COUNT, false);
			allowsMultipleValues.put(Part.INTERVAL, false);
			allowsMultipleValues.put(Part.BYSECOND, true);
			allowsMultipleValues.put(Part.BYMINUTE, true);
			allowsMultipleValues.put(Part.BYHOUR, true);
			allowsMultipleValues.put(Part.BYDAY, true);
			allowsMultipleValues.put(Part.BYMONTHDAY, true);
			allowsMultipleValues.put(Part.BYYEARDAY, true);
			allowsMultipleValues.put(Part.BYWEEKNO, true);
			allowsMultipleValues.put(Part.BYMONTH, true);
			allowsMultipleValues.put(Part.BYSETPOS, true);
			allowsMultipleValues.put(Part.WKST, false);
		}
		
		public RRULE(final String rrule, final ZonedDateTime dtstart)
		{
			parse(rrule.toUpperCase());
			
			if( freq == null ) throw new IllegalArgumentException("FREQ is required");
			if( interval <= 0 ) throw new IllegalArgumentException("INTERVAL must not be less than 1");
			if( freq != Freq.YEARLY && definition.containsKey("BYWEEKNO") ) throw new IllegalArgumentException("BYWEEKNO is only valid when FREQ=YEARLY");
			if( freq == Freq.DAILY && definition.containsKey("BYYEARDAY") ) throw new IllegalArgumentException("BYYEARDAY is not valid when FREQ=DAILY");
			if( freq == Freq.WEEKLY && definition.containsKey("BYYEARDAY") ) throw new IllegalArgumentException("BYYEARDAY is not valid when FREQ=WEEKLY");
			if( freq == Freq.MONTHLY && definition.containsKey("BYYEARDAY") ) throw new IllegalArgumentException("BYYEARDAY is not valid when FREQ=MONTHLY");
			if( freq == Freq.WEEKLY && definition.containsKey("BYMONTHDAY") ) throw new IllegalArgumentException("BYMONTHDAY is not valid when FREQ=WEEKLY");
			
			if( definition.containsKey("BYSETPOS") && !definition.containsKey("BYSECOND") && !definition.containsKey("BYMINUTE") 
					&& !definition.containsKey("BYHOUR") && !definition.containsKey("BYDAY") && !definition.containsKey("BYMONTHDAY") 
					&& !definition.containsKey("BYYEARDAY") && !definition.containsKey("BYWEEKNO") && !definition.containsKey("BYMONTH") ) 
				throw new IllegalArgumentException("BYSETPOS is only valid when another BYxxx part is set");
			
			this.referenceStart = dtstart;
			
			// check legal but unsupported cases
			if( definition.containsKey("BYSETPOS") ) throw new UnsupportedOperationException("BYSETPOS is not supported");
			if( definition.containsKey("BYWEEKNO") ) throw new UnsupportedOperationException("BYWEEKNO is not supported");
			if( definition.containsKey("BYYEARDAY") ) throw new UnsupportedOperationException("BYYEARDAY is not supported");
		}
		
		private Freq freq = null;
		private int count = -1;
		private ZonedDateTime until = null;
		private int interval = 1;
		
		private ZonedDateTime referenceStart = null;
		private Data definition = Data.map();
		
		private void parse(String source)
		{
			String key = null;
			List<String> values = new LinkedList<String>();
			
			if( source.startsWith("RRULE:") ) source = source.substring(6);
			
			for( int i = 0, mark = 0; i < source.length(); i++ )
			{
				char b = source.charAt(i);
				if( b == ';' || i == source.length()-1 )
				{
					if( key == null ) continue; // ignore empty parts
					if( i >= source.length()-1 ) values.add(source.substring(mark));
					else values.add(source.substring(mark, i));
					mark = i+1;
					
					addPart(key, values);
					
					key = null;
					values = new LinkedList<String>();
				}
				else if( b == '=' )
				{
					key = source.substring(mark, i);
					mark = i+1;
				}
				else if( b == ',' )
				{
					values.add(source.substring(mark, i));
					mark = i+1;
				}
			}
		}
		
		private void addPart(String key, List<String> values)
		{
			if( definition.containsKey(key) ) throw new IllegalArgumentException(key + " must not occur more than once");
			if( values.isEmpty() ) throw new IllegalArgumentException(key + " value must not be empty");
			
			switch(Part.valueOf(key))
			{
				case FREQ:
					freq = Freq.valueOf(values.get(0));
					break;
				case INTERVAL:
					interval = Integer.parseInt(values.get(0));
					break;
				case UNTIL:
					if( definition.containsKey(Part.COUNT.toString()) ) throw new IllegalArgumentException("UNTIL cannot be use in conjunction with COUNT");
					until = parseDate(values.get(0));
					break;
				case COUNT:
					if( definition.containsKey(Part.UNTIL.toString()) ) throw new IllegalArgumentException("COUNT cannot be use in conjunction with UNTIL");
					count = Integer.parseInt(values.get(0));
					break;
				default:
					break;
			}
			
			if( !Boolean.TRUE.equals(allowsMultipleValues.get(Part.valueOf(key))) )
			{
				if( values.size() > 1 ) throw new IllegalArgumentException(key + " accepts only one value");
				definition.put(key, values.get(0));
			}
			else
			{
				if( key.equals("BYSECOND") || key.equals("BYMINUTE") || key.equals("BYHOUR") || key.equals("BYMONTH") )
				{
					values.sort((a, b) ->
					{
						Integer ia = Integer.parseInt(a);
						Integer ib = Integer.parseInt(b);
						return ia.compareTo(ib);
					});
				}
				
				// remove duplicates
				values = new ArrayList<String>(new LinkedHashSet<String>(values));
							
				definition.put(key, values);
			}
		}
		
		public ZonedDateTime parseDate(String date)
		{
			int year = -1;
			int month = -1;
			int day = -1;
			int hour = 0;
			int minute = 0;
			int second = 0;
			ZoneId zone = TimeZone.getDefault().toZoneId();
			
			if( date.startsWith("TZID=") )
			{
				int colon = date.indexOf(':');
				zone = ZoneId.of(date.substring(5, colon));
				date = date.substring(colon+1);
			}
			
			year = Integer.parseInt(date.substring(0, 4));
			month = Integer.parseInt(date.substring(4, 6));
			day = Integer.parseInt(date.substring(6, 8));
			
			if( date.length() > 8 )
			{
				hour = Integer.parseInt(date.substring(9, 11));
				minute = Integer.parseInt(date.substring(11, 13));
				second = Integer.parseInt(date.substring(13, 15));
			}
			
			if( date.endsWith("Z") )
				zone = ZoneId.of("UTC");

			return ZonedDateTime.of(year, month, day, hour, minute, second, 0, zone);
		}
		
		public Iterator<ZonedDateTime> iterator(ZonedDateTime from1)
		{
			from1 = from1.withZoneSameInstant(referenceStart.getZone());
			
			if( freq == Freq.MONTHLY && !definition.containsKey("BYDAY") && !definition.containsKey("BYMONTHDAY") )
			{
				if( referenceStart.getDayOfMonth() > from1.getMonth().length(from1.toLocalDate().isLeapYear()) )
					from1 = from1.withDayOfMonth(from1.getMonth().length(from1.toLocalDate().isLeapYear()));
				else
					from1 = from1.withDayOfMonth(referenceStart.getDayOfMonth());
			}
			else if( freq == Freq.WEEKLY && !definition.containsKey("BYDAY") )
			{
				if( !from1.getDayOfWeek().equals(referenceStart.getDayOfWeek()) )
				{
					int add = referenceStart.getDayOfWeek().getValue() - from1.getDayOfWeek().getValue();
					if( add < 0 ) add += 7;
					from1 = from1.plusDays(add);
				}
			}
			
			switch(freq)
			{
				case YEARLY: from1 = from1.withMonth(referenceStart.getMonthValue());
					// fallthrough
				case MONTHLY:
				case WEEKLY:
				case DAILY: from1 = from1.withHour(referenceStart.getHour());
					// fallthrough
				case HOURLY: from1 = from1.withMinute(referenceStart.getMinute());
					// fallthrough
				case MINUTELY: from1 = from1.withSecond(referenceStart.getSecond());
					// fallthrough
				case SECONDLY: from1 = from1.withNano(0);
			}
			
			final ZonedDateTime from = from1;

			return new Iterator<ZonedDateTime>()
			{
				private int counter = 0;
				private ZonedDateTime previous = null;
				private ZonedDateTime next = null;
				
				public boolean hasNext()
				{
					if( previous == null ) initialize();
					if( next == null ) computeNext();
					
					if( count > 0 && counter >= count ) return false;
					if( until != null && next.isAfter(until) ) return false;
					
					return true;
				}

				public ZonedDateTime next()
				{
					if( !hasNext() ) return null;
					previous = next;
					next = null;
					return previous;
				}
				
				private void initialize()
				{
					if( count > 0 )
						previous = referenceStart.withNano(0);
					else
						previous = from.withNano(0);
					next = previous;
					
					if( definition.containsKey("BYMONTH") )
					{
						for( int i = 0; true ; i++ )
						{
							if( i >= definition.get("BYMONTH").size() )
							{
								next = next.withMonth(definition.get("BYMONTH").asInt(0));
								index.put("BYMONTH", 0);
								if( !next.isAfter(referenceStart) )
									nextYear();
								break;
							}
							
							int unit = definition.get("BYMONTH").asInt(i);
							if( unit >= next.getMonthValue() )
							{
								next = next.withMonth(unit);
								index.put("BYMONTH", i);
								break;
							}
						}
					}
					
					if( definition.containsKey("BYDAY") || definition.containsKey("BYMONTHDAY") )
					{
						List<Integer> days = currentMonthValidDays();
						
						for( int i = 0; true ; i++ )
						{
							if( i >= days.size() )
							{
								next = next.withDayOfMonth(days.get(0));
								index.put("BYDAY-BYMONTHDAY", 0);
								if( !next.isAfter(referenceStart) )
									nextMonth();
								next = next.withDayOfMonth(currentMonthValidDays().get(0));
								break;
							}
							
							int unit = days.get(i);
							if( unit >= next.getDayOfMonth() )
							{
								next = next.withDayOfMonth(unit);
								index.put("BYDAY-BYMONTHDAY", i);
								break;
							}
						}
					}
					
					if( definition.containsKey("BYHOUR") )
					{
						for( int i = 0; true ; i++ )
						{
							if( i >= definition.get("BYHOUR").size() )
							{
								next = next.withHour(definition.get("BYHOUR").asInt(0));
								index.put("BYHOUR", 0);
								if( !next.isAfter(referenceStart) )
									nextDay();
								break;
							}
							
							int unit = definition.get("BYHOUR").asInt(i);
							if( unit >= next.getHour() )
							{
								next = next.withHour(unit);
								index.put("BYHOUR", i);
								break;
							}
						}
					}
					
					if( definition.containsKey("BYMINUTE") )
					{
						for( int i = 0; true ; i++ )
						{
							if( i >= definition.get("BYMINUTE").size() )
							{
								next = next.withMinute(definition.get("BYMINUTE").asInt(0));
								index.put("BYMINUTE", 0);
								if( !next.isAfter(referenceStart) )
									nextHour();
								break;
							}
							
							int unit = definition.get("BYMINUTE").asInt(i);
							if( unit >= next.getMinute() )
							{
								next = next.withMinute(unit);
								index.put("BYMINUTE", i);
								break;
							}
						}
					}
					
					if( definition.containsKey("BYSECOND") )
					{
						for( int i = 0; true ; i++ )
						{
							if( i >= definition.get("BYSECOND").size() )
							{
								next = next.withSecond(definition.get("BYSECOND").asInt(0));
								index.put("BYSECOND", 0);
								if( !next.isAfter(referenceStart) )
									nextMinute();
								break;
							}
							
							int unit = definition.get("BYSECOND").asInt(i);
							if( unit >= next.getSecond() )
							{
								next = next.withSecond(unit);
								index.put("BYSECOND", i);
								break;
							}
						}
					}
					
					while( hasNext() && !next.isAfter(from) )
						next();
				}
				
				private Map<String, Integer> index = new HashMap<String, Integer>();
				public void computeNext()
				{
					next = previous;
					nextSecond();
					counter++;
				}
				
				private void nextSecond()
				{
					if( !definition.containsKey("BYSECOND") )
					{
						switch(freq)
						{
							case SECONDLY:
								if( next.getSecond() + interval <= 59 )
									next = next.plusSeconds(interval);
								else
								{
									nextMinute();
									next = next.withSecond(next.getSecond() + interval - 60);
								}
								break;
							case MINUTELY:
							case HOURLY:
							case DAILY:
							case WEEKLY:
							case MONTHLY:
							case YEARLY:
								next = next.withSecond(referenceStart.getSecond());
								nextMinute();
								break;
						}
					}
					else
					{
						boolean overflow = false;
						int i = index.get("BYSECOND") + 1;
						index.put("BYSECOND", i);
						if( i >= definition.get("BYSECOND").size() ) { i = 0; index.put("BYSECOND", 0); overflow = true; }
						
						int unit = definition.get("BYSECOND").asInt(i);
						if( unit < 0 || unit > 59 ) { Manager.of(Logger.class).warning(Scheduler.class, "Invalid time unit in RRULE {}. Setting to 0 instead.", definition); unit = 0; }
						if( unit <= next.getSecond() ) overflow = true;
						next = next.withSecond(unit);
						if( overflow ) nextMinute();
					}
				}
				
				private void nextMinute()
				{
					if( !definition.containsKey("BYMINUTE") )
					{
						switch(freq)
						{
							case SECONDLY:
								next = next.plusMinutes(1);
								break;
							case MINUTELY:
								if( next.getMinute() + interval <= 59 )
									next = next.plusMinutes(interval);
								else
								{
									nextHour();
									next = next.withMinute(next.getMinute() + interval - 60);
								}
								break;
							case HOURLY:
							case DAILY:
							case WEEKLY:
							case MONTHLY:
							case YEARLY:
								next = next.withMinute(referenceStart.getMinute());
								nextHour();
								break;
						}
					}
					else
					{
						boolean overflow = false;
						int i = index.get("BYMINUTE") + 1;
						index.put("BYMINUTE", i);
						if( i >= definition.get("BYMINUTE").size() ) { i = 0; index.put("BYMINUTE", 0); overflow = true; }
						int unit = definition.get("BYMINUTE").asInt(i);
						if( unit < 0 || unit > 59 ) { Manager.of(Logger.class).warning(Scheduler.class, "Invalid time unit in RRULE {}. Setting to 0 instead.", definition); unit = 0; }

						if( unit <= next.getMinute() ) overflow = true;
						next = next.withMinute(unit);
						if( overflow ) nextHour();
					}
				}
				
				private void nextHour()
				{
					if( !definition.containsKey("BYHOUR") )
					{
						switch(freq)
						{
							case SECONDLY:
							case MINUTELY:
								next = next.plusHours(1);
								break;
							case HOURLY:
								if( next.getHour() + interval <= 23 )
									next = next.plusHours(interval);
								else
								{
									nextDay();
									next = next.withHour(next.getHour() + interval - 24);
								}
								break;
							case DAILY:
							case WEEKLY:
							case MONTHLY:
							case YEARLY:
								next = next.withHour(referenceStart.getHour());
								nextDay();
								break;
						}
					}
					else
					{
						boolean overflow = false;
						int i = index.get("BYHOUR") + 1;
						index.put("BYHOUR", i);
						if( i >= definition.get("BYHOUR").size() ) { i = 0; index.put("BYHOUR", 0); overflow = true; }
						
						int unit = definition.get("BYHOUR").asInt(i);
						if( unit < 0 || unit > 23 ) { Manager.of(Logger.class).warning(Scheduler.class, "Invalid time unit in RRULE {}. Setting to 0 instead.", definition); unit = 0; }
						if( unit <= next.getHour() ) overflow = true;
						next = next.withHour(unit);
						if( overflow ) nextDay();
					}
				}
				
				private void nextDay()
				{
					if( !definition.containsKey("BYDAY") && !definition.containsKey("BYMONTHDAY") )
					{
						switch(freq)
						{
							case SECONDLY:
							case MINUTELY:
							case HOURLY:
								next = next.plusDays(1);
								break;
							case DAILY:
								if( next.getDayOfMonth() + interval <= next.toLocalDate().lengthOfMonth() )
									next = next.plusDays(interval);
								else
								{
									int max = next.toLocalDate().lengthOfMonth();
									nextMonth();
									next = next.withDayOfMonth(next.getDayOfMonth() + interval - max);
								}
								break;
							case WEEKLY:
								int intervalindays = 7*interval;
								if( next.getDayOfMonth() + intervalindays <= next.toLocalDate().lengthOfMonth() )
									next = next.plusDays(intervalindays);
								else
								{
									int max = next.toLocalDate().lengthOfMonth() - 1;
									nextMonth();
									next = next.withDayOfMonth(next.getDayOfMonth() + intervalindays - max - 1);
								}
								break;
							case MONTHLY:
							case YEARLY:
								nextMonth();
								// fix day after because we dont know the number of days of the next month until it is set
								next = next.withDayOfMonth(Math.min(referenceStart.getDayOfMonth(), next.toLocalDate().lengthOfMonth()));
								break;
						}
					}
					else
					{
						List<Integer> days = currentMonthValidDays();
						
						boolean overflow = false;
						int i = index.get("BYDAY-BYMONTHDAY") + 1;
						index.put("BYDAY-BYMONTHDAY", i);
						if( i >= days.size() ) { i = 0; index.put("BYDAY-BYMONTHDAY", 0); overflow = true; }
						
						int unit = days.get(i);
						if( unit < 0 || unit > 31 ) { Manager.of(Logger.class).warning(Scheduler.class, "Invalid time unit in RRULE {}. Setting to 0 instead.", definition); unit = 0; }
						if( unit <= next.getDayOfMonth() ) overflow = true;
						else next = next.withDayOfMonth(unit);

						if( overflow )
						{
							nextMonth();
							
							// preselected day may not fit with the new month, so reselect the first day now
							next = next.withDayOfMonth(currentMonthValidDays().get(0));
						}
					}				
				}
				
				private List<Integer> currentMonthValidDays()
				{
					List<Integer> days = new ArrayList<Integer>();
					int max = next.toLocalDate().lengthOfMonth();
					for( int i = 1; i <= max; i++ ) days.add(i);
					
					if( definition.containsKey("BYDAY") )
					{
						List<Integer> valid_days = new ArrayList<Integer>();
						int first = next.withDayOfMonth(1).getDayOfWeek().getValue();
						for( Data a : definition.get("BYDAY") )
						{
							int w = weekDayMap.get(Weekday.valueOf(a.asString())); 
							for( int i = 0; i < 6; i++ ) // generate 6 weeks in the month to be sure to get all days
							{
								int d = (1-first+w) + (7*i);
								if( d < 0 ) d = max - d;
								if( d <= 0 || d > max ) continue;
								valid_days.add(d);
							}
						}
						days.retainAll(valid_days);
					}
					
					if( definition.containsKey("BYMONTHDAY") )
					{
						List<Integer> valid_days = new ArrayList<Integer>();
						for( Data a : definition.get("BYMONTHDAY") )
						{
							int d = a.asInt();
							if( d < 0 ) d = max - d;
							if( d <= 0 || d > max ) continue;
							valid_days.add(d);
						}
						days.retainAll(valid_days);
					}
					
					return new ArrayList<Integer>(new TreeSet<Integer>(days));
				}
				
				private void nextMonth()
				{
					if( !definition.containsKey("BYMONTH") )
					{
						switch(freq)
						{
							case SECONDLY:
							case MINUTELY:
							case HOURLY:
							case DAILY:
							case WEEKLY:
								next = next.plusMonths(1);
								break;
							case MONTHLY:
								if( next.getMonthValue() + interval <= 12 )
									next = next.plusMonths(interval);
								else
								{
									nextYear();
									next = next.withMonth(next.getMonthValue() + interval - 12);
								}
								break;
							case YEARLY:
								next = next.withMonth(referenceStart.getMonthValue());
								nextYear();
								break;
						}
					}
					else
					{
						boolean overflow = false;
						int i = index.get("BYMONTH") + 1;
						index.put("BYMONTH", i);
						if( i >= definition.get("BYMONTH").size() ) { i = 0; index.put("BYMONTH", 0); overflow = true; }
						
						int unit = definition.get("BYMONTH").asInt(i);
						if( unit < 1 || unit > 12 ) { Manager.of(Logger.class).warning(Scheduler.class, "Invalid time unit in RRULE {}. Setting to 0 instead.", definition); unit = 0; }
						if( unit <= next.getMonthValue() ) overflow = true;
						next = next.withMonth(unit);
						if( overflow ) nextYear();
					}
				}
				
				private void nextYear()
				{
					switch(freq)
					{
						case SECONDLY:
						case MINUTELY:
						case HOURLY:
						case DAILY:
						case WEEKLY:
						case MONTHLY:
							next = next.plusYears(1);
							break;
						case YEARLY:
							next = next.plusYears(interval);
							break;
					}
				}
			};
		}
	}
}
