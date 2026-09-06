package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Orientation;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import fr.bordeaux.core.RuleChecker;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;

/**
 * CLI command that interprets and executes a player move. pawn movement or a wall placement
 * depending on the format of the input string.
 */
public class MoveCommand implements Command {

  /** Move that has been entered by the user */
  private String move;

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /**
   * Creates a MoveCommand using the provided move string.
   *
   * @param moveInput the move entered by the user
   */
  public MoveCommand(final String moveInput) {
    move = moveInput;
  }

  /** Creates a MoveCommand with no predefined move. */
  public MoveCommand() {
    this(null);
  }

  /**
   * Executes move command. Parses pawn move or wall placement. placement and delegates the
   * processing to the appropriate handler.
   *
   * @param engine the current game engine
   * @return updated game engine or original if invalid. is invalid
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    try {
      if (move.contains("-") && (move.length() == 5 || move.length() == 7)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("{0} {1}", I18n.get("youPlayed"), this.move);
        }
        return handleMove(engine);
      } else if ((move.contains("v") || move.contains("h")) && move.length() == 3) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("{0} {1}", I18n.get("youPlayed"), this.move);
        }
        return handleWall(engine);
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(I18n.get("helpValidCommand"));
        }
        return engine;
      }
    } catch (final Exception e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("{0} {1}", I18n.get("readingMove"), this.move);
      }
      return engine;
    }
  }

  /** Displays help information for the move command. in the CLI. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("moveHeader"), I18n.get("cliTitle"));
      LOGGER.info(I18n.get("usage"));
      LOGGER.info(I18n.get("pawnMove", "e2-e3", I18n.get("ifLegal")));
      LOGGER.info("        {0}", I18n.get("illegalPawnMove"));
      LOGGER.info(I18n.get("wallMove", "e2h", I18n.get("ifLegal")));
      LOGGER.info(
          "                : e2h "
              + I18n.get("place", I18n.get("horizontally"), I18n.get("betweenRows"), "2", "3")
              + "\n");
      LOGGER.info(
          "                : "
              + I18n.get("cantPlace", I18n.get("horizontally"), I18n.get("rightEdge")));
      LOGGER.info(I18n.get("wallMove", "e2v", I18n.get("ifLegal")));
      LOGGER.info(
          "                : e2v "
              + I18n.get("place", I18n.get("vertically"), I18n.get("betweenColumns"), "e", "f")
              + "\n");
      LOGGER.info(
          "        {0}", I18n.get("cantPlace", I18n.get("vertically"), I18n.get("bottomEdge")));
    }
  }

  /**
   * Converts a position string to a Position object (e.g. "e2"). letter followed by a number (e.g.,
   * "e2").
   *
   * @param str the string representing the board position
   * @return the corresponding Position object
   * @throws IllegalArgumentException if the format is invalid
   */
  public Position parsePosition(final String str) {
    if (str.length() < 2) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("{0} {1}", I18n.get("usage"), I18n.get("moveUsage"));
      }
      throw new IllegalArgumentException(I18n.get("positionFormat"));
    }
    final int row = Integer.parseInt(str.substring(1)) - 1;
    final int col = str.charAt(0) - 'a';

    return new Position(row, col);
  }

  /**
   * Handles execution of a pawn move, validating format and positions. applies the move to the game
   * engine.
   *
   * @param engine the current game engine
   * @return the updated game engine after the move
   */
  public GameEngine handleMove(final GameEngine engine) {
    final String[] moves = move.split("-");
    if (moves.length != 2) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("{0} {1}", I18n.get("usage"), I18n.get("moveUsage"));
      }
      throw new IllegalArgumentException(I18n.get("moveSyntax"));
    }
    final Position from = parsePosition(moves[0]);
    final Position targetPosition = parsePosition(moves[1]);

    final Player current = engine.getState().getCurrentPlayer();
    if (!current.getPosition().equals(from)) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("noPawnPlayed"));
      }
      throw new IllegalArgumentException(I18n.get("noPawn", moves[0]));
    }
    if (!engine.play(Move.pawn(from, targetPosition))) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("illegalMoveMsg"), from, targetPosition);
      }
      return engine;
    }

    new ShowCommand().execute(engine);
    return engine;
  }

  /**
   * Handles wall placement by extracting position and orientation. string and applies the wall
   * placement to the game engine.
   *
   * @param engine the current game engine
   * @return the updated game engine after the wall is placed
   */
  public GameEngine handleWall(final GameEngine engine) {
    final Position pos = parsePosition(move.substring(0, move.length() - 1));
    final Orientation orientation =
        (move.charAt(move.length() - 1) == 'v') ? Orientation.VERTICAL : Orientation.HORIZONTAL;
    if (!RuleChecker.isMoveLegal(engine.getState(), Move.wall(pos, orientation))) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("{0} {1}", I18n.get("moveIllegal"), pos);
      }
      return engine;
    }
    engine.play(Move.wall(pos, orientation));
    new ShowCommand().execute(engine);
    return engine;
  }
}
