package fr.bordeaux.main;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import java.util.List;

/** Factory used to create and configure a GameEngine. */
public final class GameEngineFactory {

  /** Default constructor. */
  private GameEngineFactory() {}

  /**
   * Creates a GameEngine from the given configuration.
   *
   * @param configManager configuration manager
   * @return an initialized GameEngine
   */
  public static GameEngine createGameEngine(final ConfigManager configManager) {
    final int boardSize = configManager.getInt("board-size", 9);
    final int nbWalls = configManager.getInt("nb-walls", 10);
    final boolean blitz = configManager.getBoolean("blitz", false);

    final Board board = new Board(boardSize);
    final List<Player> players = PlayerFactory.createPlayers(configManager);
    final GameState state = new GameState(board, players);

    if (blitz) {
      final int timeout = configManager.getInt("timeout", 30);
      state.setLimitTimePlayer(timeout * 60 * 1000L);
    }

    final GameEngine engine =
        new GameEngine(
            state,
            blitz,
            configManager.getBoolean("verbose", false),
            configManager.getBoolean("contest", false),
            configManager.getBoolean("debug", false),
            nbWalls);

    initializeWalls(engine);
    return engine;
  }

  /**
   * Initializes the number of walls for each player.
   *
   * @param engine game engine
   */
  private static void initializeWalls(final GameEngine engine) {
    for (final Player p : engine.getState().getPlayers()) {
      p.setRemainingWalls(engine.getInitialWalls());
    }
  }
}
