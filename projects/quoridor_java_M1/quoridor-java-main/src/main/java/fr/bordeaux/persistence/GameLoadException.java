package fr.bordeaux.persistence;

/** Exception thrown when an error occurs while loading a game. */
public final class GameLoadException extends Exception {

  /** Stores the type of error, for example: "ERREUR_ENTETE". */
  private final String errorType;

  /** Stores the line number where the error was found. */
  private final int lineNumber;

  /**
   * Creates a load exception.
   *
   * @param errorTypeValue error type string
   * @param lineNumberValue line number of error
   * @param message error message
   */
  public GameLoadException(
      final String errorTypeValue, final int lineNumberValue, final String message) {
    super(message);
    this.errorType = errorTypeValue;
    this.lineNumber = lineNumberValue;
  }

  /**
   * Returns the error type.
   *
   * @return the error type
   */
  public String getErrorType() {
    return errorType;
  }

  /**
   * Returns the line number where the error was found.
   *
   * @return the line number
   */
  public int getLineNumber() {
    return lineNumber;
  }
}
