package fr.bordeaux.main;

import fr.bordeaux.ai.base.*;
import fr.bordeaux.ai.mcts.*;
import fr.bordeaux.ai.minimax.*;
import fr.bordeaux.ai.random.*;
import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import java.util.ArrayList;
import java.util.List;

/** Factory for player instantiation (Composition Root). */
public final class PlayerFactory {

  /** Private constructor to prevent instantiation of this utility class. */
  private PlayerFactory() {}

  /**
   * Creates players for a game session from config.
   *
   * @param config configuration manager
   * @return list of players
   */
  public static List<Player> createPlayers(final ConfigManager config) {
    final List<Player> players = new ArrayList<>();
    final int size = ConfigManager.getInstance().getInt("board-size", 9);
    if (config.getBoolean("ai1", false)) {
      players.add(buildAi("AI WHITE", new Position(0, size / 2), config, Color.WHITE));
    } else {
      players.add(new HumanPlayer("Human White", new Position(0, size / 2), Color.WHITE));
    }

    if (config.getBoolean("ai2", false)) {
      players.add(buildAi("AI BLACK", new Position(size - 1, size / 2), config, Color.BLACK));
    } else {
      players.add(new HumanPlayer("Human Black", new Position(size - 1, size / 2), Color.BLACK));
    }

    final int nbPlayers = Integer.parseInt(config.getOption("nb-players", "2"));
    if (nbPlayers > 2 && nbPlayers <= 4) {
      players.add(new HumanPlayer("Human Blue", new Position(size / 2, 0), Color.BLUE));
      if (nbPlayers == 4) {
        players.add(new HumanPlayer("Human Red", new Position(size / 2, size - 1), Color.RED));
      }
    }

    return players;
  }

  /**
   * Builds an AI player from configuration (mode, depth, time, scoring).
   *
   * @param name AI name
   * @param pos starting position
   * @param config configuration source
   * @param color AI color
   * @return configured Player
   */
  private static Player buildAi(
      final String name, final Position pos, final ConfigManager config, final Color color) {

    // Convert the Color enum to its ID string (e.g., "0" or "1") to match config keys
    final String colorId = String.valueOf(color.getId());
    final String prefix = "ai" + colorId;

    final String mode = config.getOption(prefix + ".mode", "minimax");
    Player player = null;

    switch (mode) {
      case "mcts":
        // Get reflection time and convert to milliseconds
        final long reflexionTimeMs = config.getInt(prefix + ".time", 5) * 1000L;
        final String selection = config.getOption(prefix + ".selection", "UCT");
        final boolean useNN = "ML".equalsIgnoreCase(selection);

        try {
          player = new MctsPlayer(name, pos, reflexionTimeMs, useNN, color);
        } catch (java.io.IOException e) {
          throw new IllegalStateException(
              "Error initializing MCTS AI (" + selection + "): " + e.getMessage(), e);
        }
        break;

      case "iterative":
      case "minimax":
      default:
        final int reflexionTime = config.getInt(prefix + ".time", 5);
        final int depth = config.getInt(prefix + ".depth", depthBasedOnTime(reflexionTime));
        final String scoring = config.getOption(prefix + ".scoring", "simple");

        final MinimaxBuilder builder =
            MinimaxPlayer.builder(name, pos, color)
                .withDepth(depth)
                .withHeuristic(scoring)
                .withReflexion(reflexionTime);

        if ("iterative".equals(mode)) {
          builder.withIterativeDeepening(true);
        }
        player = builder.build();
        break;
    }

    return player;
  }

  /**
   * Determines the AI search depth based on the allocated reflection time.
   *
   * @param reflexionTime the time allowed for the AI to think, in seconds.
   * @return the calculated search depth (from 3 to 7).
   */
  /* default */ static int depthBasedOnTime(final int reflexionTime) {
    final int result;
    if (reflexionTime <= 2) {
      result = 3;
    } else if (reflexionTime <= 5) {
      result = 4;
    } else if (reflexionTime <= 8) {
      result = 5;
    } else if (reflexionTime <= 12) {
      result = 6;
    } else {
      result = 7;
    }

    return result;
  }
}
