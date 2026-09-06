package fr.bordeaux.cli.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.core.*;
import fr.bordeaux.i18n.I18n;
import java.util.List;
import org.jline.reader.LineReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuitCommandTest {

  private GameEngine engine;
  private LineReader reader;

  @BeforeEach
  void setUp() {
    Board board = new Board(9);
    Player p1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    Player p2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);
    reader = mock(LineReader.class);
  }

  @Test
  void constructorWithoutReader() {
    QuitCommand command = new QuitCommand();

    assertNotNull(command);
  }

  @Test
  void executeNoSave() {
    when(reader.readLine(I18n.get("quitSaving", " [y/N] >> "))).thenReturn("n");

    QuitCommand command = new QuitCommand(reader);
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    verify(reader).readLine(I18n.get("quitSaving", " [y/N] >> "));
    verify(reader, never()).readLine(I18n.get("filePath", " >> "));
  }

  @Test
  void executeYesSave() {
    when(reader.readLine(I18n.get("quitSaving", " [y/N] >> "))).thenReturn("y");
    when(reader.readLine(I18n.get("filePath", " >> "))).thenReturn("quit_test.txt");

    QuitCommand command = new QuitCommand(reader);
    GameEngine result = command.execute(engine);

    assertSame(engine, result);
    verify(reader).readLine(I18n.get("quitSaving", " [y/N] >> "));
    verify(reader).readLine(I18n.get("filePath", " >> "));
  }

  @Test
  void executeYesWordSave() {
    when(reader.readLine(I18n.get("quitSaving", " [y/N] >> "))).thenReturn("yes");
    when(reader.readLine(I18n.get("filePath", " >> "))).thenReturn("quit_test_yes.txt");

    QuitCommand command = new QuitCommand(reader);

    assertDoesNotThrow(() -> command.execute(engine));

    verify(reader).readLine(I18n.get("filePath", " >> "));
  }

  @Test
  void helpText() {
    QuitCommand command = new QuitCommand(reader);

    assertDoesNotThrow(command::helpText);
  }
}
