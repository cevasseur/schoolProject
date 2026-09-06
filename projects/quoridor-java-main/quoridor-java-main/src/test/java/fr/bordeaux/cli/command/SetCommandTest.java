package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetCommandTest {

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
  void constructorWithString() {
    SetCommand command = new SetCommand("debug=true");

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeWithNullTokens() {
    SetCommand command = new SetCommand();

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void executeValidSetting() {
    SetCommand command = new SetCommand(new String[] {"set", "debug=true"});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void executeWithoutArgument() {
    SetCommand command = new SetCommand(new String[] {"set"});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void executeInvalidFormat() {
    SetCommand command = new SetCommand(new String[] {"set", "debugtrue"});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void executeEmptyKey() {
    SetCommand command = new SetCommand(new String[] {"set", "=true"});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void executeEmptyValue() {
    SetCommand command = new SetCommand(new String[] {"set", "debug="});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void executeEmptyKeyAndValue() {
    SetCommand command = new SetCommand(new String[] {"set", "="});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void helpText() {
    SetCommand command = new SetCommand();

    assertDoesNotThrow(command::helpText);
  }
}
