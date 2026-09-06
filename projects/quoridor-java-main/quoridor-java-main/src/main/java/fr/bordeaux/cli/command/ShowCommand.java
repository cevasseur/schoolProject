package fr.bordeaux.cli.command;

import fr.bordeaux.core.Board;
import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Orientation;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import fr.bordeaux.util.TimeConvertor;
import java.io.PrintStream;
import java.util.List;

/** CLI command that displays game information, board state, or history. */
public class ShowCommand implements Command {

  /** Configuration translation key reused in the output. */
  private static final String CONFIG_LINE_KEY = "configLine";

  /** Header prefix used by help sections. */
  private static final String HELP_HEADER_PREFIX = "=== ";

  /** Usage translation key reused by help sections. */
  private static final String USAGE_KEY = "usage";

  /** Description translation key reused by help sections. */
  private static final String DESCRIPTION_KEY = "description";

  /** Shared translation key for show section headers. */
  private static final String SHOW_HEADER_KEY = "showHeader";

  /** Command entered by the user */
  private String[] tokens;

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /** Where the show command will write */
  private static final PrintStream WRITER = System.out;

  /** Creates a ShowCommand with the default option "board". */
  public ShowCommand() {
    this(new String[] {"show", "board"});
  }

  /**
   * Creates a ShowCommand using the provided CLI tokens.
   *
   * @param tokens the tokens entered by the user
   */
  public ShowCommand(final String... tokens) {
    this.tokens = tokens.clone();
  }

  /**
   * Creates a ShowCommand with a specific option.
   *
   * @param optionString the option specifying what should be displayed
   */
  public ShowCommand(final String optionString) {
    this(new String[] {"show", optionString});
  }

  /**
   * @return unchanged engine.
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    final String option = tokens[1];
    final GameState state = engine.getState();
    switch (option) {
      case "board":
        {
          printBoard(engine);
          break;
        }
      case "configurations":
      case "config":
        {
          WRITER.println(I18n.get(CONFIG_LINE_KEY, "verbose", engine.isVerbose()));
          WRITER.println(I18n.get(CONFIG_LINE_KEY, "blitz", engine.isBlitz()));
          WRITER.println(I18n.get(CONFIG_LINE_KEY, "contest", engine.isContest()));
          WRITER.println(I18n.get(CONFIG_LINE_KEY, "debug", engine.isDebug()));
          WRITER.println(I18n.get(CONFIG_LINE_KEY, "nb-walls", engine.getInitialWalls()));
          WRITER.println(I18n.get(CONFIG_LINE_KEY, "nb-players", state.getPlayers().size()));
          break;
        }
      case "time":
        {
          final List<Player> players = state.getPlayers();
          final Player currentPlayer = state.getCurrentPlayer();
          for (final Player player : players) {
            if (player.equals(currentPlayer)) {
              WRITER.println(
                  I18n.get("timeLeft") + TimeConvertor.formatTime(player.getRemainingTime()));
            } else {
              WRITER.println(
                  I18n.get(
                      "timeFor",
                      player.getName(),
                      " ---> ",
                      TimeConvertor.formatTime(player.getRemainingTime())));
            }
          }

          break;
        }
      case "history":
        {
          showHistory(engine);
          break;
        }
      default:
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error(I18n.get("showNot", tokens[1], I18n.get("notExist")));
        }
        break;
    }
    return engine;
  }

  /** Displays general help information for the show command. */
  @Override
  public void helpText() {
    boolean helpDisplayed = false;
    if (tokens != null && tokens.length > 1) {
      final String subOption = tokens[1];
      switch (subOption) {
        case "board":
          helpBoard();
          helpDisplayed = true;
          break;
        case "history":
          helpHistory();
          helpDisplayed = true;
          break;
        case "time":
          helpTime();
          helpDisplayed = true;
          break;
        case "configurations":
        case "config":
          helpConfig();
          helpDisplayed = true;
          break;
        default:
          break;
      }
    }

    if (!helpDisplayed) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
            HELP_HEADER_PREFIX
                + I18n.get("showHelpHeader")
                + " "
                + I18n.get("cliTitle")
                + " ===\n");
        LOGGER.info(I18n.get(USAGE_KEY));
        LOGGER.info("  {0}", I18n.get("showBoardUsage"));
        LOGGER.info("  {0}", I18n.get("showHistoUsage"));
        LOGGER.info("  {0}", I18n.get("showConfigUsage"));
        LOGGER.info("  {0}\n", I18n.get("showTimeUsage"));
        LOGGER.info(I18n.get(DESCRIPTION_KEY));
        LOGGER.info("  {0}", I18n.get("showDesc"));
      }
      LOGGER.verbose("  {0}", I18n.get("showVerb"));
      LOGGER.verbose(I18n.get("arguments"));
      LOGGER.verbose("  board         : {0}", I18n.get("showVBoard"));
      LOGGER.verbose("  history       : {0}", I18n.get("showVHisto"));
      LOGGER.verbose("  configuration : {0}", I18n.get("showVConfig"));
      LOGGER.verbose("  time          : {0}", I18n.get("showVTime"));
      LOGGER.verbose(I18n.get("showVMore"));
    }
  }

  /** Displays help information for the "show board" option. */
  public void helpBoard() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}{1} BOARD ===\n", HELP_HEADER_PREFIX, I18n.get(SHOW_HEADER_KEY));
      LOGGER.info(I18n.get(USAGE_KEY));
      LOGGER.info("  {0}\n", I18n.get("showBoardUsage"));
      LOGGER.info(I18n.get(DESCRIPTION_KEY));
      LOGGER.info("  {0}", I18n.get("showDescBoard"));
    }
  }

  /** Displays help information for the "show history" option. */
  public void helpHistory() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}{1} HISTORY ===\n", HELP_HEADER_PREFIX, I18n.get(SHOW_HEADER_KEY));
      LOGGER.info(I18n.get(USAGE_KEY));
      LOGGER.info("  {0}\n", I18n.get("showHistoUsage"));
      LOGGER.info(I18n.get(DESCRIPTION_KEY));
      LOGGER.info("  {0}", I18n.get("showDescHisto"));
    }
  }

  /** Displays help information for the "show time" option. */
  public void helpTime() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}{1} TIME ===\n", HELP_HEADER_PREFIX, I18n.get(SHOW_HEADER_KEY));
      LOGGER.info(I18n.get(USAGE_KEY));
      LOGGER.info("  {0}\n", I18n.get("showTimeUsage"));
      LOGGER.info(I18n.get(DESCRIPTION_KEY));
      LOGGER.info("  {0}", I18n.get("showDescTime"));
    }
  }

  /** Displays help information for the "show configuration" option. */
  public void helpConfig() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}{1} CONFIGURATION ===\n", HELP_HEADER_PREFIX, I18n.get(SHOW_HEADER_KEY));
      LOGGER.info(I18n.get(USAGE_KEY));
      LOGGER.info("  {0}\n", I18n.get("showConfigUsage"));
      LOGGER.info(I18n.get(DESCRIPTION_KEY));
      LOGGER.info("  {0}", I18n.get("showDescConfig"));
    }
    LOGGER.verbose("  {0}", I18n.get("showDescVConfig"));
    LOGGER.verbose("    {0}", I18n.get("showCfgs"));
    LOGGER.verbose(I18n.get("showCfgsMore"));
  }

  /**
   * Prints the board state, player positions, and walls.
   *
   * @param engine the current game engine
   */
  private void printBoard(final GameEngine engine) {
    final GameState state = engine.getState();
    final Board board = state.getBoard();
    final int boardSize = board.getSize();
    for (int col = 0; col < boardSize; col++) {
      WRITER.print(((char) ('A' + col)) + " ");
    }
    WRITER.println("");
    for (int row = 0; row < boardSize; row++) {
      for (int col = 0; col < boardSize; col++) {
        boolean playerFound = false;
        for (final Player player : state.getPlayers()) {
          if (player.getPosition().equals(new Position(row, col)) && !player.isDisabled()) {
            WRITER.print(
                player.getColor().getId()
                    + ((col != boardSize - 1) ? printWall(board, row, col, false) : " "));
            playerFound = true;
            break;
          }
        }
        if (!playerFound) {
          WRITER.print("_" + ((col != boardSize - 1) ? printWall(board, row, col, false) : " "));
        }
        if (col == boardSize - 1) {
          WRITER.print(row + 1);
        }
      }
      if (row != boardSize - 1) {
        WRITER.println("");
        for (int col = 0; col < boardSize; col++) {
          WRITER.print(printWall(board, row, col, true));
        }
        WRITER.println("");
      }
    }
    WRITER.println("");
  }

  /**
   * @return wall representation based on graph.
   */
  private String printWall(final Board board, final int row, final int col, final boolean under) {
    final String result;
    if (under) {
      if (row >= board.getSize() - 1) {
        result = "  ";
      } else {
        result =
            board.getGraph().hasEdge(board.getCellId(row, col), board.getCellId(row + 1, col))
                ? ". "
                : "X ";
      }
    } else {
      if (col >= board.getSize() - 1) {
        result = " ";
      } else {
        result =
            board.getGraph().hasEdge(board.getCellId(row, col), board.getCellId(row, col + 1))
                ? " "
                : "X";
      }
    }
    return result;
  }

  /**
   * Converts board position to a human-readable CLI coordinate. notation (e.g., a0, b3).
   *
   * @param position the position to convert
   * @return the string representation of the position
   */
  private String posToString(final Position position) {
    final char letter = (char) ('a' + position.getY());
    final int number = position.getX() + 1;
    return String.valueOf(letter) + number;
  }

  /**
   * Displays game history. Player - Move.
   *
   * @param engine the current game engine
   */
  public void showHistory(final GameEngine engine) {
    final GameState state = engine.getState();
    WRITER.println(I18n.get("oldest"));
    final List<Player> players = state.getPlayers();
    final int nbPlayers = players.size();
    int count = 0;
    final Move[] moves = state.getMovesPlayed();
    for (final Move move : moves) {
      final StringBuilder strMove = new StringBuilder();
      strMove.append(I18n.get("historyPlayer", players.get(count % nbPlayers).getName()));
      if (move.isPawn()) {
        final Position from = move.getFrom();
        strMove.append(I18n.get("historyCoordPawn", posToString(from), posToString(move.getTo())));
      } else {
        final Orientation orientation = move.getOrientation();
        strMove.append(
            I18n.get("historyCoordWall", posToString(move.getTo()), orientation.toString()));
      }
      count++;
      WRITER.println(strMove.toString());
    }
    WRITER.println(I18n.get("newest"));
  }
}
