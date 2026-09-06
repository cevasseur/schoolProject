package fr.bordeaux.core;

/**
 * Registry for core domain services like translation and logging. This class follows the Service
 * Provider pattern to avoid direct dependencies from core to infrastructure.
 */
public final class CoreServiceProvider {
  private static DomainTranslator translator;
  private static DomainLogger logger;

  /** Default translator that returns keys as-is if no provider is registered. */
  private static final DomainTranslator DEFAULT_TRANSLATOR =
      new DomainTranslator() {
        @Override
        public String translate(String key) {
          return key;
        }

        @Override
        public String translate(String key, Object... params) {
          return key;
        }
      };

  /** Default logger that does nothing if no provider is registered. */
  private static final DomainLogger DEFAULT_LOGGER =
      new DomainLogger() {
        @Override
        public void info(String m) {}

        @Override
        public void info(String m, Object... p) {}

        @Override
        public void error(String m) {}

        @Override
        public void error(String m, Throwable t) {}

        @Override
        public void error(String m, Object... p) {}

        @Override
        public void debug(String m) {}

        @Override
        public void debug(String m, Object... p) {}

        @Override
        public void verbose(String m) {}

        @Override
        public void verbose(String m, Object... p) {}

        @Override
        public void prompt(String m) {}

        @Override
        public void raw(String m) {}
      };

  private CoreServiceProvider() {}

  /**
   * Sets the translator implementation.
   *
   * @param t the implementation to use
   */
  public static void setTranslator(final DomainTranslator t) {
    translator = t;
  }

  /**
   * Gets the current translator.
   *
   * @return the registered translator or a default one
   */
  public static DomainTranslator getTranslator() {
    return (translator != null) ? translator : DEFAULT_TRANSLATOR;
  }

  /**
   * Sets the logger implementation.
   *
   * @param l the implementation to use
   */
  public static void setLogger(final DomainLogger l) {
    logger = l;
  }

  /**
   * Gets the current logger.
   *
   * @return the registered logger or a default one
   */
  public static DomainLogger getLogger() {
    return (logger != null) ? logger : DEFAULT_LOGGER;
  }
}
