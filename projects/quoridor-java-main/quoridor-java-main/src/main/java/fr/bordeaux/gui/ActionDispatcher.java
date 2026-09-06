package fr.bordeaux.gui;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.Color;
import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.HumanPlayer;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import fr.bordeaux.main.PlayerFactory;
import fr.bordeaux.network.GameClient;
import fr.bordeaux.network.MultiGameServer;
import fr.bordeaux.network.NetworkMoveParser;
import fr.bordeaux.persistence.GameLoader;
import fr.bordeaux.persistence.GameSaver;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

/** Bridge between GUI events and underlying game logic or dialogs. */
public class ActionDispatcher {

  /** Common title used by network-related dialogs. */
  private static final String NETWORK_TITLE = "Network";

  /** Number of host/port tokens when an explicit port is provided. */
  private static final int HOST_PORT_PARTS = 2;

  /** Prefix used by the server to announce the opponent move. */
  private static final String OPP_MOVE_PREFIX = "OPPONENT_MOVE ";

  /** Prefix used by the server to announce a network game start. */
  private static final String GAME_START_PREFIX = "GAME_START ";

  /** Reference to the game engine to execute game-related actions */
  private final GameEngine engine;

  /** The file handler for loading and saving games. */
  private final FileHandler fileHandler;

  /** The configuration manager for game settings. */
  private final ConfigManager configManager = ConfigManager.getInstance();

  /** The server instance for multiplayer games. */
  private final MultiGameServer multiServer = new MultiGameServer();

  /** The client instance for multiplayer games. */
  private final GameClient networkClient = new GameClient();

  /** Board view used to refresh the GUI after network messages. */
  private BoardView boardView;

  /** Last move sent to the server and still waiting for validation. */
  private String pendingMove;

  /** True when the GUI is currently playing a network game. */
  private boolean networkGame;

  /** True when the local player can play now. */
  private boolean myTurn;

  /** Local player name sent to the server. */
  private String localPlayerName = "Player";

  /** Callback and logic for hint calculation. */
  private Runnable hintHandler;

  /** Default TCP port for network games. */
  private static final int DEFAULT_PORT = 12_345;

  /** Number of milliseconds in a second. */
  private static final long MS_PER_SEC = 1000L;

  /** Number of seconds in a minute. */
  private static final int SEC_PER_MIN = 60;

  /**
   * Constructs an ActionDispatcher with a given GameEngine.
   *
   * @param engine The GameEngine instance to control the game.
   * @param fileHandler The FileHandler used to save and load.
   */
  public ActionDispatcher(final GameEngine engine, final FileHandler fileHandler) {
    this.engine = engine;
    this.fileHandler = fileHandler;
    this.networkClient.setServerMessageHandler(this::handleServerMessage);
  }

  /**
   * Registers a handler for the HINT action.
   *
   * @param hintHandler The handler to execute when HINT is dispatched.
   */
  public void setHintHandler(final Runnable hintHandler) {
    this.hintHandler = hintHandler;
  }

  /**
   * Dispatches an action. May call GameEngine (new, load, save, undo, redo, pause, hint), show
   * dialogs, or exit.
   *
   * @param action The Action representing the user command.
   * @throws IOException If an I/O error occurs.
   */
  public void dispatch(final Action action) throws IOException {
    switch (action) {
      case NEW_GAME -> handleNewGame();
      case LOAD_GAME -> handleLoadGame();
      case SAVE_GAME -> handleSaveGame();
      case CONFIGURATION -> ConfigDialog.show();
      case INFO -> InfoDialog.show();
      case REDO -> engine.redo();
      case UNDO -> engine.undo();
      case PAUSE -> engine.pause();
      case HINT -> {
        if (hintHandler != null) {
          hintHandler.run();
        }
      }
      case QUIT -> Platform.exit();
      default -> handleNetworkAction(action);
    }
  }

  private void handleLoadGame() throws IOException {
    final String path = fileHandler.chooseLoadFile();
    if (path != null) {
      try {
        if (engine.getBlitzManager() != null) {
          engine.getBlitzManager().pause();
        }
        final fr.bordeaux.core.GameState loadedState = GameLoader.loadGame(engine, path);
        engine.getState().loadState(loadedState);
        engine.initializeBlitz();
      } catch (final Exception e) {
        showMessage("Load Error", "Error loading file", e.getMessage());
      }
    }
  }

  private void handleSaveGame() throws IOException {
    final String path = fileHandler.chooseSaveFile();
    if (path != null) {
      try {
        GameSaver.saveGame(engine, path);
      } catch (final IOException e) {
        showMessage("Save Error", "Error saving file", e.getMessage());
      }
    }
  }

  private void handleNetworkAction(final Action action) {
    switch (action) {
      case START_SERVER -> startServer();
      case STOP_SERVER -> {
        multiServer.stop();
        showMessage(NETWORK_TITLE, "Server", "Server stopped.");
      }
      case SERVER_LIST -> showMessage("Network", "Server List", networkClient.serverList());
      case SERVER_STATUS ->
          showMessage(NETWORK_TITLE, "Server Status", multiServer.getServerStatus());
      case PLAYERS -> showMessage(NETWORK_TITLE, "Connected Players", multiServer.listPlayers());
      case SCOREBOARD -> showMessage(NETWORK_TITLE, "Scoreboard", multiServer.getScoreboard());
      case NEW_NETWORK_GAME -> createNetworkGame();
      case JOIN_SERVER -> joinServer();
      case SET_NETWORK_NAME -> setNetworkName();
      case PING_SERVER -> showMessage(NETWORK_TITLE, "Ping Server", networkClient.ping());
      case QUIT_SERVER -> showMessage(NETWORK_TITLE, "Quit Server", networkClient.quit());
      default -> {}
    }
  }

  private void handleNewGame() {
    final int size = configManager.getInt("board-size", 9);
    final List<Player> players = PlayerFactory.createPlayers(configManager);
    final boolean isBlitz = configManager.getBoolean("blitz", false);
    final Long timeout;
    if (isBlitz) {
      final int timeoutMinutes = configManager.getInt("timeout", 30);
      timeout = (long) timeoutMinutes * SEC_PER_MIN * MS_PER_SEC;
    } else {
      timeout = null;
    }
    networkGame = false;
    myTurn = false;
    clearPendingMove();
    engine.newGame(size, players, timeout);
  }

  private void startServer() {
    final String input =
        promptUser(
            "Start Server", "Server Port", "Enter the TCP port:", String.valueOf(DEFAULT_PORT));
    if (input == null) {
      return;
    }

    final int port = Integer.parseInt(input.trim());
    multiServer.start(port);
    showMessage("Network", "Server", "Server started on port " + port + ".");
  }

  /** Prompts user to specify player IDs and starts a new network game. */
  protected void createNetworkGame() {
    final String input =
        promptUser(
            "New Network Game",
            "Player IDs",
            "Enter 2 or 4 player IDs separated by spaces:",
            "C1 C2");
    if (input == null || input.isBlank()) {
      return;
    }

    final String[] ids = input.trim().split("\\s+");
    showMessage("Network", "New Game", multiServer.createGame(ids));
  }

  /** Prompts user for a server address and attempts to join. */
  protected void joinServer() {
    final String input =
        promptUser(
            "Join Server", "Host and Port", "Enter host[:port]:", "localhost:" + DEFAULT_PORT);
    if (input == null || input.isBlank()) {
      return;
    }

    final String[] hostPort = input.trim().split(":");
    final String host = hostPort[0];
    final int port;
    if (hostPort.length == HOST_PORT_PARTS) {
      port = Integer.parseInt(hostPort[1]);
    } else {
      port = DEFAULT_PORT;
    }

    if (networkClient.join(host, port)) {
      showMessage(NETWORK_TITLE, "Join Server", "Connected to " + host + ":" + port + ".");
    } else {
      showMessage(NETWORK_TITLE, "Join Server", "Failed to connect to " + host + ":" + port + ".");
    }
  }

  /** Prompts user for a player name and sends it to the server. */
  protected void setNetworkName() {
    final String input =
        promptUser("Set Player Name", "Player Name", "Enter your player name:", "");
    if (input == null || input.isBlank()) {
      return;
    }

    localPlayerName = input.trim();
    networkClient.hello(localPlayerName);
    showMessage(NETWORK_TITLE, "Player Name", "Name sent to server.");
  }

  /**
   * Registers the board view so the dispatcher can refresh it after network updates.
   *
   * @param boardView the board view used by the GUI
   */
  public void setBoardView(final BoardView boardView) {
    this.boardView = boardView;
  }

  /**
   * Indicates whether a network game is currently active.
   *
   * @return true if the current game is a network game
   */
  public boolean isNetworkGame() {
    return networkGame;
  }

  /**
   * Indicates whether the local player can play now.
   *
   * @return true if it is the local player's turn
   */
  public boolean isMyTurn() {
    return myTurn;
  }

  /**
   * Plays a move from the GUI board. In local mode the move is applied directly. In network mode
   * the move is sent to the server.
   *
   * @param move move chosen on the board
   */
  public void playBoardMove(final Move move) {
    if (networkGame) {
      playNetworkBoardMove(move);
    } else {
      if (engine.play(move) && boardView != null) {
        boardView.drawBoard();
      }
    }
  }

  /**
   * Handles messages received from the network server.
   *
   * @param message server message
   */
  private void handleServerMessage(final String message) {
    Platform.runLater(
        () -> {
          if (message.startsWith(GAME_START_PREFIX)) {
            final boolean startsNow = message.contains("YOUR_TURN");
            networkGame = true;
            myTurn = startsNow;
            clearPendingMove();
            setupNetworkBoard(startsNow);
            showMessage(NETWORK_TITLE, "Game Start", startsNow ? "Your turn." : "Opponent's turn.");
          } else if ("OK".equals(message) && pendingMove != null) {
            applyNetworkMove(pendingMove);
            clearPendingMove();
            myTurn = false;
          } else if (message.startsWith(OPP_MOVE_PREFIX)) {
            final String moveText = message.substring(OPP_MOVE_PREFIX.length()).trim();
            applyNetworkMove(moveText);
            myTurn = true;
          } else if (message.startsWith("ERROR ")) {
            clearPendingMove();
            showMessage(NETWORK_TITLE, "Server Error", message);
          } else if (message.startsWith("GAME_OVER ")) {
            clearPendingMove();
            myTurn = false;
            networkGame = false;
          } else if ("SERVER_STOPPED".equals(message)) {
            clearPendingMove();
            myTurn = false;
            networkGame = false;
            showMessage(NETWORK_TITLE, "Server", "Server stopped.");
          }
        });
  }

  /**
   * Creates a fresh local board that matches the network game start. This minimal version is
   * designed for a two-player network game.
   *
   * @param startsNow true if the local player starts first
   */
  private void setupNetworkBoard(final boolean startsNow) {
    final int size = engine.getState().getBoard().getSize();

    final List<Player> players;
    if (startsNow) {
      players =
          List.of(
              new HumanPlayer(localPlayerName, new Position(0, 4), Color.WHITE),
              new HumanPlayer("Opponent", new Position(8, 4), Color.BLACK));
    } else {
      players =
          List.of(
              new HumanPlayer("Opponent", new Position(0, 4), Color.WHITE),
              new HumanPlayer(localPlayerName, new Position(8, 4), Color.BLACK));
    }

    engine.newGame(size, players, null);

    if (boardView != null) {
      boardView.drawBoard();
    }
  }

  /**
   * Applies a validated network move to the local engine.
   *
   * @param moveText move text received from the server
   */
  private void applyNetworkMove(final String moveText) {
    final Move move = NetworkMoveParser.parseMove(moveText);
    if (engine.play(move) && boardView != null) {
      boardView.drawBoard();
    }
  }

  /**
   * Converts a move into the text format used by the network protocol.
   *
   * @param move move to convert
   * @return network text for the move
   */
  private String moveToText(final Move move) {
    final String moveText;
    if (move.isPawn()) {
      moveText = positionToText(move.getFrom()) + "-" + positionToText(move.getTo());
    } else {
      final char orientationLetter = move.getOrientation().name().charAt(0);
      moveText = positionToText(move.getTo()) + Character.toLowerCase(orientationLetter);
    }
    return moveText;
  }

  /**
   * Converts a board position into network text.
   *
   * @param position board position
   * @return text representation of the position
   */
  private String positionToText(final Position position) {
    final char column = (char) ('a' + position.getY());
    final int row = position.getX() + 1;
    return String.valueOf(column) + row;
  }

  /**
   * Prompts user for text input via a dialog.
   *
   * @param title dialog title
   * @param header header text
   * @param content content text
   * @param defaultValue default value
   * @return user input, or null if cancelled
   */
  protected String promptUser(
      final String title, final String header, final String content, final String defaultValue) {
    final TextInputDialog dialog = new TextInputDialog(defaultValue);
    dialog.setTitle(title);
    dialog.setHeaderText(header);
    dialog.setContentText(content);
    final Optional<String> result = dialog.showAndWait();
    return result.orElse(null);
  }

  /**
   * Displays an information message to the user using a JavaFX Alert.
   *
   * @param title the title of the alert
   * @param header the header text of the alert
   * @param content the content text (description) of the alert
   */
  protected void showMessage(final String title, final String header, final String content) {
    final Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.show();
  }

  /** Sends a move to the server when the current network state allows it. */
  private void playNetworkBoardMove(final Move move) {
    if (!myTurn) {
      showMessage(NETWORK_TITLE, "Turn", "It is not your turn.");
    } else if (pendingMove != null) {
      showMessage(NETWORK_TITLE, "Move", "A move is already waiting for validation.");
    } else {
      pendingMove = moveToText(move);
      networkClient.sendMove(pendingMove);
    }
  }

  /** Clears the move waiting for server validation. */
  private void clearPendingMove() {
    pendingMove = null;
  }
}
