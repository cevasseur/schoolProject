package fr.bordeaux.cli.command;

import fr.bordeaux.ai.base.*;
import fr.bordeaux.ai.mcts.*;
import fr.bordeaux.ai.minimax.*;
import fr.bordeaux.ai.random.*;
import fr.bordeaux.core.*;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

/**
 * CLI command that starts a new game, configuring board size and players. of players, and
 * interactively asks for the type and name of each player.
 */
public class NewCommand implements Command {

  /** Command entered by the user */
  private String[] tokens;

  /** JLine terminal for system-level input and output. */
  private Terminal terminal;

  /** Line reader for text editing and tab-completion. interaction. */
  private LineReader reader;

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /**
   * Creates a NewCommand with the given CLI tokens, terminal and line reader.
   *
   * @param tokens the tokens entered by the user
   * @param term the terminal used for input/output
   * @param reader the line reader used to interact with the user
   */
  public NewCommand(final String[] tokens, final Terminal term, final LineReader reader) {
    this.tokens = tokens;
    this.terminal = term;
    this.reader = reader;
  }

  /**
   * Creates a NewCommand with default tokens and the provided terminal and reader.
   *
   * @param terminal the terminal used for input/output
   * @param reader the line reader used to read user input
   */
  public NewCommand(final Terminal terminal, final LineReader reader) {
    this(new String[] {"9", "2"}, terminal, reader);
  }

  /** Creates an empty NewCommand. Used mainly when only help information is needed. */
  public NewCommand() {
    this(null, null, null);
  }

  /**
   * Executes new command, initializing the game engine with board and players. Prompts for player
   * types and names if needed. interactively.
   *
   * @param engine the current game engine
   * @return a new GameEngine instance representing the new game
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    GameEngine result = engine;

    if (tokens.length > 5) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("newNbArgs"));
      }
    } else if (tokens.length == 1) {
      final int size = 8;
      result =
          new GameEngine(
              new GameState(
                  new Board(size + 1),
                  List.of(
                      new HumanPlayer("Human", new Position(0, 4), Color.WHITE),
                      new HumanPlayer("Bob", new Position(8, 4), Color.BLACK))),
              engine.isBlitz(),
              engine.isVerbose(),
              engine.isContest(),
              engine.isDebug(),
              engine.getInitialWalls());
    } else {
      final int boardSizeInput = Integer.parseInt(tokens[1]);
      final int size = boardSizeInput - 1;
      final Position[] positions = {
        new Position(0, size / 2), new Position(size, size / 2),
        new Position(size / 2, 0), new Position(size / 2, size)
      };
      final int nbPlayer = tokens.length <= 2 ? 2 : Integer.parseInt(tokens[2]);
      final Board board = new Board(size + 1);
      final Player[] players = new Player[nbPlayer];
      boolean aborted = false;

      for (int index = 0; index < nbPlayer && !aborted; index++) {
        try {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info(I18n.get("playersType", I18n.get("availType")));
            LOGGER.info(I18n.get("playerTypeOptions"));
          }
          final String typePlayer = reader.readLine(I18n.get("playersType", " >> "));
          if (typePlayer == null) {
            throw new EndOfFileException();
          }
          if (!"h".equals(typePlayer)
              && !"mcts".equals(typePlayer)
              && !"mini".equals(typePlayer)
              && !"r".equals(typePlayer)
              && !"mctsnn".equals(typePlayer)) {
            if (LOGGER.isErrorEnabled()) {
              LOGGER.error(I18n.get("wrongType"));
            }
            index--;
            continue;
          }
          final String inputName = reader.readLine(I18n.get("playersName", " >> "));
          if (inputName == null) {
            if (LOGGER.isErrorEnabled()) {
              LOGGER.error(I18n.get("shouldName"));
            }
            throw new EndOfFileException();
          }
          switch (typePlayer) {
            case "h":
              players[index] =
                  new HumanPlayer(inputName, positions[index], Color.fromId(index + 1));
              break;
            case "mcts":
              players[index] = new MctsPlayer(inputName, positions[index], Color.fromId(index + 1));
              break;
            case "mini":
              players[index] =
                  MinimaxPlayer.builder(inputName, positions[index], Color.fromId(index + 1))
                      .build();
              break;
            case "mctsnn":
              players[index] =
                  new MctsPlayer(inputName, positions[index], 2000, true, Color.fromId(index + 1));
              break;
            default:
              players[index] =
                  new RandomPlayer(inputName, positions[index], Color.fromId(index + 1));
              break;
          }
        } catch (EndOfFileException | UserInterruptException exception) {
          if (LOGGER.isErrorEnabled()) {
            LOGGER.error(I18n.get("termClose"));
          }
          try {
            terminal.close();
          } catch (Exception exc) {
            if (LOGGER.isErrorEnabled()) {
              LOGGER.error(I18n.get("termClose"));
            }
          }
          aborted = true;
        } catch (final IOException e) {
          throw new java.io.UncheckedIOException(e);
        }
      }

      if (!aborted) {
        final String setNbPlayers = "nb-players=" + nbPlayer;
        new SetCommand(setNbPlayers).execute(engine);

        result =
            new GameEngine(
                new GameState(board, Arrays.stream(players).toList()),
                engine.isBlitz(),
                engine.isVerbose(),
                engine.isContest(),
                engine.isDebug(),
                engine.getInitialWalls());
      }
    }
    return result;
  }

  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("newHeader"), I18n.get("cliTitle"));
      LOGGER.info(I18n.get("usage"));
      LOGGER.info("  {0}", I18n.get("newUsageEmpty"));
      LOGGER.info("  {0}\n", I18n.get("newUsageArgs"));
      LOGGER.info(I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("newDesc"));
      LOGGER.info(I18n.get("arguments"));
      LOGGER.info("  boardSize    : {0}", I18n.get("newBoardSize"));
      LOGGER.info("  playerCount  : {0}", I18n.get("newNbPlayers"));
      LOGGER.info(I18n.get("newHelp"));
    }
    LOGGER.verbose(I18n.get("loadEx"));
    LOGGER.verbose("  new       -> 2 {0}", I18n.get("newEx", "9x9"));
    LOGGER.verbose("  new 5     -> 2 {0}", I18n.get("newEx", "5x5"));
    LOGGER.verbose("  new 7 4   -> 4 {0}", I18n.get("newEx", "7x7"));
  }
}
