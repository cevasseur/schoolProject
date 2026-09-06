package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import org.junit.jupiter.api.Test;

public class ServerBroadcasterTest {

  @Test
  void testBroadcastMessage() throws Exception {
    DatagramSocket receiver = new DatagramSocket(0);
    int listenPort = receiver.getLocalPort();
    receiver.setSoTimeout(2000);

    ServerBroadcaster broadcaster = new ServerBroadcaster("testServer", 9000, 10);
    broadcaster.setBroadcastPort(listenPort);
    Thread broadcasterThread = new Thread(broadcaster);
    broadcasterThread.start();

    byte[] buffer = new byte[1024];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    receiver.receive(packet);

    String message = new String(packet.getData(), 0, packet.getLength());
    assertEquals("testServer:9000", message);

    broadcaster.stop();
    broadcasterThread.interrupt();
    receiver.close();
  }
}
