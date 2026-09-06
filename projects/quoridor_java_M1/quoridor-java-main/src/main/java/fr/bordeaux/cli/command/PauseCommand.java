package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;

/** CLI command that pauses the Blitz mode game timer. */
public class PauseCommand implements Command {

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /** Creates a PauseCommand. */
  public PauseCommand() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(I18n.get("pauseInstantiating"));
    }
  }

  /**
   * Executes the pause command. Disables Blitz mode if it is currently active.
   *
   * @param engine the current game engine
   * @return the unchanged game engine
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    if (engine.isBlitz()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(I18n.get("pausing"));
      }
      engine.pause();
    } else {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("notBlitz"));
      }
    }
    return engine;
  }

  /** Displays help information for the pause command. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("pauseHeader"), I18n.get("cliTitle"));
      LOGGER.info(I18n.get("usage"));
      LOGGER.info("  {0}\n", I18n.get("pauseUsage"));
      LOGGER.info(I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("pauseDesc"));
    }
    LOGGER.verbose("  {0}", I18n.get("pauseVerb"));
  }
}
