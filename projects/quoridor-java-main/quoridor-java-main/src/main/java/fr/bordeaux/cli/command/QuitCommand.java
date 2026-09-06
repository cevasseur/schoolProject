package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.io.IOException;
import org.jline.reader.LineReader;

/** CLI command that exits the application after a save prompt. */
public class QuitCommand implements Command {
  /** Reader used for user interaction. */
  private final LineReader reader;

  /** Logger to display closing information. */
  private static final Logger LOGGER = Logger.getInstance();

  /**
   * Creates a QuitCommand using the provided line reader to interact with the user.
   *
   * @param reader the LineReader used to read user input
   */
  public QuitCommand(final LineReader reader) {
    this.reader = reader;
  }

  /** Creates a QuitCommand without a predefined reader. */
  public QuitCommand() {
    this(null);
  }

  /**
   * Executes quit. Asks to save before exiting.
   *
   * @param engine the current game engine
   * @return the current game engine
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    try {
      final String response = reader.readLine(I18n.get("quitSaving", " [y/N] >> "));
      if (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes")) {
        final String path = reader.readLine(I18n.get("filePath", " >> "));
        new SaveCommand(path).execute(engine);
      }
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(I18n.get("quitting"));
      }
      if (reader != null && reader.getTerminal() != null) {
        reader.getTerminal().close();
      }
    } catch (IOException e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("quitError"), e);
      }
    }
    return engine;
  }

  /** Displays help information for the quit command. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("quitHeader"), I18n.get("cliTitle"));
      LOGGER.info(I18n.get("usage"));
      LOGGER.info("  {0}\n", I18n.get("quitUsage"));
      LOGGER.info(I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("quitDesc"));
    }
    LOGGER.verbose("  {0}", I18n.get("quitVerb"));
  }
}
