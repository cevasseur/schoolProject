package fr.bordeaux.persistence;

import fr.bordeaux.ai.mcts.MctsPlayer;
import fr.bordeaux.ai.minimax.*;
import fr.bordeaux.ai.random.RandomPlayer;
import fr.bordeaux.core.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Parses player definitions and AI configurations from save files. */
public final class PlayerLoader {

  /** Default MCTS reflexion time in milliseconds. */
  private static final long MCTS_REFLEXION_DEFAULT = 2000L;

  /** Private constructor of PlayerLoader */
  private PlayerLoader() {}

  /**
   * Record bundling all parsed players and next line index.
   *
   * @param players The list of all parsed players.
   * @param nextLineIndex The index of the line where player and settings parsing stopped.
   */
  public record PlayerResult(List<Player> players, int nextLineIndex) {}

  /**
   * Reads all players (Human and AI) from the save file.
   *
   * @param lines The list of strings representing the lines of the save file.
   * @return PlayerResult with players and next line index.
   */
  public static PlayerResult loadPlayers(final List<String> lines) {
    final List<Player> lPlayers = new ArrayList<>();

    int gameSectionIndex = -1;
    for (int i = 0; i < lines.size(); i++) {
      if ("[game]".equals(lines.get(i))) {
        gameSectionIndex = i;
        break;
      }
    }

    int currentLine = 1;
    while (currentLine < lines.size()
        && currentLine < gameSectionIndex
        && !lines.get(currentLine).startsWith("player-")
        && !lines.get(currentLine).startsWith("ai-")) {
      currentLine++;
    }

    while (currentLine < lines.size() && currentLine < gameSectionIndex) {
      final PlayerAndLine res = readPlayer(lines, currentLine);
      lPlayers.add(res.player);
      currentLine = res.nextLine;
    }

    return new PlayerResult(lPlayers, gameSectionIndex);
  }

  /**
   * Reads player info and delegates AI configuration.
   *
   * @param lines The list of strings representing the lines of the save file.
   * @param currentLine The index of the line where the player's information begins.
   * @return PlayerAndLine with player and updated line index.
   */
  private static PlayerAndLine readPlayer(final List<String> lines, final int currentLine) {
    final String playerLine = lines.get(currentLine);
    final String typePart = playerLine.split("=")[0].split("-")[0];
    final String playerName = playerLine.split("=")[1];
    int myCurrentLine = currentLine + 1;

    final String colorLine = lines.get(myCurrentLine);
    final String colorValue = colorLine.split("=")[1].trim().toUpperCase(Locale.ENGLISH);
    final Color playerColor = Color.valueOf(colorValue);
    myCurrentLine++;

    final String posLine = lines.get(myCurrentLine);
    final Position playerPosition = readPlayerStartPosition(posLine);
    myCurrentLine++;

    final String timeLine = lines.get(myCurrentLine);
    final long remainingTime = Long.parseLong(timeLine.split("=")[1]);

    final PlayerAndLine result;
    if ("ai".equals(typePart)) {
      result =
          readAiPlayer(
              lines, myCurrentLine, playerName, playerPosition, playerColor, remainingTime);
    } else {
      final Player human = new HumanPlayer(playerName, playerPosition, playerColor);
      human.setRemainingTime(remainingTime);
      result = new PlayerAndLine(human, myCurrentLine + 1);
    }
    return result;
  }

  /**
   * Parses starting position coordinates from string.
   *
   * @param p The string containing the position data (e.g., "key=Position: 4, 0").
   * @return A {@link Position} object representing the parsed coordinates.
   */
  private static Position readPlayerStartPosition(final String positionString) {
    final String posValue = positionString.split("=")[1];
    final String coordPart = posValue.split(":")[1];
    final String[] coords = coordPart.split(",");
    final int posX = Integer.parseInt(coords[0].trim());
    final int posY = Integer.parseInt(coords[1].trim());
    return new Position(posX, posY);
  }

  /**
   * Reads an AI player and its settings.
   *
   * @return PlayerAndLine with configured AI.
   */
  private static PlayerAndLine readAiPlayer(
      final List<String> lines,
      final int currentLine,
      final String name,
      final Position pos,
      final Color color,
      final long time) {
    final Map<String, String> settings = parseAiSettings(lines, currentLine);
    final int nextLine = currentLine + settings.size() + 1;

    final String mode = settings.get("mode");
    final Player aiPlayer;
    switch (mode) {
      case "minimax" -> aiPlayer = createMinimax(name, pos, color, settings, false);
      case "minimax-iterative-deepening" ->
          aiPlayer = createMinimax(name, pos, color, settings, true);
      case "mcts" -> aiPlayer = createMcts(name, pos, color, settings);
      case "random" -> aiPlayer = new RandomPlayer(name, pos, color);
      default -> throw new IllegalArgumentException("Unknown AI Type : " + mode);
    }
    aiPlayer.setRemainingTime(time);
    return new PlayerAndLine(aiPlayer, nextLine);
  }

  private static Map<String, String> parseAiSettings(
      final List<String> lines, final int startLine) {
    final Map<String, String> settings = new ConcurrentHashMap<>();
    int lineRef = startLine + 1;
    final int targetIndex = Integer.parseInt(lines.get(lineRef).split("-")[1]);
    while (checkSameIndex(lines, lineRef, targetIndex)) {
      final String[] split = lines.get(lineRef).split("=");
      final String[] param = split[0].split("-");
      settings.put(param[param.length - 1], split[1]);
      lineRef++;
    }
    return settings;
  }

  private static Player createMinimax(
      final String name,
      final Position pos,
      final Color color,
      final Map<String, String> settings,
      final boolean isIterative) {
    final String heuristic = settings.get("heuristic");
    final int reflexion = Integer.parseInt(settings.get("reflexion"));
    final MinimaxBuilder builder =
        MinimaxPlayer.builder(name, pos, color).withHeuristic(heuristic).withReflexion(reflexion);

    if (isIterative) {
      builder.withIterativeDeepening(true);
    } else {
      builder.withDepth(Integer.parseInt(settings.get("depth")));
    }
    return builder.build();
  }

  private static Player createMcts(
      final String name,
      final Position pos,
      final Color color,
      final Map<String, String> settings) {
    final long reflexion =
        settings.containsKey("reflexion")
            ? Long.parseLong(settings.get("reflexion"))
            : MCTS_REFLEXION_DEFAULT;
    final boolean useNN = "ML".equals(settings.get("selection"));
    try {
      return new MctsPlayer(name, pos, reflexion, useNN, color);
    } catch (final IOException exception) {
      throw new IllegalArgumentException("Impossible de charger le modèle pour MCTS", exception);
    }
  }

  /**
   * Checks if current line player matches target index.
   *
   * @param lines The list of strings representing the lines of the save file.
   * @param currentLine The index of the line to check.
   * @param index The index of a player
   * @return {@code true} if the line has the same index player, {@code false} otherwise.
   */
  private static boolean checkSameIndex(
      final List<String> lines, final int myLineRef, final int targetIndex) {
    boolean result = false;
    if (lines.get(myLineRef).contains("-")) {
      result = Integer.parseInt(lines.get(myLineRef).split("-")[1]) == targetIndex;
    }
    return result;
  }

  /**
   * Record bundling a parsed player and next line index.
   *
   * @param player The parsed player instance.
   * @param nextLine The index of the next line in the file.
   */
  private record PlayerAndLine(Player player, int nextLine) {}
}
