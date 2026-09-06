package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;

/** CLI command that cancels previously played moves. */
public class UndoCommand implements Command {
  /** Command tokens. */
  private String[] tokens;

  /** Logger to trace undo actions. */
  private static final Logger LOGGER = Logger.getInstance();

  /** Creates an UndoCommand that undoes one move by default. */
  public UndoCommand() {
    this(new String[] {"undo", "1"});
  }

  /**
   * Creates an UndoCommand using the provided CLI tokens.
   *
   * @param commandTokens the tokens entered by the user
   */
  public UndoCommand(final String[] commandTokens) {
    tokens = commandTokens;
  }

  /**
   * Executes undo command. Cancels the specified number of moves.
   *
   * @param engine the current game engine
   * @return the updated game engine after undoing moves
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    if (tokens.length >= 3) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0}: undo [N]", I18n.get("usage"));
      }
      return engine;
    }
    final int count = (tokens.length > 1) ? Integer.parseInt(tokens[1]) : 1;
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}{1}{2}", I18n.get("undoing"), count, I18n.get("redoMove"));
    }

    for (int i = 0; i < count; i++) {
      engine.getState().undo();
    }
    return engine;
  }

  /** Displays help information for the undo command. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("undoHeader"), I18n.get("cliTitle"));
      LOGGER.info("{0}:", I18n.get("usage"));
      LOGGER.info("  {0}", I18n.get("undoUsageOne"));
      LOGGER.info("  {0}\n", I18n.get("undoUsageNb"));
      LOGGER.info("{0}:", I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("undoDesc"));
    }
    LOGGER.verbose("  {0}", I18n.get("undoOne"));
    LOGGER.verbose("  {0}", I18n.get("undoNb"));
    LOGGER.verbose(I18n.get("loadEx"));
    LOGGER.verbose("  undo      -> {0}", I18n.get("undoExOne"));
    LOGGER.verbose("  undo 2    -> {0}", I18n.get("undoExNb"));
  }
}
