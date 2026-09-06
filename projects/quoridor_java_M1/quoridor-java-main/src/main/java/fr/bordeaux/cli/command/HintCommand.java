package fr.bordeaux.cli.command;

import fr.bordeaux.ai.mcts.MctsPlayer;
import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Orientation;
import fr.bordeaux.core.Position;
import fr.bordeaux.core.RuleChecker;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.io.IOException;

/**
 * Suggests the best move computed by the AI based on the current state. Minimax AI based on the
 * current game state.
 */
public class HintCommand implements Command {

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /** Creates a HintCommand. */
  public HintCommand() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(I18n.get("hintInstantiating"));
    }
  }

  /**
   * Executes the hint command, suggesting the best move. state and suggests the best move for the
   * current player.
   *
   * @param engine the current game engine
   * @return the unchanged game engine
   */
  @Override
  public GameEngine execute(final GameEngine engine) throws IOException {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(I18n.get("hintCalc"));
    }
    final GameState state = engine.getState();
    final Move hint = MctsPlayer.hint(state.getCurrentPlayer(), state, 3000, true);

    if (!RuleChecker.isMoveLegal(state, hint)) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("suggestedLegal"), hint);
      }
    }
    final String bestMove = formatMove(hint);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}{1}", I18n.get("hintPlay"), bestMove);
    }
    return engine;
  }

  /**
   * Formats a Move object into a readable string (e.g., e2-e3 or a1h).
   *
   * @param move the move to format
   * @return the string representation of the move
   */
  private String formatMove(final Move move) {
    if (move.getType() == Move.Type.PAWN) {
      return posToString(move.getFrom()) + "-" + posToString(move.getTo());
    }
    final char orientationChar;
    if (move.getOrientation() == Orientation.HORIZONTAL) {
      orientationChar = 'h';
    } else {
      orientationChar = 'v';
    }
    return posToString(move.getTo()) + orientationChar;
  }

  /**
   * Converts board position to a human-readable CLI coordinate string. notation (e.g., a0, b3).
   *
   * @param position the position to convert
   * @return the string representation of the position
   */
  private String posToString(final Position position) {
    return (char) ('a' + position.getY()) + "" + (position.getX() + 1);
  }

  /** Displays help information for the hint command. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("hintHeader"), I18n.get("cliTitle"));
      LOGGER.info(I18n.get("usage"));
      LOGGER.info("  {0}\n", I18n.get("hintUsage"));
      LOGGER.info(I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("hintHelp1"));
      LOGGER.info("  {0}", I18n.get("hintHelp3"));
    }
    LOGGER.verbose(I18n.get("arguments"));
    LOGGER.verbose("  {0}", I18n.get("hintHelp2"));
    LOGGER.verbose(I18n.get("hintHelpEx"));
  }
}
