package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.core.*;
import fr.bordeaux.i18n.I18n;
import java.util.List;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewCommandTest {

  private GameEngine engine;
  private Terminal terminal;
  private LineReader reader;

  @BeforeEach
  void setUp() {
    Board board = new Board(9);
    Player p1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    Player p2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);

    terminal = mock(Terminal.class);
    reader = mock(LineReader.class);
  }

  @Test
  void constructorWithTerminalAndReader() {
    NewCommand command = new NewCommand(terminal, reader);

    assertNotNull(command);
  }

  @Test
  void executeDefaultGame() {
    NewCommand command = new NewCommand(new String[] {"new"}, terminal, reader);

    GameEngine result = command.execute(engine);

    assertNotNull(result);
    assertEquals(2, result.getState().getPlayers().size());
    assertEquals(9, result.getState().getBoard().getSize());
  }

  @Test
  void executeWithTwoPlayersByDefault() {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn("h", "h");
    when(reader.readLine(I18n.get("playersName", " >> "))).thenReturn("Alice", "Bob");

    NewCommand command = new NewCommand(new String[] {"new", "9"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertNotNull(result);
    assertEquals(2, result.getState().getPlayers().size());
  }

  @Test
  void executeTwoHumans() {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn("h", "h");
    when(reader.readLine(I18n.get("playersName", " >> "))).thenReturn("Alice", "Bob");

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertNotNull(result);
    assertEquals(2, result.getState().getPlayers().size());
    assertEquals("Alice", result.getState().getPlayers().get(0).getName());
    assertEquals("Bob", result.getState().getPlayers().get(1).getName());
  }

  @Test
  void executeMiniPlayer() {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn("mini", "h");
    when(reader.readLine(I18n.get("playersName", " >> "))).thenReturn("Mini", "Bob");

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertNotNull(result);
    assertEquals(2, result.getState().getPlayers().size());
  }

  @Test
  void executeMctsPlayer() {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn("mcts", "h");
    when(reader.readLine(I18n.get("playersName", " >> "))).thenReturn("Mcts", "Bob");

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertNotNull(result);
    assertEquals(2, result.getState().getPlayers().size());
  }

  @Test
  void executeRandomPlayer() {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn("r", "h");
    when(reader.readLine(I18n.get("playersName", " >> "))).thenReturn("Rand", "Bob");

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertNotNull(result);
    assertEquals(2, result.getState().getPlayers().size());
  }

  @Test
  void executeWrongType() {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn("wrong", "h", "h");
    when(reader.readLine(I18n.get("playersName", " >> "))).thenReturn("Alice", "Bob");

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertNotNull(result);
    assertEquals(2, result.getState().getPlayers().size());
  }

  @Test
  void executeNullType() throws Exception {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn((String) null);

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    verify(terminal).close();
  }

  @Test
  void executeInterrupted() throws Exception {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenThrow(new EndOfFileException());

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    verify(terminal).close();
  }

  @Test
  void executeNullName() throws Exception {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn("h");
    when(reader.readLine(I18n.get("playersName", " >> "))).thenReturn((String) null);

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    verify(terminal).close();
  }

  @Test
  void executeTooManyArguments() {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenReturn("h", "h");
    when(reader.readLine(I18n.get("playersName", " >> "))).thenReturn("Alice", "Bob");

    NewCommand command =
        new NewCommand(new String[] {"new", "9", "2", "a", "b", "c"}, terminal, reader);

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void executeTerminalCloseError() throws Exception {
    when(reader.readLine(I18n.get("playersType", " >> "))).thenThrow(new EndOfFileException());
    doThrow(new RuntimeException("close error")).when(terminal).close();

    NewCommand command = new NewCommand(new String[] {"new", "9", "2"}, terminal, reader);

    assertDoesNotThrow(() -> command.execute(engine));
  }

  @Test
  void helpText() {
    NewCommand command = new NewCommand();

    assertDoesNotThrow(command::helpText);
  }
}
