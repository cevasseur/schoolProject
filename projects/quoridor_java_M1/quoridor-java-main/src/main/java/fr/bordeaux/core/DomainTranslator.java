package fr.bordeaux.core;

/**
 * Interface representing translation capabilities required by the core domain. Decouples the domain
 * logic from the specific internationalization implementation.
 */
public interface DomainTranslator {
  /**
   * Translates a key into the current language.
   *
   * @param key the message key
   * @return the translated message
   */
  String translate(String key);

  /**
   * Translates a key with parameters into the current language.
   *
   * @param key the message key
   * @param params parameters for formatting
   * @return the formatted, translated message
   */
  String translate(String key, Object... params);
}
