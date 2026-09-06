package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.network.GameClient;
import fr.bordeaux.network.MultiGameServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetworkLauncherTest {

  private MultiGameServer multiServer;
  private GameClient client;
  private ConfigManager config;
  private NetworkLauncher launcher;
  private final InputStream originalIn = System.in;
  private final PrintStream originalOut = System.out;
  private ByteArrayOutputStream testOut;

  @BeforeEach
  void setUp() {
    fr.bordeaux.util.Logger.resetInstance();
    System.setProperty("quoridor.test", "true");
    I18n.setLocale(Locale.ENGLISH);

    multiServer = mock(MultiGameServer.class);
    client = mock(GameClient.class);
    config = mock(ConfigManager.class);
    launcher = new NetworkLauncher(multiServer, client);

    testOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(testOut));
  }

  @AfterEach
  void tearDown() {
    System.setIn(originalIn);
    System.setOut(originalOut);
  }

  private void setInput(String data) {
    System.setIn(new ByteArrayInputStream(data.getBytes()));
  }

  /**
   * Tests the "quit" command to ensure the loop terminates correctly.
   *
   * <p>Simulates: Entering "quit" at the CLI prompt.
   *
   * <p>Verifies: The client disconnects and the "Network mode exited" message is printed.
   */
  @Test
  void testLaunch_Quit() {
    setInput("quit\n");
    when(config.getOption("server", "false")).thenReturn("false");

    launcher.launch(new String[] {}, config);

    verify(client).quit();
    assertTrue(testOut.toString().contains("Network mode exited."));
  }

  /**
   * Tests single-game server management through "server start" and "server stop".
   *
   * <p>Simulates: 1. "server start 12345": attempts to start a server on the specified port. 2.
   * "server stop": stops the currently running server. 3. "quit": exits the interactive loop.
   *
   * <p>Verifies: Correct calls are made to the GameServer mock instance.
   */
  @Test
  void testLaunch_ServerStartStop() {
    setInput("server start 12345\nserver stop\nquit\n");
    when(config.getOption("server", "false")).thenReturn("false");

    launcher.launch(new String[] {}, config);

    verify(multiServer).start(12345);
    verify(multiServer).stop();
  }

  /**
   * Tests multi-game server management commands.
   *
   * <p>Simulates: 1. "multi start 54321": starts a multiplayer server. 2. "multi stop": stops the
   * multiplayer server. 3. "server status": requests current server health/status. 4. "players":
   * lists all connected players. 5. "scoreboard": retrieves global game scores.
   *
   * <p>Verifies: Corresponding data retrieval methods are called on MultiGameServer.
   */
  @Test
  void testLaunch_MultiStartStopStatus() {
    setInput("server start 54321\nserver stop\nserver status\nplayers\nscoreboard\nquit\n");
    when(config.getOption("server", "false")).thenReturn("false");
    when(multiServer.getServerStatus()).thenReturn("Status OK");
    when(multiServer.listPlayers()).thenReturn("Players: 0");
    when(multiServer.getScoreboard()).thenReturn("Scoreboard: Empty");

    launcher.launch(new String[] {}, config);

    verify(multiServer).start(54321);
    verify(multiServer).stop();
    verify(multiServer, atLeastOnce()).getServerStatus();
    verify(multiServer, atLeastOnce()).listPlayers();
    verify(multiServer, atLeastOnce()).getScoreboard();
    assertTrue(testOut.toString().contains("Status OK"));
  }

  /**
   * Tests the creation of new game sessions on the multiplayer server.
   *
   * <p>Simulates: 1. "new Alice Bob": creates a game between two players. 2. "new Alice": an
   * invalid command missing a second player name.
   *
   * <p>Verifies: - "createGame" is called only for the valid command. - UI feedback is provided for
   * the missing argument case.
   */
  @Test
  void testLaunch_MultiNewGame() {
    setInput("new Alice Bob\nnew Alice\nquit\n");
    when(config.getOption("server", "false")).thenReturn("false");
    when(multiServer.createGame("Alice", "Bob")).thenReturn("Game created");

    launcher.launch(new String[] {}, config);

    verify(multiServer).createGame("Alice", "Bob");
    assertTrue(testOut.toString().contains("Game created"));
    assertTrue(testOut.toString().contains("Usage: new PLAYER1 PLAYER2"));
  }

  /**
   * Tests client-side networking commands to interact with a remote server.
   *
   * <p>Simulates: 1. "join 127.0.0.1:12345": connects to a server. 2. "hello Alice": identifies the
   * player to the server. 3. "ping": measures latency. 4. "move e1-e2": sends a move notation. 5.
   * "server list": requests a list of discovered local servers.
   *
   * <p>Verifies: All commands are correctly delegated to the GameClient.
   */
  @Test
  void testLaunch_ClientJoinHelloPingMoveList() {
    setInput("join 127.0.0.1:12345\nhello Alice\nping\nmove e1-e2\nserver list\nquit\n");
    when(config.getOption("server", "false")).thenReturn("false");
    when(client.join("127.0.0.1", 12345)).thenReturn(true);

    launcher.launch(new String[] {}, config);

    verify(client).join("127.0.0.1", 12345);
    verify(client).hello("Alice");
    verify(client).ping();
    verify(client).sendMove("e1-e2");
    verify(client).serverList();
  }

  @Test
  void testLaunch_ClientJoinDefaultHostAndPort() {
    setInput("join\nquit\n");
    when(config.getOption("server", "false")).thenReturn("false");
    when(client.join("localhost", 12345)).thenReturn(true);

    launcher.launch(new String[] {}, config);

    verify(client).join("localhost", 12345);
  }

  /**
   * Verifies behavior when a network connection fails.
   *
   * <p>Simulates: A "join" command to an unreachable address.
   *
   * <p>Verifies: An error message is displayed when GameClient returns false.
   */
  @Test
  void testLaunch_ClientJoinFail() {
    setInput("join localhost:11111\nquit\n");
    when(config.getOption("server", "false")).thenReturn("false");
    when(client.join("localhost", 11111)).thenReturn(false);

    launcher.launch(new String[] {}, config);

    verify(client).join("localhost", 11111);
    assertTrue(testOut.toString().contains("Failed to connect to localhost:11111"));
  }

  /**
   * Tests the initial automation where a server starts based on ConfigManager options.
   *
   * <p>Setup: ConfigManager set with server=true and server-port=9999.
   *
   * <p>Verifies: The MultiGameServer is started automatically at launch time.
   */
  @Test
  void testLaunch_InitialServerMode() {
    setInput("quit\n");
    when(config.getOption("server", "false")).thenReturn("true");
    when(config.getOption("server-port", "12345")).thenReturn("9999");
    when(config.getOption("daemon", "false")).thenReturn("false");

    launcher.launch(new String[] {}, config);

    verify(multiServer).start(9999);
  }

  /**
   * Tests common user error handling for unrecognized commands.
   *
   * <p>Simulates: Entering an unsupported command string.
   *
   * <p>Verifies: UI provides feedback that the command is unknown.
   */
  @Test
  void testLaunch_UnknownCommand() {
    setInput("magic\nquit\n");
    when(config.getOption("server", "false")).thenReturn("false");

    launcher.launch(new String[] {}, config);

    assertTrue(testOut.toString().contains("Unknown command"));
  }

  /**
   * Tests the application's global exception handler in the launch loop.
   *
   * <p>Simulates: Providing an alphabetical string where a numeric port is expected.
   *
   * <p>Verifies: The exception is caught and displayed without crashing the application.
   */
  @Test
  void testLaunch_ExceptionHandling() {
    setInput("server start abc\nquit\n");
    when(config.getOption("server", "false")).thenReturn("false");

    launcher.launch(new String[] {}, config);

    assertTrue(testOut.toString().contains("Error: For input string: \"abc\""));
  }

  /**
   * Tests the "daemon mode" where the server starts and enters an infinite loop.
   *
   * <p>Simulates: server=true, daemon=true.
   *
   * <p>Verifies: The server starts on the specified port and correctly handles interruption.
   */
  @Test
  void testLaunch_DaemonMode() throws InterruptedException {
    when(config.getOption("server", "false")).thenReturn("true");
    when(config.getOption("server-port", "12345")).thenReturn("12345");
    when(config.getOption("daemon", "false")).thenReturn("true");

    final Thread thread = new Thread(() -> launcher.launch(new String[] {}, config));
    thread.start();

    // Give it a bit of time to start and enter the loop
    Thread.sleep(500);

    assertTrue(thread.isAlive(), "Thread should be running in daemon mode");

    thread.interrupt();
    thread.join(2000); // Wait for thread to finish

    assertFalse(thread.isAlive(), "Thread should have terminated after interruption");
    verify(multiServer).start(12345);
    assertTrue(testOut.toString().contains("Server started in daemon mode on port 12345"));
  }
}
