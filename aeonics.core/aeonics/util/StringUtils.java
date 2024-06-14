package aeonics.util;

import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;

/**
 * Simple fast alternatives to regular String operations
 */
public class StringUtils
{
	private StringUtils() { /* no instances */ }
	
	/**
	 * Splits the input string at the specified delimiter.
	 * This method is similar to <code>String.split()</code> but does not use regex.
	 * Any empty trailing parts are removed from the returning array.
	 * @param input the input string
	 * @param delimiter the delimiter
	 * @return the sliced string parts
	 */
	public static String[] split(String input, String delimiter)
	{
		ArrayList<String> parts = new ArrayList<>();
		
		int i = 0, mark = 0;
		while( (i = input.indexOf(delimiter, mark)) != -1 )
		{
			parts.add(input.substring(mark, i));
			mark = i+delimiter.length();
		}
		parts.add(input.substring(mark));
		
		// remove trailing empty values to mimick String.split()
		for( i = parts.size()-1; i >= 0; i-- )
			if( parts.get(i).isEmpty() )
				parts.remove(i);
		
		return parts.toArray(new String[0]);
	}
	
	/**
	 * Performs a substitution of all tokens in the original text with the provided values in order.
	 * If some tokens do not have a matching value, they are replaced by an empty string.
	 * @param text the original text
	 * @param token the token to replace
	 * @param values the values to substitute
	 * @return the substituted text
	 */
	public static String substitute(String text, String token, String... values)
	{
		if( text == null || text.isEmpty() ) return "";
		if( token == null || token.isEmpty() ) return text;
		
		StringBuilder b = new StringBuilder(text.length());
		
		int i = 0, mark = 0, v = 0;
		while( (i = text.indexOf(token, mark)) != -1 )
		{
			b.append(text, mark, i);
			if( values.length > v )
			{
				b.append(values[v]);
				v++;
			}
			mark = i+token.length();
		}
		b.append(text, mark, text.length());
		
		return b.toString();
	}
	
	/**
	 * The Class<?> cache for toLowerCase
	 */
	private static WeakHashMap<Class<?>, String> c1 = new WeakHashMap<>();
	
	/**
	 * Converts the provided class's simple name to lowercase
	 * @param clazz the class instance
	 * @return the lower case class name
	 */
	public static String toLowerCase(Class<?> clazz)
	{
		if( clazz == null ) return "";
		return c1.computeIfAbsent(clazz, (c) -> c.getName().toLowerCase(Locale.ROOT));
	}
	
	/**
	 * The String cache for toLowerCase
	 */
	private static WeakHashMap<String, String> c2 = new WeakHashMap<>();
	
	/**
	 * Converts the provided string to lowercase
	 * @param string the input string
	 * @return the lower case of the input string
	 */
	public static String toLowerCase(String string)
	{
		if( string == null || string.isEmpty() ) return "";
		return c2.computeIfAbsent(string, (s) -> s.toLowerCase(Locale.ROOT));
	}
	
	/**
	 * Checks if the subject matches the pattern.
	 * <p>The subject and pattern are sequence of possibly multiple words separated by a boundary character. The following rules apply:</p>
	 * <ul><li>The subject word boundary character is either '.' or '/'.</li> 
	 * <li>The pattern word wildcard character is either '+' or '*'.</li>
	 * <li>The pattern global wildcard character is '#'.</li>
	 * <li>The pattern can be inverted with a single leading '!' character.</li>
	 * <li>Both the subject and pattern are trimmed from leading and trailing word boundary or space characters.</li>
	 * <li>In the pattern, if some characters follow a word wildcard, they are ignored until the next word. 
	 * If some characters follow a global wildcard, they are ignored overall.
	 * If multiple wildcard characters follow each other, they are ignored.</li>
	 * <li>If any of the subject or pattern is null, then it is never a match</li>
	 * <li>An empty word is valid except at the beginning or the end in which case they are trimmed.</li>
	 * </ul>
	 * @param subject the subject to match
	 * @param pattern the matching rule
	 * @return true if the subject matches the pattern
	 */
	public static boolean simplePathMatches(String pattern, String subject) { return simplePathMatches(pattern, subject, defaultWordWildcards, defaultGlobalWildcards, defaultNegators, defaultWordDelimiters); }
	
	/**
	 * Default word wildcards used by {@link #simplePathMatches(String, String)}: <code>+</code> and <code>*</code>.
	 */
	public static final char[] defaultWordWildcards = new char[] { '+', '*' };
	/**
	 * Default global wildcards used by {@link #simplePathMatches(String, String)}: <code>#</code>.
	 */
	public static final char[] defaultGlobalWildcards = new char[] { '#' };
	/**
	 * Default negators used by {@link #simplePathMatches(String, String)}: <code>!</code>.
	 */
	public static final char[] defaultNegators = new char[] { '!' };
	/**
	 * Default word delimiters used by {@link #simplePathMatches(String, String)}: <code>.</code> and <code>/</code>.
	 */
	public static final char[] defaultWordDelimiters = new char[] { '.', '/' };
	
	/**
	 * Checks if the needle is in the haystack
	 * @param haystack the haystack
	 * @param needle the needle
	 * @return true if the needle is in the haystack
	 */
	private static boolean inArray(char[] haystack, char needle) { for( char hay : haystack ) if( hay == needle ) return true; return false; }
	
	/**
	 * Checks if the subject matches the pattern.
	 * <p>The subject and pattern are sequence of possibly multiple words separated by a boundary character. The following rules apply:</p>
	 * <ul>
	 * <li>Both the subject and pattern are trimmed from leading and trailing word boundary or space characters.</li>
	 * <li>In the pattern, if some characters follow a word wildcard, they are ignored until the next word. 
	 * If some characters follow a global wildcard, they are ignored overall.
	 * If multiple wildcard characters follow each other, they are ignored.</li>
	 * <li>If any of the subject or pattern is null, then it is never a match</li>
	 * <li>An empty word is valid except at the beginning or the end in which case they are trimmed.</li>
	 * </ul>
	 * @param subject the subject to match
	 * @param pattern the matching rule
	 * @param wordWildcards the list of characters that act as word wildcards
	 * @param globalWildcards the list of characters that act as global wildcards
	 * @param negators the list of characters that act as negators
	 * @param wordDelimiters the list of characters that act as word delimiters
	 * @return true if the subject matches the pattern
	 */
	public static boolean simplePathMatches(String pattern, String subject, char[] wordWildcards, char[] globalWildcards, char[] negators, char[] wordDelimiters)
	{
		int pattern_index = 0, subject_index = 0;
		
		// ====================
		// 1. check if we should invert the pattern
		boolean invert = false;
		if( pattern != null && pattern.length() > 0 && inArray(negators, pattern.charAt(0)) ) { pattern_index = 1; invert = true; }

		// ====================
		// 2. in case any of them are null or empty
		if( pattern == null || subject == null ) return false;
		int pattern_size = pattern.length(), subject_size = subject.length();
		if( pattern_size == pattern_index ) return (invert ? subject_size > 0 : subject_size == 0);
		
		char p, s;
		
		// ====================
		// 3. skip leading word separators or spaces
		for( ; pattern_index < pattern_size; pattern_index++ )
		{
			p = pattern.charAt(pattern_index);
			if( !inArray(wordDelimiters, p) && p != ' ' )
				break;
		}
		for( ; subject_index < subject_size; subject_index++ )
		{
			s = subject.charAt(subject_index);
			if( !inArray(wordDelimiters, s) && s != ' ' )
				break;
		}
		
		// ====================
		// 4. skip trailing word separators or spaces
		for( ; pattern_index < pattern_size; pattern_size-- )
		{
			p = pattern.charAt(pattern_size-1);
			if( !inArray(wordDelimiters, p) && p != ' ' )
				break;
		}
		for( ; subject_index < subject_size; subject_size-- )
		{
			s = subject.charAt(subject_size-1);
			if( !inArray(wordDelimiters, s) && s != ' ' )
				break;
		}
		
		// ====================
		// 5. quick checks
		if( pattern_size == pattern_index ) return (invert ? subject_size > 0 : subject_size == 0);
		if( pattern_size == subject_size && pattern.equals(subject) ) return (invert ? false : true);
		
		for( ; pattern_index < pattern_size && subject_index < subject_size; subject_index++ )
		{
			p = pattern.charAt(pattern_index);
			s = subject.charAt(subject_index);
			
			// ====================
			// 6. pattern has global wildcard -> match
			if( inArray(globalWildcards, p) ) return (invert ? false : true);
			
			// ====================
			// 7. pattern has word wildcard -> skip subject word and remaining pattern if needed
			if( inArray(wordWildcards, p) )
			{
				for( ; pattern_index < pattern_size; pattern_index++ )
				{
					p = pattern.charAt(pattern_index);
					if( inArray(wordDelimiters, p) )
						break;
					if( inArray(globalWildcards, p) ) return (invert ? false : true);
				}
				for( ; subject_index < subject_size; subject_index++ )
				{
					s = subject.charAt(subject_index);
					if( inArray(wordDelimiters, s) )
						break;
				}
				subject_index--; // because will be ++ in the for loop
				continue;
			}
			
			// ====================
			// 8. check if both are at a word separator
			if( inArray(wordDelimiters, p) )
			{
				if( !inArray(wordDelimiters, s) ) return (invert ? true : false);
				pattern_index++;
				continue;
			}
			
			// ====================
			// 9. check if current letter match
			if( s != p ) return (invert ? true : false);
			
			pattern_index++;
		}
		
		// ====================
		// 10. we did not match the entire subject
		if( subject_index < subject_size )
			return (invert ? true : false);
		
		// ====================
		// 11. we did not check the entire pattern
		if( pattern_index < pattern_size )
		{
			for( ; pattern_index < pattern_size; pattern_index++ )
			{
				p = pattern.charAt(pattern_index);
				if( inArray(globalWildcards, p) ) return (invert ? false : true);
				if( inArray(wordWildcards, p) )
				{
					for( ; pattern_index < pattern_size; pattern_index++ )
					{
						p = pattern.charAt(pattern_index);
						if( inArray(wordDelimiters, p) )
							break;
						if( inArray(globalWildcards, p) ) return (invert ? false : true);
					}
					continue;
				}
				return (invert ? true : false);
			}
		}
		
		// ====================
		// 12. it is a match
		return (invert ? false : true);
	}
	
	/**
	 * Removes the specified characters from the front and end of the input string.
	 * @param input the input string
	 * @param trim all characters that shall be removed
	 * @return the trimmed string
	 */
	public static String trim(String input, char...trim)
	{
		if( input == null || input.isEmpty() ) return input;
		
		int start = 0, end = input.length();
		
		for( int i = 0; i < input.length(); i++ )
		{
			char s = input.charAt(i);
			char e = input.charAt(input.length()-i-1);
			for( int j = 0; j < trim.length; j++ )
			{
				char c = trim[j];
				if( s == c && start == i ) start++;
				if( e == c && end == input.length()-i) end--;
			}
			
			if( start >= end ) return "";
			if( start < i && end > input.length()-i ) break;
		}
		
		return input.substring(start, end);
	}
	
	/**
	 * Remove characters from the end of the input string
	 * @param s the input string
	 * @param i the number of chars remove (positive or negative yields the same result)
	 * @return the truncated string
	 */
	public static String truncate(String s, int i)
	{
		if( s == null ) return null;
		if( i >= 0 ) return s.substring(i >= s.length() ? s.length() - 1 : i);
		else
		{
			i = s.length() + i;
			return s.substring(0, i < 0 ? 0 : i);
		}
	}
	
	/**
	 * Checks if the input string contains any of the provided characters
	 * @param s the input string
	 * @param characters list of characters
	 * @return true if the string contains any of the specified characters
	 */
	public static boolean contains(String s, CharSequence characters)
	{
		if( characters == null || characters.length() == 0 ) return false;
		
		for( int i = 0; i < characters.length(); i++ )
		{
			if( s.indexOf(characters.charAt(i)) > 0 )
				return true;
		}
		return false;
	}
	
	/**
	 * Checks if the input string is composed only of the provided characters
	 * @param s the input string
	 * @param characters list of characters
	 * @return true if the string is composed only of the specified characters
	 */
	public static boolean isComposedOf(String s, String characters)
	{
		if( characters == null || characters.length() == 0 ) return false;
		
		int ca = 0;
		int cb = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			ca = s.codePointAt(i);
			boolean found = false;
			for( int j = 0; j < characters.length(); j++ )
			{
				cb = characters.codePointAt(j);
				if( ca == cb )
				{
					found = true;
					break;
				}
			}
			if( !found ) return false;
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of digits (0-9)
	 * @param s the input string
	 * @return true if the string only contains digits
	 */
	public static boolean isDigit(String s) { return isDigit(s, false); }
	
	/**
	 * Checks if the input string is composed only of digits (0-9)
	 * @param s the input string
	 * @param allowSpace if true, spaces are allowed too
	 * @return true if the string only contains digits
	 */
	public static boolean isDigit(String s, boolean allowSpace)
	{
		if( s == null || s.length() == 0 ) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < '0' || cp > '9' )
			{
				if( !(allowSpace && cp == ' ') )
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of digits (0-9) eventually preceded by the minus sign
	 * @param s the input string
	 * @return true if the string only contains digits eventually preceded by the minus sign
	 */
	public static boolean isInteger(String s)
	{
		if( s == null || s.length() == 0 ) return false;
		
		int cp = 0;
		int i = 0;
		if( s.codePointAt(0) == '-' ) i++;
		for( ; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < '0' || cp > '9' )
				return false;
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of digits (0-9) eventually preceded by the minus sign and may contain one decimal point separator
	 * @param s the input string
	 * @return true if the string only contains digits eventually preceded by the minus sign and may contain one decimal point separator
	 */
	public static boolean isFloatingPoint(String s)
	{
		if( s == null || s.length() == 0 ) return false;
		
		boolean point = false;
		int cp = 0;
		int i = 0;
		if( s.codePointAt(0) == '-' ) i++;
		for( ; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp == '.' )
			{
				if( point ) return false;
				point = true;
			}
			else if( cp < '0' || cp > '9' )
				return false;
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of lower case characters (a-z)
	 * @param s the input string
	 * @return true if the string only contains lower case characters
	 */
	public static boolean isLower(String s) { return isLower(s, false); }
	
	/**
	 * Checks if the input string is composed only of lower case characters (a-z)
	 * @param s the input string
	 * @param allowSpace if true, spaces are allowed too
	 * @return true if the string only contains lower case characters
	 */
	public static boolean isLower(String s, boolean allowSpace)
	{
		if( s == null || s.length() == 0 ) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < 'a' || cp > 'z' )
			{
				if( !(allowSpace && cp == ' ') )
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of upper case characters (A-Z)
	 * @param s the input string
	 * @return true if the string only contains upper case characters
	 */
	public static boolean isUpper(String s) { return isUpper(s, false); }
	
	/**
	 * Checks if the input string is composed only of upper case characters (A-Z)
	 * @param s the input string
	 * @param allowSpace if true, spaces are allowed too
	 * @return true if the string only contains upper case characters
	 */
	public static boolean isUpper(String s, boolean allowSpace)
	{
		if( s == null || s.length() == 0 ) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < 'A' || cp > 'Z' )
			{
				if( !(allowSpace && cp == ' ') )
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of alphabetic characters (a-z A-Z)
	 * @param s the input string
	 * @return true if the string only contains alphanumeric characters
	 */
	public static boolean isAlpha(String s) { return isAlpha(s, false); }
	
	/**
	 * Checks if the input string is composed only of alphabetic characters (a-z A-Z)
	 * @param s the input string
	 * @param allowSpace if true, spaces are allowed too
	 * @return true if the string only contains alphanumeric characters
	 */
	public static boolean isAlpha(String s, boolean allowSpace)
	{
		if( s == null || s.length() == 0 ) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < 'A' || cp > 'z' )
			{
				if( !(allowSpace && cp == ' ') )
					return false;
			}
			if( cp > 'Z' && cp < 'a' ) return false;
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of alphanumeric characters (a-z A-Z 0-9)
	 * @param s the input string
	 * @return true if the string only contains alphanumeric characters
	 */
	public static boolean isAlphaNum(String s) { return isAlphaNum(s, false); }
	
	/**
	 * Checks if the input string is composed only of alphanumeric characters (a-z A-Z 0-9)
	 * @param s the input string
	 * @param allowSpace if true, spaces are allowed too
	 * @return true if the string only contains alphanumeric characters
	 */
	public static boolean isAlphaNum(String s, boolean allowSpace)
	{
		if( s == null || s.length() == 0 ) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < '0' || cp > 'z' )
			{
				if( !(allowSpace && cp == ' ') )
					return false;
			}
			if( cp > '9' && cp < 'A' ) return false;
			if( cp > 'Z' && cp < 'a' ) return false;
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of lower case hexadecimal characters (a-f 0-9)
	 * @param s the input string
	 * @return true if the string only contains lower case hexadecimal characters
	 */
	public static boolean isHexaLower(String s) { return isHexaLower(s, 0); }
	
	/**
	 * Checks if the input string is composed only of lower case hexadecimal characters (a-f 0-9)
	 * @param s the input string
	 * @param maxLength the maximum length (&lt;= 0 for unspecified)
	 * @return true if the string only contains lower case hexadecimal characters
	 */
	public static boolean isHexaLower(String s, int maxLength)
	{
		if( s == null || s.length() == 0 || (maxLength > 0 && s.length() > maxLength)) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < '0' || cp > 'f' ) return false;
			if( cp > '9' && cp < 'a' ) return false;
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of upper case hexadecimal characters (A-F 0-9)
	 * @param s the input string
	 * @return true if the string only contains upper case hexadecimal characters
	 */
	public static boolean isHexaUpper(String s) { return isHexaUpper(s, 0); }
	
	/**
	 * Checks if the input string is composed only of upper case hexadecimal characters (A-F 0-9)
	 * @param s the input string
	 * @param maxLength the maximum length (&lt;= 0 for unspecified)
	 * @return true if the string only contains upper case hexadecimal characters
	 */
	public static boolean isHexaUpper(String s, int maxLength)
	{
		if( s == null || s.length() == 0 || (maxLength > 0 && s.length() > maxLength)) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < '0' || cp > 'F' ) return false;
			if( cp > '9' && cp < 'A' ) return false;
		}
		return true;
	}
	
	/**
	 * Checks if the input string is composed only of hexadecimal characters (a-f A-F 0-9)
	 * @param s the input string
	 * @return true if the string only contains hexadecimal characters
	 */
	public static boolean isHexa(String s) { return isHexa(s, 0); }
	
	/**
	 * Checks if the input string is composed only of hexadecimal characters (a-f A-F 0-9)
	 * @param s the input string
	 * @param maxLength the maximum length (&lt;= 0 for unspecified)
	 * @return true if the string only contains hexadecimal characters
	 */
	public static boolean isHexa(String s, int maxLength)
	{
		if( s == null || s.length() == 0 || (maxLength > 0 && s.length() > maxLength) ) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < '0' || cp > 'f' ) return false;
			if( cp > '9' && cp < 'A' ) return false;
			if( cp > 'F' && cp < 'a' ) return false;
		}
		return true;
	}
	
	/**
	 * Checks if the input string is null, "" or case invariant "null"
	 * @param s the input string
	 * @return true if the string is null, "" or case invariant "null"
	 */
	public static boolean isNull(String s)
	{
		if( s == null || s.length() == 0 ) return true;
		return "null".equalsIgnoreCase(s);
	}
	
	/**
	 * Checks if the input string is a boolean (case invariant true/false 1/0)
	 * @param s the input string
	 * @return true if the string is a boolean
	 */
	public static boolean isBoolean(String s) { return isBoolean(s, false); }
	
	/**
	 * Checks if the input string is a boolean (case invariant true/false 1/0)
	 * @param s the input string
	 * @param allowYesNo allow case invariant yes/no too
	 * @return true if the string is a boolean
	 */
	public static boolean isBoolean(String s, boolean allowYesNo)
	{
		if( s == null || s.length() == 0 || s.length() > 5 ) return false;
		if( allowYesNo && ("yes".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)) ) return true;
		return "1".equals(s) || "0".equals(s) || "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
	}
	
	/**
	 * Checks if the input string is a boolean true (case invariant "true" or 1)
	 * @param s the input string
	 * @return true if the string is a boolean true
	 */
	public static boolean isTrue(String s) { return isTrue(s, false); }
	
	/**
	 * Checks if the input string is a boolean true (case invariant "true" or 1)
	 * @param s the input string
	 * @param allowYesNo allow case invariant "yes" too
	 * @return true if the string is a boolean true
	 */
	public static boolean isTrue(String s, boolean allowYesNo)
	{
		if( s == null || s.length() == 0 || s.length() > 4 ) return false;
		if( allowYesNo && "yes".equalsIgnoreCase(s) ) return true;
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}
	
	/**
	 * Checks if the input string is a boolean false (case invariant "false" or 1)
	 * @param s the input string
	 * @return true if the string is a boolean false
	 */
	public static boolean isFalse(String s) { return isFalse(s, false); }
	
	/**
	 * Checks if the input string is a boolean false (case invariant "false" or 1)
	 * @param s the input string
	 * @param allowYesNo allow case invariant "no" too
	 * @return true if the string is a boolean false
	 */
	public static boolean isFalse(String s, boolean allowYesNo)
	{
		if( s == null || s.length() == 0 || s.length() > 5 ) return false;
		if( allowYesNo && "no".equalsIgnoreCase(s) ) return true;
		return "0".equals(s) || "false".equalsIgnoreCase(s);
	}
	
	/**
	 * Checks if the input string contains only base64 valid characters.
	 * It does not imply that the input is valid Base64.
	 * @param s the input string
	 * @return true if the string contains only base64 valid characters
	 */
	public static boolean isBase64(String s)
	{
		if( s == null || s.length() == 0 ) return false;
		
		int cp = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			switch(cp)
			{
				case '\r': case '\n': case '\t': case ' ': case '+': case '/': case '=': case '-': case '_': break;
				default:
					if( cp < '0' || cp > 'z' ) return false;
					if( cp > '9' && cp < 'A' ) return false;
					if( cp > 'Z' && cp < 'a' ) return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if the input string seems to be an email address.
	 * It does not imply that the input is a valid email address.
	 * @param s the input string
	 * @return true if the string seems to be an email address
	 */
	public static boolean isEmailSimple(String s)
	{
		if( s == null || s.length() == 0 || s.length() > 255 ) return false;
		int at = s.indexOf('@');
		if( at < 1 || at > (s.length()-3) || at != s.lastIndexOf('@') ) return false;
		
		int i = 0;
		int cp = 0;
		
		for( ; i < at; i++ )
		{
			cp = s.codePointAt(i);
			switch(cp)
			{
				case '-': case ':': case '+': case '.': case '_': break;
				default:
					if( cp < '0' || cp > 'z' ) return false;
					if( cp > '9' && cp < 'A' ) return false;
					if( cp > 'Z' && cp < 'a' ) return false;
			}
		}
		
		for( i++; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp == '.' || cp == '-' || cp == '_' ) break;
			if( cp < '0' || cp > 'z' ) return false;
			if( cp > '9' && cp < 'A' ) return false;
			if( cp > 'Z' && cp < 'a' ) return false;
		}
		
		if( s.length() - i < 2 ) return false; // no dot or last char is dot 
		
		for( i++; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < '0' || cp > 'z' )
				if( cp != '.' && cp != '-' )
					return false;
			if( cp > '9' && cp < 'A' ) return false;
			if( cp > 'Z' && cp < 'a' && cp != '_' ) return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the input string seems to be a URL.
	 * It does not imply that the input is a valid URL.
	 * @param s the input string
	 * @return true if the string seems to be a URL
	 */
	public static boolean isUrlSimple(String s)
	{
		if( s == null || s.length() < 10 || s.length() > 500 ) return false;
		if( !s.startsWith("http") ) return false;
		
		int i = 4;
		if( s.charAt(i) == 's' ) i++;
		if( s.charAt(i++) != ':' ) return false;
		if( s.charAt(i++) != '/' ) return false;
		if( s.charAt(i++) != '/' ) return false;
		if( s.charAt(i++) == '/' ) return false;
		
		return isComposedOf(s, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~()'!*:@,;/%?#&=+");
	}
	
	/**
	 * Checks if the input string seems to be a routing path
	 * @param s the input string
	 * @return true if the string seems to be a routing path
	 */
	public static boolean isPath(String s) { return isPath(s, false); }
	
	/**
	 * Checks if the input string seems to be a wildcard routing path
	 * @param s the input string
	 * @return true if the string seems to be a wildcard routing path
	 */
	public static boolean isWildcardPath(String s) { return isPath(s, true); }
	
	/**
	 * Checks if the input string seems to be a routing path
	 * @param s the input string
	 * @param allowWildcard allow wildcard characters too
	 * @return true if the string seems to be a routing path
	 */
	public static boolean isPath(String s, boolean allowWildcard)
	{
		if( s == null || s.length() == 0 ) return false;
		
		int cp = 0;
		int i = 0;
		if( allowWildcard && s.codePointAt(0) == '!' ) i = 1;
		for( ; i < s.length(); i++ )
		{
			cp = s.codePointAt(i);
			if( cp < '.' || cp > 'z' )
			{
				if( cp != '-' && cp != ' ' && !(allowWildcard && (cp == '+' || cp == '*' || cp == '#')) )
					return false;
			}
			else if( cp > '9' && cp < 'A' )
			{
				if( cp != ':' )
					return false;
			}
			else if( cp > 'Z' && cp < 'a' )
			{
				if( cp != '_' )
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Simple ASCII case-invariant equality.
	 * Only characters from A to Z are compared in case-insensitive manner.
	 * @param a the first input string
	 * @param b the second input string
	 * @return true if both string and are equal in the ASCII range.
	 */
	public static boolean equalsIgnoreCase(String a, String b)
	{
		if( a == null || b == null || a.length() != b.length() ) return false;
		
		for( int i = 0; i < a.length(); i++ )
		{
			int ca = a.codePointAt(i);
			int cb = b.codePointAt(i);
			
			if( ca == cb || 
				(ca >= 'a' && ca <= 'z' && ca == (cb+32)) || // convert bb to lower
				(ca >= 'A' && ca <= 'Z' && ca == (cb-32)) // convert bb to upper
				)
				continue;
			return false;
		}
		return true;
	}
}
