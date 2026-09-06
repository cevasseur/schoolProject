package fr.bordeaux.core;

/**
 * Interface representing logging capabilities required by the core domain. Decouples the domain
 * logic from the specific logging implementation.
 */
public interface DomainLogger {
  /**
   * Logs an informational message.
   *
   * @param message the message to log
   */
  void info(String message);

  /**
   * Logs an informational message with parameters.
   *
   * @param message the message pattern
   * @param params parameters for formatting
   */
  void info(String message, Object... params);

  /**
   * Logs an error message.
   *
   * @param message the message to log
   */
  void error(String message);

  /**
   * Logs an error message with an associated exception.
   *
   * @param message the message to log
   * @param throwable the exception causing the entry
   */
  void error(String message, Throwable throwable);

  /**
   * Logs an error message with parameters.
   *
   * @param message the message pattern
   * @param params parameters for formatting
   */
  void error(String message, Object... params);

  /**
   * Logs a debug message.
   *
   * @param message the message to log
   */
  void debug(String message);

  /**
   * Logs a debug message.
   *
   * @param message the message pattern
   * @param params parameters for formatting
   */
  void debug(String message, Object... params);

  /**
   * Logs a verbose message.
   *
   * @param message the message to log
   */
  void verbose(String message);

  /**
   * Logs a verbose message.
   *
   * @param message the message pattern
   * @param params parameters for formatting
   */
  void verbose(String message, Object... params);

  /**
   * Prints a message without a newline (useful for CLI prompts).
   *
   * @param message the message to print
   */
  void prompt(String message);

  /**
   * Prints a raw message followed by a newline (no prefixes, no colors).
   *
   * @param message the message to print
   */
  void raw(String message);

  /**
   * Returns whether info-level logging is enabled.
   *
   * @return true if info messages should be logged
   */
  default boolean isInfoEnabled() {
    return true;
  }

  /**
   * Returns whether debug-level logging is enabled.
   *
   * @return true if debug messages should be logged
   */
  default boolean isDebugEnabled() {
    return true;
  }

  /**
   * Returns whether error-level logging is enabled.
   *
   * @return true if error messages should be logged
   */
  default boolean isErrorEnabled() {
    return true;
  }
}
