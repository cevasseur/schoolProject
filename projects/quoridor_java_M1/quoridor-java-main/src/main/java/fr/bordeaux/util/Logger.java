package fr.bordeaux.util;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.CoreServiceProvider;
import fr.bordeaux.core.DomainLogger;
import fr.bordeaux.i18n.I18n;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;

/**
 * Simple logger for the Quoridor application.
 *
 * <p>Supports console or file output (INFO, WARNING, DEBUG, VERBOSE, ERROR).
 *
 * <p>Thread-safe Singleton pattern.
 */
public final class Logger implements DomainLogger {
  /** ANSI escape code to reset text formatting to default. */
  private static final String RESET = "\u001B[0m";

  /** Coulor for INFO message */
  private static final String CYAN = "\u001B[36m";

  /** Coulor for DEBUG message */
  private static final String YELLOW = "\u001B[33m";

  /** Couleur WARNING */
  private static final String YELLOW_BOLD = "\u001B[33;1m";

  /** Couleur VERBOSE / TRACE */
  private static final String GREEN = "\u001B[32m";

  /** Color for ERROR message */
  private static final String RED = "\u001B[31m";

  /** Singleton instance of the logger. */
  private static Logger instance;

  /** File to log into (if any) */
  private static String logFile;

  /** Flux de sortie (file or System.out) */
  private final PrintStream writer;

  /** Boolean in case debug is enabled */
  private boolean debugEnabled;

  /** Boolean in case verbose is enabled */
  private boolean verboseEnabled;

  /** Boolean in case info is enabled */
  private boolean infoEnabled = true;

  /** Boolean in case warning is enabled */
  private boolean warnEnabled = true;

  /** Boolean in case error is enabled */
  private boolean errorEnabled = true;

  /**
   * Private constructor for console-based logging. Initializes settings from {@link ConfigManager}
   * and sets output to System.out.
   */
  private Logger() {
    this.writer = null;
  }

  /**
   * Private constructor (with file).
   *
   * @param logFile file to put log messages
   */
  private Logger(final String logFile) {
    PrintStream out;

    try {
      final File file = new File(System.getProperty("user.home"), logFile);
      out = new PrintStream(file);
    } catch (final IOException exception) {
      System.err
          .append(I18n.get("error", I18n.get("creatingLogFile")))
          .append(System.lineSeparator());
      out = null;
    }

    this.writer = out;
  }

  /**
   * Return the Logger Singleton instance.
   *
   * @return logger's unique instance
   */
  public static synchronized Logger getInstance() {
    if (instance == null) {
      if (logFile != null) {
        instance = new Logger(logFile);
      } else {
        instance = new Logger();
      }
      instance.refreshConfig();
      // Register as the domain logger
      CoreServiceProvider.setLogger(instance);
    }
    return instance;
  }

  /**
   * Set the log file for the singleton instance. Must be called before the first call to
   * getInstance().
   *
   * @param logFile file where to print the logs
   */
  public static synchronized void setLogFile(final String logFile) {
    Logger.logFile = logFile;
  }

  /** Resets the logger singleton instance. Useful for unit tests. */
  public static synchronized void resetInstance() {
    instance = null;
    logFile = null;
  }

  // =========================================================
  // PUBLIC API
  // =========================================================

  /**
   * INFO level message to display.
   *
   * @param message message to display
   */
  @Override
  public void info(final String message) {
    if (isInfoEnabled()) {
      log("[INFO]", message, CYAN);
    }
  }

  /**
   * Log un message de niveau INFO avec des paramètres.
   *
   * @param format message format (MessageFormat style, e.g., "{0}")
   * @param args les arguments du message
   */
  @Override
  public void info(final String format, final Object... args) {
    if (isInfoEnabled()) {
      log("[INFO]", MessageFormat.format(format, args), CYAN);
    }
  }

  /**
   * Log un message de niveau WARNING.
   *
   * @param message message à afficher
   */
  public void warning(final String message) {
    if (isWarnEnabled()) {
      log("[WARNING]", message, YELLOW_BOLD);
    }
  }

  /**
   * Log un message de niveau WARNING avec des paramètres.
   *
   * @param format le format du message
   * @param args les arguments du message
   */
  public void warning(final String format, final Object... args) {
    if (isWarnEnabled()) {
      log("[WARNING]", MessageFormat.format(format, args), YELLOW_BOLD);
    }
  }

  /**
   * Log un message de niveau DEBUG.
   *
   * @param message message to display
   */
  @Override
  public void debug(final String message) {
    if (isDebugEnabled()) {
      log("[DEBUG]", message, YELLOW);
    }
  }

  /**
   * Log un message de niveau DEBUG avec des paramètres.
   *
   * @param format le format du message
   * @param args les arguments du message
   */
  @Override
  public void debug(final String format, final Object... args) {
    if (isDebugEnabled()) {
      log("[DEBUG]", MessageFormat.format(format, args), YELLOW);
    }
  }

  /**
   * Log un message de niveau VERBOSE.
   *
   * @param message message to display
   */
  @Override
  public void verbose(final String message) {
    if (isVerboseEnabled()) {
      log("[VERBOSE]", message, GREEN);
    }
  }

  /**
   * Log un message de niveau VERBOSE avec des paramètres.
   *
   * @param format le format du message
   * @param args les arguments du message
   */
  @Override
  public void verbose(final String format, final Object... args) {
    if (isVerboseEnabled()) {
      log("[VERBOSE]", MessageFormat.format(format, args), GREEN);
    }
  }

  /**
   * Print a prompt without newline.
   *
   * @param message the message to display
   */
  @Override
  public void prompt(final String message) {
    final PrintStream stream = (writer != null) ? writer : System.out;
    if (stream != null) {
      stream.append(message);
      stream.flush();
    }
  }

  /**
   * Print a raw message with newline.
   *
   * @param message the message to display
   */
  @Override
  public void raw(final String message) {
    final PrintStream stream = (writer != null) ? writer : System.out;
    if (stream != null) {
      stream.append(message).append(System.lineSeparator());
    }
  }

  /**
   * Log un message d'erreur.
   *
   * @param message message to display
   */
  @Override
  public void error(final String message) {
    if (isErrorEnabled()) {
      log("[ERROR]", message, RED);
    }
  }

  /**
   * Log un message de niveau ERROR avec des paramètres.
   *
   * @param format le format du message
   * @param args les arguments du message
   */
  @Override
  public void error(final String format, final Object... args) {
    if (isErrorEnabled()) {
      log("[ERROR]", MessageFormat.format(format, args), RED);
    }
  }

  /**
   * Log une erreur avec exception associée.
   *
   * @param message message d'erreur
   * @param exception exception associée
   */
  @Override
  public void error(final String message, final Throwable exception) {
    if (isErrorEnabled()) {
      log(
          "[ERROR]",
          message + " | " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
          RED);
    }
  }

  // =========================================================
  // CONFIG
  // =========================================================

  /** Reload the configuration from the {@link ConfigManager}. */
  public void refreshConfig() {
    final ConfigManager config = ConfigManager.getInstance();

    this.debugEnabled = Boolean.parseBoolean(config.getOption("debug", "false"));
    this.verboseEnabled = Boolean.parseBoolean(config.getOption("verbose", "false"));
  }

  // =========================================================
  // INTERNAL
  // =========================================================

  /**
   * Intern method to print a message.
   *
   * @param level log level
   * @param message the content to display
   * @param color color to display the level
   */
  private void log(final String level, final String message, final String color) {
    PrintStream stream = (writer != null) ? writer : System.out;
    // Si on écrit sur la sortie standard (console), on bascule ERROR/WARNING sur stderr
    if (writer == null && (level.contains("ERROR") || level.contains("WARNING"))) {
      stream = System.err;
    }
    if (stream != null) {
      stream
          .append(color)
          .append(level)
          .append(RESET)
          .append(" ")
          .append(message)
          .append(System.lineSeparator());
    }
  }

  // =========================================================
  // LEVEL CHECKS
  // =========================================================

  /**
   * Check if DEBUG level is activated.
   *
   * @return true if activated, false otherwise
   */
  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  /**
   * Check if INFO level is activated.
   *
   * @return true if activated
   */
  public boolean isInfoEnabled() {
    return infoEnabled;
  }

  /**
   * Check if WARNING level is activated.
   *
   * @return true if activated
   */
  public boolean isWarnEnabled() {
    return warnEnabled;
  }

  /**
   * Check if ERROR level is activated.
   *
   * @return true if activated
   */
  public boolean isErrorEnabled() {
    return errorEnabled;
  }

  /**
   * Sets whether the INFO level is enabled.
   *
   * @param enabled true to enable, false to disable
   */
  public void setInfoEnabled(final boolean enabled) {
    this.infoEnabled = enabled;
  }

  /**
   * Sets whether the WARNING level is enabled.
   *
   * @param enabled true to enable, false to disable
   */
  public void setWarnEnabled(final boolean enabled) {
    this.warnEnabled = enabled;
  }

  /**
   * Sets whether the ERROR level is enabled.
   *
   * @param enabled true to enable, false to disable
   */
  public void setErrorEnabled(final boolean enabled) {
    this.errorEnabled = enabled;
  }

  /**
   * Check if VERBOSE level is activated.
   *
   * @return true if activated, false otherwise
   */
  public boolean isVerboseEnabled() {
    return verboseEnabled;
  }

  /**
   * Sets whether the DEBUG level is enabled.
   *
   * @param enabled true to enable, false to disable
   */
  public void setDebugEnabled(final boolean enabled) {
    this.debugEnabled = enabled;
  }

  /**
   * Sets whether the VERBOSE level is enabled.
   *
   * @param enabled true to enable, false to disable
   */
  public void setVerboseEnabled(final boolean enabled) {
    this.verboseEnabled = enabled;
  }
}
