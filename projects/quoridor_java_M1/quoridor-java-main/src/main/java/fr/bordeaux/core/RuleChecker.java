package fr.bordeaux.core;

import java.util.ArrayList;
import java.util.List;

/**
 * RuleChecker checks if moves (pawn moves or wall placements) are valid according to Quoridor
 * rules.
 */
public final class RuleChecker {

  /** Private constructor to prevent instantiation. */
  private RuleChecker() {
    throw new AssertionError(CoreServiceProvider.getTranslator().translate("assertChecker"));
  }

  /**
   * Checks if a given move is legal in the current game state.
   *
   * @param state The current {@link GameState}.
   * @param move The {@link Move} to validate.
   * @return {@code true} if the move is legal, {@code false} otherwise.
   */
  public static boolean isMoveLegal(final GameState state, final Move move) {
    final boolean legal;
    if (move.isPawn()) {
      legal = pawnMoveLegal(state, move);
    } else {
      legal = wallMoveLegal(state, move);
    }
    return legal;
  }

  /**
   * Checks if a jumping move is possible over an opponent.
   *
   * @param state The current {@link GameState}.
   * @param move The {@link Move} containing the target jump position.
   * @return {@code true} if the jump is valid, {@code false} otherwise.
   */
  public static boolean canJump(final GameState state, final Move move) {
    final Player currentPlay = state.getCurrentPlayer();
    final Board board = state.getBoard();
    final Position from = currentPlay.getPosition();
    final Position targetPos = move.getTo();

    final int fromId = board.getCellId(from.getX(), from.getY());
    boolean result = false;

    for (final Player otherPlayer : state.getPlayers()) {
      if (otherPlayer == currentPlay || otherPlayer.isDisabled()) {
        continue;
      }

      final Position posOtherPlayer = otherPlayer.getPosition();
      if (!isAdjacent(from, posOtherPlayer)) {
        continue;
      }

      if (checkStraightJump(board, from, posOtherPlayer, targetPos, fromId)
          || checkDiagonalJump(board, from, posOtherPlayer, targetPos, fromId)) {
        result = true;
        break;
      }
    }
    return result;
  }

  /** Checks if a straight jump over an opponent is possible. */
  private static boolean checkStraightJump(
      final Board board,
      final Position from,
      final Position opponentPlayer,
      final Position target,
      final int fromId) {
    final int oppX = opponentPlayer.getX();
    final int oppY = opponentPlayer.getY();
    final int deltaX = oppX - from.getX();
    final int deltaY = oppY - from.getY();
    final Position jump = new Position(oppX + deltaX, oppY + deltaY);

    boolean possible = false;
    if (board.isInside(jump)) {
      final int opId = board.getCellId(oppX, oppY);
      final int jumpId = board.getCellId(jump.getX(), jump.getY());
      if (board.getGraph().hasEdge(opId, jumpId) && board.getGraph().hasEdge(fromId, opId)) {
        possible = target.equals(jump);
      }
    }
    return possible;
  }

  /** Checks if a diagonal jump is possible if straight is blocked. */
  private static boolean checkDiagonalJump(
      final Board board,
      final Position from,
      final Position opponentPlayer,
      final Position target,
      final int fromId) {
    final int oppX = opponentPlayer.getX();
    final int oppY = opponentPlayer.getY();
    final int deltaX = oppX - from.getX();
    final int deltaY = oppY - from.getY();
    final Position jump = new Position(oppX + deltaX, oppY + deltaY);

    boolean possible = false;
    final int opId = board.getCellId(oppX, oppY);
    final boolean straightBlocked =
        !board.isInside(jump)
            || !board.getGraph().hasEdge(opId, board.getCellId(jump.getX(), jump.getY()));

    if (straightBlocked) {
      final List<Position> diagonalCandidates = getDiagonalJumps(from, opponentPlayer);
      for (final Position diag : diagonalCandidates) {
        if (board.isInside(diag)) {
          final int diagId = board.getCellId(diag.getX(), diag.getY());
          final boolean canSideStep =
              board.getGraph().hasEdge(fromId, opId) && board.getGraph().hasEdge(opId, diagId);

          if (canSideStep && target.equals(diag)) {
            possible = true;
            break;
          }
        }
      }
    }
    return possible;
  }

  /**
   * Validates a pawn movement (boundaries, collisions, graph).
   *
   * @param state The current game state.
   * @param move The pawn move to check.
   * @return {@code true} if the pawn can move to the target position.
   */
  private static boolean pawnMoveLegal(final GameState state, final Move move) {
    final Player currentPlay = state.getCurrentPlayer();
    final Board board = state.getBoard();
    final Position from = currentPlay.getPosition();
    final Position targetPos = move.getTo();

    boolean legal = false;
    if (board.isInside(targetPos)) {
      final int fromId = board.getCellId(from.getX(), from.getY());
      final int toId = board.getCellId(targetPos.getX(), targetPos.getY());

      boolean occupied = false;
      final List<Player> players = state.getPlayers();
      for (final Player player : players) {
        if (player.isDisabled()) {
          continue;
        }
        final Position playerPos = player.getPosition();
        if (playerPos.equals(targetPos)) {
          occupied = true;
          break;
        }
      }

      if (!occupied) {
        if (board.getGraph().hasEdge(fromId, toId)) {
          legal = true;
        } else {
          legal = canJump(state, move);
        }
      }
    }

    return legal;
  }

  /**
   * Determines if two positions are horizontally or vertically adjacent.
   *
   * @param a The first position.
   * @param b The second position.
   * @return {@code true} if the positions are neighbors.
   */
  private static boolean isAdjacent(final Position pos1, final Position pos2) {
    final int deltaX = Math.abs(pos1.getX() - pos2.getX());
    final int deltaY = Math.abs(pos1.getY() - pos2.getY());
    return deltaX + deltaY == 1;
  }

  /**
   * Computes diagonal jump positions when a straight jump is blocked.
   *
   * @param from The current player's position.
   * @param op The opponent's position.
   * @return A list of candidate {@link Position}s for a diagonal jump.
   */
  private static List<Position> getDiagonalJumps(
      final Position from, final Position opponentPlayer) {
    final List<Position> diags = new ArrayList<>();
    final int oppX = opponentPlayer.getX();
    final int oppY = opponentPlayer.getY();
    final int deltaX = oppX - from.getX();
    final int deltaY = oppY - from.getY();

    if (deltaX == 1 || deltaX == -1) {
      diags.add(new Position(oppX, oppY + 1));
      diags.add(new Position(oppX, oppY - 1));
    } else if (deltaY == 1 || deltaY == -1) {
      diags.add(new Position(oppX + 1, oppY));
      diags.add(new Position(oppX - 1, oppY));
    }

    return diags;
  }

  /**
   * Validates a wall placement (remaining, collisions, path).
   *
   * @param state The current game state.
   * @param move The wall placement move.
   * @return {@code true} if the wall placement is legal.
   */
  private static boolean wallMoveLegal(final GameState state, final Move move) {
    final Player player = state.getCurrentPlayer();
    final Board board = state.getBoard();

    boolean legal = false;
    if (player.getRemainingWalls() > 0) {
      if (!board.isWallCollision(move.getTo(), move.getOrientation())) {
        legal = doesPathExist(state, move);
      }
    }

    return legal;
  }

  /**
   * Verifies if all players can still reach their goals via BFS.
   *
   * @param state The current game state.
   * @param move The wall move to test.
   * @return {@code true} if a path still exists for every player.
   */
  private static boolean doesPathExist(final GameState state, final Move move) {
    final Board board = state.getBoard();
    final Position wallPos = move.getTo();
    final Orientation wallOrient = move.getOrientation();
    board.addWall(wallPos, wallOrient);

    boolean canWin = true;
    final List<Player> players = state.getPlayers();
    for (final Player player : players) {
      if (player.isDisabled()) {
        continue;
      }
      final Position pos = player.getPosition();
      final int startId = board.getCellId(pos.getX(), pos.getY());
      if (board.getGraph().findPathBFS(startId, state.getWinCondition(player)).isEmpty()) {
        canWin = false;
        break;
      }
    }
    board.removeWall(wallPos, wallOrient);
    return canWin;
  }
}
