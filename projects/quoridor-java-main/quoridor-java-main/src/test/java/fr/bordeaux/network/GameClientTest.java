package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GameClientTest {

  static class MockServer extends Thread {
    private final int port;
    private final List<String> receivedMessages = new ArrayList<>();

    public MockServer() {
      this.port = 0;
    }

    private int actualPort;

    public int getPort() {
      return actualPort;
    }

    public void run() {
      try (ServerSocket serverSocket = new ServerSocket(0)) {
        actualPort = serverSocket.getLocalPort();
        synchronized (this) {
          this.notifyAll();
        }
        Socket client = serverSocket.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        String line = null;
        while ((line = in.readLine()) != null) {
          receivedMessages.add(line);
          if (line.startsWith("PING")) {
            out.println("PONG");
          } else if (line.startsWith("HELLO ")) {
            out.println("WELCOME");
          } else if (line.startsWith("MOVE ")) {
            out.println("OK");
          } else if (line.equals("QUIT")) {
            out.println("BYE");
            break;
          }
        }
        client.close();
      } catch (IOException ignored) {
      }
    }

    public List<String> getReceivedMessages() {
      return receivedMessages;
    }
  }

  private ByteArrayOutputStream outContent;

  @BeforeEach
  public void setUp() throws IOException {
    System.setProperty("quoridor.test", "true");
    I18n.setLocale(java.util.Locale.ENGLISH);
    outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
  }

  @AfterEach
  void restore() {
    System.setOut(System.out);
  }

  @Test
  void testJoinSuccess() throws Exception {
    MockServer server = new MockServer();
    server.start();
    synchronized (server) {
      if (server.getPort() == 0) {
        server.wait(1000);
      }
    }
    int port = server.getPort();
    GameClient client = new GameClient();
    assertTrue(client.join("localhost", port), "Join should succeed");
  }

  @Test
  void testJoinFail() throws Exception {
    GameClient client = new GameClient();
    assertFalse(client.join("localhost", 9999));
  }

  @Test
  void testJoinInvalideHost() throws Exception {
    GameClient client = new GameClient();
    assertFalse(client.join("localhost_invalide", 9999));
  }

  @Test
  void testPingWithoutConnection() throws Exception {
    GameClient client = new GameClient();
    client.ping();
    assertTrue(outContent.toString().contains(I18n.get("notCo")));
  }

  @Test
  void testPingSuccess() throws Exception {
    MockServer server = new MockServer();
    server.start();
    synchronized (server) {
      if (server.getPort() == 0) {
        server.wait(1000);
      }
    }
    int port = server.getPort();

    GameClient client = new GameClient();
    assertTrue(client.join("localhost", port), "Join should succeed");
    client.ping();
    String output = outContent.toString();
    assertTrue(output.contains("PONG"), "Output should contain PONG, but was: " + output);
  }

  @Test
  void testQuitWithoutConnection() throws Exception {
    GameClient client = new GameClient();
    client.quit();
    assertTrue(outContent.toString().contains(I18n.get("clientQuit")));
  }

  @Test
  void testQuitSuccess() throws Exception {
    MockServer server = new MockServer();
    server.start();
    synchronized (server) {
      if (server.getPort() == 0) {
        server.wait(1000);
      }
    }
    int port = server.getPort();

    GameClient client = new GameClient();
    assertTrue(client.join("localhost", port), "Join should succeed");
    client.quit();
    String output = outContent.toString();
    assertTrue(output.contains("BYE"));
    assertTrue(output.contains(I18n.get("clientQuit")));
  }

  @Test
  void testPingAfterClose() throws Exception {
    MockServer server = new MockServer();
    server.start();
    synchronized (server) {
      if (server.getPort() == 0) {
        server.wait(1000);
      }
    }
    int port = server.getPort();

    GameClient client = new GameClient();
    assertTrue(client.join("localhost", port), "Join should succeed");

    client.quit();

    client.ping();

    assertTrue(outContent.toString().contains(I18n.get("notCo")));
  }

  @Test
  void hello_sendsPlayerName() throws Exception {
    MockServer server = new MockServer();
    server.start();
    synchronized (server) {
      if (server.getPort() == 0) {
        server.wait(1000);
      }
    }
    int port = server.getPort();

    GameClient client = new GameClient();
    assertTrue(client.join("localhost", port), "Join should succeed");

    // Send player name to server
    client.hello("Alice");
    client.quit();
    server.join(1000);

    assertTrue(server.getReceivedMessages().contains("HELLO Alice"));
  }

  @Test
  void sendMove_sendsMoveText() throws Exception {
    MockServer server = new MockServer();
    server.start();
    synchronized (server) {
      if (server.getPort() == 0) {
        server.wait(1000);
      }
    }
    int port = server.getPort();

    GameClient client = new GameClient();
    assertTrue(client.join("localhost", port), "Join should succeed");

    // Send one move to server
    client.sendMove("e2-e3");
    client.quit();
    server.join(1000);

    assertTrue(server.getReceivedMessages().contains("MOVE e2-e3"));
  }

  // Tests reseau client ajoutes

  @Test
  void serverList_findsOneServer() throws Exception {
    GameClient client = new GameClient();

    // First call starts the passive UDP listener
    client.serverList();

    try (DatagramSocket socket = new DatagramSocket()) {
      byte[] response = "testServer:9000".getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet =
          new DatagramPacket(response, response.length, InetAddress.getByName("127.0.0.1"), 12346);
      socket.send(packet);
    }

    Thread.sleep(300);

    String result = client.serverList();

    assertTrue(result.contains("testServer"));
    assertTrue(result.contains("127.0.0.1:9000"));
  }

  @Test
  void connectedFalseByDefault() {
    GameClient client = new GameClient();

    assertFalse(client.isConnected());
  }

  @Test
  void helloWithoutConnection() {
    GameClient client = new GameClient();

    assertDoesNotThrow(() -> client.hello("Alice"));
    assertTrue(outContent.toString().contains(I18n.get("notCo")));
  }

  @Test
  void sendMoveWithoutConnection() {
    GameClient client = new GameClient();

    assertDoesNotThrow(() -> client.sendMove("e1-e2"));
    assertTrue(outContent.toString().contains(I18n.get("notCo")));
  }

  @Test
  void testPingTimeout() throws Exception {
    GameClient client = new GameClient();
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      client.join("localhost", serverSocket.getLocalPort());

      Field instanceField = Logger.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      Logger originalLogger = (Logger) instanceField.get(null);
      Logger mockLogger = mock(Logger.class);
      when(mockLogger.isInfoEnabled()).thenReturn(true);
      when(mockLogger.isVerboseEnabled()).thenReturn(true);
      instanceField.set(null, mockLogger);

      try {
        Field syncField = GameClient.class.getDeclaredField("syncResponse");
        syncField.setAccessible(true);
        SynchronousQueue<String> mockQueue = mock(SynchronousQueue.class);
        syncField.set(client, mockQueue);
        // Simulate timeout by returning null
        when(mockQueue.poll(anyLong(), any())).thenReturn(null);

        String result = client.ping();
        assertTrue(result.contains(I18n.get("pingTimeout")));
        verify(mockLogger, atLeastOnce()).info(I18n.get("pingTimeout"));
      } finally {
        instanceField.set(null, originalLogger);
      }
    }
  }

  @Test
  void testPingInterrupted() throws Exception {
    GameClient client = new GameClient();
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      client.join("localhost", serverSocket.getLocalPort());

      Field instanceField = Logger.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      Logger originalLogger = (Logger) instanceField.get(null);
      Logger mockLogger = mock(Logger.class);
      when(mockLogger.isInfoEnabled()).thenReturn(true);
      when(mockLogger.isVerboseEnabled()).thenReturn(true);
      instanceField.set(null, mockLogger);

      try {
        Field syncField = GameClient.class.getDeclaredField("syncResponse");
        syncField.setAccessible(true);
        SynchronousQueue<String> mockQueue = mock(SynchronousQueue.class);
        syncField.set(client, mockQueue);
        // Simulate interruption
        when(mockQueue.poll(anyLong(), any())).thenThrow(new InterruptedException());

        String result = client.ping();
        assertTrue(result.contains(I18n.get("pingInterrupt")));
        verify(mockLogger, atLeastOnce()).info(I18n.get("pingInterrupt"));
      } finally {
        instanceField.set(null, originalLogger);
      }
    }
  }

  @Test
  void testQuitNoBye() throws Exception {
    GameClient client = new GameClient();
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      client.join("localhost", serverSocket.getLocalPort());

      Field instanceField = Logger.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      Logger originalLogger = (Logger) instanceField.get(null);
      Logger mockLogger = mock(Logger.class);
      when(mockLogger.isInfoEnabled()).thenReturn(true);
      when(mockLogger.isVerboseEnabled()).thenReturn(true);
      instanceField.set(null, mockLogger);

      try {
        Field syncField = GameClient.class.getDeclaredField("syncResponse");
        syncField.setAccessible(true);
        SynchronousQueue<String> mockQueue = mock(SynchronousQueue.class);
        syncField.set(client, mockQueue);
        // Simulate no BYE response
        when(mockQueue.poll(anyLong(), any())).thenReturn(null);

        String result = client.quit();
        assertTrue(result.contains(I18n.get("noBye")));
        verify(mockLogger, atLeastOnce()).info(I18n.get("noBye"));
      } finally {
        instanceField.set(null, originalLogger);
      }
    }
  }

  @Test
  void testQuitException() throws Exception {
    GameClient client = new GameClient();
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      client.join("localhost", serverSocket.getLocalPort());

      // Inject a mock socket that throws IOException on close
      Field socketField = GameClient.class.getDeclaredField("socket");
      socketField.setAccessible(true);
      Socket mockSocket = mock(Socket.class);
      when(mockSocket.isConnected()).thenReturn(true);
      doThrow(new IOException("Close Error")).when(mockSocket).close();
      socketField.set(client, mockSocket);

      String result = client.quit();
      // Should handle exception silently or log it, but method should finish
      assertNotNull(result);
    }
  }
}
