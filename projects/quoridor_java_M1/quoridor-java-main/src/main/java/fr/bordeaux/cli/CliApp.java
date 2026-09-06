package fr.bordeaux.cli;

import fr.bordeaux.cli.command.*;
import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.network.GameClient;
import fr.bordeaux.network.MultiGameServer;
import fr.bordeaux.util.Logger;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.jline.reader.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/** Manages Quoridor CLI loop and commands. */
public class CliApp {

  /** Network move command literal. */
  private static final String CMD_MOVE = "move";

  /** Help command literal. */
  private static final String CMD_HELP = "help";

  /** Quit command literal. */
  private static final String CMD_QUIT = "quit";

  /** New game command literal. */
  private static final String CMD_NEW = "new";

  /** Load game command literal. */
  private static final String CMD_LOAD = "load";

  /** Save game command literal. */
  private static final String CMD_SAVE = "save";

  /** Pause command literal. */
  private static final String CMD_PAUSE = "pause";

  /** Hint command literal. */
  private static final String CMD_HINT = "hint";

  /** Undo command literal. */
  private static final String CMD_UNDO = "undo";

  /** Redo command literal. */
  private static final String CMD_REDO = "redo";

  /** Show command literal. */
  private static final String CMD_SHOW = "show";

  /** Set command literal. */
  private static final String CMD_SET = "set";

  /** Server command literal. */
  private static final String CMD_SERVER = "server";

  /** Join command literal. */
  private static final String CMD_JOIN = "join";

  /** Ping command literal. */
  private static final String CMD_PING = "ping";

  /** Players command literal. */
  private static final String CMD_PLAYERS = "players";

  /** Scoreboard command literal. */
  private static final String CMD_SCOREBOARD = "scoreboard";

  /** Hello command literal. */
  private static final String CMD_HELLO = "hello";

  /** Default host used when the CLI connects to a network server. */
  private static final String DEFAULT_HOST = "localhost";

  /** Default port used when the CLI connects to a network server. */
  private static final int DEFAULT_PORT = 12_345;

  /** Board option literal. */
  private static final String OPT_BOARD = "board";

  /** Literals to avoid duplication. */
  private static final String STR_FALSE = "false";

  /** Constant representing a single unique match for command completion. */
  private static final int UNIQUE_MATCH = 1;

  /** The core engine managing the game logic, board state, and Quoridor rules. */
  private GameEngine engine;

  /** System terminal. */
  private Terminal terminal;

  /** The line reader responsible for text editing */
  private LineReader reader;

  /** State flag that controls the main execution loop of the CLI application. */
  private boolean running = true;

  /** Internal state used to track the "Tab" key behavior */
  private boolean firstTab = true;

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /** A registry mapping command names */
  private Map<String, Function<String[], Command>> commandRegistry;

  /** Local network server controlled from the CLI. */
  private final MultiGameServer networkServer;

  /** TCP client controlled from the CLI. */
  private final GameClient networkClient;

  /** Last move sent to the server and waiting for confirmation. */
  private String pendingMove;

  /**
   * Creates a CLI application with a specific game engine, terminal and reader.
   *
   * @param engine the game engine used to run the game
   * @param terminal the terminal used for input/output
   * @param reader the line reader used to read user commands
   */
  public CliApp(final GameEngine engine, final Terminal terminal, final LineReader reader) {
    this(engine, terminal, reader, new MultiGameServer(), new GameClient());
  }

  /* default */ CliApp(
      final GameEngine engine,
      final Terminal terminal,
      final LineReader reader,
      final MultiGameServer networkServer,
      final GameClient networkClient) {
    this.engine = engine;
    this.terminal = terminal;
    this.reader = reader;
    this.networkServer = networkServer;
    this.networkClient = networkClient;
    this.networkClient.setServerMessageHandler(this::handleServerMessage);
    commandRegistry = new HashMap<>();

    registerAllCommands();
  }

  /** Initialises the command registry to avoid code duplication in constructors. */
  private void registerAllCommands() {
    commandRegistry.put(CMD_HELP, tokens -> new HelpCommand(tokens, commandRegistry));
    commandRegistry.put(
        CMD_QUIT,
        tokens -> {
          this.running = false;
          return new QuitCommand(reader);
        });
    commandRegistry.put(CMD_NEW, tokens -> new NewCommand(tokens, terminal, reader));
    commandRegistry.put(CMD_LOAD, LoadCommand::new);
    commandRegistry.put(CMD_SAVE, SaveCommand::new);
    commandRegistry.put(CMD_PAUSE, tokens -> new PauseCommand());
    commandRegistry.put(CMD_HINT, tokens -> new HintCommand());
    commandRegistry.put(CMD_UNDO, UndoCommand::new);
    commandRegistry.put(CMD_REDO, RedoCommand::new);
    commandRegistry.put(CMD_SHOW, ShowCommand::new);
    commandRegistry.put(CMD_SET, tokens -> new SetCommand());
  }

  /**
   * Creates a CLI application using the provided game engine.
   *
   * @param engine the game engine used to run the game
   * @throws IOException if the terminal cannot be initialized
   */
  public CliApp(final GameEngine engine) throws IOException {
    // Terminal initialisation
    this.terminal = TerminalBuilder.builder().system(true).build();
    this.engine = engine;
    this.running = true;
    this.networkServer = new MultiGameServer();
    this.networkClient = new GameClient();
    this.networkClient.setServerMessageHandler(this::handleServerMessage);

    // Create smart completer
    final Completer smartCompleter = new SmartCompleter();

    // Create the LineReader with the smart Completer
    this.reader =
        LineReaderBuilder.builder().terminal(this.terminal).completer(smartCompleter).build();

    // init the command register
    this.commandRegistry = new HashMap<>();
    registerAllCommands();
  }

  /**
   * Creates a CLI application with a default game configuration.
   *
   * @throws IOException if the terminal setup fails
   */
  public CliApp() throws IOException {
    this(
        new GameEngine(
            new GameState(
                new Board(9),
                List.of(
                    new HumanPlayer("Human", new Position(0, 4), Color.WHITE),
                    new HumanPlayer("Bob", new Position(8, 4), Color.BLACK))),
            Boolean.parseBoolean(ConfigManager.getInstance().getOption("blitz", STR_FALSE)),
            Boolean.parseBoolean(ConfigManager.getInstance().getOption("verbose", STR_FALSE)),
            Boolean.parseBoolean(ConfigManager.getInstance().getOption("contest", STR_FALSE)),
            Boolean.parseBoolean(ConfigManager.getInstance().getOption("debug", STR_FALSE)),
            Integer.parseInt(ConfigManager.getInstance().getOption("nb-walls", "10"))));
  }

  /** Triggers an audible alert (beep) through the terminal. */
  private void beep() {
    if (this.terminal != null) {
      this.terminal.writer().print("\007");
      this.terminal.flush();
    }
  }

  /**
   * Indicates whether the CLI application is still running.
   *
   * @return true if the application loop is active, false otherwise
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Returns the current game engine used by the CLI.
   *
   * @return the current GameEngine instance
   */
  public GameEngine getEngine() {
    return engine;
  }

  /**
   * Starts the CLI loop. Displays the board, reads commands until the application is stopped.
   *
   * @throws IOException Possible if mcts with NN selection used
   */
  public void run() throws IOException {
    if (engine != null) {
      final ShowCommand show = new ShowCommand(new String[] {CMD_SHOW, OPT_BOARD});
      show.execute(engine);
    }
    while (running) {
      final GameState state = engine.getState();
      final Player currentPlayer = state.getCurrentPlayer();
      if (currentPlayer.isAI()) {
        if (terminal != null && terminal.writer() != null) {
          terminal.writer().println("\n" + currentPlayer.getName() + " " + I18n.get("thinking"));
          terminal.flush();
        }
        executeAiTurn();
        new ShowCommand(new String[] {CMD_SHOW, OPT_BOARD}).execute(engine);
      } else {
        final String line = reader.readLine(">> ");
        fetch(line);
      }
      if (state.isGameOver() && !networkClient.isConnected()) {
        running = false;
      }
    }
    try {
      if (terminal != null) {
        terminal.close();
      }
    } catch (IOException exception) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("brutalClose"));
      }
    }
  }

  /* default */ void executeAiTurn() {
    final Player currentAi = engine.getState().getCurrentPlayer();
    final Move move = currentAi.getNextMove(engine.getState());
    engine.play(move);

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}{1}", I18n.get("iaPlayed"), move);
    }
  }

  /**
   * Parses user input and executes the command. If unrecognized, treats as a move. it is
   * interpreted as a move.
   *
   * @param input the raw command entered by the user
   * @throws IOException Possible if mcts with NN selection used
   */
  public void fetch(final String input) throws IOException {
    firstTab = true;
    final String[] tokens = input.trim().split("\\s+");
    boolean shouldExecute = tokens.length != 0 && !tokens[0].isEmpty();

    if (shouldExecute) {
      final String command = tokens[0].toLowerCase(Locale.US);
      shouldExecute = !handleNetworkCommand(input, tokens, command);
      if (shouldExecute) {
        final Command cmd;

        if (commandRegistry.containsKey(command)) {
          cmd = commandRegistry.get(command).apply(tokens);
        } else {
          String suggestion = null;
          int bestDistance = 3; // Only suggest if the mistake is small (1 or 2 chars)

          for (final String registered : commandRegistry.keySet()) {
            final int dist = calculateDistance(command, registered);
            if (dist < bestDistance) {
              bestDistance = dist;
              suggestion = registered;
            }
          }

          if (suggestion != null) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("{0}'{1}'", I18n.get("unknownCmd"), command);
            }
            if (LOGGER.isInfoEnabled()) {
              LOGGER.info("{0}{1} ? ", I18n.get("meant"), suggestion);
            }
          }

          // Default behavior (interpret as move)
          cmd = new MoveCommand(input);
        }
        engine = cmd.execute(engine);
      }
    }
  }

  private int calculateDistance(final String userCmd, final String command) {
    final int userLen = userCmd.length();
    final int cmdLen = command.length();
    final int[][] distance = new int[userLen + 1][cmdLen + 1];
    for (int i = 0; i <= userLen; i++) {
      for (int j = 0; j <= cmdLen; j++) {
        if (i == 0) {
          distance[i][j] = j;
        } else if (j == 0) {
          distance[i][j] = i;
        } else {
          final int substitutionCost = userCmd.charAt(i - 1) == command.charAt(j - 1) ? 0 : 1;
          distance[i][j] =
              Math.min(
                  distance[i - 1][j - 1] + substitutionCost,
                  Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1));
        }
      }
    }
    return distance[userLen][cmdLen];
  }

  private boolean handleNetworkCommand(
      final String input, final String[] tokens, final String command) {
    boolean handled = true;
    try {
      switch (command) {
        case CMD_SERVER -> handleServerCommand(tokens);
        case CMD_JOIN -> handleJoinCommand(tokens);
        case CMD_PING -> networkClient.ping();
        case CMD_PLAYERS -> LOGGER.raw(networkServer.listPlayers());
        case CMD_SCOREBOARD -> LOGGER.raw(networkServer.getScoreboard());
        case CMD_HELLO -> handleHelloCommand(input);
        case CMD_NEW -> {
          if (tokens.length > 1 && looksLikePlayerId(tokens[1])) {
            handleNetworkNewCommand(tokens);
          } else {
            handled = false;
          }
        }
        case CMD_MOVE -> {
          if (networkClient.isConnected() && tokens.length > 1) {
            pendingMove = input.substring(CMD_MOVE.length() + 1).trim();
            networkClient.sendMove(pendingMove);
          } else {
            handled = false;
          }
        }
        case CMD_QUIT -> {
          if (!networkClient.isConnected()) {
            handled = false;
          } else {
            networkClient.quit();
            if (LOGGER.isInfoEnabled()) {
              LOGGER.info(I18n.get("returnedLocal"));
            }
          }
        }
        default -> {
          if (networkClient.isConnected()) {
            pendingMove = input;
            networkClient.sendMove(pendingMove);
          } else {
            handled = false;
          }
        }
      }
    } catch (final IllegalArgumentException exception) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0}{1}", I18n.get("errorPrefix"), exception.getMessage());
      }
      handled = true;
    }
    return handled;
  }

  /**
   * Handles server management subcommands from the CLI.
   *
   * @param tokens the command tokens, starting with "server"
   */
  private void handleServerCommand(final String[] tokens) {
    if (tokens.length < 2) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0} {1}", I18n.get("usage"), I18n.get("serverStartUsage"));
      }
      return;
    }

    switch (tokens[1].toLowerCase(Locale.US)) {
      case "start" -> {
        final int port = tokens.length > 2 ? Integer.parseInt(tokens[2]) : DEFAULT_PORT;
        networkServer.start(port);
      }
      case "stop" -> networkServer.stop();
      case "list" -> LOGGER.info(networkClient.serverList());
      case "status" -> LOGGER.raw(networkServer.getServerStatus());
      default -> LOGGER.raw(I18n.get("unknownServerCmd"));
    }
  }

  /**
   * Handles the join command and connects the client to a remote server.
   *
   * @param tokens the command tokens, optionally containing an IP address and port
   */
  private void handleJoinCommand(final String[] tokens) {
    final String target = tokens.length > 1 ? tokens[1] : DEFAULT_HOST;
    final String[] hostPort = target.split(":");
    final String host = hostPort[0].isBlank() ? DEFAULT_HOST : hostPort[0];
    final int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : DEFAULT_PORT;

    if (!networkClient.join(host, port)) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0}{1}", I18n.get("failedConnect", host, ":"), port);
      }
    }
  }

  /**
   * Handles the hello command and sends the local player name to the server.
   *
   * @param input the raw command entered by the user
   */
  private void handleHelloCommand(final String input) {
    final String name = input.substring(CMD_HELLO.length()).trim();
    if (name.isEmpty()) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0} {1}", I18n.get("usage"), I18n.get("helloUsage"));
      }
    } else {
      networkClient.hello(name);
    }
  }

  /**
   * Handles the network new command by creating a game between connected player IDs.
   *
   * @param tokens the command tokens containing the player identifiers
   */
  private void handleNetworkNewCommand(final String[] tokens) {
    if (tokens.length < 3 || tokens.length > 5) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0} {1}", I18n.get("usage"), I18n.get("netNewUsage"));
      }
      return;
    }

    final String[] playerIds = new String[tokens.length - 1];
    System.arraycopy(tokens, 1, playerIds, 0, playerIds.length);
    final String result = networkServer.createGame(playerIds);
    LOGGER.raw(result);
  }

  /**
   * Checks whether a token looks like a server-side player identifier.
   *
   * @param value the token to check
   * @return true if the token matches the expected player ID format, false otherwise
   */
  private boolean looksLikePlayerId(final String value) {
    return value.toUpperCase(Locale.US).matches("C\\d+");
  }

  /**
   * Handles server messages and keeps the local CLI board synchronized with network moves.
   *
   * @param message message received from the server
   */
  private void handleServerMessage(final String message) {
    synchronized (this) {
      if ("OK".equals(message) && pendingMove != null) {
        engine = new MoveCommand(pendingMove).execute(engine);
        clearPendingMove();
      } else if (message.startsWith("OPPONENT_MOVE ")) {
        final String move = message.substring("OPPONENT_MOVE ".length()).trim();
        engine = new MoveCommand(move).execute(engine);
      } else if (message.startsWith("ERROR ")) {
        clearPendingMove();
      }
    }
  }

  /** Package-private inner class to handle smart tab-completion for the CLI. */
  /* default */ class SmartCompleter implements Completer {
    @Override
    public void complete(
        final LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
      final String buffer = line.line();
      // Finding the current token
      final String[] tokens = buffer.trim().split("\\s+");

      if (buffer.isEmpty()) {
        firstTab = true;
        return;
      }

      final List<String> mainCommands =
          List.of(
              CMD_HELP, CMD_QUIT, CMD_NEW, CMD_SAVE, CMD_LOAD, CMD_SHOW, CMD_HINT, CMD_PAUSE,
              CMD_UNDO, CMD_REDO, CMD_SET);
      final List<String> showOptions = List.of(OPT_BOARD, "time", "configurations", "history");
      final List<String> helpOptions =
          List.of(
              CMD_HELP,
              CMD_HINT,
              CMD_LOAD,
              CMD_SAVE,
              CMD_QUIT,
              CMD_PAUSE,
              CMD_REDO,
              CMD_UNDO,
              CMD_SET,
              CMD_SHOW + " " + OPT_BOARD,
              CMD_MOVE,
              CMD_SHOW + " time",
              CMD_SHOW + " configurations",
              CMD_SHOW + " history");

      final List<String> matches;

      if (tokens.length >= 1 && CMD_SHOW.equals(tokens[0])) {
        final String subToken = (tokens.length == 2) ? tokens[1] : "";
        matches = showOptions.stream().filter(opt -> opt.startsWith(subToken)).toList();
      } else if (tokens.length >= 1 && CMD_HELP.equals(tokens[0])) {
        final String subToken = (tokens.length == 2) ? tokens[1] : "";
        matches = helpOptions.stream().filter(opt -> opt.startsWith(subToken)).toList();
      } else {
        matches = mainCommands.stream().filter(cmd -> cmd.startsWith(tokens[0])).toList();
      }

      // Three cases

      if (matches.isEmpty()) {
        // No match -> Bip
        beep();
      } else if (matches.size() == UNIQUE_MATCH) {
        // Only one match -> Completion
        candidates.add(new Candidate(matches.get(0)));
        firstTab = true;
      } else {
        // Several matches
        if (firstTab) {
          // First Tab : bip to indicate multiple choices
          beep();
          firstTab = false;
        } else {
          // Second Tab : show candidates
          for (final String match : matches) {
            candidates.add(new Candidate(match));
          }
          firstTab = true;
        }
      }
    }
  }

  /** Clears the move waiting for a network validation. */
  private void clearPendingMove() {
    pendingMove = null;
  }
}
