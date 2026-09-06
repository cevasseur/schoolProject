package fr.bordeaux.persistence;

import fr.bordeaux.core.*;
import java.util.*;

/**
 * Utility class responsible for parsing the game's move history and converting algebraic board
 * coordinates into {@link Position} objects.
 */
public final class HistoryLoader {

  /** Private constructor of HistoryLoader */
  private HistoryLoader() {}

  /**
   * Parses the history section of the save file and returns a list of moves.
   *
   * @param lines The list of strings representing the lines of the save file.
   * @return A {@link List} of {@link Move} objects reconstructed from the history section.
   */
  public static List<Move> parseHistory(final List<String> lines) {
    final List<Move> history = new ArrayList<>();
    readHistory(lines, history);
    return history;
  }

  /**
   * Converts a string description of a move into a {@link Move} object.
   *
   * <p>Supports both pawn moves (e.g., "e2-e3") and wall placements (e.g., "e2h").
   *
   * @param desc The string representation of the move.
   * @return A {@link Move} object corresponding to the description.
   */
  public static Move parseMove(final String desc) {
    final Move result;
    if (desc.contains("-")) {
      final String[] coords = desc.split("-");
      result = Move.pawn(coordToPos(coords[0]), coordToPos(coords[1]));
    } else {
      final String posStr = desc.substring(0, desc.length() - 1);
      final char orientChar = desc.charAt(desc.length() - 1);
      final Orientation orient =
          (orientChar == 'h') ? Orientation.HORIZONTAL : Orientation.VERTICAL;
      result = Move.wall(coordToPos(posStr), orient);
    }
    return result;
  }

  /**
   * Converts an algebraic coordinate string (e.g., "a1", "e5") into a {@link Position}.
   *
   * @param coord The coordinate string where the first character is a letter (column) and the
   *     following characters are digits (row).
   * @return A {@link Position} object with zero-based integer coordinates.
   */
  private static Position coordToPos(final String coord) {
    final int posY = coord.charAt(0) - 'a';
    final int posX = Integer.parseInt(coord.substring(1)) - 1;
    return new Position(posX, posY);
  }

  /**
   * Scans the lines to locate the "[History]" section and extracts move data.
   *
   * @param lines The lines of the file.
   * @param history The list to populate with parsed moves.
   */
  private static void readHistory(final List<String> lines, final List<Move> history) {
    boolean inHistory = false;
    for (final String line : lines) {
      final String trimmed = line.trim();
      if ("[History]".equals(trimmed)) {
        inHistory = true;
        continue;
      }
      if (!inHistory) {
        continue;
      }
      if (trimmed.startsWith("[")) {
        break;
      }
      if (!trimmed.isEmpty()) {
        parseHistoryLine(trimmed, history);
      }
    }
  }

  /**
   * Parses a single line of history containing one or more moves separated by semicolons.
   *
   * @param line The raw string line from the history section.
   * @param history The list to which successfully parsed moves are added.
   */
  private static void parseHistoryLine(final String line, final List<Move> history) {
    final String[] parts = line.split(";");
    for (final String part : parts) {
      final String moveStr = part.trim();
      if (!moveStr.isEmpty()) {
        final String[] tokens = moveStr.split(" ");
        if (tokens.length >= 2) {
          history.add(parseMove(tokens[1]));
        }
      }
    }
  }
}
