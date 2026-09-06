package fr.bordeaux.cli;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.core.*;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.network.GameClient;
import fr.bordeaux.network.MultiGameServer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CliAppTest {

  private CliApp app;
  private GameEngine engine;
  private LineReader reader;
  private Terminal terminal;
  private Player p1;
  private MultiGameServer networkServer;
  private GameClient networkClient;

  @BeforeEach
  void setUp() {
    fr.bordeaux.util.Logger.resetInstance();
    System.setProperty("quoridor.test", "true");
    I18n.get("activ"); // Force I18n class loading if not already done
    I18n.setLocale(java.util.Locale.ENGLISH);

    engine = createEngine();
    p1 = engine.getState().getPlayers().getFirst();
    reader = mock(LineReader.class);
    terminal = mock(Terminal.class);
    when(terminal.writer()).thenReturn(mock(PrintWriter.class));
    app = new CliApp(engine, terminal, reader);
    networkServer = mock(MultiGameServer.class);
    networkClient = mock(GameClient.class);
    app = new CliApp(engine, terminal, reader, networkServer, networkClient);
  }

  @Test
  void defaultConstructor() throws Exception {
    CliApp defaultApp = new CliApp();

    assertNotNull(defaultApp);
    assertNotNull(defaultApp.getEngine());
    assertTrue(defaultApp.isRunning());
  }

  @Test
  void constructorWithEngine() throws Exception {
    CliApp appWithEngine = new CliApp(engine);

    assertNotNull(appWithEngine);
    assertSame(engine, appWithEngine.getEngine());
    assertTrue(appWithEngine.isRunning());
  }

  @Test
  void beep() throws Exception {
    PrintWriter writer = mock(PrintWriter.class);
    Terminal term = mock(Terminal.class);
    when(term.writer()).thenReturn(writer);

    CliApp appWithTerminal = new CliApp(engine, term, reader);

    var method = CliApp.class.getDeclaredMethod("beep");
    method.setAccessible(true);
    method.invoke(appWithTerminal);

    verify(writer).print("\007");
    verify(term).flush();
  }

  @Test
  void runQuit() throws Exception {
    when(reader.readLine(">> ")).thenReturn("quit");
    when(reader.readLine(I18n.get("quitSaving", " [y/N] >> "))).thenReturn("n");

    assertDoesNotThrow(() -> app.run());

    assertFalse(app.isRunning());
    verify(reader).readLine(">> ");
    verify(reader).readLine(I18n.get("quitSaving", " [y/N] >> "));
    verify(terminal).close();
  }

  @Test
  void fetchEmpty() throws IOException {
    GameEngine before = app.getEngine();

    app.fetch("   ");

    assertSame(before, app.getEngine());
    assertTrue(app.isRunning());
    assertEquals(new Position(0, 4), p1.getPosition());
  }

  @Test
  void fetchQuit() throws IOException {
    when(reader.readLine(anyString())).thenReturn("n");

    app.fetch("quit");

    assertFalse(app.isRunning());
    verify(reader).readLine(I18n.get("quitSaving", " [y/N] >> "));
  }

  @Test
  void fetchHelp() throws IOException {
    GameEngine before = app.getEngine();

    app.fetch("help");

    assertSame(before, app.getEngine());
    assertTrue(app.isRunning());
  }

  @Test
  void fetchSuggestion() throws IOException {
    GameEngine before = app.getEngine();

    // "helq" is 1 character away from "help", should trigger suggestion logic
    // where dist < bestDistance
    app.fetch("helq");

    assertSame(before, app.getEngine());
    assertTrue(app.isRunning());
  }

  @Test
  void fetchMove() throws IOException {
    app.fetch("e1-e2");

    assertEquals(new Position(1, 4), p1.getPosition());
  }

  private GameEngine createEngine() {
    Board board = new Board(9);
    Player player1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    Player player2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    GameState state = new GameState(board, List.of(player1, player2));
    return new GameEngine(state, false, false, false, false, 10);
  }

  @Test
  void testSmartCompleter() {
    CliApp.SmartCompleter completer = app.new SmartCompleter();
    List<Candidate> candidates = new ArrayList<>();
    ParsedLine line = mock(ParsedLine.class);

    // Test empty input
    when(line.line()).thenReturn("");
    completer.complete(reader, line, candidates);
    assertTrue(candidates.isEmpty());

    // Test unique match: "he" -> "help"
    candidates.clear();
    when(line.line()).thenReturn("he");
    completer.complete(reader, line, candidates);
    assertEquals(1, candidates.size());
    assertEquals("help", candidates.get(0).value());

    // Test sub-command: "show bo" -> "board"
    candidates.clear();
    when(line.line()).thenReturn("show bo");
    completer.complete(reader, line, candidates);
    assertEquals(1, candidates.size());
    assertEquals("board", candidates.get(0).value());

    // Test help sub-command: "help sa" -> "save"
    candidates.clear();
    when(line.line()).thenReturn("help sa");
    completer.complete(reader, line, candidates);
    assertEquals(1, candidates.size());
    assertEquals("save", candidates.get(0).value());

    // Test no match -> beep
    candidates.clear();
    when(line.line()).thenReturn("unknown");
    PrintWriter writer = mock(PrintWriter.class);
    when(terminal.writer()).thenReturn(writer);
    completer.complete(reader, line, candidates);
    assertTrue(candidates.isEmpty());
    verify(writer).print("\007");

    // Test multiple matches: "s" -> "save", "show", "set"
    candidates.clear();
    when(line.line()).thenReturn("s");
    // First Tab : beep
    completer.complete(reader, line, candidates);
    assertTrue(candidates.isEmpty());
    verify(writer, times(2)).print("\007"); // one from before, one now

    // Second Tab : candidates
    completer.complete(reader, line, candidates);
    assertFalse(candidates.isEmpty());
    assertTrue(candidates.stream().anyMatch(c -> c.value().equals("save")));
    assertTrue(candidates.stream().anyMatch(c -> c.value().equals("show")));
    assertTrue(candidates.stream().anyMatch(c -> c.value().equals("set")));
  }

  @Test
  void testExecuteAiTurn() {
    GameEngine mockEngine = mock(GameEngine.class);
    GameState mockState = mock(GameState.class);
    Player mockAi = mock(Player.class);
    Move mockMove = mock(Move.class);

    when(mockEngine.getState()).thenReturn(mockState);
    when(mockState.getCurrentPlayer()).thenReturn(mockAi);
    when(mockAi.getNextMove(mockState)).thenReturn(mockMove);
    when(mockMove.toString()).thenReturn("test-move");

    CliApp appWithMock = new CliApp(mockEngine, terminal, reader);
    appWithMock.executeAiTurn();

    verify(mockAi).getNextMove(mockState);
    verify(mockEngine).play(mockMove);
  }

  @Test
  void testRunAiTurn() throws Exception {
    Board realBoard = new Board(9);
    Player player1 = spy(new HumanPlayer("Human", new Position(0, 4), Color.WHITE));
    Player player2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);

    // Make player1 an AI for this test
    when(player1.isAI()).thenReturn(true);
    when(player1.getNextMove(any())).thenReturn(Move.pawn(new Position(0, 4), new Position(1, 4)));

    GameState state = spy(new GameState(realBoard, List.of(player1, player2)));
    GameEngine spyEngine = spy(new GameEngine(state, false, false, false, false, 10));

    // End loop after first iteration
    when(state.isGameOver()).thenReturn(true);

    CliApp appWithSpy = new CliApp(spyEngine, terminal, reader);
    appWithSpy.run();

    verify(player1).getNextMove(any());
    verify(spyEngine, atLeastOnce()).play(any());
  }

  @Test
  void testRunTerminalCloseError() throws Exception {
    Terminal mockTerminal = mock(Terminal.class);
    doThrow(new IOException("Forced Terminal Close Error")).when(mockTerminal).close();

    GameEngine realEngine = createEngine();
    GameState spyState = spy(realEngine.getState());
    when(spyState.isGameOver()).thenReturn(true);

    // Make current player an AI so we don't call readLine
    Player mockAi = mock(Player.class);
    when(mockAi.isAI()).thenReturn(true);
    when(mockAi.getName()).thenReturn("AI");
    when(mockAi.getNextMove(any())).thenReturn(Move.pawn(new Position(0, 4), new Position(1, 4)));
    when(spyState.getCurrentPlayer()).thenReturn(mockAi);

    GameEngine spyEngine = spy(new GameEngine(spyState, false, false, false, false, 10));

    CliApp appWithBrokenTerminal = new CliApp(spyEngine, mockTerminal, reader);

    // Should not throw exception
    assertDoesNotThrow(appWithBrokenTerminal::run);

    verify(mockTerminal).close();
  }

  // Tests de reseau CLI

  @Test
  void serverCommand() throws IOException {
    app.fetch("server start 23456");
    app.fetch("server stop");
    app.fetch("server status");
    app.fetch("server list");
    // Invalid server command (should not crash)
    app.fetch("server");
    app.fetch("players");
    app.fetch("scoreboard");

    verify(networkServer).start(23456);
    verify(networkServer).stop();
    verify(networkServer).getServerStatus();
    verify(networkClient).serverList();
    verify(networkServer).listPlayers();
    verify(networkServer).getScoreboard();
  }

  @Test
  void clientCommand() throws IOException {
    app.fetch("join localhost:11111");
    app.fetch("ping");
    app.fetch("hello Alice");

    verify(networkClient).join("localhost", 11111);
    verify(networkClient).ping();
    verify(networkClient).hello("Alice");
  }

  @Test
  void gameCommand() throws IOException {
    when(networkClient.isConnected()).thenReturn(true);

    app.fetch("new C1 C2");
    app.fetch("move e1-e2");
    // Default fallback when network client is connected
    app.fetch("some_unhandled_action");
    app.fetch("quit");

    verify(networkServer).createGame("C1", "C2");
    verify(networkClient).sendMove("some_unhandled_action");
    verify(networkClient).sendMove("e1-e2");
    verify(networkClient).quit();
    assertTrue(app.isRunning());
  }

  @Test
  void handleNetworkCommandException() throws IOException {
    doThrow(new IllegalArgumentException("Invalid port")).when(networkServer).start(anyInt());
    // This should be caught by catch (final IllegalArgumentException exception)
    assertDoesNotThrow(() -> app.fetch("server start 999999"));
  }

  @Test
  void boardSync() throws IOException {
    final ArgumentCaptor<Consumer<String>> captor = ArgumentCaptor.forClass(Consumer.class);

    verify(networkClient).setServerMessageHandler(captor.capture());
    when(networkClient.isConnected()).thenReturn(true);

    app.fetch("move e1-e2");
    captor.getValue().accept("OK");

    assertEquals(new Position(1, 4), p1.getPosition());
  }

  @Test
  void opponentSync() {
    final ArgumentCaptor<Consumer<String>> captor = ArgumentCaptor.forClass(Consumer.class);

    verify(networkClient).setServerMessageHandler(captor.capture());

    captor.getValue().accept("OPPONENT_MOVE e1-e2");

    assertEquals(new Position(1, 4), p1.getPosition());
  }
}
