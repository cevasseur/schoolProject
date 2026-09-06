package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.GameState;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.persistence.GameLoader;
import fr.bordeaux.util.Logger;

/** CLI command that loads a saved game from a file. state. */
public class LoadCommand implements Command {

  /** Command entered by the user */
  private String[] tokens;

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /**
   * Creates a LoadCommand with the provided command tokens.
   *
   * @param tokens the tokens entered by the user in the CLI
   */
  public LoadCommand(final String[] tokens) {
    this.tokens = tokens;
  }

  /**
   * Executes load command and loads game from file. If loading succeeds, a new GameEngine is
   * created with the loaded state.
   *
   * @param engine the current game engine
   * @return Loaded GameEngine or the current one if failed. fails
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    if (tokens == null || tokens.length != 2) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0}: {1}", I18n.get("usage"), I18n.get("loadUsage"));
      }
    } else {
      try {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("{0} {1}", I18n.get("loading"), tokens[1]);
        }
        if (engine.getBlitzManager() != null) {
          engine.getBlitzManager().pause();
        }
        final GameState loadedState = GameLoader.loadGame(engine, tokens[1]);
        engine.getState().loadState(loadedState);
        engine.initializeBlitz();
      } catch (final Exception e) {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("{0} {1}: {2}", I18n.get("loadError"), tokens[1], e.getMessage());
        }
      }
    }
    return engine;
  }

  /** Displays help information for the load command. example. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("loadHeader"), I18n.get("cliTitle"));
      LOGGER.info(I18n.get("usage"));
      LOGGER.info("  {0}\n", I18n.get("loadUsage"));
      LOGGER.info(I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("loadDesc"));
    }
    LOGGER.verbose(I18n.get("arguments"));
    LOGGER.verbose("  {0}", I18n.get("loadArg"));
    LOGGER.verbose(I18n.get("loadEx"));
    LOGGER.verbose("  {0}\n", I18n.get("loadExampleCmd"));
  }
}
