package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import fr.bordeaux.persistence.GameSaver;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoadCommandTest {

  private GameEngine engine;
  private final String fileName = "load_command_test.txt";

  @BeforeEach
  void setUp() throws Exception {
    Board board = new Board(9);
    Player p1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    Player p2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);

    GameSaver.saveGame(engine, fileName);
  }

  @AfterEach
  void cleanUp() {
    new File(fileName).delete();
  }

  @Test
  void execute() {
    LoadCommand command = new LoadCommand(new String[] {"load", fileName});

    GameEngine result = command.execute(engine);

    assertNotNull(result);
    assertNotNull(result.getState());
    assertEquals(2, result.getState().getPlayers().size());
  }

  @Test
  void executeWithoutFile() {
    LoadCommand command = new LoadCommand(new String[] {"load"});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void executeWithInvalidFile() {
    LoadCommand command = new LoadCommand(new String[] {"load", "file_that_does_not_exist.txt"});

    GameEngine result = command.execute(engine);

    assertSame(engine, result);
  }

  @Test
  void helpText() {
    LoadCommand command = new LoadCommand(new String[] {"load", fileName});

    assertDoesNotThrow(command::helpText);
  }
}
