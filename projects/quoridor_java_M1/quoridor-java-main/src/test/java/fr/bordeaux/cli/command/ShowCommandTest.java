package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowCommandTest {

  private GameEngine engine;
  private Player p1;
  private Player p2;

  @BeforeEach
  void setUp() {
    Board board = new Board(9);
    p1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    p2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);
  }

  @Test
  void executeBoard() {
    ShowCommand command = new ShowCommand(new String[] {"show", "board"});

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeHistory() {
    engine.play(Move.pawn(new Position(0, 4), new Position(1, 4)));

    ShowCommand command = new ShowCommand(new String[] {"show", "history"});

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeConfigurations() {
    ShowCommand command = new ShowCommand(new String[] {"show", "configurations"});

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeConfig() {
    ShowCommand command = new ShowCommand(new String[] {"show", "config"});

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeTime() {
    ShowCommand command = new ShowCommand(new String[] {"show", "time"});

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeInvalidOption() {
    ShowCommand command = new ShowCommand(new String[] {"show", "unknown"});

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeWithStringConstructor() {
    ShowCommand command = new ShowCommand("board");

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void showHistoryWithoutMoves() {
    ShowCommand command = new ShowCommand(new String[] {"show", "history"});

    assertDoesNotThrow(() -> command.showHistory(engine));
  }

  @Test
  void showHistoryWithPawnMove() {
    engine.play(Move.pawn(new Position(0, 4), new Position(1, 4)));

    ShowCommand command = new ShowCommand(new String[] {"show", "history"});

    assertDoesNotThrow(() -> command.showHistory(engine));
  }

  @Test
  void showHistoryWithWallMove() {
    engine.play(Move.wall(new Position(1, 1), Orientation.HORIZONTAL));

    ShowCommand command = new ShowCommand(new String[] {"show", "history"});

    assertDoesNotThrow(() -> command.showHistory(engine));
  }

  @Test
  void helpText() {
    ShowCommand command = new ShowCommand();

    assertDoesNotThrow(command::helpText);
  }

  @Test
  void helpBoard() {
    ShowCommand command = new ShowCommand();

    assertDoesNotThrow(command::helpBoard);
  }

  @Test
  void helpHistory() {
    ShowCommand command = new ShowCommand();

    assertDoesNotThrow(command::helpHistory);
  }

  @Test
  void helpTime() {
    ShowCommand command = new ShowCommand();

    assertDoesNotThrow(command::helpTime);
  }

  @Test
  void helpConfig() {
    ShowCommand command = new ShowCommand();

    assertDoesNotThrow(command::helpConfig);
  }

  @Test
  void helpTextWithBoard() {
    ShowCommand command = new ShowCommand(new String[] {"show", "board"});

    assertDoesNotThrow(command::helpText);
  }

  @Test
  void helpTextWithHistory() {
    ShowCommand command = new ShowCommand(new String[] {"show", "history"});

    assertDoesNotThrow(command::helpText);
  }

  @Test
  void helpTextWithTime() {
    ShowCommand command = new ShowCommand(new String[] {"show", "time"});

    assertDoesNotThrow(command::helpText);
  }

  @Test
  void helpTextWithConfig() {
    ShowCommand command = new ShowCommand(new String[] {"show", "config"});

    assertDoesNotThrow(command::helpText);
  }
}
