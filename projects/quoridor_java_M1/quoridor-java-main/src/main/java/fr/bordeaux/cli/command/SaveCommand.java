package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.persistence.GameSaver;
import fr.bordeaux.util.Logger;

/** Command that saves the current game state to a file. */
public class SaveCommand implements Command {

  /** Command entered by the user */
  private String[] tokens;

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /**
   * Creates a SaveCommand using the specified file path.
   *
   * @param path the path of the file where the game will be saved
   */
  public SaveCommand(final String path) {
    this(new String[] {"save", path});
  }

  /**
   * Creates a SaveCommand using the provided CLI tokens.
   *
   * @param tokens the tokens entered by the user
   */
  public SaveCommand(final String[] tokens) {
    this.tokens = tokens;
  }

  /**
   * Executes save command. Writes the game state to the specified file.
   *
   * @param engine the current game engine
   * @return the unchanged game engine
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    if (tokens.length < 2) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0} {1}", I18n.get("usage"), I18n.get("saveUsage"));
      }
    } else {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0} {1}", I18n.get("attempt"), tokens[1]);
      }
      try {
        GameSaver.saveGame(engine, tokens[1]);
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(I18n.get("success"));
        }
      } catch (final Exception e) {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("{0} {1}", I18n.get("failed"), tokens[1]);
        }
      }
    }
    return engine;
  }

  /** Displays help information for the save command. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("saveHeader"), I18n.get("cliTitle"));
      LOGGER.info(I18n.get("usage"));
      LOGGER.info("  {0}\n", I18n.get("saveUsage"));
      LOGGER.info(I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("saveDesc"));
    }
    LOGGER.verbose(I18n.get("arguments"));
    LOGGER.verbose("  FILE   : {0}", I18n.get("saveFile"));
  }
}
