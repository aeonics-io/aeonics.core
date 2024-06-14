package aeonics.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;

import com.sun.management.OperatingSystemMXBean;

import aeonics.data.Data;
import aeonics.manager.Manager;
import aeonics.manager.Logger;

/**
 * Represents (virtual) hardware resources that the Java Virtual Machine is currently running on.
 */
public class Hardware
{
	private Hardware() { /* no instances */ }
	
	/**
	 * CPU
	 */
	public static class CPU
	{
		private CPU() { /* no instances */ }
		
		/**
		 * System bean
		 */
		static final OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		
		/**
		 * Returns the "recent cpu usage" for the Java Virtual Machine process.
		 * This value is a double in the [0.0,1.0] interval.
		 * @return the "recent cpu usage" for the Java Virtual Machine process
		 */
		public static double load()
		{
			return os.getProcessCpuLoad();
		}
		
		/**
		 * Returns the "recent cpu usage" for the whole system.
		 * This value is adouble in the [0.0,1.0] interval.
		 * @return the "recent cpu usage" for the whole system
		 */
		public static double system()
		{
			// this method has changed in JRE14 so we have to use reflection
			
			// >= JRE14
			try { return (double) os.getClass().getMethod("getCpuLoad").invoke(os); }
			catch (Exception e) { /* ignore */ }
			
			// < JRE14
			try { return (double) os.getClass().getMethod("getSystemCpuLoad").invoke(os); }
			catch (Exception e) { /* ignore */ }
			
			return 0.0;
		}
		
		/**
		 * Number of cores
		 */
		static final int CORES = os.getAvailableProcessors();
		/**
		 * Returns the number of processors available to the Java virtual machine.
		 * @return the number of processors available to the Java virtual machine
		 */
		public static int cores()
		{
			return CORES;
		}
		
		/**
		 * Process PID
		 */
		static final long PID = ProcessHandle.current().pid();
		/**
		 * Returns the native process ID of the Java virtual machine process.
		 * @return the native process ID of the Java virtual machine process
		 */
		public static long pid()
		{
			return PID;
		}
		
		/**
		 * Returns a data representation of this class at this point in time
		 * @return a data representation of this class at this point in time
		 */
		public static Data export()
		{
			return Data.map().put("process", load()).put("system", system()).put("cores", cores()).put("pid", pid());
		}
	}
	
	/**
	 * RAM
	 */
	public static class RAM
	{
		private RAM() { /* no instances */ }
		
		/**
		 * System bean
		 */
		static final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		
		/**
		 * Waits for the specified amount of heap memory to be available.
		 * <p>If a timeout is specified (&gt; 0) then this method will throw if it cannot observe enough free memory in time.</p>
		 * <p>If the timeout is not specified (&lt;= 0) then this method will wait at most 10 seconds and proceed anyway.</p>
		 * <p>In all cases, an operating space of 15% of the maximum heap space is reserved. So the requested memory is deducted
		 * from these 15%.</p>
		 * 
		 * @param limit the minimum amount of memory bytes while leaving 15% free heap space for normal operations
		 * @param timeout the maximum ms to wait, or a default of 10 seconds if &lt;= 0
		 * @throws IllegalStateException if the amount of free heap memory cannot be acheived in time and a timeout has been set explicitly
		 */
		public static void waitForSpace(long limit, long timeout)
		{
			long t = System.currentTimeMillis() + (timeout <= 0 ? 10_000 : timeout);
			long l = (long) (RAM.Heap.MAX * 0.85) - limit;
			
			while( RAM.Heap.used() >= l )
			{
				try { Thread.sleep(1); } catch(InterruptedException ie) { /* ignore */ }
				
				if( System.currentTimeMillis() > t )
				{
					Manager.of(Logger.class).severe(RAM.class, "Unable to acquire requested memory within {}ms. Max: {}, Used: {}, Limit: {}, Requested: {}",
							(timeout <= 0 ? 10_000 : timeout),
							RAM.Heap.max(),
							RAM.Heap.used(),
							l,
							limit);
					
					if( timeout <= 0 ) throw new IllegalStateException("Not enough memory available");
					else break; // do not throw, allow to proceed above the fixed limit
				}
			}
		}
		
		/**
		 * Heap space
		 */
		public static class Heap
		{
			private Heap() { /* no instances */ }
			
			/**
			 * Returns the amount of used memory in bytes
			 * @return the amount of used memory in bytes
			 */
			public static long used()
			{
				return memory.getHeapMemoryUsage().getUsed();
			}
			
			/**
			 * Max memory
			 */
			static final long MAX = memory.getHeapMemoryUsage().getMax();
			/**
			 * Returns the maximum amount of memory in bytes that can be used
			 * @return the maximum amount of memory in bytes that can be used
			 */
			public static long max()
			{
				return MAX;
			}
			
			/**
			 * Returns the amount of memory in bytes that is committed forthe Java virtual machine to use
			 * @return the amount of memory in bytes that is committed forthe Java virtual machine to use
			 */
			public static long committed()
			{
				return memory.getHeapMemoryUsage().getCommitted();
			}
			
			/**
			 * Returns the detail of all internal memory pools
			 * @return the detail of all internal memory pools
			 */
			public static Data detail()
			{
				Data d = Data.list();
				for(MemoryPoolMXBean m : ManagementFactory.getMemoryPoolMXBeans())
				{
					if( m.getType() == MemoryType.HEAP )
					{
						MemoryUsage u = m.getUsage();
						d.add(Data.map()
							.put("name", m.getName())
							.put("used", u.getUsed())
							.put("max", u.getMax())
							.put("committed", u.getCommitted())
						);
					}
				}
				return d;
			}
			
			/**
			 * Returns a data representation of this class at this point in time
			 * @return a data representation of this class at this point in time
			 */
			public static Data export()
			{
				return Data.map().put("used", used()).put("max", max()).put("committed", committed()).put("detail", detail());
			}
		}
		
		/**
		 * Non-heap space
		 */
		public static class NonHeap
		{
			private NonHeap() { /* no instances */ }
			
			/**
			 * Returns the amount of used memory in bytes.
			 * @return the amount of used memory in bytes
			 */
			public static long used()
			{
				return memory.getNonHeapMemoryUsage().getUsed();
			}
			
			/**
			 * Max memory
			 */
			static final long MAX = memory.getNonHeapMemoryUsage().getMax();
			/**
			 * Returns the maximum amount of memory in bytes that can be used.
			 * @return the maximum amount of memory in bytes that can be used
			 */
			public static long max()
			{
				return MAX;
			}
			
			/**
			 * Returns the amount of memory in bytes that is committed forthe Java virtual machine to use.
			 * @return the amount of memory in bytes that is committed forthe Java virtual machine to use
			 */
			public static long committed()
			{
				return memory.getNonHeapMemoryUsage().getCommitted();
			}
			
			/**
			 * Returns the detail of all internal memory pools
			 * @return the detail of all internal memory pools
			 */
			public static Data detail()
			{
				Data d = Data.list();
				for(MemoryPoolMXBean m : ManagementFactory.getMemoryPoolMXBeans())
				{
					if( m.getType() == MemoryType.NON_HEAP )
					{
						MemoryUsage u = m.getUsage();
						d.add(Data.map()
							.put("name", m.getName())
							.put("used", u.getUsed())
							.put("max", u.getMax())
							.put("committed", u.getCommitted())
						);
					}
				}
				return d;
			}
			
			/**
			 * Returns a data representation of this class at this point in time
			 * @return a data representation of this class at this point in time
			 */
			public static Data export()
			{
				return Data.map().put("used", used()).put("max", max()).put("committed", committed()).put("detail", detail());
			}
		}
		
		public static class Physical
		{
			private Physical() { /* no instances */ }
			
			/**
			 * Returns the amount of free physical memory in bytes.
			 * @return the amount of free physical memory in bytes
			 */
			public static long free()
			{
				// this method has changed in JRE14 so we have to use reflection
				
				// >= JRE14
				try { return (long) CPU.os.getClass().getMethod("getFreeMemorySize").invoke(CPU.os); }
				catch (Exception e) { /* ignore */ }
				
				// < JRE14
				try { return (long) CPU.os.getClass().getMethod("getFreePhysicalMemorySize").invoke(CPU.os); }
				catch (Exception e) { /* ignore */ }
				
				return 0L;
			}
			
			/**
			 * Max memory
			 */
			static long MAX = 0;
			static
			{
				// this method has changed in JRE14 so we have to use reflection
				
				// >= JRE14
				try { MAX = (long) CPU.os.getClass().getMethod("getTotalMemorySize").invoke(CPU.os); }
				catch (Exception e)
				{
					// < JRE14
					try { MAX = (long) CPU.os.getClass().getMethod("getTotalPhysicalMemorySize").invoke(CPU.os); }
					catch (Exception x) { /* ignore */ }
				}
			}
			
			/**
			 * Returns the total amount of physical memory in bytes.
			 * @return the total amount of physical memory in bytes
			 */
			public static long max()
			{
				return MAX;
			}
			
			/**
			 * Returns the amount of virtual memory that is guaranteed tobe available to the running process in bytes.
			 * @return the amount of virtual memory that is guaranteed tobe available to the running process in bytes
			 */
			public static long process()
			{
				return CPU.os.getCommittedVirtualMemorySize();
			}
			
			/**
			 * Returns a data representation of this class at this point in time
			 * @return a data representation of this class at this point in time
			 */
			public static Data export()
			{
				return Data.map().put("free", free()).put("max", max()).put("process", process());
			}
		}
		
		/**
		 * Returns a data representation of this class at this point in time
		 * @return a data representation of this class at this point in time
		 */
		public static Data export()
		{
			return Data.map().put("heap", Heap.export()).put("nonheap", NonHeap.export()).put("physical", Physical.export());
		}
	}
	
	/**
	 * Returns a data representation of this class at this point in time
	 * @return a data representation of this class at this point in time
	 */
	public static Data export()
	{
		return Data.map().put("cpu", CPU.export()).put("ram", RAM.export());
	}
}
