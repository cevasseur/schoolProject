package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedoCommandTest {

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
  void executeOneRedo() {
    engine.play(Move.pawn(new Position(0, 4), new Position(1, 4)));
    engine.undo();

    RedoCommand command = new RedoCommand(new String[] {"redo"});
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    assertEquals(new Position(1, 4), p1.getPosition());
  }

  @Test
  void executeTwoRedos() {
    engine.play(Move.pawn(new Position(0, 4), new Position(1, 4)));
    engine.play(Move.pawn(new Position(8, 4), new Position(7, 4)));
    engine.undo();
    engine.undo();

    RedoCommand command = new RedoCommand(new String[] {"redo", "2"});
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    assertEquals(new Position(1, 4), p1.getPosition());
    assertEquals(new Position(7, 4), p2.getPosition());
  }

  @Test
  void executeTooManyArguments() {
    RedoCommand command = new RedoCommand(new String[] {"redo", "2", "extra"});
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void helpText() {
    RedoCommand command = new RedoCommand();

    assertDoesNotThrow(command::helpText);
  }
}
