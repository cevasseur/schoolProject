package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GameServerTest {

  private GameServer server;
  private int port = 12344;

  @BeforeEach
  public void setUp() throws InterruptedException {
    server = new GameServer();
    server.start(port);
    Thread.sleep(500);
  }

  @AfterEach
  public void tearDown() throws InterruptedException, IOException {
    server.stop();
  }

  @Test
  public void testPingReponse() throws Exception {
    Socket socket = new Socket("localhost", port);
    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    out.println("PING");
    String reponse = in.readLine();
    assertEquals("PONG", reponse);
    socket.close();
  }

  @Test
  void testUnknownCommand() throws Exception {
    Socket socket = new Socket("localhost", port);

    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

    out.println("HELLO");

    String response = in.readLine();

    assertEquals("OK", response);

    socket.close();
  }

  @Test
  void testQuitCommand() throws Exception {
    Socket socket = new Socket("localhost", port);

    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

    out.println("QUIT");

    String response = in.readLine();

    assertEquals("BYE", response);

    socket.close();
  }
}
