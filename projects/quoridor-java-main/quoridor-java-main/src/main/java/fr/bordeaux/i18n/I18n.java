package fr.bordeaux.i18n;

import fr.bordeaux.core.CoreServiceProvider;
import fr.bordeaux.core.DomainTranslator;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Provides internationalization support for the application. Manages resource bundles, locale
 * settings, and translated UI strings.
 */
public final class I18n implements DomainTranslator {
  /** Base path of the translation bundles. */
  private static final String BUNDLE_NAME = "international/messages";

  /** Currently loaded translation bundle. */
  private static ResourceBundle bundle;

  /** Initializes the default locale used by the application. */
  static {
    initialize();
  }

  /** Force initialization and registration. */
  public static void initialize() {
    if (System.getProperty("quoridor.test") != null) {
      setLocale(Locale.ENGLISH);
    } else {
      setLocale(getLocaleFromEnv(System.getenv()));
    }
    // Register as the domain translator
    CoreServiceProvider.setTranslator(getInstance());
  }

  /** Singleton instance for domain service registration. */
  private static final I18n INSTANCE = new I18n();

  /**
   * Get the singleton instance.
   *
   * @return the singleton instance.
   */
  public static I18n getInstance() {
    return INSTANCE;
  }

  /**
   * Determine the locale from environment variables LC_ALL or LANG.
   *
   * @param env the environment variables map
   * @return the determined Locale
   */
  public static Locale getLocaleFromEnv(final java.util.Map<String, String> env) {
    final String lcAll = env.get("LC_ALL");
    final String lang = env.get("LANG");
    final String localeStr = (lcAll != null && !lcAll.isEmpty()) ? lcAll : lang;

    if (localeStr != null && !localeStr.isEmpty()) {
      // Handle formats like fr_FR.UTF-8 or fr
      final String[] parts = localeStr.split("\\.")[0].split("_");
      if (parts.length > 1) {
        return Locale.of(parts[0], parts[1]);
      } else if (parts.length == 1 && !parts[0].isEmpty()) {
        return Locale.of(parts[0]);
      }
    }
    return Locale.getDefault();
  }

  /**
   * Set the language that will be used
   *
   * @param locale the location to set the language
   */
  public static void setLocale(final Locale locale) {
    bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
  }

  @Override
  public String translate(final String key) {
    return get(key);
  }

  @Override
  public String translate(final String key, final Object... params) {
    return get(key, params);
  }

  /**
   * Get a simple message
   *
   * @param key the text to display from properties
   * @return text depending on the language choosen
   */
  public static String get(final String key) {
    return bundle.getString(key);
  }

  /**
   * Get a dynamic message.
   *
   * @param key the text to display from properties
   * @param params more text
   * @return text depending on the language choosen
   */
  public static String get(final String key, final Object... params) {
    final String pattern = bundle.getString(key);
    final String message;
    if (params.length > 0 && pattern.contains("{0}")) {
      message = java.text.MessageFormat.format(pattern, params);
    } else {
      final StringBuilder messageBuilder = new StringBuilder(pattern);
      for (final Object param : params) {
        messageBuilder.append(' ').append(param);
      }
      message = messageBuilder.toString();
    }
    return message;
  }

  /** Constructs a new I18n manager. */
  private I18n() {}
}
