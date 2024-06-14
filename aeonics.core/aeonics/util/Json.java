package aeonics.util;

import java.util.HashMap;
import java.util.Map;

import aeonics.data.Data;

/**
 * Fault tolerant JSON encoder/decoder
 */
public class Json 
{
	private Json() { /* no instances */ }
	
	/**
	 * Encodes the specified object in JSON.
	 * The object is first wrapped in a data structure then converted <code>toString()</code>.
	 * @see Data
	 * @param object the object to convert to JSON
	 * @return a JSON string representation of the object
	 */
	public static String encode(Object object) { return encode(Data.of(object)); }
	
	/**
	 * Encodes the specified object in JSON.
	 * The object is first {@link Exportable#export()} then converted <code>toString()</code>.
	 * @param exportable the object to convert to JSON
	 * @return a JSON string representation of the object
	 */
	public static String encode(Exportable exportable) { return exportable == null ? "null" : encode(exportable.export()); }
	
	/**
	 * Encodes the specified object in JSON.
	 * The object converted <code>toString()</code>.
	 * @param data the object to convert to JSON
	 * @return a JSON string representation of the object
	 */
	public static String encode(Data data) { return data == null ? "null" : data.toString(); }
	
	/**
	 * Escapes special characters for JSON output
	 * @param s the text to escape
	 * @return the escaped text
	 */
	public static String escape(final String s)
	{
		if( s == null ) return null;
		
		// first loop to check if there are chars to escape

		int i = 0;
	    for( ; i < s.length(); i++ )
	    {
	        char c = s.charAt(i);
	        if( c < 0x32 || c > 0x7E) break;
	    }
	    if( i == s.length() ) return s;
	    
	    // second loop if there were escape characters
	    
		StringBuilder e = new StringBuilder(s.length());
		e.append(s, 0, i);
		
		for( ; i < s.length(); i++ )
		{
			int cp = s.codePointAt(i);
			if( Character.isSupplementaryCodePoint(cp) )
			{
				String unicode = "0000" + Integer.toString(Character.highSurrogate(cp), 16);
				e.append("\\u");
				for( int u = 4; u > 0; u-- )
					e.append(unicode.charAt(unicode.length()-u));
				
				unicode = "0000" + Integer.toString(Character.lowSurrogate(cp), 16);
				e.append("\\u");
				for( int u = 4; u > 0; u-- )
					e.append(unicode.charAt(unicode.length()-u));
				
				i++; // because the codePointAt eats 2 in case of supplementary
				continue;
			}
			
			switch(cp)
			{
				case '\\':
				case '"':
					e.append('\\');
					e.appendCodePoint(cp);
					break;
				case '\b': e.append("\\b"); break;
				case '\f': e.append("\\f"); break;
				case '\n': e.append("\\n"); break;
				case '\r': e.append("\\r"); break;
				case '\t': e.append("\\t"); break;
				default:
					if( cp <= 0x1F || cp == 0x7F || cp >= 0xF5 || cp == 0xC0 || cp == 0xC1 || (cp >= 0x80 && cp <= 0xBF))
					{
						// definately not utf8
						String unicode = "0000" + Integer.toString(cp, 16);
						e.append("\\u");
						for( int u = 4; u > 0; u-- )
							e.append(unicode.charAt(unicode.length()-u));
					}
					else if( cp <= 0x7E ) // ascii
						e.appendCodePoint(cp);
					else
					{
						// maybe utf8
						int utf8chars = 0;
						int cp2 = 0;
						if( (cp & 0xF8) == 0xF0 ) { utf8chars = 3; cp2 = cp & 0x7; }
						else if( (cp & 0xF0) == 0xE0 ) { utf8chars = 2; cp2 = cp & 0xF; }
						else if( (cp & 0xE0) == 0xC0 ) { utf8chars = 1; cp2 = cp & 0x1F; }
						
						if( i + utf8chars >= s.length() )
						{
							String unicode = "0000" + Integer.toString(cp, 16);
							e.append("\\u");
							for( int u = 4; u > 0; u-- )
								e.append(unicode.charAt(unicode.length()-u));
						}
						else
						{
							boolean isValid = true;
							for( int j = 1; j <= utf8chars && j+i < s.length(); j++ )
							{
								cp = s.codePointAt(i+j);
								if( cp < 0x80 || cp > 0xBF ) isValid = false; /* expected low surrogate but not found */
								cp2 <<= 6;
								cp2 |= cp & 0x3F;
							}
							
							if( !isValid ) cp2 = s.codePointAt(i);
							else i += utf8chars;
							
							String unicode = "0000" + Integer.toString(cp2, 16);
							e.append("\\u");
							for( int u = 4; u > 0; u-- )
								e.append(unicode.charAt(unicode.length()-u));
						}
					}
			}
		}
		
		return e.toString();
	}

	/**
	 * Unescapes special characters from encoded JSON input
	 * @param s the text to unescape
	 * @return the unescaped text
	 */
	public static String unescape(final String s)
	{
		if( s == null ) return s;
		if( s.indexOf('\\') < 0 ) return s;
		
		char[] u = new char[s.length()];
		int size = 0;
		char b = 0;
		
		for( int i = 0; i < u.length; i++ )
		{
			b = s.charAt(i);
			if( b == '\\' )
			{
				i++;
				if( i >= u.length ) { u[size++] = b; break; } // trailing backslash
				b = s.charAt(i);
				
				switch(b)
				{
					case '\\': u[size++] = '\\'; break;
					case 'n':  u[size++] = '\n'; break;
					case 'r':  u[size++] = '\r'; break;
					case 't':  u[size++] = '\t'; break;
					case 'f':  u[size++] = '\f'; break;
					case '"':  u[size++] = '"'; break;
					case '\'': u[size++] = '\''; break;
					case '/':  u[size++] = '/'; break;
					
					case 'u': // unicode escape
						try
						{
							if( u.length - i >= 4 )
							{
								char c = (char) Integer.parseInt(s.substring(i+1, i+5), 16);
								u[size++] = c;
								i += 4;
								break;
							}
						}
						catch(NumberFormatException x)
						{
							// invalid escape sequence
						}
						// fall through
						
					default: // unknown escape sequence, so keep it as is
						u[size++] = '\\';
						u[size++] = b;
				}
			}
			else
				u[size++] = b;
		}
		
		return new String(u, 0, size);
	}

	/**
	 * Decodes the input json into a data structure
	 * @param json the input text
	 * @return the parsed data
	 */
	public static Data decode(String json) { return Decoder.parse(json, new Decoder.State()); }
	
	private static class Decoder
	{
		private static class State
		{
			int mark, i, length = 0;
		}
			
		private enum Mode
		{
			ARRAY,
			MAP,
			END,
			UNQUOTED,
			DOUBLEQUOTED,
			SINGLEQUOTED,
			EOF,
			COMMENTBLOCK,
			COMMENTLINE
		}
			
		private static Data parse(String data, State state)
		{
			state.length = data.length();
			
			while( true )
			{
				switch(whatIsNext(data, state))
				{
					case ARRAY: return parseArray(data, state);
					case MAP: return parseMap(data, state);
					case END: // cannot parse this, assume all is a unquoted literal
						return Data.of(data);
					case UNQUOTED: return Data.of(parseUnQuoted(data, state));
					case SINGLEQUOTED: return Data.of(parseSingleQuoted(data, state));
					case DOUBLEQUOTED: return Data.of(parseDoubleQuoted(data, state));
					case EOF: return Data.empty();
					case COMMENTBLOCK: skipCommentBlock(data, state); break;
					case COMMENTLINE: skipCommentLine(data, state); break;
				}
			}
		}
		
		private static Mode whatIsNext(String data, State state)
		{
			char b = 0;
			
			// eat whitespaces
			for( ; state.i < state.length ; state.i++ )
			{
				b = data.charAt(state.i);
				if( b != ' ' && b != '\t' && b != '\r' && b != '\n' && b != ',' && b != ':' ) break;
			}
			
			if( state.i >= state.length ) return Mode.EOF;
			
			switch(b)
			{
				case '\'': state.mark = ++state.i; return Mode.SINGLEQUOTED;
				case '"': state.mark = ++state.i; return Mode.DOUBLEQUOTED;
				case '[': case '(': state.mark = ++state.i; return Mode.ARRAY;
				case '{': state.mark = ++state.i; return Mode.MAP;
				case '}': case ']': case ')': state.mark = ++state.i; return Mode.END;
				case '/':
				{
					if( state.i < state.length-1 )
					{
						b = data.charAt(state.i+1);
						if( b == '/' ) { state.i += 2; return Mode.COMMENTLINE; }
						if( b == '*' ) { state.i += 2; return Mode.COMMENTBLOCK; }
					}
					// fall through
				}
				default: state.mark = state.i; return Mode.UNQUOTED;
			}
		}
		
		private static String parseSingleQuoted(String data, State state)
		{
			char b = 0;
			boolean needUnescape = false;
			
			for( ; state.i < state.length; state.i++ )
			{
				b = data.charAt(state.i);
				if( b == '\\' && state.i + 1 < state.length ) { needUnescape = true; state.i++; }
				else if( b == '\'' )
				{
					String value = data.substring(state.mark, state.i);
					state.mark = ++state.i;
					return (needUnescape ? unescape(value) : value);
				}
			}
			
			String value = data.substring(state.mark, state.i);
			state.mark = ++state.i;
			return (needUnescape ? unescape(value) : value);
		}
		
		private static String parseDoubleQuoted(String data, State state)
		{
			char b = 0;
			boolean needUnescape = false;
			
			for( ; state.i < state.length; state.i++ )
			{
				b = data.charAt(state.i);
				if( b == '\\' && state.i + 1 < state.length ) { needUnescape = true; state.i++; }
				else if( b == '"' )
				{
					String value = data.substring(state.mark, state.i);
					state.mark = ++state.i;
					return (needUnescape ? unescape(value) : value);
				}
			}
			
			String value = data.substring(state.mark, state.i);
			state.mark = ++state.i;
			return (needUnescape ? unescape(value) : value);
		}
		
		private static String parseUnQuoted(String data, State state)
		{
			char b = 0;
			boolean needUnescape = false;
			
			for( ; state.i < state.length; state.i++ )
			{
				b = data.charAt(state.i);
				if( b == '\\' && state.i + 1 < state.length ) { needUnescape = true; state.i++; }
				else if( b == ' ' || b == '{' || b == '}' || b == '[' || b == ']' || b == ',' || b == ';' || b == ':' || b == '=' || b == '\t' || b == '\r' || b == '\n' )
				{
					String value = data.substring(state.mark, state.i);
					if( needUnescape ) return unescape(value);
					else if( value.length() == 4 && value.equals("null") ) return null;
					else return value;
				}
			}
			
			String value = data.substring(state.mark, state.i);
			if( needUnescape ) return unescape(value);
			else if( value.length() == 4 && value.equals("null") ) return null;
			else return value;
		}
		
		private static Data parseMap(String data, State state)
		{
			Map<String, Object> map = new HashMap<String, Object>();
			
			String key = null;
			while( true )
			{
				Mode next = whatIsNext(data, state);
				switch(next)
				{
					case EOF:
						if( key != null ) map.put(key, null);
						// fallthrough
					case END: return Data.of(map);
					case ARRAY:
						if( key == null ) throw new IllegalArgumentException("Array cannot be used as Object key");
						map.put(key, parseArray(data, state));
						key = null;
						break;
					case MAP:
						if( key == null ) throw new IllegalArgumentException("Object cannot be used as Object key");
						map.put(key, parseMap(data, state));
						key = null;
						break;
					case UNQUOTED:
						if( key == null ) key = parseUnQuoted(data, state);
						else
						{
							map.put(key, parseUnQuoted(data, state));
							key = null;
						}
						break;
					case SINGLEQUOTED:
						if( key == null ) key = parseSingleQuoted(data, state);
						else
						{
							map.put(key, parseSingleQuoted(data, state));
							key = null;
						}
						break;
					case DOUBLEQUOTED:
						if( key == null ) key = parseDoubleQuoted(data, state);
						else
						{
							map.put(key, parseDoubleQuoted(data, state));
							key = null;
						}
						break;
					case COMMENTBLOCK: skipCommentBlock(data, state); break;
					case COMMENTLINE: skipCommentLine(data, state); break;
				}
			}
		}
		
		private static Data parseArray(String data, State state)
		{
			Data array = Data.list();
			
			while( true )
			{
				Mode next = whatIsNext(data, state);
				switch(next)
				{
					case EOF:
					case END:          return array;
					case ARRAY:        array.add(parseArray(data, state));        break;
					case MAP:          array.add(parseMap(data, state));          break;
					case UNQUOTED:     array.add(parseUnQuoted(data, state));     break;
					case SINGLEQUOTED: array.add(parseSingleQuoted(data, state)); break;
					case DOUBLEQUOTED: array.add(parseDoubleQuoted(data, state)); break;
					case COMMENTBLOCK: skipCommentBlock(data, state);             break;
					case COMMENTLINE:  skipCommentLine(data, state);              break;
				}
			}
		}
		
		private static void skipCommentBlock(String data, State state)
		{
			char b = 0;
			for( ; state.i < state.length-1; state.i++ )
			{
				b = data.charAt(state.i);
				if( b == '*' )
				{
					b = data.charAt(++state.i);
					if( b == '/' )
					{
						state.i++;
						return;
					}
				}
			}
		}
		
		private static void skipCommentLine(String data, State state)
		{
			char b = 0;
			for( ; state.i < state.length; state.i++ )
			{
				b = data.charAt(state.i);
				if( b == '\r' || b == '\n' ) { state.i++; return; }
			}
		}
	}
}
