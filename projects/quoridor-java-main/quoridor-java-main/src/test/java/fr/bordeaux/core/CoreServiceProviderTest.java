package fr.bordeaux.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests for CoreServiceProvider, specifically the default implementations of DomainLogger and
 * DomainTranslator to ensure full branch coverage.
 */
class CoreServiceProviderTest {

  @Test
  void testDefaultImplementations() {
    // 1. Save current state
    DomainLogger originalLogger = CoreServiceProvider.getLogger();
    DomainTranslator originalTranslator = CoreServiceProvider.getTranslator();

    try {
      // 2. Clear state to force use of DEFAULT_LOGGER and DEFAULT_TRANSLATOR
      CoreServiceProvider.setLogger(null);
      CoreServiceProvider.setTranslator(null);

      DomainLogger defaultLogger = CoreServiceProvider.getLogger();
      DomainTranslator defaultTranslator = CoreServiceProvider.getTranslator();

      assertNotNull(defaultLogger);
      assertNotNull(defaultTranslator);

      // 3. Exercise all methods of DEFAULT_LOGGER
      defaultLogger.info("test");
      defaultLogger.info("test {0}", "param");
      defaultLogger.error("test");
      defaultLogger.error("test", new RuntimeException());
      defaultLogger.error("test {0}", "param");
      defaultLogger.debug("test");
      defaultLogger.debug("test {0}", "param");
      defaultLogger.verbose("test");
      defaultLogger.verbose("test {0}", "param");
      defaultLogger.prompt("test");
      defaultLogger.raw("test");

      // 4. Exercise all methods of DEFAULT_TRANSLATOR
      assertSame("key", defaultTranslator.translate("key"));
      assertSame("key", defaultTranslator.translate("key", "p1"));

    } finally {
      // 5. Restore state
      CoreServiceProvider.setLogger(originalLogger);
      CoreServiceProvider.setTranslator(originalTranslator);
    }
  }
}
