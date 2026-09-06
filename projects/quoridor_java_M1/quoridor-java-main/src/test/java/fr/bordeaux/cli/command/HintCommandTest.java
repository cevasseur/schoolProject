package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HintCommandTest {

  private GameEngine engine;

  @BeforeEach
  void setUp() {
    Board board = new Board(9);
    Player p1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    Player p2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);
  }

  @Test
  void execute() {
    HintCommand command = new HintCommand();

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeReturnsSameEngine() throws IOException {
    HintCommand command = new HintCommand();

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void helpText() {
    HintCommand command = new HintCommand();

    assertDoesNotThrow(command::helpText);
  }

  @Test
  void formatPawnMove() throws Exception {
    HintCommand command = new HintCommand();
    Move move = Move.pawn(new Position(0, 4), new Position(1, 4));

    var method = HintCommand.class.getDeclaredMethod("formatMove", Move.class);
    method.setAccessible(true);

    String result = (String) method.invoke(command, move);

    assertEquals("e1-e2", result);
  }

  @Test
  void formatWallMove() throws Exception {
    HintCommand command = new HintCommand();
    Move move = Move.wall(new Position(1, 1), Orientation.HORIZONTAL);

    var method = HintCommand.class.getDeclaredMethod("formatMove", Move.class);
    method.setAccessible(true);

    String result = (String) method.invoke(command, move);

    assertEquals("b2h", result);
  }

  @Test
  void formatVerticalWallMove() throws Exception {
    HintCommand command = new HintCommand();
    Move move = Move.wall(new Position(1, 1), Orientation.VERTICAL);

    var method = HintCommand.class.getDeclaredMethod("formatMove", Move.class);
    method.setAccessible(true);

    String result = (String) method.invoke(command, move);

    assertEquals("b2v", result);
  }

  @Test
  void posToString() throws Exception {
    HintCommand command = new HintCommand();

    var method = HintCommand.class.getDeclaredMethod("posToString", Position.class);
    method.setAccessible(true);

    String result = (String) method.invoke(command, new Position(1, 4));

    assertEquals("e2", result);
  }
}
