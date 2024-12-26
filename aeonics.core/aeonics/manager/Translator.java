package aeonics.manager;

import java.util.Map;

import aeonics.data.Data;

/**
 * Manages the translation of text intented for end-users.
 * The availability of any language depends on the implementation, however it is recommended to always provide english ('en') as a fallback.
 */
public abstract class Translator extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Translator.class; }
	
	/**
	 * Returns the current active instance of this manager type.
	 * @return the current active instance of this manager type
	 */
	public static Translator get() { return Manager.of(Translator.class); }
	
	/**
	 * Sets the default language
	 * @param language the ISO-639 (2 letter) language code
	 */
	public abstract void language(String language);
	
	/**
	 * Returns the current default language
	 * @return the current default language
	 */
	public abstract String language();
	
	/**
	 * Gets the translation of the specified key in the default language.
	 * @see #language()
	 * @param key the translation key
	 * @return the translated text
	 */
	public String get(String key) { return get(key, language()); }
	
	/**
	 * Gets the translation of the specified key in the specified language.
	 * The implementation may return the translated value in the default language if the translation does not exist in the specified language.
	 * @param key the translation key
	 * @param language the ISO-639 (2 letter) language code
	 * @return the translated text
	 */
	public abstract String get(String key, String language);
	
	/**
	 * Sets the translation of the specified key in the specified language.
	 * @param key the translation key
	 * @param text the translated text
	 * @param language the ISO-639 (2 letter) language code
	 */
	public abstract void set(String key, String text, String language);
	
	/**
	 * Sets the translation of the specified keys in the specified language.
	 * @param keys the translated key-value pairs
	 * @param language the ISO-639 (2 letter) language code
	 */
	public void set(Data keys, String language)
	{
		if( keys == null || keys.size() == 0 ) return;
		if( !keys.isMap() ) throw new IllegalArgumentException("Invalid input data");
		
		for( Map.Entry<String, Data> entry : keys.entrySet() )
			set(entry.getKey(), entry.getValue().asString(), language);
	}
	
	/**
	 * Clears all translations in the specified language
	 * @param language the ISO-639 (2 letter) language code
	 */
	public abstract void clear(String language);
	
	/**
	 * Clears all translations in all languages
	 */
	public abstract void clear();
}
