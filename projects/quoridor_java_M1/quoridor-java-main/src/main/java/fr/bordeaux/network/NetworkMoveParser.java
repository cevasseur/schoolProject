package fr.bordeaux.network;

import fr.bordeaux.core.Move;
import fr.bordeaux.core.Orientation;
import fr.bordeaux.core.Position;

/** Utility class used to convert textual network moves into Move objects. */
public final class NetworkMoveParser {

  private NetworkMoveParser() {}

  /**
   * Parses a board coordinate such as e2 into a Position.
   *
   * @param str textual board coordinate
   * @return parsed position
   */
  public static Position parsePosition(final String str) {
    if (str == null || str.length() < 2) {
      throw new IllegalArgumentException("Invalid position: " + str);
    }

    final int row = Integer.parseInt(str.substring(1)) - 1;
    final int col = str.charAt(0) - 'a';

    return new Position(row, col);
  }

  /**
   * Parses a textual move received from the network.
   *
   * @param move move in textual format
   * @return corresponding Move object
   */
  public static Move parseMove(final String move) {
    if (move == null || move.isBlank()) {
      throw new IllegalArgumentException("Empty move");
    }

    final String trimmedMove = move.trim();

    if (trimmedMove.contains("-")) {
      final String[] parts = trimmedMove.split("-");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid pawn move: " + trimmedMove);
      }

      final Position from = parsePosition(parts[0]);
      final Position to = parsePosition(parts[1]);
      return Move.pawn(from, to);
    }

    if (trimmedMove.endsWith("h") || trimmedMove.endsWith("v")) {
      final Position pos = parsePosition(trimmedMove.substring(0, trimmedMove.length() - 1));
      final Orientation orientation =
          trimmedMove.charAt(trimmedMove.length() - 1) == 'v'
              ? Orientation.VERTICAL
              : Orientation.HORIZONTAL;
      return Move.wall(pos, orientation);
    }

    throw new IllegalArgumentException("Unknown move format: " + trimmedMove);
  }
}
