package fr.bordeaux.network;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import java.util.ArrayList;
import java.util.List;

/** Represents one active Quoridor game hosted by the server. */
public class ServerGameSession {

  /** Unique identifier for the game session. */
  private final String gameId;

  /** Current state of the game. */
  private final GameState gameState;

  /** Engine used to process moves and enforce rules. */
  private final GameEngine gameEngine;

  /** List of connected client sessions involved in this game. */
  private final List<ClientSession> players;

  /** Configuration manager used to retrieve game settings. */
  private final ConfigManager config = ConfigManager.getInstance();

  /**
   * Creates a new server-side game session for the given connected players.
   *
   * @param gameId identifier of the game session
   * @param sessions client sessions participating in the game
   */
  public ServerGameSession(final String gameId, final List<ClientSession> sessions) {
    if (sessions.size() != 2 && sessions.size() != 4) {
      throw new IllegalArgumentException(
          "ServerGameSession requires 2 or 4 players, but got: " + sessions.size());
    }

    this.gameId = gameId;
    this.players = new ArrayList<>(sessions);

    final Board board = new Board(9);
    final List<Player> gamePlayers = new ArrayList<>();

    if (sessions.size() >= 1) {
      gamePlayers.add(new HumanPlayer(sessions.get(0).getName(), new Position(0, 4), Color.WHITE));
    }
    if (sessions.size() >= 2) {
      gamePlayers.add(new HumanPlayer(sessions.get(1).getName(), new Position(8, 4), Color.BLACK));
    }
    if (sessions.size() >= 3) {
      gamePlayers.add(new HumanPlayer(sessions.get(2).getName(), new Position(4, 0), Color.BLUE));
    }
    if (sessions.size() >= 4) {
      gamePlayers.add(new HumanPlayer(sessions.get(3).getName(), new Position(4, 8), Color.RED));
    }

    this.gameState = new GameState(board, gamePlayers, 0);
    final boolean blitz = config.getBoolean("blitz", false);
    final boolean verbose = config.getBoolean("verbose", false);
    final boolean contest = config.getBoolean("contest", false);
    final boolean debug = config.getBoolean("debug", false);
    final int initialWalls = config.getInt("initialWalls", 20);
    this.gameEngine = new GameEngine(gameState, blitz, verbose, contest, debug, initialWalls);
  }

  /**
   * Returns the game session identifier.
   *
   * @return game ID
   */
  public String getGameId() {
    return gameId;
  }

  /**
   * Returns the game state.
   *
   * @return current game state
   */
  public GameState getGameState() {
    return gameState;
  }

  /**
   * Returns the server-side game engine.
   *
   * @return game engine
   */
  public GameEngine getGameEngine() {
    return gameEngine;
  }

  /**
   * Returns the list of connected players in this game.
   *
   * @return list of client sessions
   */
  public List<ClientSession> getPlayers() {
    return players;
  }

  /**
   * Checks if a player with the given ID is in this game session.
   *
   * @param playerId player identifier
   * @return true if the player is in the game
   */
  public boolean containsPlayer(final String playerId) {
    for (final ClientSession player : players) {
      if (player.getId().equals(playerId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the opponent of the specified player.
   *
   * @param playerId player identifier
   * @return the opponent client session, or null if not found
   */
  public ClientSession getOpponent(final String playerId) {
    for (final ClientSession player : players) {
      if (!player.getId().equals(playerId)) {
        return player;
      }
    }
    return null;
  }

  /**
   * Returns every other player in the game except the provided one.
   *
   * @param playerId player identifier
   * @return the other client sessions in turn order
   */
  public List<ClientSession> getOtherPlayers(final String playerId) {
    final List<ClientSession> others = new ArrayList<>();
    for (final ClientSession player : players) {
      if (!player.getId().equals(playerId)) {
        others.add(player);
      }
    }
    return others;
  }

  /**
   * Returns the current player's client session.
   *
   * @return current player session, or null if invalid index
   */
  public ClientSession getCurrentPlayerSession() {
    final int currentIndex = gameState.getCurrentIndexPlayer();
    if (currentIndex < 0 || currentIndex >= players.size()) {
      return null;
    }
    return players.get(currentIndex);
  }

  /**
   * Returns the winning player session if the game is finished.
   *
   * @return winner session or null if game is not finished
   */
  public ClientSession getWinnerSession() {
    for (int i = 0; i < gameState.getPlayers().size(); i++) {
      final Player player = gameState.getPlayers().get(i);
      if (gameState.hasReachedGoal(player)) {
        return players.get(i);
      }
    }
    return null;
  }

  /**
   * Checks if it is the specified player's turn.
   *
   * @param playerId player identifier
   * @return true if it is this player's turn
   */
  public boolean isPlayerTurn(final String playerId) {
    final ClientSession current = getCurrentPlayerSession();
    return current != null && current.getId().equals(playerId);
  }

  /**
   * Applies a move through the server-side game engine.
   *
   * @param move move to validate and apply
   * @return true if the move was legal and applied
   */
  public boolean applyMove(final Move move) {
    return gameEngine.play(move);
  }

  /**
   * Checks if the game session is finished.
   *
   * @return true if the game is over
   */
  public boolean isFinished() {
    return gameState.isGameOver();
  }
}
