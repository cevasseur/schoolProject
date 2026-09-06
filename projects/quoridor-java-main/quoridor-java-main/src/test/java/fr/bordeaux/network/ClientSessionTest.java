package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import org.junit.jupiter.api.Test;

public class ClientSessionTest {

  @Test
  void constructorInitializesDefaultState() {
    Socket socket = new Socket();
    BufferedReader in = new BufferedReader(new StringReader("hello"));
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    long before = System.currentTimeMillis();
    ClientSession session = new ClientSession("player-1", socket, in, out);
    long after = System.currentTimeMillis();

    assertEquals("player-1", session.getId());
    assertEquals("player-1", session.getName());
    assertSame(socket, session.getSocket());
    assertSame(in, session.getIn());
    assertSame(out, session.getOut());
    assertEquals(PlayerStatus.IDLE, session.getStatus());
    assertNull(session.getCurrentGameId());
    assertTrue(session.getLastPing() >= before);
    assertTrue(session.getLastPing() <= after);
  }

  @Test
  void sendWritesMessageToOutput() {
    StringWriter buffer = new StringWriter();
    ClientSession session =
        new ClientSession(
            "player-1",
            new Socket(),
            new BufferedReader(new StringReader("")),
            new PrintWriter(buffer, true));

    session.send("PING");

    assertEquals("PING" + System.lineSeparator(), buffer.toString());
  }

  @Test
  void settersUpdateMutableFields() {
    ClientSession session =
        new ClientSession(
            "player-1",
            new Socket(),
            new BufferedReader(new StringReader("")),
            new PrintWriter(new StringWriter(), true));

    session.setName("Alice");
    session.setStatus(PlayerStatus.INGAME);
    session.setCurrentGameId("game-42");
    session.setLastPing(123456L);

    assertEquals("Alice", session.getName());
    assertEquals(PlayerStatus.INGAME, session.getStatus());
    assertEquals("game-42", session.getCurrentGameId());
    assertEquals(123456L, session.getLastPing());
  }
}
