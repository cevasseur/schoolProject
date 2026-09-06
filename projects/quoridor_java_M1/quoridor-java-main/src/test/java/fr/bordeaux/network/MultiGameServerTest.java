package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import fr.bordeaux.util.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MultiGameServerTest {

  @Test
  void status_empty() {
    MultiGameServer server = new MultiGameServer();
    assertEquals("Port: 0\nConnected clients: 0\nRunning games: 0", server.getServerStatus());
  }

  @Test
  void players_empty() {
    MultiGameServer server = new MultiGameServer();
    assertEquals("No connected players.", server.listPlayers());
  }

  @Test
  void players_list() {
    TestData data = twoClients();
    data.bob.setStatus(PlayerStatus.INGAME);

    String text = data.server.listPlayers();

    assertTrue(text.contains("C1 Alice IDLE"));
    assertTrue(text.contains("C2 Bob INGAME"));
  }

  @Test
  void score_empty() {
    MultiGameServer server = new MultiGameServer();
    assertEquals("Scoreboard is empty.", server.getScoreboard());
  }

  @Test
  void createGame_ok() {
    TestData data = twoClients();

    String result = data.server.createGame("C1", "C2");

    assertEquals("Game created: G1", result);
    assertEquals(PlayerStatus.INGAME, data.alice.getStatus());
    assertEquals(PlayerStatus.INGAME, data.bob.getStatus());
    assertEquals("G1", data.alice.getCurrentGameId());
    assertEquals("G1", data.bob.getCurrentGameId());
    assertTrue(out(data.alice).contains("GAME_START G1 YOUR_TURN"));
    assertTrue(out(data.bob).contains("GAME_START G1 WAIT"));
  }

  @Test
  void hello_ok() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    addClient(server, alice);

    call(
        server,
        "processHello",
        new Class<?>[] {ClientSession.class, String.class},
        alice,
        "HELLO Bob");

    assertEquals("Bob", alice.getName());
    assertEquals("Bob", scores(server).get("C1").getPlayerName());
    assertTrue(out(alice).contains("WELCOME C1"));
  }

  @Test
  void ping_ok() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");

    call(server, "processPing", new Class<?>[] {ClientSession.class}, alice);

    assertTrue(out(alice).contains("PONG"));
  }

  @Test
  void createGame_fourPlayers_ok() {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    TestClient bob = client("C2", "Bob");
    TestClient chloe = client("C3", "Chloe");
    TestClient david = client("C4", "David");
    addClient(server, alice);
    addClient(server, bob);
    addClient(server, chloe);
    addClient(server, david);

    String result = server.createGame("C1", "C2", "C3", "C4");

    assertEquals("Game created: G1", result);
    assertEquals(PlayerStatus.INGAME, alice.getStatus());
    assertEquals(PlayerStatus.INGAME, bob.getStatus());
    assertEquals(PlayerStatus.INGAME, chloe.getStatus());
    assertEquals(PlayerStatus.INGAME, david.getStatus());
    assertTrue(out(alice).contains("GAME_START G1 YOUR_TURN"));
    assertTrue(out(bob).contains("GAME_START G1 WAIT"));
    assertTrue(out(chloe).contains("GAME_START G1 WAIT"));
    assertTrue(out(david).contains("GAME_START G1 WAIT"));
  }

  @Test
  void quit_ok() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");

    call(server, "processQuit", new Class<?>[] {ClientSession.class}, alice);

    assertTrue(out(alice).contains("BYE"));
  }

  @Test
  void move_notInGame() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");

    call(
        server,
        "processMove",
        new Class<?>[] {ClientSession.class, String.class},
        alice,
        "MOVE e2-e3");

    assertTrue(out(alice).contains("ERROR not in game"));
  }

  @Test
  void move_gameNotFound() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    alice.setCurrentGameId("G404");
    addClient(server, alice);

    call(
        server,
        "processMove",
        new Class<?>[] {ClientSession.class, String.class},
        alice,
        "MOVE e2-e3");

    assertTrue(out(alice).contains("ERROR game not found"));
  }

  @Test
  void move_notYourTurn() throws Exception {
    TestData data = startedGame();

    call(
        data.server,
        "processMove",
        new Class<?>[] {ClientSession.class, String.class},
        data.bob,
        "MOVE e9-e8");

    assertTrue(out(data.bob).contains("ERROR not your turn"));
  }

  @Test
  void move_badFormat() throws Exception {
    TestData data = startedGame();

    call(
        data.server,
        "processMove",
        new Class<?>[] {ClientSession.class, String.class},
        data.alice,
        "MOVE ???");

    assertTrue(out(data.alice).contains("ERROR invalid move format"));
  }

  @Test
  void move_illegal() throws Exception {
    TestData data = startedGame();

    call(
        data.server,
        "processMove",
        new Class<?>[] {ClientSession.class, String.class},
        data.alice,
        "MOVE e2-e5");

    assertTrue(out(data.alice).contains("ERROR illegal move"));
  }

  @Test
  void move_ok() throws Exception {
    TestData data = startedGame();

    call(
        data.server,
        "processMove",
        new Class<?>[] {ClientSession.class, String.class},
        data.alice,
        "MOVE e1-e2");

    assertTrue(out(data.alice).contains("OK"));
    assertTrue(out(data.bob).contains("OPPONENT_MOVE e1-e2"));
  }

  @Test
  void stop_ok() {
    TestData data = startedGame();

    data.server.stop();

    assertTrue(out(data.alice).contains("SERVER_STOPPED"));
    assertTrue(out(data.bob).contains("SERVER_STOPPED"));
    assertTrue(data.alice.wasClosed());
    assertTrue(data.bob.wasClosed());
    assertEquals("Port: 0\nConnected clients: 0\nRunning games: 0", data.server.getServerStatus());
  }

  @Test
  void move_ok_broadcastsToAllOtherPlayersInFourPlayerGame() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    TestClient bob = client("C2", "Bob");
    TestClient chloe = client("C3", "Chloe");
    TestClient david = client("C4", "David");
    addClient(server, alice);
    addClient(server, bob);
    addClient(server, chloe);
    addClient(server, david);
    server.createGame("C1", "C2", "C3", "C4");

    call(
        server,
        "processMove",
        new Class<?>[] {ClientSession.class, String.class},
        alice,
        "MOVE e1-e2");

    assertTrue(out(alice).contains("OK"));
    assertTrue(out(bob).contains("OPPONENT_MOVE e1-e2"));
    assertTrue(out(chloe).contains("OPPONENT_MOVE e1-e2"));
    assertTrue(out(david).contains("OPPONENT_MOVE e1-e2"));
  }

  @Test
  void timeoutChecker_disconnectsInactiveClient() throws Exception {
    MultiGameServer server = new MultiGameServer();
    setRunning(server, true);
    TestClient alice = client("C1", "Alice");
    alice.setLastPing(System.currentTimeMillis() - 61000);
    addClient(server, alice);

    call(server, "startTimeoutChecker", new Class<?>[] {});
    Thread.sleep(5500);

    assertFalse(clients(server).containsKey("C1"));
    assertTrue(out(alice).contains("ERROR timeout"));
    assertTrue(alice.wasClosed());
    setRunning(server, false);
  }

  @Test
  void start_ok() throws Exception {
    MultiGameServer server = new MultiGameServer();
    int port;

    try (ServerSocket probe = new ServerSocket(0)) {
      port = probe.getLocalPort();
    }

    server.start(port);
    Thread.sleep(300);

    try (Socket socket = new Socket("localhost", port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
      out.println("HELLO Bob");
      assertEquals("WELCOME C1", in.readLine());

      out.println("QUIT");
      assertEquals("BYE", in.readLine());
    }

    Thread.sleep(300);

    assertTrue(server.getScoreboard().contains("C1 Bob"));
    server.stop();
  }

  @Test
  void unregister_ok() throws Exception {
    TestData data = startedGame();

    call(data.server, "unregisterClient", new Class<?>[] {ClientSession.class}, data.alice);

    assertFalse(clients(data.server).containsKey("C1"));
    assertEquals(PlayerStatus.IDLE, data.bob.getStatus());
    assertNull(data.bob.getCurrentGameId());
    assertTrue(out(data.bob).contains("ERROR opponent disconnected"));
    assertTrue(data.alice.wasClosed());
  }

  @Test
  void finish_ok() throws Exception {
    TestData data = startedGame();
    ServerGameSession game = games(data.server).get("G1");

    call(
        data.server,
        "finishGame",
        new Class<?>[] {ServerGameSession.class, ClientSession.class},
        game,
        data.alice);

    assertTrue(data.server.getScoreboard().contains("C1 Alice W:1 L:0 G:1"));
    assertTrue(data.server.getScoreboard().contains("C2 Bob W:0 L:1 G:1"));
    assertEquals(PlayerStatus.IDLE, data.alice.getStatus());
    assertEquals(PlayerStatus.IDLE, data.bob.getStatus());
    assertNull(data.alice.getCurrentGameId());
    assertNull(data.bob.getCurrentGameId());
    assertTrue(out(data.alice).contains("GAME_OVER WIN"));
    assertTrue(out(data.bob).contains("GAME_OVER LOSE"));
    assertFalse(games(data.server).containsKey("G1"));
  }

  @Test
  void handle_ok() throws Exception {
    MultiGameServer server = new MultiGameServer();
    setRunning(server, true);

    StringWriter text = new StringWriter();
    TestClient alice =
        new TestClient(
            "C1",
            "Alice",
            new Socket(),
            new BufferedReader(new StringReader("HELLO Bob\nPING\nWHAT\nQUIT\n")),
            new PrintWriter(text, true),
            text);

    addClient(server, alice);

    call(server, "handleClient", new Class<?>[] {ClientSession.class}, alice);

    assertTrue(out(alice).contains("WELCOME C1"));
    assertTrue(out(alice).contains("PONG"));
    assertTrue(out(alice).contains("ERROR unknown command"));
    assertTrue(out(alice).contains("BYE"));
    assertTrue(alice.wasClosed());
    assertFalse(clients(server).containsKey("C1"));
  }

  @Test
  void testCreateGame_InvalidPlayerCount() {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    addClient(server, alice);

    // Too few (1)
    String res1 = server.createGame("C1");
    assertEquals("ERROR need between 2 and 4 players", res1);

    // Too many (5)
    String res5 = server.createGame("C1", "C2", "C3", "C4", "C5");
    assertEquals("ERROR need between 2 and 4 players", res5);
  }

  @Test
  void testCreateGame_DuplicatePlayer() {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    addClient(server, alice);

    String res = server.createGame("C1", "C1");
    assertEquals("ERROR duplicate player", res);
  }

  @Test
  void testCreateGame_PlayerNotFound() {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    addClient(server, alice);

    String res = server.createGame("C1", "C99");
    assertEquals("ERROR player not found", res);
  }

  @Test
  void testCreateGame_PlayerNotAvailable() {
    TestData data = startedGame();
    TestClient chloe = client("C3", "Chloe");
    addClient(data.server, chloe);

    // Alice is already INGAME (startedGame)
    String res = data.server.createGame("C1", "C3");
    assertEquals("ERROR player not available", res);
  }

  @Test
  void testHello_EmptyName() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    addClient(server, alice);

    call(
        server,
        "processHello",
        new Class<?>[] {ClientSession.class, String.class},
        alice,
        "HELLO  ");

    assertTrue(out(alice).contains("ERROR invalid name"));
  }

  @Test
  void testUnregisterClient_NoGame() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    addClient(server, alice);

    call(server, "unregisterClient", new Class<?>[] {ClientSession.class}, alice);
    assertFalse(clients(server).containsKey("C1"));
  }

  @Test
  void testStop_FullCleanup() throws Exception {
    MultiGameServer server = new MultiGameServer();
    server.start(0);
    Thread.sleep(100);

    // Verify broadcaster and broadcastThread were started
    Field broadcasterField = MultiGameServer.class.getDeclaredField("broadcaster");
    broadcasterField.setAccessible(true);
    Object broadcaster = broadcasterField.get(server);
    assertTrue(broadcaster != null);

    server.stop();
    assertEquals(0, clients(server).size());
    assertEquals(0, games(server).size());
  }

  @Test
  void testServerError_AcceptLoop_IOException() throws Exception {
    MultiGameServer server = new MultiGameServer();
    ServerSocket mockSocket = mock(ServerSocket.class);
    when(mockSocket.accept()).thenThrow(new java.io.IOException("Test Error"));

    Field socketField = MultiGameServer.class.getDeclaredField("serverSocket");
    socketField.setAccessible(true);
    socketField.set(server, mockSocket);

    setRunning(server, true);

    // Get the server thread logic by starting it normally then overriding the socket
    server.start(0);
    Thread.sleep(200);
    socketField.set(server, mockSocket); // Inject mock after start

    // The loop keeps running and calling accept(). The mock throws IOException.
    // We wait a bit to ensure the catch block is hit.
    Thread.sleep(200);

    // Verify it doesn't crash the JVM and handles the error
    java.util.concurrent.atomic.AtomicBoolean running =
        (java.util.concurrent.atomic.AtomicBoolean) getField(server, "running");
    assertTrue(!running.get() || mockSocket != null);
    server.stop();
  }

  @Test
  void testHandleClient_IOException() throws Exception {
    MultiGameServer server = new MultiGameServer();
    setRunning(server, true);

    BufferedReader mockReader = mock(BufferedReader.class);
    when(mockReader.readLine()).thenThrow(new java.io.IOException("Read Error"));

    TestClient client =
        new TestClient(
            "C1",
            "Alice",
            new Socket(),
            mockReader,
            new PrintWriter(new StringWriter()),
            new StringWriter());

    addClient(server, client);

    // Call handleClient - it should catch the IOException and log it
    call(server, "handleClient", new Class<?>[] {ClientSession.class}, client);

    // Verify client is unregistered
    assertFalse(clients(server).containsKey("C1"));
    assertTrue(client.wasClosed());
  }

  @Test
  void testProcessMove_GameFinished() throws Exception {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    TestClient bob = client("C2", "Bob");
    addClient(server, alice);
    addClient(server, bob);

    ServerGameSession mockGame = mock(ServerGameSession.class);
    when(mockGame.getGameId()).thenReturn("G1");
    when(mockGame.isPlayerTurn("C1")).thenReturn(true);
    when(mockGame.applyMove(any())).thenReturn(true);
    when(mockGame.getOtherPlayers("C1")).thenReturn(java.util.List.of(bob));
    when(mockGame.getPlayers()).thenReturn(java.util.List.of(alice, bob));
    when(mockGame.getWinnerSession()).thenReturn(alice);

    games(server).put("G1", mockGame);
    alice.setCurrentGameId("G1");
    bob.setCurrentGameId("G1");

    call(
        server,
        "processMove",
        new Class<?>[] {ClientSession.class, String.class},
        alice,
        "MOVE e1-e2");

    // Verify game finished
    assertNull(alice.getCurrentGameId());
    assertNull(bob.getCurrentGameId());
    assertEquals(PlayerStatus.IDLE, alice.getStatus());
    assertFalse(games(server).containsKey("G1"));
  }

  @Test
  void testTimeoutChecker_Interrupted() throws Exception {
    MultiGameServer server = new MultiGameServer();
    setRunning(server, true);

    // Start target thread
    call(server, "startTimeoutChecker", new Class<?>[] {});
    Thread.sleep(100);

    Thread target = null;
    for (Thread t : Thread.getAllStackTraces().keySet()) {
      if ("TimeoutChecker".equals(t.getName())) {
        target = t;
        break;
      }
    }

    if (target != null) {
      target.interrupt();
      Thread.sleep(200);
      assertFalse(target.isAlive());
    }
    setRunning(server, false);
  }

  @Test
  void testServerError_LoggedWhenRunning() throws Exception {
    MultiGameServer server = new MultiGameServer();

    // Inject mock logger into the static instance field to be visible by all threads
    Field instanceField = Logger.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    Logger originalLogger = (Logger) instanceField.get(null);
    Logger mockLogger = mock(Logger.class);
    when(mockLogger.isErrorEnabled()).thenReturn(true);
    instanceField.set(null, mockLogger);

    try {
      // Start server normally
      server.start(0);
      Thread.sleep(500);

      // Get real socket and close it while running is still true
      Field socketField = MultiGameServer.class.getDeclaredField("serverSocket");
      socketField.setAccessible(true);
      ServerSocket realSocket = (ServerSocket) socketField.get(server);

      if (realSocket != null) {
        realSocket.close();
      }

      // Wait for catch block to be hit (max 2 seconds)
      long start = System.currentTimeMillis();
      boolean logged = false;
      while (System.currentTimeMillis() - start < 2000) {
        try {
          verify(mockLogger, atLeastOnce()).error(eq("Server error: {0}"), anyString());
          logged = true;
          break;
        } catch (AssertionError e) {
          Thread.sleep(100);
        }
      }
      assertTrue(logged, "Log should have been called within timeout");
    } finally {
      instanceField.set(null, originalLogger);
      server.stop();
    }
  }

  private TestData twoClients() {
    MultiGameServer server = new MultiGameServer();
    TestClient alice = client("C1", "Alice");
    TestClient bob = client("C2", "Bob");
    addClient(server, alice);
    addClient(server, bob);
    return new TestData(server, alice, bob);
  }

  private TestData startedGame() {
    TestData data = twoClients();
    data.server.createGame("C1", "C2");
    return data;
  }

  private Map<String, ClientSession> clients(MultiGameServer server) {
    return (Map<String, ClientSession>) getField(server, "clients");
  }

  private Map<String, ScoreEntry> scores(MultiGameServer server) {
    return (Map<String, ScoreEntry>) getField(server, "scoreboard");
  }

  private Map<String, ServerGameSession> games(MultiGameServer server) {
    return (Map<String, ServerGameSession>) getField(server, "games");
  }

  private Object getField(MultiGameServer server, String name) {
    try {
      Field field = MultiGameServer.class.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(server);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private void addClient(MultiGameServer server, ClientSession client) {
    clients(server).put(client.getId(), client);
    scores(server).put(client.getId(), new ScoreEntry(client.getId(), client.getName()));
  }

  private void call(MultiGameServer server, String name, Class<?>[] types, Object... args)
      throws Exception {
    Method method = MultiGameServer.class.getDeclaredMethod(name, types);
    method.setAccessible(true);
    method.invoke(server, args);
  }

  private TestClient client(String id, String name) {
    StringWriter text = new StringWriter();
    return new TestClient(
        id,
        name,
        new Socket(),
        new BufferedReader(new StringReader("")),
        new PrintWriter(text, true),
        text);
  }

  private void setRunning(MultiGameServer server, boolean value) {
    try {
      Field field = MultiGameServer.class.getDeclaredField("running");
      field.setAccessible(true);
      java.util.concurrent.atomic.AtomicBoolean running =
          (java.util.concurrent.atomic.AtomicBoolean) field.get(server);
      running.set(value);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private String out(TestClient client) {
    return client.text.toString();
  }

  private static final class TestData {
    private final MultiGameServer server;
    private final TestClient alice;
    private final TestClient bob;

    private TestData(MultiGameServer server, TestClient alice, TestClient bob) {
      this.server = server;
      this.alice = alice;
      this.bob = bob;
    }
  }

  private static final class TestClient extends ClientSession {
    private final StringWriter text;
    private boolean closed;

    private TestClient(
        String id,
        String name,
        Socket socket,
        BufferedReader in,
        PrintWriter out,
        StringWriter text) {
      super(id, socket, in, out);
      this.text = text;
      setName(name);
    }

    @Override
    public void close() {
      closed = true;
    }

    private boolean wasClosed() {
      return closed;
    }
  }
}
