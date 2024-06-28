package aeonics.template;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aeonics.data.Data;
import aeonics.entity.Entity;
import aeonics.manager.Config;
import aeonics.manager.Manager;
import aeonics.manager.Translator;
import aeonics.manager.Vault;
import aeonics.util.Documented;
import aeonics.util.Internal;
import aeonics.util.StringUtils;

/**
 * This class represents an initialization parameter for an {@link Entity}. 
 * It is used by the corresponding {@link Template} to create and populate new instances of the entity.
 */
@SuppressWarnings("unchecked")
public class Parameter implements Documented
{
	/**
	 * Creates a new parameter
	 * @param name the parameter name (should be or will be converted to lower case)
	 * @throws IllegalArgumentException if the name is null or blank.
	 */
	public Parameter(String name)
	{
		if( name == null || name.isBlank() ) throw new IllegalArgumentException("Parameter name is mandatory");
		this.name = StringUtils.toLowerCase(name);
	}
	
	/**
	 * The parameter name
	 */
	private String name;
	/**
	 * Returns the parameter name
	 * @return the parameter name
	 */
	public String name() { return name; }
	
	/**
	 * The parameter summary
	 */
	private String summary = "";
	/**
	 * Returns the parameter summary
	 * @return the parameter summary
	 */
	public String summary() { return summary; }
	/**
	 * Sets the parameter summary
	 * @param <P> this parameter type
	 * @param value the summary
	 * @return this
	 */
	public <P extends Parameter> P summary(String value) { summary = value; return (P)this; }
	
	/**
	 * The parameter expected format
	 */
	private String format = "text";
	/**
	 * Returns the parameter expected format.
	 * The parameter format is just an indication on how the parameter should be formatted and/or displayed to the user.
	 * There is no automatic {@link #rule()} that match the format, you should set both. 
	 * @return the parameter expected format
	 */
	public String format() { return format; }
	/**
	 * Sets the parameter expected format.
	 * The parameter format is just an indication on how the parameter should be formatted and/or displayed to the user.
	 * There is no automatic {@link #rule()} that match the format, you should set both. 
	 * @param <P> this parameter type
	 * @param value the expected format
	 * @return this
	 */
	public <P extends Parameter> P format(String value) { format = value; return (P)this; }
	
	/**
	 * The parameter description
	 */
	private String description = "";
	/**
	 * Returns the parameter description
	 * @return the parameter description
	 */
	public String description() { return description; }
	/**
	 * Sets the parameter description
	 * @param <P> this parameter type
	 * @param value the description
	 * @return this
	 */
	public <P extends Parameter> P description(String value) { description = value; return (P)this; }
	
	/**
	 * Whether or not this parameter is bindable
	 */
	private boolean bindable = true;
	/**
	 * Returns whether or not this parameter is bindable.
	 * @return whether or not this parameter is bindable
	 * @see #bindable(boolean)
	 */
	public boolean bindable() { return bindable; }
	/**
	 * Sets whether or not this parameter is bindable.
	 * Bindable parameters may contain <code>${...}</code> constructs that will be substituted at runtime
	 * depending on the context, or the configuration.
	 * @param <P> this parameter type
	 * @param value whether or not to perform binding at runtime
	 * @return this
	 */
	public <P extends Parameter> P bindable(boolean value) { bindable = value; return (P)this; }
	
	/**
	 * Validates the provided value against the {@link #validator()} of this parameter
	 * @param value the value to validate
	 * @return true if the value is valid for this parameter, false otherwise
	 */
	public boolean validate(Data value)
	{
		if( value == null || value.isNull() ) value = defaultValue();
		if( value == null ) value = Data.empty();
		
		Predicate<Data> v = validator();
		if( v != null ) return v.test(value);
		else return true;
	}
	
	/**
	 * The parameter validator
	 */
	private Predicate<Data> validator = (Data value) ->
	{
		String s = value.asString();
		if( s.length() == 0 && defaultValue() != null ) s = defaultValue().asString(); 
		if( s.length() == 0 && !optional() ) return false;
		if( s.length() == 0 && optional() ) return true;
		if( s.length() < min() || s.length() > max() ) return false;
		if( values().size() > 0 && !values().contains(s) ) return false;
		if( rule() != null ) return rule().test(s);
		return true;
	};
	
	/**
	 * Returns the validator for this parameter.
	 * <p>The default validator is checking the <code>toString()</code> representation with
	 * regard to the {@link #defaultValue()}, {@link #optional()}, {@link #min()}, {@link #max()}, {@link #values()} and {@link #rule()}.</p>
	 * <p>If you need a custom validation, then you should set one using {@link #validator(Predicate)}.</p> 
	 * @return the validator for this parameter
	 */
	public Predicate<Data> validator() { return validator; }
	
	/**
	 * Sets the validator for this parameter.
	 * <p>This method will replace the default validator so no other checks other than the one provided will be performed.</p>
	 * @param <P> this parameter type
	 * @param value the validator for this parameter
	 * @return this
	 */
	public <P extends Parameter> P validator(Predicate<Data> value) { this.validator = value; return (P)this; }
	
	/**
	 * The default value
	 */
	private Data defaultValue = null;
	/**
	 * Returns the default value of this parameter
	 * @return the default value of this parameter
	 */
	public Data defaultValue() { return defaultValue; }
	/**
	 * Defines an optional default value for this parameter.
	 * @param <P> this parameter type
	 * @param value the default value
	 * @return this
	 */
	public <P extends Parameter> P defaultValue(Data value) { defaultValue = value; return (P)this; }
	
	/**
	 * The binding pattern
	 */
	private static Pattern bindingPattern = Pattern.compile("\\$\\{([a-z]+):(.+?)\\}");
	
	/**
	 * Computes the final value of this parameter considering the binding context if applicable.
	 * @param value the parameter value
	 * @param context the current context
	 * @return the final parameter value
	 * @hidden
	 */
	@Internal
	public Data resolve(Data value, Data context)
	{
		if( value == null || value.isEmpty() )
		{
			Data v = defaultValue(); 
			if( v == null ) return Data.empty();
			else value = v;
		}
		if( value.isEmpty() || !bindable() || !value.isString() ) return value;
		
		StringBuilder sb = new StringBuilder();
		Matcher matcher = bindingPattern.matcher(value.asString());
		while( matcher.find() )
		{
			String name = matcher.group(2);
			switch(matcher.group(1))
			{
				case "config":
					try { matcher.appendReplacement(sb, Objects.requireNonNullElse(Manager.of(Config.class).get(name).asString(), "")); }
					catch(Exception e) { matcher.appendReplacement(sb, ""); }
					break;
				case "context":
					try { matcher.appendReplacement(sb, context != null ? context.getNested(name).asString() : ""); }
					catch(Exception e) { matcher.appendReplacement(sb, ""); }
					break;
				case "secret":
					try { matcher.appendReplacement(sb, Objects.requireNonNullElse(Manager.of(Vault.class).get(name).toString(), "")); }
					catch(Exception e) { matcher.appendReplacement(sb, ""); }
					break;
				case "translate":
					try
					{
						String[] parts = StringUtils.split(name, "|");
						if( parts.length == 0 ) { matcher.appendReplacement(sb, ""); break; }
						String language = Manager.of(Translator.class).language();
						int i = 0;
						if( parts.length > 1 && parts[0].length() == 2 ) { language = parts[0]; i = 1; }
						String text = StringUtils.substitute(Manager.of(Translator.class).get(parts[i], language), "{}", Arrays.copyOfRange(parts, i, parts.length));
						matcher.appendReplacement(sb, text);
					}
					catch(Exception e) { matcher.appendReplacement(sb, ""); }
					break;
				default:
					matcher.appendReplacement(sb, "");
					break;
			}
		}
		matcher.appendTail(sb);
		
		return Data.of(sb.toString());
	}
	
	/**
	 * Whether or not this parameter allows empty values
	 */
	private boolean optional = false;
	/**
	 * Returns whether or not this parameter allows empty values
	 * @return true if this parameter allows empty values
	 */
	public boolean optional() { return optional; }
	/**
	 * Sets whether or not this parameter allows empty values
	 * @param value whether or not this parameter allows empty values
	 * @return this
	 */
	public Parameter optional(boolean value) { this.optional = value; return this; }
	
	/**
	 * Minimum length of this parameter
	 */
	private int min = 0;
	/**
	 * Returns the minimum length of this parameter
	 * @return the minimum length of this parameter
	 */
	public int min() { return min; }
	/**
	 * Sets the minimum length of this parameter
	 * @param value the minimum length of this parameter
	 * @return this
	 */
	public Parameter min(int value) { this.min = value; return this; }
	
	/**
	 * Maximum length if this parameter
	 */
	private int max = Integer.MAX_VALUE;
	/**
	 * Returns the maximum length of this parameter
	 * @return the maximum length of this parameter
	 */
	public int max() { return max; }
	/**
	 * Sets the maximum length of this parameter
	 * @param value the maximum length of this parameter
	 * @return this
	 */
	public Parameter max(int value) { this.max = value; return this; }
	
	/**
	 * Set of acceptable values
	 */
	private Set<String> values = new HashSet<>();
	/**
	 * Returns the set of acceptable values
	 * @return the set of acceptable values
	 */
	public Set<String> values() { return values; }
	/**
	 * Sets the acceptable values. This method overrides the existing acceptables values with the ones provided.
	 * @param values the acceptable values
	 * @return this
	 */
	public Parameter values(String... values) { for( String v : values ) this.values.add(v); return this; }

	/**
	 * Specific validation rule
	 */
	private Predicate<String> rule = null;
	/**
	 * Returns the validation rule for this parameter
	 * @return the validation rule for this parameter
	 */
	public Predicate<String> rule() { return rule; }
	/**
	 * Sets the validation rule for this parameter
	 * @param value the validation rule for this parameter
	 * @return this
	 */
	public Parameter rule(Predicate<String> value) { this.rule = value; return this; }
	/**
	 * Sets the validation rule for this parameter as the specified character list 
	 * @param value the list of allowed characters
	 * @return this
	 */
	public Parameter rule(String value) { this.rule = (String input) -> { return StringUtils.isComposedOf(input, value); }; return this; }

	@Override
	public Data export()
	{
		return Documented.super.export()
			.put("bindable", bindable())
			.put("defaultValue", defaultValue())
			.put("optional", optional())
			.put("min", min())
			.put("max", max())
			.put("format", format())
			.put("values", values())
			.put("rule", rule != null);
	}
	
	/**
	 * This class provides default values that can be used in {@link Parameter#format(String)}.
	 */
	public static class Format
	{
		private Format() { /* no instances */ }
		public static final String TEXT = "text";
		public static final String LONGTEXT = "longtext";
		public static final String NUMBER = "number";
		public static final String PASSWORD = "password";
		public static final String BOOLEAN = "boolean";
		public static final String DATE = "date";
		public static final String TIME = "time";
		public static final String DATETIME = "datetime";
		public static final String JSON = "json";
		public static final String CODE = "code";
		public static final String SELECT = "select";
		public static final String OPAQUE = "opaque";
	}
	
	/**
	 * This class provides default values that can be used in {@link Parameter#rule(Predicate)}. 
	 */
	public static class Rule
	{
		private Rule() { /* no instances */ }
		
		/**
		 * Rule for upper case A-Z
		 */
		public static final Predicate<String> UPPER = StringUtils::isUpper;
		
		/**
		 * Rule for lower case a-z
		 */
		public static final Predicate<String> LOWER = StringUtils::isLower;
		
		/**
		 * Rule for digits 0-9
		 */
		public static final Predicate<String> DIGIT = StringUtils::isDigit;
		
		/**
		 * Rule for letters a-z A-Z
		 */
		public static final Predicate<String> ALPHA = StringUtils::isAlpha;
		
		/**
		 * Rule for letters and digits a-z A-Z 0-9
		 */
		public static final Predicate<String> ALPHANUM = StringUtils::isAlphaNum;
		
		/**
		 * Rule for letters, digits and space a-z A-Z 0-9
		 */
		public static final Predicate<String> ALPHANUMSPACE = (String value) -> StringUtils.isAlphaNum(value, true);
		
		/**
		 * Rule for boolean
		 */
		public static final Predicate<String> BOOLEAN = StringUtils::isBoolean;
		
		/**
		 * Rule for base64
		 */
		public static final Predicate<String> BASE64 = StringUtils::isBase64;
		
		/**
		 * Rule for hexa
		 */
		public static final Predicate<String> HEXA = StringUtils::isHexa;
		
		/**
		 * Rule for integer
		 */
		public static final Predicate<String> INTEGER = StringUtils::isInteger;
		
		/**
		 * Rule for floating point number
		 */
		public static final Predicate<String> FLOAT = StringUtils::isFloatingPoint;
		
		/**
		 * Rule for routing path
		 */
		public static final Predicate<String> PATH = StringUtils::isPath;
		
		/**
		 * Rule for routing path with wildcards
		 */
		public static final Predicate<String> WILDCARD_PATH = StringUtils::isWildcardPath;
		
		/**
		 * Rule for entity id
		 */
		public static final Predicate<String> ID = (String value) -> value != null && value.length() == 25 && value.charAt(8) == '-' && StringUtils.isHexa(value.substring(0, 8)) && StringUtils.isHexa(value.substring(9));
		
		/**
		 * Rule for email address
		 */
		public static final Predicate<String> EMAIL = StringUtils::isEmailSimple;
		
		/**
		 * Rule for file name
		 */
		public static final Predicate<String> FILENAME = (String value) -> StringUtils.isComposedOf(value, "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789_.");
		
		/**
		 * Rule for URL
		 */
		public static final Predicate<String> URL = StringUtils::isUrlSimple;
		
		/**
		 * Rule for JSON Object
		 * Note that this is a simple trivial check, it does not mean that the value is a valid JSON Object.
		 */
		public static final Predicate<String> JSON_MAP = (String value) -> value != null && value.startsWith("{") && value.endsWith("}");
		
		/**
		 * Rule for JSON Array
		 * Note that this is a simple trivial check, it does not mean that the value is a valid JSON Array.
		 */
		public static final Predicate<String> JSON_LIST = (String value) -> value != null && value.startsWith("[") && value.endsWith("]");
	}
}
