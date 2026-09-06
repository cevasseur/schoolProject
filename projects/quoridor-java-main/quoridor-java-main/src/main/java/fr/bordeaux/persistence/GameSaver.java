package fr.bordeaux.persistence;

import fr.bordeaux.ai.base.AiPlayer;
import fr.bordeaux.core.Board;
import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Utility class used to save a game state to a file. */
public final class GameSaver {
  /** The size of the board (usually 9x9). */
  private static int size = 9;

  /** Default board size. */
  private static final int DEFAULT_SIZE = 9;

  /** Character for vertical walls. */
  private static final char VERTICAL_WALL_CHAR = '|';

  /** Character for horizontal walls. */
  private static final char HORIZONTAL_WALL_CHAR = '_';

  /** Character for empty cells. */
  private static final char EMPTY_CELL_CHAR = '.';

  /** Starting character for coordinate column mapping. */
  private static final char POSITION_START_CHAR = 'a';

  /** Starting character for player index digits. */
  private static final char PLAYER_INDEX_START_CHAR = '1';

  /** Private constructor to prevent instantiation. */
  private GameSaver() {}

  /**
   * Saves the current game state to a file.
   *
   * @param engine the engine holding the game state and configuration to save
   * @param filename path of the destination file
   * @throws IOException if an input/output error occurs while writing the file
   */
  public static void saveGame(final GameEngine engine, final String filename) throws IOException {
    final Path path = Path.of(filename);
    try (PrintWriter writer =
        new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
      final GameState state = engine.getState();
      final Board board = state.getBoard();
      size = board.getSize();
      saveParameters(writer, engine, state);
      writeHeader(writer, state);
      writeBoard(writer, state);
      writeWalls(writer, state);
      writeHistory(writer, state);
    }
  }

  /**
   * Writes the game section header to the save file.
   *
   * @param writer writer used to save the data
   * @param state game state containing the current player information
   */
  private static void writeHeader(final PrintWriter writer, final GameState state) {
    writer.println("[game]");
    final Player currentPlayer = state.getCurrentPlayer();
    final fr.bordeaux.core.Color playerColor = currentPlayer.getColor();
    writer.println(playerColor.getId() + "  # Current player color");
    final Board board = state.getBoard();
    writer.println(board.getSize() + "  # Board size");
    final List<Player> players = state.getPlayers();
    writer.println("# Board (" + players.size() + " players)");
  }

  /**
   * Writes the board representation to the save file.
   *
   * @param writer writer used to save the data
   * @param state game state containing the board to write
   */
  private static void writeBoard(final PrintWriter writer, final GameState state) {
    final Board board = state.getBoard();
    for (int row = 0; row < size; row++) {
      writer.println(buildCellLine(row, board, state.getPlayers()));
      if (row < size - 1) {
        writer.println(buildWallLine(row, board));
      }
    }
  }

  /**
   * Builds the textual representation of one board row.
   *
   * @param row row index to build
   * @param board board to inspect
   * @param players list of players placed on the board
   * @return string representing the row content
   */
  private static String buildCellLine(
      final int row, final Board board, final List<Player> players) {
    final StringBuilder line = new StringBuilder();
    for (int col = 0; col < size; col++) {
      // Add '|' if there is a vertical wall between col-1 and col
      if (col > 0) {
        line.append(hasVerticalWall(board, row, col) ? '|' : ' ');
      }
      // Add player number '1'/'2' or '.' if cell is empty
      line.append(getCellChar(row, col, players));
    }
    return line.toString();
  }

  /**
   * Builds one horizontal wall line between two rows.
   *
   * @param row row index to build
   * @param board board to inspect
   * @return string representing the wall line content
   */
  private static String buildWallLine(final int row, final Board board) {
    final StringBuilder line = new StringBuilder();
    for (int col = 0; col < size; col++) {
      if (col > 0) {
        line.append(' ');
      }
      // Add '_' if there is a horizontal wall between row and row+1
      line.append(hasHorizontalWall(board, row, col) ? HORIZONTAL_WALL_CHAR : ' ');
    }
    return line.toString();
  }

  /**
   * Returns the character representing a board cell.
   *
   * @param row row of the cell
   * @param col column of the cell
   * @param players list of players to inspect
   * @return '1' for player 1, '2' for player 2, or '.' if the cell is empty
   */
  private static char getCellChar(final int row, final int col, final List<Player> players) {
    char result = EMPTY_CELL_CHAR;
    for (int i = 0; i < players.size(); i++) {
      final Position position = players.get(i).getPosition();
      if (position.getX() == row && position.getY() == col) {
        result = (char) (PLAYER_INDEX_START_CHAR + i);
        break;
      }
    }
    return result;
  }

  /**
   * Checks whether a vertical wall exists between two adjacent cells.
   *
   * @param board board to inspect
   * @param row row of the cell
   * @param col column of the right cell
   * @return true if a vertical wall is present, false otherwise
   */
  private static boolean hasVerticalWall(final Board board, final int row, final int col) {
    return !board.getGraph().hasEdge(board.getCellId(row, col - 1), board.getCellId(row, col));
  }

  /**
   * Checks whether a horizontal wall exists between two adjacent rows.
   *
   * @param board board to inspect
   * @param row row of the upper cell
   * @param col column of the cell
   * @return true if a horizontal wall is present, false otherwise
   */
  private static boolean hasHorizontalWall(final Board board, final int row, final int col) {
    return !board.getGraph().hasEdge(board.getCellId(row, col), board.getCellId(row + 1, col));
  }

  /**
   * Writes the remaining wall count for each player.
   *
   * @param writer writer used to save the data
   * @param state game state containing the players
   */
  private static void writeWalls(final PrintWriter writer, final GameState state) {
    final StringBuilder line = new StringBuilder("walls:");
    for (final Player player : state.getPlayers()) {
      line.append(" ").append(player.getRemainingWalls());
    }
    writer.println(line);
  }

  /**
   * Writes a single key-value pair to the output stream in the format "key=value".
   *
   * @param writer The {@link PrintWriter} used to write to the file.
   * @param key The setting key.
   * @param value The setting value.
   */
  private static void writeLine(final PrintWriter writer, final String key, final Object value) {
    writer.println(key + "=" + value);
  }

  /**
   * Saves AI player settings to the output file.
   *
   * @param writer The {@link PrintWriter} used to write to the file.
   * @param state The current game state.
   * @param aiPlayer The AI player whose settings are being saved.
   * @param index The index of the player (used for formatting the key).
   */
  private static void saveAiSettings(
      final PrintWriter writer, final GameState state, final AiPlayer aiPlayer, final int index) {
    writeLine(writer, "ai-" + index + "-name", aiPlayer.getName());
    writeLine(writer, "ai-" + index + "-color", aiPlayer.getColor());
    final Position startPos = aiPlayer.getStartingPosition();
    writeLine(writer, "ai-" + index + "-starting-position", startPos.toString());
    writeLine(writer, "ai-" + index + "-remaining-time", aiPlayer.getRemainingTime());
    final Map<String, Object> aiSettings = aiPlayer.getSettings();
    for (final String key : aiSettings.keySet()) {
      writeLine(writer, "ai-" + index + "-" + key, aiSettings.get(key));
    }
  }

  /**
   * Saves game parameters and player info. Writes the "[settings]" header and configurations.
   *
   * @param writer the {@link PrintWriter} used to write to the file
   * @param engine the engine providing the configuration
   * @param state the game state containing player data
   */
  private static void saveParameters(
      final PrintWriter writer, final GameEngine engine, final GameState state) {
    writer.println("[settings]");

    writeLine(writer, "verbose", engine.isVerbose());
    writeLine(writer, "blitz", engine.isBlitz());
    writeLine(writer, "constest", engine.isContest());
    writeLine(writer, "debug", engine.isDebug());
    final List<Player> players = state.getPlayers();
    writeLine(writer, "nb-players", players.size());

    int index = 1;
    for (final Player player : players) {
      if (player.isAI()) {
        final AiPlayer aiPlayer = (AiPlayer) player;
        saveAiSettings(writer, state, aiPlayer, index);
      } else {
        writeLine(writer, "player-" + index + "-name", player.getName());
        writeLine(writer, "player-" + index + "-color", player.getColor());
        final Position startPos = player.getStartingPosition();
        writeLine(writer, "player-" + index + "-starting-position", startPos.toString());
        writeLine(writer, "player-" + index + "-remaining-time", player.getRemainingTime());
      }
      index++;
    }
  }

  /**
   * Writes the complete game history to the save file.
   *
   * @param writer writer used to save the data
   * @param state game state containing the history
   */
  private static void writeHistory(final PrintWriter writer, final GameState state) {
    writer.println("[History]");
    final Move[] moves = state.getMovesPlayed();
    final List<Player> players = state.getPlayers();
    final int playersCount = players.size();
    int currentPlayerIndex = 0;

    for (int index = moves.length - 1; index >= 0; index--) {
      final Move move = moves[index];
      final String strMove = (currentPlayerIndex + 1) + " " + formatMove(move) + ";";
      if (currentPlayerIndex == playersCount - 1) {
        writer.println(strMove);
      } else {
        writer.print(strMove + " ");
      }
      currentPlayerIndex = (currentPlayerIndex + 1) % playersCount;
    }
  }

  /**
   * Formats a move into its save-file string representation.
   *
   * @param move move to format
   * @return formatted string (e.g., "e2-e4" or "e2h")
   */
  private static String formatMove(final Move move) {
    final StringBuilder builder = new StringBuilder();
    if (move.isPawn()) {
      builder.append(posToCoord(move.getFrom())).append('-').append(posToCoord(move.getTo()));
    } else {
      builder.append(posToCoord(move.getTo())).append(move.getOrientation().toString());
    }
    return builder.toString();
  }

  /**
   * Converts a board position to its coordinate string (e.g., "e2").
   *
   * @param pos position to convert
   * @return the coordinate string, or empty string if pos is null
   */
  private static String posToCoord(final Position pos) {
    String result = "";
    if (pos != null) {
      final char colChar = (char) (POSITION_START_CHAR + pos.getY());
      final int rowNum = pos.getX() + 1;
      result = String.valueOf(colChar) + rowNum;
    }
    return result;
  }
}
