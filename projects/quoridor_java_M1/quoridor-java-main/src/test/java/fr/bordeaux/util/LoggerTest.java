package fr.bordeaux.util;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.i18n.I18n;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for Logger class. */
public class LoggerTest {

  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
  private final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
  private static final String LOG_FILE_NAME = "test_quoridor_unit.log";

  @BeforeEach
  void setUp() throws Exception {
    resetSingleton();
    capturedOut.reset();
    capturedErr.reset();
    System.setOut(new PrintStream(capturedOut, true));
    System.setErr(new PrintStream(capturedErr, true));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
    // Cleanup
    File logFile = new File(System.getProperty("user.home"), LOG_FILE_NAME);
    if (logFile.exists()) {
      logFile.delete();
    }
  }

  /**
   * Resets the Singleton instance using reflection to allow testing different constructor paths.
   */
  private void resetSingleton() throws Exception {
    Logger.resetInstance();
  }

  @Test
  void testGetInstance_NewInstanceCreation() {
    Logger.setLogFile(LOG_FILE_NAME);
    Logger logger = Logger.getInstance();
    assertNotNull(logger, "Logger should be successfully created");

    File logFile = new File(System.getProperty("user.home"), LOG_FILE_NAME);
    assertTrue(logFile.exists(), "Log file should be physically created on disk");
  }

  @Test
  void testInfo_Stdout() {
    Logger logger = Logger.getInstance();
    logger.info("Test Info");
    String output = capturedOut.toString();
    assertTrue(output.contains("[INFO]"), "Output was: " + output);
    assertTrue(output.contains("Test Info"), "Output was: " + output);
    assertFalse(capturedErr.toString().contains("[INFO]"));
  }

  @Test
  void testWarn_Stderr() {
    Logger logger = Logger.getInstance();
    logger.warning("Test Warning");
    // Logger with System.out writer should redirect WARNING to System.err
    String errorOutput = capturedErr.toString();
    assertTrue(errorOutput.contains("[WARNING]"), "Error Output was: " + errorOutput);
    assertTrue(errorOutput.contains("Test Warning"), "Error Output was: " + errorOutput);
    assertFalse(capturedOut.toString().contains("[WARNING]"));
  }

  @Test
  void testError_Stderr() {
    Logger logger = Logger.getInstance();
    logger.error("Test Error");
    String errorOutput = capturedErr.toString();
    assertTrue(errorOutput.contains("[ERROR]"), "Error Output was: " + errorOutput);
    assertTrue(errorOutput.contains("Test Error"), "Error Output was: " + errorOutput);
    assertFalse(capturedOut.toString().contains("[ERROR]"));
  }

  @Test
  void testError_WithThrowable() {
    Logger logger = Logger.getInstance();
    Exception e = new RuntimeException("Something went wrong");
    logger.error("Error occurred", e);
    String errorOutput = capturedErr.toString();
    assertTrue(errorOutput.contains("[ERROR]"), "Error Output was: " + errorOutput);
    assertTrue(errorOutput.contains("Error occurred"), "Error Output was: " + errorOutput);
    assertTrue(
        errorOutput.contains("RuntimeException: Something went wrong"),
        "Error Output was: " + errorOutput);
  }

  @Test
  void testPrivateConstructor_IOExceptionFallback() {
    String invalidFileName = ".";
    Logger.setLogFile(invalidFileName);
    Logger logger = Logger.getInstance();

    assertNotNull(logger, "Logger should fall back to a valid instance even on IO failure");
    String errOutput = capturedErr.toString();
    assertTrue(
        errOutput.contains(I18n.get("creatingLogFile")),
        "Fallback message should be printed to stderr. Output was: " + errOutput);

    // Verify it still works at INFO level (fallback console logic)
    logger.info("Test message after fallback");
    assertTrue(capturedOut.toString().contains("Test message after fallback"));
  }

  @Test
  void testParameterizedLogging() throws Exception {
    Logger logger = Logger.getInstance();

    fr.bordeaux.config.ConfigManager config = fr.bordeaux.config.ConfigManager.getInstance();
    config.setOption("debug", "true");
    config.setOption("verbose", "true");
    logger.refreshConfig();

    logger.info("Info {0}", "param");
    logger.warning("Warn {0}", "param");
    logger.error("Error {0}", "param");
    logger.debug("Debug {0}", "param");
    logger.verbose("Verbose string");
    logger.verbose("Verbose {0}", "param");

    String out = capturedOut.toString();
    String err = capturedErr.toString();

    assertTrue(out.contains("[INFO]"));
    assertTrue(out.contains("Info param"));
    assertTrue(err.contains("[WARNING]"));
    assertTrue(err.contains("Warn param"));
    assertTrue(err.contains("[ERROR]"));
    assertTrue(err.contains("Error param"));
    assertTrue(out.contains("[DEBUG]"));
    assertTrue(out.contains("Debug param"));
    assertTrue(out.contains("[VERBOSE]"));
    assertTrue(out.contains("Verbose string"));
    assertTrue(out.contains("Verbose param"));
  }
}
