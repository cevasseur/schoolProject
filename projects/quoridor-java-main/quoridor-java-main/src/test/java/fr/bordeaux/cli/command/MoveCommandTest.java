package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.core.*;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoveCommandTest {

  private GameEngine engine;
  private Player p1;
  private Player p2;

  @BeforeEach
  void setUp() {
    I18n.initialize();
    Board board = new Board(9);
    p1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    p2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);
  }

  @Test
  void parsePosition() {
    MoveCommand command = new MoveCommand();

    Position pos = command.parsePosition("e2");

    assertEquals(new Position(1, 4), pos);
  }

  @Test
  void executePawnMove() {
    MoveCommand command = new MoveCommand("e1-e2");

    command.execute(engine);

    assertEquals(new Position(1, 4), p1.getPosition());
  }

  @Test
  void executeInvalidMove() {
    MoveCommand command = new MoveCommand("zzz");
    Position before = p1.getPosition();

    command.execute(engine);

    assertEquals(before, p1.getPosition());
  }

  @Test
  void executeHorizontalWall() {
    MoveCommand command = new MoveCommand("e2h");

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeVerticalWall() {
    MoveCommand command = new MoveCommand("e2v");

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeCatchBranch() {
    MoveCommand command = new MoveCommand("ea-e2");
    Position before = p1.getPosition();

    assertDoesNotThrow(() -> command.execute(engine));

    assertEquals(before, p1.getPosition());
  }

  @Test
  void executeWallCatchBranch() {
    MoveCommand command = new MoveCommand("eav");
    Position before = p1.getPosition();

    assertDoesNotThrow(() -> command.execute(engine));

    assertEquals(before, p1.getPosition());
  }

  @Test
  void executeNullMove() {
    MoveCommand command = new MoveCommand();
    Position before = p1.getPosition();

    assertDoesNotThrow(() -> command.execute(engine));

    assertEquals(before, p1.getPosition());
  }

  @Test
  void executeLongMoveFormat() {
    MoveCommand command = new MoveCommand("e10-e11");

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void handleMove() {
    MoveCommand command = new MoveCommand("e1-e2");

    command.handleMove(engine);

    assertEquals(new Position(1, 4), p1.getPosition());
  }

  @Test
  void handleMoveBadSyntax() {
    MoveCommand command = new MoveCommand("e1-e2-e3");

    assertThrows(IllegalArgumentException.class, () -> command.handleMove(engine));
  }

  @Test
  void handleWallHorizontal() {
    MoveCommand command = new MoveCommand("e2h");

    assertDoesNotThrow(() -> command.handleWall(engine));
  }

  @Test
  void handleWallVertical() {
    MoveCommand command = new MoveCommand("e2v");

    assertDoesNotThrow(() -> command.handleWall(engine));
  }

  @Test
  void helpText() {
    MoveCommand command = new MoveCommand();

    assertDoesNotThrow(command::helpText);
  }

  @Test
  void testParsePosition_ShortString_LoggerEnabled() {
    MoveCommand command = new MoveCommand();
    Logger.getInstance().setErrorEnabled(true);

    assertThrows(IllegalArgumentException.class, () -> command.parsePosition("e"));
    // This covers the branch if (LOGGER.isErrorEnabled())
  }

  @Test
  void testParsePosition_ShortString_LoggerDisabled() {
    MoveCommand command = new MoveCommand();
    Logger.getInstance().setErrorEnabled(false);

    assertThrows(IllegalArgumentException.class, () -> command.parsePosition("e"));
    // This covers the branch when isErrorEnabled is false
  }

  @Test
  void testHelpText() {
    MoveCommand command = new MoveCommand();
    Logger.getInstance().setInfoEnabled(false);

    assertDoesNotThrow(command::helpText);
  }

  @Test
  void testHandleMove_NullMove() {
    MoveCommand command = new MoveCommand("e");
    Logger.getInstance().setInfoEnabled(false);

    assertThrows(IllegalArgumentException.class, () -> command.handleMove(engine));
  }
}
