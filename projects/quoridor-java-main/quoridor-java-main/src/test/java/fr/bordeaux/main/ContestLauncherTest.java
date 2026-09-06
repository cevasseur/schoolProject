package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/** Unit tests for ContestLauncher */
public class ContestLauncherTest {

  private ConfigManager config;
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
  private final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

  @BeforeEach
  public void setUp() {
    fr.bordeaux.util.Logger.resetInstance();
    config = ConfigManager.getInstance();
    // Reset config to avoid interference between tests
    config.setOption("contest", "false");
    config.setOption("contest-file", "");

    capturedOut.reset();
    capturedErr.reset();
    System.setOut(new PrintStream(capturedOut));
    System.setErr(new PrintStream(capturedErr));
  }

  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  public void testContestLauncherConstructor() throws Exception {
    Constructor<ContestLauncher> c = ContestLauncher.class.getDeclaredConstructor();
    c.setAccessible(true);
    assertNotNull(c.newInstance());
  }

  @Test
  public void testContestLaunchInvalidFileContent(@TempDir Path tempDir) throws IOException {
    Path badFile = tempDir.resolve("bad_save.txt");
    Files.writeString(badFile, "this is not a valid save file\n");
    config.enableContest(badFile.toString());
    ContestLauncher launcher = newContestLauncher();
    assertDoesNotThrow(() -> launcher.launch(new String[] {}, config));
  }

  @Test
  public void testContestLaunchInvalidFile(@TempDir Path tempDir) throws IOException {
    Path badFile = tempDir.resolve("bad_save.txt");
    Files.writeString(badFile, "this is not a valid save file\n");
    config.enableContest(badFile.toString());
    ContestLauncher launcher = newContestLauncher();
    assertDoesNotThrow(() -> launcher.launch(new String[] {}, config));
  }

  @Test
  public void testMoveToStringPawn() throws Exception {
    Method m = ContestLauncher.class.getDeclaredMethod("moveToString", Move.class);
    m.setAccessible(true);

    Position from = new Position(0, 4);
    Position to = new Position(1, 4);
    Move pawnMove = Move.pawn(from, to);

    String result = (String) m.invoke(null, pawnMove);
    assertEquals("a4-b4", result);
  }

  @Test
  public void testMoveToStringWallHorizontal() throws Exception {
    Method m = ContestLauncher.class.getDeclaredMethod("moveToString", Move.class);
    m.setAccessible(true);

    Position pos = new Position(3, 2);
    Move wallMove = Move.wall(pos, Orientation.HORIZONTAL);

    String result = (String) m.invoke(null, wallMove);
    assertEquals("d2h", result);
  }

  @Test
  public void testMoveToStringWallVertical() throws Exception {
    Method m = ContestLauncher.class.getDeclaredMethod("moveToString", Move.class);
    m.setAccessible(true);

    Position pos = new Position(5, 1);
    Move wallMove = Move.wall(pos, Orientation.VERTICAL);

    String result = (String) m.invoke(null, wallMove);
    assertEquals("f1v", result);
  }

  @Test
  public void testPosToString() throws Exception {
    Method m = ContestLauncher.class.getDeclaredMethod("posToString", Position.class);
    m.setAccessible(true);

    assertEquals("a0", m.invoke(null, new Position(0, 0)));
    assertEquals("e4", m.invoke(null, new Position(4, 4)));
    assertEquals("i8", m.invoke(null, new Position(8, 8)));
  }

  @Test
  public void testCreateDummyEngine() throws Exception {
    Method m = ContestLauncher.class.getDeclaredMethod("createDummyEngine", ConfigManager.class);
    m.setAccessible(true);

    Object engine = m.invoke(null, config);
    assertNotNull(engine);
    assertInstanceOf(GameEngine.class, engine);
  }

  @Test
  public void testComputeBestMove() throws Exception {
    Board board = new Board(9);
    List<Player> players =
        List.of(
            new HumanPlayer("P1", new Position(0, 4), Color.WHITE),
            new HumanPlayer("P2", new Position(8, 4), Color.BLACK));
    GameState state = new GameState(board, players, 0);

    Method m =
        ContestLauncher.class.getDeclaredMethod(
            "computeBestMove", GameState.class, ConfigManager.class);
    m.setAccessible(true);

    Move best = (Move) m.invoke(null, state, config);
    assertNotNull(best);
    assertTrue(state.generateLegalMoves().contains(best));
  }

  @Test
  public void testComputeBestMove_MctsExceptionFallback() throws Exception {
    Board board = new Board(9);
    List<Player> players =
        List.of(
            new HumanPlayer("P1", new Position(0, 4), Color.WHITE),
            new HumanPlayer("P2", new Position(8, 4), Color.BLACK));
    GameState state = new GameState(board, players, 0);
    List<Move> legalMoves = state.generateLegalMoves();
    Move expectedFallback = legalMoves.get(0);

    try (MockedStatic<fr.bordeaux.ai.mcts.MctsPlayer> mockedMcts =
        mockStatic(fr.bordeaux.ai.mcts.MctsPlayer.class)) {
      mockedMcts
          .when(() -> fr.bordeaux.ai.mcts.MctsPlayer.hint(any(), any(), anyLong(), anyBoolean()))
          .thenThrow(new RuntimeException("MCTS Failure"));

      Method m =
          ContestLauncher.class.getDeclaredMethod(
              "computeBestMove", GameState.class, ConfigManager.class);
      m.setAccessible(true);

      Move result = (Move) m.invoke(null, state, config);

      assertEquals(
          expectedFallback, result, "Should fallback to first legal move on MCTS exception");
      assertTrue(capturedErr.toString().contains("MCTS failed (MCTS Failure)"));
    }
  }

  @Test
  public void testComputeBestMove_NoLegalMovesFallback() throws Exception {
    GameState state = mock(GameState.class);
    when(state.generateLegalMoves()).thenReturn(List.of());
    when(state.getCurrentPlayer())
        .thenReturn(new HumanPlayer("P1", new Position(0, 4), Color.WHITE));

    try (MockedStatic<fr.bordeaux.ai.mcts.MctsPlayer> mockedMcts =
        mockStatic(fr.bordeaux.ai.mcts.MctsPlayer.class)) {
      mockedMcts
          .when(() -> fr.bordeaux.ai.mcts.MctsPlayer.hint(any(), any(), anyLong(), anyBoolean()))
          .thenThrow(new RuntimeException("MCTS Failure"));

      Method m =
          ContestLauncher.class.getDeclaredMethod(
              "computeBestMove", GameState.class, ConfigManager.class);
      m.setAccessible(true);

      InvocationTargetException ite =
          assertThrows(InvocationTargetException.class, () -> m.invoke(null, state, config));
      assertInstanceOf(IllegalStateException.class, ite.getCause());
      assertEquals("No legal moves available.", ite.getCause().getMessage());
    }
  }

  @Test
  public void testLaunch(@TempDir Path tempDir) throws IOException {
    Path saveFile = tempDir.resolve("valid_save.txt");
    StringBuilder sb = new StringBuilder();
    sb.append("[settings]\n");
    sb.append("player-0=Alice\ncolor-0=WHITE\nposition-0=Position: 0, 4\ntime-0=100000\n");
    sb.append("player-1=Bob\ncolor-1=BLACK\nposition-1=Position: 8, 4\ntime-1=100000\n");
    sb.append("[game]\n");
    sb.append("1\n"); // Current player
    sb.append("9\n"); // Size
    for (int i = 0; i < 8; i++) {
      sb.append("1 . . . . . . . .\n");
      sb.append(". . . . . . . . .\n");
    }
    sb.append(". . . . . . . . 2\n");
    sb.append("walls: 10 10\n");
    sb.append("[History]\n");

    Files.writeString(saveFile, sb.toString());
    config.enableContest(saveFile.toString());
    fr.bordeaux.util.Logger.getInstance().setVerboseEnabled(false);

    ContestLauncher launcher = newContestLauncher();
    launcher.launch(new String[] {}, config);

    String output = capturedOut.toString().trim();
    String error = capturedErr.toString().trim();

    assertTrue(
        output.matches("[a-i][0-8]-[a-i][0-8]|[a-i][0-8][hv]"),
        "Output should be algebraic notation. Output: [" + output + "], Error: [" + error + "]");
  }

  private ContestLauncher newContestLauncher() {
    try {
      Constructor<ContestLauncher> c = ContestLauncher.class.getDeclaredConstructor();
      c.setAccessible(true);
      return c.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
