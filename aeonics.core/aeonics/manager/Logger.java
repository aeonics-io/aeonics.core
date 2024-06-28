package aeonics.manager;

import java.util.function.Supplier;

import aeonics.template.Factory;
import aeonics.template.Template;
import aeonics.util.Internal;
import aeonics.util.Json;

/**
 * This entity manages the logs of the entire system.
 * From anywhere, just call one of the <code>log()</code> variants.
 * <code>Manager.of(Logger.class).log(...);</code>
 */
public abstract class Logger extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Logger.class; }
	
	/**
	 * Severe errors (1000)
	 */
	public static final int SEVERE = 1000;
	/**
	 * Noteworthy warnings (900)
	 */
	public static final int WARNING = 900;
	/**
	 * Meaningful information (800)
	 */
	public static final int INFO = 800;
	/**
	 * Config checkup (700)
	 */
	public static final int CONFIG = 700;
	/**
	 * Fine details (500)
	 */
	public static final int FINE = 500;
	/**
	 * More tiny details (400)
	 */
	public static final int FINER = 400;
	/**
	 * Most verbose (300)
	 */
	public static final int FINEST = 300;
	/**
	 * Include all internal stack traces (-1)
	 */
	public static final int ALL = -1;
	
	/**
	 * The current log level
	 */
	private int level = CONFIG;
	/**
	 * Returns the current log level
	 * @return the current log level
	 */
	public int level() { return level; }
	/**
	 * Sets the current log level
	 * @param value the current log level
	 */
	public synchronized void level(int value)
	{
		if( value > level )
			log(level, Logger.class, "Log level raised from {} to {}", level, value);
		level = value;
	}
	
	/**
	 * Log the specified object if the current level is smaller or equal to the specified level.
	 * Alias of {@link #log(int, Class, String, Object...)} with level {@link #SEVERE}
	 * @param level the log level for this entry
	 * @param type the class type this log entry relates to
	 * @param o the object to log
	 */
	public void log(final int level, final Class<?> type, final Object o) { log(level, (type == null ? null : type.getCanonicalName()), "{}", o); }
	
	/**
	 * Log the specified exception if the current level is smaller or equal to the specified level.
	 * @param level the log level for this entry
	 * @param type the class type this log entry relates to
	 * @param t the exception to log
	 */
	public void log(final int level, final Class<?> type, final Throwable t) { log(level, (type == null ? null : type.getCanonicalName()), "{}", t); }
	
	/**
	 * Log the specified message and optional other parameters if the current level is smaller or equal to the specified level.
	 * By convention, the optional parameters are substituted in the message where "{}" is encountered, by order of appearance.
	 * @param level the log level for this entry
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void log(final int level, final Class<?> type, final String message, final Object ...params) { log(level, (type == null ? null : type.getCanonicalName()), message, params); }
	
	/**
	 * Log the specified object if the current level is smaller or equal to the specified level.
	 * @param level the log level for this entry
	 * @param type the textual context this log entry relates to
	 * @param o the object to log
	 */
	public void log(final int level, final String type, final Object o) { log(level, type, "{}", o); }
	
	/**
	 * Log the specified exception if the current level is smaller or equal to the specified level.
	 * @param level the log level for this entry
	 * @param type the textual context this log entry relates to
	 * @param t the exception to log
	 */
	public void log(final int level, final String type, final Throwable t) { log(level, type, "{}", t); }
	
	/**
	 * Log the specified message and optional other parameters if the current level is smaller or equal to the specified level.
	 * By convention, the optional parameters are substituted in the message where "{}" is encountered, by order of appearance.
	 * @param level the log level for this entry
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void log(final int level, final String type, final String message, final Object ...params)
	{
		if( level < level() || message == null ) return;
		handle(level, (type == null || type.length() == 0 ? "global" : type), message, params);
	}
	
	/**
	 * Log the specified message and optional other parameters if the current level is smaller or equal to the specified level.
	 * By convention, the optional parameters are substituted in the message where "{}" is encountered, by order of appearance.
	 * @param level the log level for this entry
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void log(final int level, final Class<?> type, final String message, final Supplier<Object[]> supplier)
	{
		if( level < level() || message == null ) return;
		log(level, (type == null ? null : type.getCanonicalName()), message, supplier.get());
	}
	
	/**
	 * Log the specified message and optional other parameters if the current level is smaller or equal to the specified level.
	 * By convention, the optional parameters are substituted in the message where "{}" is encountered, by order of appearance.
	 * @param level the log level for this entry
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void log(final int level, final String type, final String message, final Supplier<Object[]> supplier)
	{
		if( level < level() || message == null ) return;
		log(level, type, message, supplier.get());
	}
	
	// ============================
	// SEVERE
	// ============================
	
	/**
	 * Alias of {@link log(int, Class, Object)} with level {@link #SEVERE}
	 * @param type the class type this log entry relates to
	 * @param o the object to log
	 */
	public void severe(final Class<?> type, final Object o) { log(SEVERE, (type == null ? null : type.getCanonicalName()), "{}", o); }
	
	/**
	 * Alias of {@link log(int, Class, Throwable)} with level {@link #SEVERE}
	 * @param type the class type this log entry relates to
	 * @param t the exception to log
	 */
	public void severe(final Class<?> type, final Throwable t) { log(SEVERE, (type == null ? null : type.getCanonicalName()), "{}", t); }
	
	/**
	 * Alias of {@link log(int, Class, String, Object...)} with level {@link #SEVERE}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void severe(final Class<?> type, final String message, final Object ...params) { log(SEVERE, (type == null ? null : type.getCanonicalName()), message, params); }
	
	/**
	 * Alias of {@link log(int, String, Object)} with level {@link #SEVERE}
	 * @param type the textual context this log entry relates to
	 * @param o the object to log
	 */
	public void severe(final String type, final Object o) { log(SEVERE, type, "{}", o); }
	
	/**
	 * Alias of {@link log(int, String, Throwable)} with level {@link #SEVERE}
	 * @param type the textual context this log entry relates to
	 * @param t the exception to log
	 */
	public void severe(final String type, final Throwable t) { log(SEVERE, type, "{}", t); }
	
	/**
	 * Alias of {@link log(int, String, String, Object...)} with level {@link #SEVERE}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void severe(final String type, final String message, final Object ...params) { log(SEVERE, type, message, params); }
	
	/**
	 * Alias of {@link log(int, Class, String, Supplier)} with level {@link #SEVERE}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void severe(final Class<?> type, final String message, final Supplier<Object[]> supplier) { log(SEVERE, type, message, supplier); }
	
	/**
	 * Alias of {@link log(int, String, String, Supplier)} with level {@link #SEVERE}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void severe(final String type, final String message, final Supplier<Object[]> supplier) { log(SEVERE, type, message, supplier); }

	// ============================
	// WARNING
	// ============================
	
	/**
	 * Alias of {@link log(int, Class, Object)} with level {@link #WARNING}
	 * @param type the class type this log entry relates to
	 * @param o the object to log
	 */
	public void warning(final Class<?> type, final Object o) { log(WARNING, (type == null ? null : type.getCanonicalName()), "{}", o); }
	
	/**
	 * Alias of {@link log(int, Class, Throwable)} with level {@link #WARNING}
	 * @param type the class type this log entry relates to
	 * @param t the exception to log
	 */
	public void warning(final Class<?> type, final Throwable t) { log(WARNING, (type == null ? null : type.getCanonicalName()), "{}", t); }
	
	/**
	 * Alias of {@link log(int, Class, String, Object...)} with level {@link #WARNING}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void warning(final Class<?> type, final String message, final Object ...params) { log(WARNING, (type == null ? null : type.getCanonicalName()), message, params); }
	
	/**
	 * Alias of {@link log(int, String, Object)} with level {@link #WARNING}
	 * @param type the textual context this log entry relates to
	 * @param o the object to log
	 */
	public void warning(final String type, final Object o) { log(WARNING, type, "{}", o); }
	
	/**
	 * Alias of {@link log(int, String, Throwable)} with level {@link #WARNING}
	 * @param type the textual context this log entry relates to
	 * @param t the exception to log
	 */
	public void warning(final String type, final Throwable t) { log(WARNING, type, "{}", t); }
	
	/**
	 * Alias of {@link log(int, String, String, Object...)} with level {@link #WARNING}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void warning(final String type, final String message, final Object ...params) { log(WARNING, type, message, params); }
	
	/**
	 * Alias of {@link log(int, Class, String, Supplier)} with level {@link #WARNING}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void warning(final Class<?> type, final String message, final Supplier<Object[]> supplier) { log(WARNING, type, message, supplier); }
	
	/**
	 * Alias of {@link log(int, String, String, Supplier)} with level {@link #WARNING}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void warning(final String type, final String message, final Supplier<Object[]> supplier) { log(WARNING, type, message, supplier); }
	
	
	// ============================
	// INFO
	// ============================
	
	/**
	 * Alias of {@link log(int, Class, Object)} with level {@link #INFO}
	 * @param type the class type this log entry relates to
	 * @param o the object to log
	 */
	public void info(final Class<?> type, final Object o) { log(INFO, (type == null ? null : type.getCanonicalName()), "{}", o); }
	
	/**
	 * Alias of {@link log(int, Class, Throwable)} with level {@link #INFO}
	 * @param type the class type this log entry relates to
	 * @param t the exception to log
	 */
	public void info(final Class<?> type, final Throwable t) { log(INFO, (type == null ? null : type.getCanonicalName()), "{}", t); }
	
	/**
	 * Alias of {@link log(int, Class, String, Object...)} with level {@link #INFO}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void info(final Class<?> type, final String message, final Object ...params) { log(INFO, (type == null ? null : type.getCanonicalName()), message, params); }
	
	/**
	 * Alias of {@link log(int, String, Object)} with level {@link #INFO}
	 * @param type the textual context this log entry relates to
	 * @param o the object to log
	 */
	public void info(final String type, final Object o) { log(INFO, type, "{}", o); }
	
	/**
	 * Alias of {@link log(int, String, Throwable)} with level {@link #INFO}
	 * @param type the textual context this log entry relates to
	 * @param t the exception to log
	 */
	public void info(final String type, final Throwable t) { log(INFO, type, "{}", t); }
	
	/**
	 * Alias of {@link log(int, String, String, Object...)} with level {@link #INFO}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void info(final String type, final String message, final Object ...params) { log(INFO, type, message, params); }
	
	/**
	 * Alias of {@link log(int, Class, String, Supplier)} with level {@link #INFO}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void info(final Class<?> type, final String message, final Supplier<Object[]> supplier) { log(INFO, type, message, supplier); }
	
	/**
	 * Alias of {@link log(int, String, String, Supplier)} with level {@link #INFO}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void info(final String type, final String message, final Supplier<Object[]> supplier) { log(INFO, type, message, supplier); }
		
	// ============================
	// CONFIG
	// ============================
	
	/**
	 * Alias of {@link log(int, Class, Object)} with level {@link #CONFIG}
	 * @param type the class type this log entry relates to
	 * @param o the object to log
	 */
	public void config(final Class<?> type, final Object o) { log(CONFIG, (type == null ? null : type.getCanonicalName()), "{}", o); }
	
	/**
	 * Alias of {@link log(int, Class, Throwable)} with level {@link #CONFIG}
	 * @param type the class type this log entry relates to
	 * @param t the exception to log
	 */
	public void config(final Class<?> type, final Throwable t) { log(CONFIG, (type == null ? null : type.getCanonicalName()), "{}", t); }
	
	/**
	 * Alias of {@link log(int, Class, String, Object...)} with level {@link #CONFIG}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void config(final Class<?> type, final String message, final Object ...params) { log(CONFIG, (type == null ? null : type.getCanonicalName()), message, params); }
	
	/**
	 * Alias of {@link log(int, String, Object)} with level {@link #CONFIG}
	 * @param type the textual context this log entry relates to
	 * @param o the object to log
	 */
	public void config(final String type, final Object o) { log(CONFIG, type, "{}", o); }
	
	/**
	 * Alias of {@link log(int, String, Throwable)} with level {@link #CONFIG}
	 * @param type the textual context this log entry relates to
	 * @param t the exception to log
	 */
	public void config(final String type, final Throwable t) { log(CONFIG, type, "{}", t); }
	
	/**
	 * Alias of {@link log(int, String, String, Object...)} with level {@link #CONFIG}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void config(final String type, final String message, final Object ...params) { log(CONFIG, type, message, params); }
	
	/**
	 * Alias of {@link log(int, Class, String, Supplier)} with level {@link #CONFIG}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void config(final Class<?> type, final String message, final Supplier<Object[]> supplier) { log(CONFIG, type, message, supplier); }
	
	/**
	 * Alias of {@link log(int, String, String, Supplier)} with level {@link #CONFIG}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void config(final String type, final String message, final Supplier<Object[]> supplier) { log(CONFIG, type, message, supplier); }
		
	// ============================
	// FINE
	// ============================
	
	/**
	 * Alias of {@link log(int, Class, Object)} with level {@link #FINE}
	 * @param type the class type this log entry relates to
	 * @param o the object to log
	 */
	public void fine(final Class<?> type, final Object o) { log(FINE, (type == null ? null : type.getCanonicalName()), "{}", o); }
	
	/**
	 * Alias of {@link log(int, Class, Throwable)} with level {@link #FINE}
	 * @param type the class type this log entry relates to
	 * @param t the exception to log
	 */
	public void fine(final Class<?> type, final Throwable t) { log(FINE, (type == null ? null : type.getCanonicalName()), "{}", t); }
	
	/**
	 * Alias of {@link log(int, Class, String, Object...)} with level {@link #FINE}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void fine(final Class<?> type, final String message, final Object ...params) { log(FINE, (type == null ? null : type.getCanonicalName()), message, params); }
	
	/**
	 * Alias of {@link log(int, String, Object)} with level {@link #FINE}
	 * @param type the textual context this log entry relates to
	 * @param o the object to log
	 */
	public void fine(final String type, final Object o) { log(FINE, type, "{}", o); }
	
	/**
	 * Alias of {@link log(int, String, Throwable)} with level {@link #FINE}
	 * @param type the textual context this log entry relates to
	 * @param t the exception to log
	 */
	public void fine(final String type, final Throwable t) { log(FINE, type, "{}", t); }
	
	/**
	 * Alias of {@link log(int, String, String, Object...)} with level {@link #FINE}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void fine(final String type, final String message, final Object ...params) { log(FINE, type, message, params); }
	
	/**
	 * Alias of {@link log(int, Class, String, Supplier)} with level {@link #FINE}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void fine(final Class<?> type, final String message, final Supplier<Object[]> supplier) { log(FINE, type, message, supplier); }
	
	/**
	 * Alias of {@link log(int, String, String, Supplier)} with level {@link #FINE}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void fine(final String type, final String message, final Supplier<Object[]> supplier) { log(FINE, type, message, supplier); }
		
	// ============================
	// FINER
	// ============================
	
	/**
	 * Alias of {@link log(int, Class, Object)} with level {@link #FINER}
	 * @param type the class type this log entry relates to
	 * @param o the object to log
	 */
	public void finer(final Class<?> type, final Object o) { log(FINER, (type == null ? null : type.getCanonicalName()), "{}", o); }
	
	/**
	 * Alias of {@link log(int, Class, Throwable)} with level {@link #FINER}
	 * @param type the class type this log entry relates to
	 * @param t the exception to log
	 */
	public void finer(final Class<?> type, final Throwable t) { log(FINER, (type == null ? null : type.getCanonicalName()), "{}", t); }
	
	/**
	 * Alias of {@link log(int, Class, String, Object...)} with level {@link #FINER}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void finer(final Class<?> type, final String message, final Object ...params) { log(FINER, (type == null ? null : type.getCanonicalName()), message, params); }
	
	/**
	 * Alias of {@link log(int, String, Object)} with level {@link #FINER}
	 * @param type the textual context this log entry relates to
	 * @param o the object to log
	 */
	public void finer(final String type, final Object o) { log(FINER, type, "{}", o); }
	
	/**
	 * Alias of {@link log(int, String, Throwable)} with level {@link #FINER}
	 * @param type the textual context this log entry relates to
	 * @param t the exception to log
	 */
	public void finer(final String type, final Throwable t) { log(FINER, type, "{}", t); }
	
	/**
	 * Alias of {@link log(int, String, String, Object...)} with level {@link #FINER}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void finer(final String type, final String message, final Object ...params) { log(FINER, type, message, params); }
	
	/**
	 * Alias of {@link log(int, Class, String, Supplier)} with level {@link #FINER}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void finer(final Class<?> type, final String message, final Supplier<Object[]> supplier) { log(FINER, type, message, supplier); }
	
	/**
	 * Alias of {@link log(int, String, String, Supplier)} with level {@link #FINER}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void finer(final String type, final String message, final Supplier<Object[]> supplier) { log(FINER, type, message, supplier); }
		
	// ============================
	// FINEST
	// ============================
	
	/**
	 * Alias of {@link log(int, Class, Object)} with level {@link #FINEST}
	 * @param type the class type this log entry relates to
	 * @param o the object to log
	 */
	public void finest(final Class<?> type, final Object o) { log(FINEST, (type == null ? null : type.getCanonicalName()), "{}", o); }
	
	/**
	 * Alias of {@link log(int, Class, Throwable)} with level {@link #FINEST}
	 * @param type the class type this log entry relates to
	 * @param t the exception to log
	 */
	public void finest(final Class<?> type, final Throwable t) { log(FINEST, (type == null ? null : type.getCanonicalName()), "{}", t); }
	
	/**
	 * Alias of {@link log(int, Class, String, Object...)} with level {@link #FINEST}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void finest(final Class<?> type, final String message, final Object ...params) { log(FINEST, (type == null ? null : type.getCanonicalName()), message, params); }
	
	/**
	 * Alias of {@link log(int, String, Object)} with level {@link #FINEST}
	 * @param type the textual context this log entry relates to
	 * @param o the object to log
	 */
	public void finest(final String type, final Object o) { log(FINEST, type, "{}", o); }
	
	/**
	 * Alias of {@link log(int, String, Throwable)} with level {@link #FINEST}
	 * @param type the textual context this log entry relates to
	 * @param t the exception to log
	 */
	public void finest(final String type, final Throwable t) { log(FINEST, type, "{}", t); }
	
	/**
	 * Alias of {@link log(int, String, String, Object...)} with level {@link #FINEST}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	public void finest(final String type, final String message, final Object ...params) { log(FINEST, type, message, params); }
	
	/**
	 * Alias of {@link log(int, Class, String, Supplier)} with level {@link #FINEST}
	 * @param type the class type this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void finest(final Class<?> type, final String message, final Supplier<Object[]> supplier) { log(FINEST, type, message, supplier); }
	
	/**
	 * Alias of {@link log(int, String, String, Supplier)} with level {@link #FINEST}
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param supplier function that provides the optional parameters as an <em>Object[]</em>
	 */
	public void finest(final String type, final String message, final Supplier<Object[]> supplier) { log(FINEST, type, message, supplier); }
	
	// ============================
	// ABSTRACT AND UTILS
	// ============================
	
	/**
	 * Implementations should override this method for actual logging.
	 * This method *should* only be called if the entry log level matches the current level
	 * @param level the log level for this entry
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 */
	protected abstract void handle(final int level, final String type, final String message, final Object ...params);
	
	/**
	 * Converts the logging parameters to a JSON string representation by substituting optional parameters in the message.
	 * This method is typically called from the {@link #handle(int, String, String, Object...)} method.
	 * @param level the log level for this entry
	 * @param type the textual context this log entry relates to
	 * @param message the message to log (substitution placeholder is "{}")
	 * @param params optional additional parameters to log
	 * @return the JSON string representation of this log entry
	 */
	protected String toJson(final int level, final String type, final String message, final Object ...params)
	{
		StringBuilder b = new StringBuilder();

		b.append("{\"date\": ");
		b.append(System.currentTimeMillis());
		b.append(", \"level\": ");
		b.append(level);
		b.append(", \"type\": \"");
		b.append(Json.escape(type));
		b.append("\", \"message\": \"");
		
		if( params != null && params.length > 0 )
		{
			int start = 0;
			for( int p = 0; p < params.length; p++ )
			{
				int end = message.indexOf("{}", start);
				if( end >= 0 )
				{
					b.append(Json.escape(message.substring(start, end)));
					start = end+2;
					
					if( params[p] == null )
						b.append("null");
					else if( params[p] instanceof Throwable )
					{
						b.append(Json.escape(printStackTrace((Throwable)params[p])));
					}
					else
						b.append(Json.escape(params[p].toString()));
				}
				else
					break;
			}
			if( start < message.length() )
				b.append(Json.escape(message.substring(start)));
		}
		else
		{
			b.append(Json.escape(message));
		}
		
		b.append("\"}");
		
		return b.toString();
	}
	
	/**
	 * Generates a string stack trace that omits all <code>java.*</code> or <code>aeonics.*</code> frames unless the current log level is {@link #ALL}.
	 * @param t the exception for which to print the stack trace
	 * @return the stack trace
	 */
	protected String printStackTrace(Throwable t)
	{
		StringBuilder b = new StringBuilder();
		
		for( Throwable x = t; x != null; x = x.getCause() )
		{
			b.append(t.toString());
			b.append('\n');
			
			for( StackTraceElement e : x.getStackTrace() )
			{
				if( level() > ALL && ((e.getModuleName() != null && e.getModuleName().startsWith("java.")) 
						|| e.getClassName().startsWith("java.") 
						|| e.getClassName().startsWith("sun.") 
						|| e.getClassName().startsWith("aeonics.")) )
					continue;
				
				b.append("\tat ");
				b.append(e.toString());
				b.append('\n');
			}
			
			if( x.getCause() != null )
			{
				b.append("Caused by:");
				b.append('\n');
			}
		}
		
		return b.toString();
	}
	
	/**
	 * Default initial logger template and entity implementation
	 */
	private static final class ConsoleLogger extends Manager<Logger>
	{
		private static class Implementation extends Logger
		{
			protected void handle(int level, String type, String message, Object... params) 
			{
				System.out.println(toJson(level, type, message, params));
			}
		}

		@Override
		public Template<? extends Logger> template()
		{
			return new Template<Logger>(target(), type(), category())
				.creator(creator())
				.summary("Console logger")
				.description("Sends all logs in JSON format to the standard output console regardless of the log level.");
		}
		
		protected Class<? extends ConsoleLogger.Implementation> defaultTarget() { return ConsoleLogger.Implementation.class; }
		protected Supplier<? extends ConsoleLogger.Implementation> defaultCreator() { return ConsoleLogger.Implementation::new; }
	}
	
	/**
	 * Default logger to the console.
	 * This is used when the actual logger is not (yet/longer) available
	 * @hidden
	 */
	@Internal
	public static final Logger CONSOLE = Manager.set(Logger.class, Factory.add(new ConsoleLogger()).build().name("Console Logger"));
}
