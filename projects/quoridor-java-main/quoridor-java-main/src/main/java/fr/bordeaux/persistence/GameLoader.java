package fr.bordeaux.persistence;

import fr.bordeaux.core.*;
import fr.bordeaux.i18n.I18n;
import java.io.*;
import java.util.*;

/** Utility class used to load a game state from a save file. */
public final class GameLoader {
  /** The size of the board (9x9). */
  private static int size = 9;

  /** The line index where the board section starts in the save file. */
  private static final int BOARD_START = 3;

  /** Private constructor to prevent instantiation. */
  private GameLoader() {}

  /**
   * Loads a saved game from a file.
   *
   * @param engine the engine for configuration
   * @param filename path of the save file
   * @return the reconstructed game state
   * @throws IOException on I/O error
   * @throws GameLoadException on invalid format
   */
  public static GameState loadGame(final GameEngine engine, final String filename)
      throws IOException, GameLoadException {
    final List<String> lines = GameLoaderComment.readCleanLines(filename);
    validateSettingsHeader(lines);
    final PlayerLoader.PlayerResult playerResult = PlayerLoader.loadPlayers(lines);
    final List<Player> players = playerResult.players();
    final int currentLine = playerResult.nextLineIndex();
    validateHeader(lines, currentLine);
    size = readBoardSize(lines, currentLine);
    final Board board = new Board(size);
    readBoard(lines, board, players, currentLine);
    readRemainingWalls(lines, players, currentLine);

    final List<Move> history = HistoryLoader.parseHistory(lines);
    final int playerIndex = readCurrentPlayer(lines, currentLine);
    final GameState gameState = new GameState(board, players, playerIndex);
    applyMode(engine, gameState, lines);
    gameState.loadHistoryWithoutApplying(history);
    return gameState;
  }

  // Checks that the first line is "[game]".
  // Throws GameLoadException with error type if not.
  private static void validateHeader(final List<String> lines, final int currentLine)
      throws GameLoadException {
    if (!"[game]".equals(lines.get(currentLine))) {
      throw new GameLoadException(
          "ERREUR_ENTETE", 1, I18n.get("expectGame") + lines.get(currentLine));
    }
  }

  /**
   * Checks that the save file starts with the settings header.
   *
   * @param lines The list of strings representing the lines of the save file.
   * @throws GameLoadException if the header is missing or incorrect.
   */
  private static void validateSettingsHeader(final List<String> lines) throws GameLoadException {
    if (!"[settings]".equals(lines.get(0))) {
      throw new GameLoadException("ERREUR_ENTETE", 1, I18n.get("expectSett") + lines.get(0));
    }
  }

  // Reads the current player index from line 2
  // Example : "2  # Current player color" , returns 1
  private static int readCurrentPlayer(final List<String> lines, final int currentLine) {
    return Integer.parseInt(lines.get(currentLine + 1).trim().split("#")[0].trim()) - 1;
  }

  private static int readBoardSize(final List<String> lines, final int currentLine) {
    return Integer.parseInt(lines.get(currentLine + 2).trim().split("#")[0].trim());
  }

  // Reads board, positions players and places walls.
  private static void readBoard(
      final List<String> lines,
      final Board board,
      final List<Player> players,
      final int currentLine) {
    for (int row = 0; row < size; row++) {
      readCellLine(lines.get(BOARD_START + currentLine + row * 2), row, board, players);
      if (row < size - 1) {
        readHorizontalWalls(lines.get(BOARD_START + currentLine + row * 2 + 1), row, board);
      }
    }
  }

  // Reads a cell line, positions players and vertical walls.
  private static void readCellLine(
      final String line, final int row, final Board board, final List<Player> players) {
    for (int col = 0; col < size; col++) {
      // Each cell character is at index col*2 in the line
      final char cell = line.charAt(col * 2);
      if (cell == '1') {
        players.get(0).setPosition(new Position(row, col));
      } else if (cell == '2') {
        players.get(1).setPosition(new Position(row, col));
      }
      if (col < size - 1 && line.charAt(col * 2 + 1) == '|') {
        board.getGraph().removeEdge(board.getCellId(row, col), board.getCellId(row, col + 1));
      }
    }
  }

  // Reads and places horizontal walls.
  private static void readHorizontalWalls(final String line, final int row, final Board board) {
    for (int col = 0; col < size; col++) {
      if (line.charAt(col * 2) == '_') {
        board.getGraph().removeEdge(board.getCellId(row, col), board.getCellId(row + 1, col));
      }
    }
  }

  // Restores the correct number of walls for each player.
  private static void readRemainingWalls(
      final List<String> lines, final List<Player> players, final int currentLine) {
    final String wallLine = lines.get(BOARD_START + currentLine + size * 2 - 1);
    final String[] parts = wallLine.replace("walls:", "").trim().split(" ");
    for (int index = 0; index < players.size(); index++) {
      final int walls = Integer.parseInt(parts[index]);
      final Player player = players.get(index);
      while (player.getRemainingWalls() > walls) {
        player.useWall();
      }
    }
  }

  /**
   * Applies game configuration (verbose, blitz, etc.).
   *
   * @throws IllegalArgumentException if player count mismatch.
   */
  private static void applyMode(
      final GameEngine engine, final GameState gameState, final List<String> lines) {
    int nbPlayers = 0;
    for (final String line : lines) {
      if ("[game]".equals(line)) {
        break;
      }
      if (line.contains("=")) {
        final String[] parts = line.split("=");
        final String key = parts[0].trim();
        final String value = parts[1].trim();
        nbPlayers = handleModeSetting(engine, key, value, nbPlayers);
      }
    }

    validatePlayerCount(nbPlayers, gameState.getPlayers().size());
  }

  private static int handleModeSetting(
      final GameEngine engine,
      final String key,
      final String value,
      final int currentPlayersCount) {
    int playersCount = currentPlayersCount;
    switch (key) {
      case "verbose" -> engine.setVerbose(Boolean.parseBoolean(value));
      case "blitz" -> engine.setBlitz(Boolean.parseBoolean(value));
      case "constest" -> engine.setContest(Boolean.parseBoolean(value));
      case "debug" -> engine.setDebug(Boolean.parseBoolean(value));
      case "nb-players" -> playersCount = Integer.parseInt(value);
      default -> {}
    }
    return playersCount;
  }

  private static void validatePlayerCount(final int expected, final int actual) {
    if (expected > 0 && actual != expected) {
      throw new IllegalArgumentException(
          I18n.get("nbPlayer") + expected + I18n.get("actualPlayer") + actual + I18n.get("diff"));
    }
  }
}
