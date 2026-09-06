package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;

/** CLI command that replays previously undone moves. */
public class RedoCommand implements Command {
  /** Command tokens. */
  private String[] tokens;

  /** Logger to trace redo actions. */
  private static final Logger LOGGER = Logger.getInstance();

  /** Creates a RedoCommand that replays one move by default. */
  public RedoCommand() {
    this(new String[] {"redo", "1"});
  }

  /**
   * Creates a RedoCommand using the provided CLI tokens.
   *
   * @param tokens the tokens entered by the user
   */
  public RedoCommand(final String[] tokens) {
    this.tokens = tokens;
  }

  /**
   * Executes redo command. Replays the specified number of moves.
   *
   * @param engine the current game engine
   * @return the updated game engine after redoing moves
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    if (tokens.length >= 3) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0}: redo [N]", I18n.get("usage"));
      }
      return engine;
    }
    final int count = (tokens.length > 1) ? Integer.parseInt(tokens[1]) : 1;
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}{1}{2}", I18n.get("redoing"), count, I18n.get("redoMove"));
    }

    for (int i = 0; i < count; i++) {
      engine.getState().redo();
    }
    return engine;
  }

  /** Displays help information for the redo command. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("redoHeader"), I18n.get("cliTitle"));
      LOGGER.info("{0}:", I18n.get("usage"));
      LOGGER.info("  {0}", I18n.get("redoUsageOne"));
      LOGGER.info("  {0}\n", I18n.get("redoUsageNb"));
      LOGGER.info("{0}:", I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("redoDesc"));
    }
    LOGGER.verbose("  {0}", I18n.get("redoOne"));
    LOGGER.verbose("  {0}", I18n.get("redoNb"));
    LOGGER.verbose(I18n.get("loadEx"));
    LOGGER.verbose("  redo      -> {0}", I18n.get("redoExOne"));
    LOGGER.verbose("  redo 3    -> {0}", I18n.get("redoExNb"));
  }
}
