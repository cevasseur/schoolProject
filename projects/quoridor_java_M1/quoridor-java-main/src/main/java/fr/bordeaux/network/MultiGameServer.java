package fr.bordeaux.network;

import fr.bordeaux.core.Move;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Multi-client server for connectivity, creation, and communication. */
public class MultiGameServer {

  /** TCP server socket used to accept client connections. */
  private ServerSocket serverSocket;

  /** Indicates whether the server is currently running. */
  private final AtomicBoolean running;

  /** UDP broadcaster announcing the server on the local network. */
  private ServerBroadcaster broadcaster;

  /** Thread responsible for running the server UDP broadcast. */
  private Thread broadcastThread;

  /** TCP port used by the server. */
  private int port;

  /** Lock used to synchronize access to the server shared data. */
  private final Object lock = new Object();

  /** Connected clients indexed by their identifier. */
  private final Map<String, ClientSession> clients;

  /** Running games indexed by their identifier. */
  private final Map<String, ServerGameSession> games;

  /** Scoreboard of players known by the server. */
  private final Map<String, ScoreEntry> scoreboard;

  /** Counter used to generate unique client identifiers. */
  private int nextClientId = 1;

  /** Counter used to generate unique game identifiers. */
  private int nextGameId = 1;

  /** Maximum inactivity duration before a client is disconnected. */
  private static final long CLIENT_TIMEOUT_MS = 60_000;

  /** Delay between timeout checks. */
  private static final long TIMEOUT_CHECK_MS = 5_000;

  /** Prefix used to generate client identifiers. */
  private static final String CLIENT_ID_PREFIX = "C";

  /** Prefix used to generate game identifiers. */
  private static final String GAME_ID_PREFIX = "G";

  /** Minimum number of players accepted for a server game. */
  private static final int MIN_PLAYERS = 2;

  /** Maximum number of players accepted for a server game. */
  private static final int MAX_PLAYERS = 4;

  /** Command prefix used when a client introduces itself. */
  private static final String HELLO_PREFIX = "HELLO ";

  /** Command prefix used when a client sends a move. */
  private static final String MOVE_PREFIX = "MOVE ";

  /** Exact command used for a ping. */
  private static final String PING_COMMAND = "PING";

  /** Exact command used to quit. */
  private static final String QUIT_COMMAND = "QUIT";

  /** Exact client status sent after a successful ping. */
  private static final String PONG_RESPONSE = "PONG";

  /** Exact client status sent after a quit request. */
  private static final String BYE_RESPONSE = "BYE";

  /** Message sent when no connected players are available. */
  private static final String NO_PLAYERS = "No connected players.";

  /** Message sent when the scoreboard is empty. */
  private static final String EMPTY_SCOREBOARD = "Scoreboard is empty.";

  /** Prefix used when notifying a player that a game has started. */
  private static final String GAME_START_PREFIX = "GAME_START ";

  /** Suffix used when the first player may act immediately. */
  private static final String YOUR_TURN_SUFFIX = " YOUR_TURN";

  /** Suffix used when the player must wait for the opponent. */
  private static final String WAIT_SUFFIX = " WAIT";

  /** Default constructor. */
  public MultiGameServer() {
    this.running = new AtomicBoolean(false);
    this.clients = new HashMap<>();
    this.games = new HashMap<>();
    this.scoreboard = new HashMap<>();
  }

  /**
   * Starts the multi-client server on the given TCP port.
   *
   * @param port TCP port used by the server
   */
  public void start(final int port) {
    final Thread serverThread =
        new Thread(
            () -> {
              try {
                this.port = port;
                serverSocket = new ServerSocket(port);
                running.set(true);

                Logger.getInstance().info("Server started on port {0}", port);

                broadcaster = new ServerBroadcaster("Quoridor", port);
                broadcastThread = new Thread(broadcaster);
                broadcastThread.start();
                startTimeoutChecker();

                while (running.get()) {
                  final Socket socket = serverSocket.accept();

                  final BufferedReader clientReader =
                      new BufferedReader(
                          new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                  final PrintWriter clientWriter = new PrintWriter(socket.getOutputStream(), true);

                  final String clientId;
                  synchronized (lock) {
                    clientId = CLIENT_ID_PREFIX + nextClientId;
                    nextClientId++;
                  }

                  final ClientSession client =
                      new ClientSession(clientId, socket, clientReader, clientWriter);

                  synchronized (lock) {
                    final String cid = client.getId();
                    clients.put(cid, client);
                    scoreboard.put(cid, new ScoreEntry(cid, client.getName()));
                  }

                  Logger.getInstance().info("Client connected: {0}", clientId);

                  final Thread clientThread =
                      new Thread(
                          () -> {
                            handleClient(client);
                          });
                  clientThread.start();
                }

              } catch (final IOException e) {
                if (running.get()) {
                  if (Logger.getInstance().isErrorEnabled()) {
                    Logger.getInstance().error("Server error: {0}", e.getMessage());
                  }
                }
              }
            });
    serverThread.start();
  }

  /**
   * Handles all commands received from one connected client.
   *
   * @param client the connected client session
   */
  private void handleClient(final ClientSession client) {
    try (BufferedReader reader = getClientReader(client)) {
      while (this.running.get()) {
        final String line = reader.readLine();
        if (line == null) {
          break;
        }
        client.setLastPing(System.currentTimeMillis());
        if (Logger.getInstance().isInfoEnabled()) {
          Logger.getInstance().info("{0} -> {1}", client.getId(), line);
        }

        if (line.startsWith(HELLO_PREFIX)) {
          processHello(client, line);
        } else if (PING_COMMAND.equals(line)) {
          processPing(client);
        } else if (QUIT_COMMAND.equals(line)) {
          processQuit(client);
          break;
        } else if (line.startsWith(MOVE_PREFIX)) {
          processMove(client, line);
        } else {
          Logger.getInstance().verbose("Unknown command from {0}: {1}", client.getId(), line);
          client.send("ERROR unknown command");
        }
      }
    } catch (final IOException e) {
      if (Logger.getInstance().isInfoEnabled()) {
        Logger.getInstance().info("Client disconnected: {0}", e.getMessage());
      }
    } finally {
      unregisterClient(client);
    }
  }

  /**
   * Removes a client from the server and cleans up its active game if needed.
   *
   * @param client the disconnected client
   */
  private void unregisterClient(final ClientSession client) {
    final boolean removed;
    final String clientId = client.getId();
    synchronized (lock) {
      removed = clients.remove(clientId) != null;
      if (removed) {
        removeClientFromActiveGame(clientId, client.getCurrentGameId());
      }
    }

    client.close();
    Logger.getInstance().info("Client disconnected: {0}", clientId);
  }

  private void removeClientFromActiveGame(final String clientId, final String gameId) {
    if (gameId != null) {
      final ServerGameSession game = games.get(gameId);
      if (game != null) {
        for (final ClientSession player : game.getPlayers()) {
          if (!player.getId().equals(clientId)) {
            player.setStatus(PlayerStatus.IDLE);
            player.setCurrentGameId(null);
            player.send("ERROR opponent disconnected");
          }
        }
        games.remove(gameId);
      }
    }
  }

  /**
   * Registers the player name sent by a client and confirms the connection.
   *
   * @param client the connected client
   * @param line the received HELLO command
   */
  private void processHello(final ClientSession client, final String line) {
    final String name = line.substring(HELLO_PREFIX.length()).trim();
    if (name.isEmpty()) {
      client.send("ERROR invalid name");
    } else {
      final String clientId = client.getId();
      synchronized (lock) {
        client.setName(name);
        final ScoreEntry entry = scoreboard.get(clientId);
        if (entry != null) {
          entry.setPlayerName(name);
        }
      }
      client.send("WELCOME " + clientId);
      Logger.getInstance().verbose("Player {0} renamed to {1}", clientId, name);
    }
  }

  /** Handles the PING command by sending PONG back to the client. */
  private void processPing(final ClientSession client) {
    client.send(PONG_RESPONSE);
  }

  /** Handles a QUIT command. */
  private void processQuit(final ClientSession client) {
    client.send(BYE_RESPONSE);
  }

  /** Validates and applies a move sent by a client. */
  private void processMove(final ClientSession client, final String line) {
    final String moveText = line.substring(MOVE_PREFIX.length()).trim();
    final String clientId = client.getId();

    synchronized (lock) {
      final String gameId = client.getCurrentGameId();
      final ServerGameSession game = validateMoveRequest(client, clientId, gameId);
      if (game != null) {
        applyClientMove(client, clientId, gameId, game, moveText);
      }
    }
  }

  private ServerGameSession validateMoveRequest(
      final ClientSession client, final String clientId, final String gameId) {
    final ServerGameSession game;
    if (gameId == null) {
      client.send("ERROR not in game");
      game = null;
    } else {
      final ServerGameSession requestedGame = games.get(gameId);
      if (requestedGame == null) {
        client.send("ERROR game not found");
        game = null;
      } else if (!requestedGame.isPlayerTurn(clientId)) {
        client.send("ERROR not your turn");
        game = null;
      } else {
        game = requestedGame;
      }
    }
    return game;
  }

  private void applyClientMove(
      final ClientSession client,
      final String clientId,
      final String gameId,
      final ServerGameSession game,
      final String moveText) {
    try {
      final Move move = NetworkMoveParser.parseMove(moveText);
      if (!game.applyMove(move)) {
        client.send("ERROR illegal move");
        Logger.getInstance().verbose(I18n.get("serverInvalidMove", clientId, gameId, moveText));
      } else {
        notifyMoveAccepted(client, clientId, gameId, game, moveText);
      }
    } catch (final IllegalArgumentException e) {
      Logger.getInstance().debug("Invalid move format from {0}: {1}", clientId, moveText);
      client.send("ERROR invalid move format");
    }
  }

  private void notifyMoveAccepted(
      final ClientSession client,
      final String clientId,
      final String gameId,
      final ServerGameSession game,
      final String moveText) {
    client.send("OK");
    for (final ClientSession otherPlayer : game.getOtherPlayers(clientId)) {
      otherPlayer.send("OPPONENT_MOVE " + moveText);
    }
    Logger.getInstance().verbose(I18n.get("serverMoveReceived", moveText, clientId, gameId));
    final ClientSession winner = game.getWinnerSession();
    if (winner != null) {
      finishGame(game, winner);
    }
  }

  /**
   * Ends a game, updates the scoreboard and resets player states.
   *
   * @param game the finished game session
   * @param winner the winning client
   */
  private void finishGame(final ServerGameSession game, final ClientSession winner) {
    final String winnerId = winner.getId();
    final ScoreEntry winnerScore = scoreboard.get(winnerId);
    if (winnerScore != null) {
      winnerScore.recordWin();
    }

    for (final ClientSession player : game.getPlayers()) {
      final String pid = player.getId();
      if (!pid.equals(winnerId)) {
        final ScoreEntry loserScore = scoreboard.get(pid);
        if (loserScore != null) {
          loserScore.recordLoss();
        }
      }
    }

    for (final ClientSession player : game.getPlayers()) {
      player.setStatus(PlayerStatus.IDLE);
      player.setCurrentGameId(null);

      if (player.getId().equals(winnerId)) {
        player.send("GAME_OVER WIN");
      } else {
        player.send("GAME_OVER LOSE");
      }
    }

    if (Logger.getInstance().isInfoEnabled()) {
      Logger.getInstance().info("Game {0} finished. Winner: {1}", game.getGameId(), winnerId);
    }
    games.remove(game.getGameId());
  }

  /**
   * Returns a textual summary of the server state.
   *
   * @return the port, number of connected clients and number of running games
   */
  public String getServerStatus() {
    synchronized (lock) {
      return "Port: "
          + port
          + "\nConnected clients: "
          + clients.size()
          + "\nRunning games: "
          + games.size();
    }
  }

  /**
   * Lists all connected players with their identifier, name and status.
   *
   * @return a textual representation of connected players
   */
  public String listPlayers() {
    final String result;
    synchronized (lock) {
      if (clients.isEmpty()) {
        result = NO_PLAYERS;
      } else {
        final StringBuilder statusBuilder = new StringBuilder();
        for (final ClientSession client : clients.values()) {
          statusBuilder
              .append(client.getId())
              .append(' ')
              .append(client.getName())
              .append(' ')
              .append(client.getStatus())
              .append('\n');
        }
        result = statusBuilder.toString().trim();
      }
    }
    return result;
  }

  /**
   * Returns the current scoreboard of players who have played on the server.
   *
   * @return a textual representation of wins, losses and played games
   */
  public String getScoreboard() {
    final String result;
    synchronized (lock) {
      if (scoreboard.isEmpty()) {
        result = EMPTY_SCOREBOARD;
      } else {
        final StringBuilder scoreBuilder = new StringBuilder();
        for (final ScoreEntry entry : scoreboard.values()) {
          scoreBuilder.append(entry).append('\n');
        }
        result = scoreBuilder.toString().trim();
      }
    }
    return result;
  }

  /**
   * Creates a new game between the given available players.
   *
   * @param playerIds identifiers of the players
   * @return a success or error message
   */
  public String createGame(final String... playerIds) {
    String result;
    synchronized (lock) {
      final int playerCount = playerIds.length;
      if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
        result = "ERROR need between 2 and 4 players";
      } else {
        final List<ClientSession> sessions = new ArrayList<>();
        result = validatePlayersAndCollectSessions(playerIds, sessions);
        if (result.isEmpty()) {
          final String gameId = GAME_ID_PREFIX + nextGameId;
          nextGameId++;
          final ServerGameSession game = new ServerGameSession(gameId, sessions);
          games.put(gameId, game);

          for (int index = 0; index < sessions.size(); index++) {
            final ClientSession session = sessions.get(index);
            session.setStatus(PlayerStatus.INGAME);
            session.setCurrentGameId(gameId);
            final String turnInfo;
            if (index == 0) {
              turnInfo = YOUR_TURN_SUFFIX;
            } else {
              turnInfo = WAIT_SUFFIX;
            }
            session.send(GAME_START_PREFIX + gameId + turnInfo);
          }
          Logger.getInstance().info("Game {0} created with {1} players", gameId, playerCount);
          result = "Game created: " + gameId;
        }
      }
    }
    return result;
  }

  private String validatePlayersAndCollectSessions(
      final String[] playerIds, final List<ClientSession> sessions) {
    String validationResult = "";
    final java.util.Set<String> seenIds = new java.util.HashSet<>();

    for (final String playerId : playerIds) {
      if (!seenIds.add(playerId)) {
        validationResult = "ERROR duplicate player";
        break;
      }

      final ClientSession player = clients.get(playerId);
      if (player == null) {
        validationResult = "ERROR player not found";
        break;
      }

      final PlayerStatus playerStatus = getPlayerStatus(player);
      if (playerStatus != PlayerStatus.IDLE) {
        validationResult = "ERROR player not available";
        break;
      }

      sessions.add(player);
    }

    return validationResult;
  }

  /** Stops the server and closes all active client connections. */
  public void stop() {
    running.set(false);

    try {
      if (broadcaster != null) {
        broadcaster.stop();
      }

      if (broadcastThread != null && broadcastThread.isAlive()) {
        broadcastThread.interrupt();
      }

      synchronized (lock) {
        for (final ClientSession client : clients.values()) {
          client.send("SERVER_STOPPED");
          client.close();
        }
        clients.clear();
        games.clear();
      }

      if (serverSocket != null) {
        serverSocket.close();
      }

    } catch (IOException ignored) {
    }

    Logger.getInstance().info("Server stopped.");
  }

  /** Disconnects every client that has been inactive for more than one minute. */
  private void startTimeoutChecker() {
    final Thread timeoutThread =
        new Thread(
            () -> {
              while (running.get()) {
                final List<ClientSession> expiredClients = collectExpiredClients();

                for (final ClientSession client : expiredClients) {
                  if (Logger.getInstance().isInfoEnabled()) {
                    Logger.getInstance().info(I18n.get("serverClientTimeout", client.getId()));
                  }
                  client.send("ERROR timeout");
                  client.close();
                  unregisterClient(client);
                }

                try {
                  Thread.sleep(TIMEOUT_CHECK_MS);
                } catch (InterruptedException ignored) {
                  Thread.currentThread().interrupt();
                  return;
                }
              }
            },
            "TimeoutChecker");
    timeoutThread.setDaemon(true);
    timeoutThread.start();
  }

  private List<ClientSession> collectExpiredClients() {
    final List<ClientSession> expiredClients = new ArrayList<>();
    synchronized (lock) {
      final long now = System.currentTimeMillis();
      for (final ClientSession client : clients.values()) {
        if (now - client.getLastPing() > CLIENT_TIMEOUT_MS) {
          expiredClients.add(client);
        }
      }
    }
    return expiredClients;
  }

  private BufferedReader getClientReader(final ClientSession client) {
    return client.getIn();
  }

  private PlayerStatus getPlayerStatus(final ClientSession player) {
    return player.getStatus();
  }
}
