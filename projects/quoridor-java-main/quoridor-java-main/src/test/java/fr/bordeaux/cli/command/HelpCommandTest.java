package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HelpCommandTest {
  private GameEngine engine;
  private Map<String, Function<String[], Command>> registry;

  @BeforeEach
  void setUp() {
    Board board = new Board(9);
    Player p1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    Player p2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);

    registry = new HashMap<>();
    registry.put("help", tokens -> new HelpCommand(tokens, registry));
    registry.put("show", ShowCommand::new);
    registry.put("quit", tokens -> new QuitCommand());
    registry.put("move", tokens -> new MoveCommand());
  }

  @Test
  void executeWithNullTokens() {
    HelpCommand command = new HelpCommand(null, registry);

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeWithOnlyHelp() {
    HelpCommand command = new HelpCommand(new String[] {"help"}, registry);

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeWithKnownCommand() {
    HelpCommand command = new HelpCommand(new String[] {"help", "show"}, registry);

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeWithUnknownCommand() {
    HelpCommand command = new HelpCommand(new String[] {"help", "unknown"}, registry);

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void helpText() {
    HelpCommand command = new HelpCommand();

    assertDoesNotThrow(command::helpText);
  }

  @Test
  void helpCli() {
    HelpCommand command = new HelpCommand(new String[] {"help"}, registry);

    assertDoesNotThrow(command::helpCli);
  }

  @Test
  void helpFetchWithNoCommand() {
    HelpCommand command = new HelpCommand(new String[] {"help"}, registry);

    assertDoesNotThrow(command::helpFetch);
  }

  @Test
  void helpFetchWithKnownCommand() {
    HelpCommand command = new HelpCommand(new String[] {"help", "show"}, registry);

    assertDoesNotThrow(command::helpFetch);
  }

  @Test
  void helpFetchWithUnknownCommand() {
    HelpCommand command = new HelpCommand(new String[] {"help", "unknown"}, registry);

    assertDoesNotThrow(command::helpFetch);
  }
}
