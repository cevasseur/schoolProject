package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.core.Board;
import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Position;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.network.GameClient;
import fr.bordeaux.network.MultiGameServer;
import fr.bordeaux.persistence.GameLoader;
import fr.bordeaux.persistence.GameSaver;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

class ActionDispatcherTest extends ApplicationTest {

  private GameEngine engine;
  private FileHandler fileHandler;
  private TestActionDispatcher dispatcher;
  private MultiGameServer multiServerMock;
  private GameClient networkClientMock;

  @Override
  public void start(Stage stage) {
    // Toolkit initialization for JavaFX tests
  }

  /**
   * Test double that bypasses real JavaFX dialogs (Alert, TextInputDialog) to prevent blocking the
   * test execution. Original methods are tested independently to maintain code coverage.
   */
  private static class TestActionDispatcher extends ActionDispatcher {
    private String nextPromptResult = "mock-input";
    private final List<String> messagesShown = new ArrayList<>();

    public TestActionDispatcher(GameEngine engine, FileHandler fileHandler) {
      super(engine, fileHandler);
    }

    @Override
    protected void showMessage(String title, String header, String content) {
      messagesShown.add(title + ":" + header + ":" + content);
    }

    @Override
    protected String promptUser(String title, String header, String content, String defaultValue) {
      return nextPromptResult;
    }

    public void setNextPromptResult(String result) {
      this.nextPromptResult = result;
    }

    public List<String> getMessagesShown() {
      return messagesShown;
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    engine = mock(GameEngine.class);
    fileHandler = mock(FileHandler.class);

    TestActionDispatcher realDispatcher = new TestActionDispatcher(engine, fileHandler);
    multiServerMock = mock(MultiGameServer.class);
    networkClientMock = mock(GameClient.class);

    injectMock(realDispatcher, "multiServer", multiServerMock);
    injectMock(realDispatcher, "networkClient", networkClientMock);

    // We spy the TestActionDispatcher so we can still use verify() on it.
    dispatcher = spy(realDispatcher);
  }

  private static void setPrivateField(Object target, String fieldName, Object value)
      throws Exception {
    Field field = ActionDispatcher.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Object getPrivateField(Object target, String fieldName) throws Exception {
    Field field = ActionDispatcher.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void invokeHandleServerMessage(ActionDispatcher dispatcher, String message) {
    try {
      invokePrivate(dispatcher, "handleServerMessage", new Class<?>[] {String.class}, message);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void injectMock(Object target, String fieldName, Object mockValue) throws Exception {
    Field field = ActionDispatcher.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, mockValue);
  }

  private static Object invokePrivate(
      Object target, String methodName, Class<?>[] parameterTypes, Object... args)
      throws Exception {
    Method method = ActionDispatcher.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return method.invoke(target, args);
  }

  @Test
  void testNewGame() throws IOException {
    dispatcher.dispatch(Action.NEW_GAME);
    verify(engine).newGame(anyInt(), anyList(), any());
  }

  @Test
  void testLoadGame() throws IOException {
    try (MockedStatic<GameLoader> mockedLoader = mockStatic(GameLoader.class)) {
      when(fileHandler.chooseLoadFile()).thenReturn("test.txt");
      mockedLoader
          .when(() -> GameLoader.loadGame(any(), anyString()))
          .thenReturn(mock(GameState.class));
      dispatcher.dispatch(Action.LOAD_GAME);
      mockedLoader.verify(() -> GameLoader.loadGame(engine, "test.txt"));
    }
  }

  @Test
  void testSaveGame() throws IOException {
    try (MockedStatic<GameSaver> mockedSaver = mockStatic(GameSaver.class)) {
      when(fileHandler.chooseSaveFile()).thenReturn("test.txt");
      dispatcher.dispatch(Action.SAVE_GAME);
      mockedSaver.verify(() -> GameSaver.saveGame(engine, "test.txt"));
    }
  }

  @Test
  void testUndo() throws IOException {
    dispatcher.dispatch(Action.UNDO);
    verify(engine).undo();
  }

  @Test
  void testRedo() throws IOException {
    dispatcher.dispatch(Action.REDO);
    verify(engine).redo();
  }

  @Test
  void testPause() throws IOException {
    dispatcher.dispatch(Action.PAUSE);
    verify(engine).pause();
  }

  /* No interaction in this mock
  @Test
  void testHint() throws IOException {
    dispatcher.dispatch(Action.HINT);
    verify(engine).hint();
  } */

  @Test
  void testConfiguration() throws IOException {
    try (MockedStatic<ConfigDialog> mocked = mockStatic(ConfigDialog.class)) {
      dispatcher.dispatch(Action.CONFIGURATION);
      mocked.verify(ConfigDialog::show);
    }
  }

  @Test
  void testInfo() throws IOException {
    try (MockedStatic<InfoDialog> mocked = mockStatic(InfoDialog.class)) {
      dispatcher.dispatch(Action.INFO);
      mocked.verify(InfoDialog::show);
    }
  }

  @Test
  void testQuit() throws IOException {
    try (MockedStatic<Platform> mocked = mockStatic(Platform.class)) {
      dispatcher.dispatch(Action.QUIT);
      mocked.verify(Platform::exit);
    }
  }

  @Test
  void testServerStatusShowsCurrentState() throws IOException {
    when(multiServerMock.getServerStatus()).thenReturn("Mocked Status");
    dispatcher.dispatch(Action.SERVER_STATUS);
    verify(dispatcher).showMessage(eq("Network"), eq("Server Status"), eq("Mocked Status"));
  }

  @Test
  void testServerListShowsDiscoveryMessage() throws IOException {
    when(networkClientMock.serverList()).thenReturn("ServerA:12345");
    dispatcher.dispatch(Action.SERVER_LIST);
    verify(networkClientMock).serverList();
    verify(dispatcher).showMessage(eq("Network"), eq("Server List"), eq("ServerA:12345"));
  }

  @Test
  void testStartServerPromptsAndShowsConfirmation() throws IOException {
    dispatcher.setNextPromptResult("12345");
    dispatcher.dispatch(Action.START_SERVER);
    verify(multiServerMock).start(12345);
    verify(dispatcher).showMessage(eq("Network"), eq("Server"), contains("started"));
  }

  @Test
  void testStartServer_Cancel() throws IOException {
    dispatcher.setNextPromptResult(null);
    dispatcher.dispatch(Action.START_SERVER);
    verify(multiServerMock, never()).start(anyInt());
  }

  @Test
  void testStopServer() throws IOException {
    dispatcher.dispatch(Action.STOP_SERVER);
    verify(multiServerMock).stop();
    verify(dispatcher).showMessage(eq("Network"), eq("Server"), contains("stopped"));
  }

  @Test
  void testPlayers() throws IOException {
    when(multiServerMock.listPlayers()).thenReturn("Player1\nPlayer2");
    dispatcher.dispatch(Action.PLAYERS);
    verify(dispatcher).showMessage(eq("Network"), eq("Connected Players"), eq("Player1\nPlayer2"));
  }

  @Test
  void testScoreboard() throws IOException {
    when(multiServerMock.getScoreboard()).thenReturn("Scoreboard Data");
    dispatcher.dispatch(Action.SCOREBOARD);
    verify(dispatcher).showMessage(eq("Network"), eq("Scoreboard"), eq("Scoreboard Data"));
  }

  @Test
  void testJoinServer_Cancel() {
    dispatcher.setNextPromptResult(null);
    dispatcher.joinServer();
    verify(networkClientMock, never()).join(anyString(), anyInt());
  }

  @Test
  void testJoinServer_Blank() {
    dispatcher.setNextPromptResult("  ");
    dispatcher.joinServer();
    verify(networkClientMock, never()).join(anyString(), anyInt());
  }

  @Test
  void testJoinServer_DefaultPort() {
    dispatcher.setNextPromptResult("localhost");
    when(networkClientMock.join("localhost", 12345)).thenReturn(true);
    dispatcher.joinServer();
    verify(networkClientMock).join("localhost", 12345);
  }

  @Test
  void testJoinServer_Success() {
    dispatcher.setNextPromptResult("localhost:12345");
    when(networkClientMock.join("localhost", 12345)).thenReturn(true);

    dispatcher.joinServer();

    verify(networkClientMock).join("localhost", 12345);
    verify(dispatcher).showMessage(eq("Network"), eq("Join Server"), contains("Connected"));
  }

  @Test
  void testJoinServer_Failure() {
    dispatcher.setNextPromptResult("localhost:12345");
    when(networkClientMock.join("localhost", 12345)).thenReturn(false);

    dispatcher.joinServer();

    verify(dispatcher).showMessage(eq("Network"), eq("Join Server"), contains("Failed"));
  }

  @Test
  void testCreateNetworkGame_Cancel() {
    dispatcher.setNextPromptResult(null);
    dispatcher.createNetworkGame();
    verify(multiServerMock, never()).createGame(any());
  }

  @Test
  void testCreateNetworkGame_Blank() {
    dispatcher.setNextPromptResult("");
    dispatcher.createNetworkGame();
    verify(multiServerMock, never()).createGame(any());
  }

  @Test
  void testCreateNetworkGame() {
    dispatcher.setNextPromptResult("C1 C2");
    when(multiServerMock.createGame(any(String[].class))).thenReturn("Mocked Game Created");

    dispatcher.createNetworkGame();

    verify(multiServerMock).createGame(new String[] {"C1", "C2"});
    verify(dispatcher).showMessage(eq("Network"), eq("New Game"), eq("Mocked Game Created"));
  }

  @Test
  void testSetNetworkName_Cancel() {
    dispatcher.setNextPromptResult(null);
    dispatcher.setNetworkName();
    verify(networkClientMock, never()).hello(anyString());
  }

  @Test
  void testSetNetworkName_Blank() {
    dispatcher.setNextPromptResult(" ");
    dispatcher.setNetworkName();
    verify(networkClientMock, never()).hello(anyString());
  }

  @Test
  void testSetNetworkName() {
    dispatcher.setNextPromptResult("Player1");

    dispatcher.setNetworkName();

    verify(networkClientMock).hello("Player1");
    verify(dispatcher).showMessage(eq("Network"), eq("Player Name"), contains("Name sent"));
  }

  @Test
  void testPingServer() throws IOException {
    dispatcher.dispatch(Action.PING_SERVER);
    verify(networkClientMock).ping();
  }

  @Test
  void testQuitServer() throws IOException {
    dispatcher.dispatch(Action.QUIT_SERVER);
    verify(networkClientMock).quit();
  }

  @Test
  void testDispatch_NetworkActions() throws IOException {
    // Verify that dispatch correctly routes to network methods
    doNothing().when(dispatcher).createNetworkGame();
    dispatcher.dispatch(Action.NEW_NETWORK_GAME);
    verify(dispatcher).createNetworkGame();

    doNothing().when(dispatcher).joinServer();
    dispatcher.dispatch(Action.JOIN_SERVER);
    verify(dispatcher).joinServer();

    doNothing().when(dispatcher).setNetworkName();
    dispatcher.dispatch(Action.SET_NETWORK_NAME);
    verify(dispatcher).setNetworkName();
  }

  @Test
  void testRealShowMessage() {
    ActionDispatcher real = new ActionDispatcher(engine, fileHandler);
    interact(
        () -> {
          try (MockedConstruction<Alert> mockedAlert = mockConstruction(Alert.class)) {
            real.showMessage("T", "H", "C");
            Alert alert = mockedAlert.constructed().get(0);
            verify(alert).setTitle("T");
            verify(alert).show();
          }
        });
  }

  @Test
  void testRealPromptUser() {
    ActionDispatcher real = new ActionDispatcher(engine, fileHandler);
    interact(
        () -> {
          try (MockedConstruction<TextInputDialog> mockedDialog =
              mockConstruction(
                  TextInputDialog.class,
                  (mock, context) -> {
                    when(mock.showAndWait()).thenReturn(Optional.of("input123"));
                  })) {
            String result = real.promptUser("T", "H", "C", "D");
            assertEquals("input123", result);
            TextInputDialog dialog = mockedDialog.constructed().get(0);
            verify(dialog).setTitle("T");
          }
        });
  }

  @Test
  void testPingServerShowsNetworkReply() throws IOException {
    GameEngine engine = mock(GameEngine.class);
    FileHandler fileHandler = mock(FileHandler.class);
    ActionDispatcher dispatcher = spy(new ActionDispatcher(engine, fileHandler));

    doNothing().when(dispatcher).showMessage(anyString(), anyString(), anyString());

    dispatcher.dispatch(Action.PING_SERVER);

    verify(dispatcher)
        .showMessage(eq("Network"), eq("Ping Server"), contains(I18n.get("notConnected")));
  }

  @Test
  void testQuitServerShowsNetworkReply() throws IOException {
    GameEngine engine = mock(GameEngine.class);
    FileHandler fileHandler = mock(FileHandler.class);
    ActionDispatcher dispatcher = spy(new ActionDispatcher(engine, fileHandler));

    doNothing().when(dispatcher).showMessage(anyString(), anyString(), anyString());

    dispatcher.dispatch(Action.QUIT_SERVER);

    verify(dispatcher).showMessage(eq("Network"), eq("Quit Server"), contains("Client exited"));
  }

  @Test
  void isMyTurnFalse() {
    assertFalse(dispatcher.isMyTurn());
  }

  @Test
  void playBoardMoveLocal() {
    BoardView boardView = mock(BoardView.class);
    Move move = Move.pawn(new Position(0, 4), new Position(1, 4));

    when(engine.play(move)).thenReturn(true);
    dispatcher.setBoardView(boardView);

    dispatcher.playBoardMove(move);

    verify(engine).play(move);
    verify(boardView).drawBoard();
  }

  @Test
  void moveToTextPawn() throws Exception {
    Move move = Move.pawn(new Position(0, 4), new Position(1, 4));

    String text =
        (String) invokePrivate(dispatcher, "moveToText", new Class<?>[] {Move.class}, move);

    assertEquals("e1-e2", text);
  }

  @Test
  void positionToTextOk() throws Exception {
    String text =
        (String)
            invokePrivate(
                dispatcher, "positionToText", new Class<?>[] {Position.class}, new Position(2, 6));

    assertEquals("g3", text);
  }

  @Test
  void playBoardMoveNotMyTurn() throws Exception {
    Move move = Move.pawn(new Position(0, 4), new Position(1, 4));

    setPrivateField(dispatcher, "networkGame", true);
    setPrivateField(dispatcher, "myTurn", false);

    dispatcher.playBoardMove(move);

    verify(networkClientMock, never()).sendMove(anyString());
    verify(dispatcher).showMessage("Network", "Turn", "It is not your turn.");
  }

  @Test
  void playBoardMoveSendMove() throws Exception {
    Move move = Move.pawn(new Position(0, 4), new Position(1, 4));

    setPrivateField(dispatcher, "networkGame", true);
    setPrivateField(dispatcher, "myTurn", true);
    setPrivateField(dispatcher, "pendingMove", null);

    dispatcher.playBoardMove(move);

    verify(networkClientMock).sendMove("e1-e2");
    assertEquals("e1-e2", getPrivateField(dispatcher, "pendingMove"));
  }

  @Test
  void applyNetworkMoveOk() throws Exception {
    BoardView boardView = mock(BoardView.class);
    dispatcher.setBoardView(boardView);
    when(engine.play(any(Move.class))).thenReturn(true);

    invokePrivate(dispatcher, "applyNetworkMove", new Class<?>[] {String.class}, "e1-e2");

    verify(engine).play(any(Move.class));
    verify(boardView).drawBoard();
  }

  @Test
  void setupNetworkBoardOk() throws Exception {
    GameState state = mock(GameState.class);
    Board board = mock(Board.class);
    BoardView boardView = mock(BoardView.class);

    when(engine.getState()).thenReturn(state);
    when(state.getBoard()).thenReturn(board);
    when(board.getSize()).thenReturn(9);

    dispatcher.setBoardView(boardView);
    setPrivateField(dispatcher, "localPlayerName", "Khadidja");

    invokePrivate(dispatcher, "setupNetworkBoard", new Class<?>[] {boolean.class}, true);

    verify(engine).newGame(eq(9), anyList(), isNull());
    verify(boardView).drawBoard();
  }

  @Test
  void handleServerMessageStart() throws Exception {
    GameState state = mock(GameState.class);
    Board board = mock(Board.class);
    BoardView boardView = mock(BoardView.class);

    when(engine.getState()).thenReturn(state);
    when(state.getBoard()).thenReturn(board);
    when(board.getSize()).thenReturn(9);

    dispatcher.setBoardView(boardView);

    interact(() -> invokeHandleServerMessage(dispatcher, "GAME_START YOUR_TURN"));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue((Boolean) getPrivateField(dispatcher, "networkGame"));
    assertTrue(dispatcher.isMyTurn());
    verify(engine).newGame(eq(9), anyList(), isNull());
    verify(dispatcher).showMessage("Network", "Game Start", "Your turn.");
  }

  @Test
  void handleServerMessageStop() throws Exception {
    setPrivateField(dispatcher, "pendingMove", "e1-e2");
    setPrivateField(dispatcher, "myTurn", true);
    setPrivateField(dispatcher, "networkGame", true);

    interact(() -> invokeHandleServerMessage(dispatcher, "SERVER_STOPPED"));
    WaitForAsyncUtils.waitForFxEvents();

    assertNull(getPrivateField(dispatcher, "pendingMove"));
    assertFalse(dispatcher.isMyTurn());
    assertFalse((Boolean) getPrivateField(dispatcher, "networkGame"));
    verify(dispatcher).showMessage("Network", "Server", "Server stopped.");
  }

  @Test
  void setupBoardFalse() throws Exception {
    GameState state = mock(GameState.class);
    Board board = mock(Board.class);
    BoardView boardView = mock(BoardView.class);

    when(engine.getState()).thenReturn(state);
    when(state.getBoard()).thenReturn(board);
    when(board.getSize()).thenReturn(9);

    dispatcher.setBoardView(boardView);
    setPrivateField(dispatcher, "localPlayerName", "Khadidja");

    invokePrivate(dispatcher, "setupNetworkBoard", new Class<?>[] {boolean.class}, false);

    verify(engine).newGame(eq(9), anyList(), isNull());
    verify(boardView).drawBoard();
  }

  @Test
  void messageOk() throws Exception {
    BoardView boardView = mock(BoardView.class);
    dispatcher.setBoardView(boardView);
    when(engine.play(any(Move.class))).thenReturn(true);

    setPrivateField(dispatcher, "pendingMove", "e1-e2");
    setPrivateField(dispatcher, "myTurn", true);

    interact(() -> invokeHandleServerMessage(dispatcher, "OK"));
    WaitForAsyncUtils.waitForFxEvents();

    verify(engine).play(any(Move.class));
    verify(boardView).drawBoard();
    assertNull(getPrivateField(dispatcher, "pendingMove"));
    assertFalse(dispatcher.isMyTurn());
  }

  @Test
  void messageError() throws Exception {
    setPrivateField(dispatcher, "pendingMove", "e1-e2");

    interact(() -> invokeHandleServerMessage(dispatcher, "ERROR bad move"));
    WaitForAsyncUtils.waitForFxEvents();

    assertNull(getPrivateField(dispatcher, "pendingMove"));
    verify(dispatcher).showMessage("Network", "Server Error", "ERROR bad move");
  }

  @Test
  void messageOpponent() throws Exception {
    BoardView boardView = mock(BoardView.class);
    dispatcher.setBoardView(boardView);
    when(engine.play(any(Move.class))).thenReturn(true);

    setPrivateField(dispatcher, "myTurn", false);

    interact(() -> invokeHandleServerMessage(dispatcher, "OPPONENT_MOVE e8-e7"));
    WaitForAsyncUtils.waitForFxEvents();

    verify(engine).play(any(Move.class));
    verify(boardView).drawBoard();
    assertTrue(dispatcher.isMyTurn());
  }
}
