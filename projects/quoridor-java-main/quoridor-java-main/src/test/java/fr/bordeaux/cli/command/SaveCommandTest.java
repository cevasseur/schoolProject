package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SaveCommandTest {

  private GameEngine engine;
  private final String fileName = "save_command_test.txt";

  @BeforeEach
  void setUp() {
    Board board = new Board(9);
    Player p1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    Player p2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);
  }

  @AfterEach
  void cleanUp() {
    new File(fileName).delete();
  }

  @Test
  void execute() {
    SaveCommand command = new SaveCommand(new String[] {"save", fileName});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    assertTrue(new File(fileName).exists());
  }

  @Test
  void executeWithoutFile() {
    SaveCommand command = new SaveCommand(new String[] {"save"});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    assertFalse(new File(fileName).exists());
  }

  @Test
  void executeWithStringConstructor() {
    SaveCommand command = new SaveCommand(fileName);

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    assertTrue(new File(fileName).exists());
  }

  @Test
  void executeWithInvalidPath() {
    SaveCommand command = new SaveCommand(new String[] {"save", "?:/invalid/save.txt"});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void savedFileContainsGameSection() throws Exception {
    SaveCommand command = new SaveCommand(fileName);

    command.execute(engine);

    List<String> lines = Files.readAllLines(new File(fileName).toPath());

    assertTrue(lines.contains("[settings]"));
    assertTrue(lines.contains("[game]"));
  }

  @Test
  void helpText() {
    SaveCommand command = new SaveCommand(fileName);

    assertDoesNotThrow(command::helpText);
  }
}
