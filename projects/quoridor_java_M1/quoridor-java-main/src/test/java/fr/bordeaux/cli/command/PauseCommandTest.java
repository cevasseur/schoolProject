package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PauseCommandTest {

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
  void executeInBlitzMode() {
    engine = new GameEngine(engine.getState(), true, false, false, false, 10);
    PauseCommand command = new PauseCommand();
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    assertTrue(engine.getState().isPaused());
  }

  @Test
  void executeWithoutBlitzMode() {
    engine = new GameEngine(engine.getState(), false, false, false, false, 10);
    PauseCommand command = new PauseCommand();
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    assertFalse(engine.getState().isPaused());
  }

  @Test
  void helpText() {
    PauseCommand command = new PauseCommand();

    assertDoesNotThrow(command::helpText);
  }
}
