package fr.bordeaux.network;

import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** TCP client for network commands and server discovery. */
public class GameClient {

  /** TCP socket used for the connection */
  private Socket socket;

  /** Input stream to read server responses */
  private BufferedReader reader;

  /** Output stream to send commands to the server */
  private PrintWriter writer;

  /** Background listener thread responsible for reading messages from the server */
  private Thread listenerThread;

  /** Sync queue to transfer response from listener thread to caller. */
  private final SynchronousQueue<String> syncResponse;

  /** Flag to treat next message as a direct reply to a sync command. */
  private final AtomicBoolean expectingDirectReply;

  /** Handler notified when an asynchronous message is received from the server. */
  private Consumer<String> serverMessageHandler;

  /** UDP port used for server discovery. */
  private static final int DISCOVERY_PORT = 12346;

  /** Discovery listening window in milliseconds. */
  private static final int DISCOVERY_SOCKET_TIMEOUT_MS = 1000;

  /** Delay after which a server is considered expired. */
  private static final long SERVER_EXPIRATION_MS = 30000L;

  private DatagramSocket discoverySocket;
  private Thread discoveryThread;
  private boolean discoveryStarted;

  /** Servers discovered recently on the local network. */
  private final Map<String, DiscoveredServer> discoveredServers = new ConcurrentHashMap<>();

  /** Constructs a GameClient for joining servers or discovery. */
  public GameClient() {
    this.syncResponse = new SynchronousQueue<>();
    this.expectingDirectReply = new AtomicBoolean(false);
  }

  /**
   * Connects to host/port with a 5s timeout.
   *
   * @param host Server hostname or IP.
   * @param port Server TCP port.
   * @return true if success.
   */
  public boolean join(final String host, final int port) {
    try {
      socket = new Socket();
      socket.connect(new InetSocketAddress(host, port), 5000);

      reader =
          new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      writer = new PrintWriter(socket.getOutputStream(), true);
      if (Logger.getInstance().isInfoEnabled()) {
        Logger.getInstance().info(I18n.get("connected") + " {0}:{1,number,0}", host, port);
      }

      startListener();
      return true;

    } catch (final IOException exception) {
      if (Logger.getInstance().isErrorEnabled()) {
        Logger.getInstance().error(I18n.get("failedCo", ": ") + exception.getMessage());
      }
      return false;
    }
  }

  /** Starts listener thread for server messages. Handles sync/async responses. */
  private void startListener() {
    listenerThread =
        new Thread(
            () -> {
              try {
                String line;
                while ((line = reader.readLine()) != null) {
                  if (expectingDirectReply.get()) {
                    try {
                      final boolean delivered = syncResponse.offer(line, 3, TimeUnit.SECONDS);
                      if (!delivered) {
                        if (Logger.getInstance().isInfoEnabled()) {
                          Logger.getInstance().info(I18n.get("server", line));
                        }
                      }
                      notifyServerMessage(line);
                    } catch (InterruptedException ie) {
                      Thread.currentThread().interrupt();
                    } finally {
                      expectingDirectReply.set(false);
                    }
                  } else {
                    if (Logger.getInstance().isVerboseEnabled()) {
                      Logger.getInstance().verbose("Net In: {0}", line);
                    }
                    if (Logger.getInstance().isInfoEnabled()) {
                      Logger.getInstance().info(I18n.get("server", line));
                    }
                    notifyServerMessage(line);
                  }
                }
              } catch (final IOException exception) {
                if (Logger.getInstance().isInfoEnabled()) {
                  Logger.getInstance().info(I18n.get("connecClosed", exception.getMessage()));
                }
              }
            });
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  /**
   * Notifies the registered handler that a server message was received.
   *
   * @param message message received from the server
   */
  private void notifyServerMessage(final String message) {
    if (serverMessageHandler != null) {
      serverMessageHandler.accept(message);
    }
  }

  /**
   * Sets the handler called when an asynchronous message is received from the server.
   *
   * @param serverMessageHandler handler receiving the server message
   */
  public void setServerMessageHandler(final Consumer<String> serverMessageHandler) {
    this.serverMessageHandler = serverMessageHandler;
  }

  /**
   * Sends "PING" to the server and waits for a response ("PONG"). The time between sending and
   * receiving is measured and displayed.
   *
   * @return a status message indicating the result of the ping command.
   */
  public String ping() {

    String message;
    if (!checkConnection()) {
      message = I18n.get("notConnected");
    } else {
      try {
        final long start = System.currentTimeMillis();

        expectingDirectReply.set(true);
        if (Logger.getInstance().isInfoEnabled()) {
          Logger.getInstance().info("Client: PING");
        }
        if (Logger.getInstance().isVerboseEnabled()) {
          Logger.getInstance().verbose("Net Out: PING");
        }
        writer.println("PING");

        final String res = syncResponse.poll(5, TimeUnit.SECONDS);
        final long rtt = System.currentTimeMillis() - start;

        if (res != null) {
          message = MessageFormat.format(I18n.get("server") + " {0} TIME={1,number,0}ms", res, rtt);
          if (Logger.getInstance().isInfoEnabled()) {
            Logger.getInstance().info(message);
          }
        } else {
          expectingDirectReply.set(false);
          message = I18n.get("pingTimeout");
          if (Logger.getInstance().isInfoEnabled()) {
            Logger.getInstance().info(message);
          }
        }
      } catch (final InterruptedException exception) {
        Thread.currentThread().interrupt();
        message = I18n.get("pingInterrupt");
        if (Logger.getInstance().isInfoEnabled()) {
          Logger.getInstance().info(message);
        }
      }
    }

    return message;
  }

  /**
   * Sends a "QUIT" command to the server, waits for the "BYE" response, and then closes the socket
   * connection.
   *
   * @return a message indicating the final status of the connection termination.
   */
  public String quit() {
    String message = "Client exited.";
    if (checkConnection()) {
      try {
        expectingDirectReply.set(true);
        if (Logger.getInstance().isInfoEnabled()) {
          Logger.getInstance().info("Client: QUIT");
        }
        if (Logger.getInstance().isVerboseEnabled()) {
          Logger.getInstance().verbose("Net Out: QUIT");
        }
        writer.println("QUIT");

        final String res = syncResponse.poll(5, TimeUnit.SECONDS);
        if ("BYE".equals(res)) {
          message = I18n.get("server", " BYE");
          if (Logger.getInstance().isInfoEnabled()) {
            Logger.getInstance().info(message);
          }
        } else if (res != null) {
          if (Logger.getInstance().isInfoEnabled()) {
            Logger.getInstance().info(I18n.get("server", res));
          }
        } else {
          expectingDirectReply.set(false);
          message = I18n.get("noBye");
          if (Logger.getInstance().isInfoEnabled()) {
            Logger.getInstance().info(message);
          }
        }

        socket.close();
        socket = null;

      } catch (final IOException | InterruptedException exception) {
        if (exception instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }

    if (Logger.getInstance().isInfoEnabled()) {
      Logger.getInstance().info(I18n.get("clientQuit"));
    }
    return message;
  }

  /**
   * Broadcasts "DISCOVER" and listens for server responses for 5s.
   *
   * @return a message indicating the result of the server list command.
   */
  public String serverList() {
    startDiscoveryListenerIfNeeded();
    removeExpiredServers();

    if (discoveredServers.isEmpty()) {
      return "No servers discovered yet.";
    }

    StringBuilder builder = new StringBuilder("Available servers:\n");
    for (DiscoveredServer server : discoveredServers.values()) {
      builder
          .append("- ")
          .append(server.name())
          .append(' ')
          .append(server.host())
          .append(':')
          .append(server.port())
          .append('\n');
    }

    return builder.toString().trim();
  }

  /** Starts the UDP discovery listener if needed. */
  private synchronized void startDiscoveryListenerIfNeeded() {
    if (discoveryStarted) {
      return;
    }

    try {
      discoverySocket = new DatagramSocket(DISCOVERY_PORT);
      discoverySocket.setSoTimeout(DISCOVERY_SOCKET_TIMEOUT_MS);
      discoveryStarted = true;
    } catch (IOException e) {
      if (Logger.getInstance().isErrorEnabled()) {
        Logger.getInstance().error(I18n.get("discoFail", ": ", e.getMessage()));
      }
      return;
    }

    discoveryThread =
        new Thread(
            () -> {
              while (!discoverySocket.isClosed()) {
                try {
                  byte[] buf = new byte[1024];
                  DatagramPacket packet = new DatagramPacket(buf, buf.length);
                  discoverySocket.receive(packet);
                  registerServerAnnouncement(packet);
                  removeExpiredServers();
                } catch (SocketTimeoutException ignored) {
                  removeExpiredServers();
                } catch (IOException e) {
                  break;
                }
              }
            });

    discoveryThread.setDaemon(true);
    discoveryThread.start();
  }

  /** Registers one server announcement received by UDP. */
  private void registerServerAnnouncement(final DatagramPacket packet) {
    String message =
        new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();

    int separator = message.lastIndexOf(':');
    if (separator <= 0 || separator == message.length() - 1) {
      return;
    }

    try {
      String name = message.substring(0, separator).trim();
      int port = Integer.parseInt(message.substring(separator + 1).trim());
      String host = packet.getAddress().getHostAddress();
      String key = host + ":" + port;

      discoveredServers.put(
          key, new DiscoveredServer(name, host, port, System.currentTimeMillis()));
    } catch (NumberFormatException ignored) {
    }
  }

  /** Removes servers not seen for more than 30 seconds. */
  private void removeExpiredServers() {
    long now = System.currentTimeMillis();
    discoveredServers
        .entrySet()
        .removeIf(entry -> now - entry.getValue().lastSeen() > SERVER_EXPIRATION_MS);
  }

  /** One server discovered on the local network. */
  private record DiscoveredServer(String name, String host, int port, long lastSeen) {}

  /**
   * @return true if the socket exists and is open, false otherwise.
   * @brief Checks whether the client is currently connected.
   */
  private boolean checkConnection() {
    if (!isConnected()) {
      if (Logger.getInstance().isInfoEnabled()) {
        Logger.getInstance().info(I18n.get("notConnected"));
      }
      return false;
    }
    return true;
  }

  /**
   * Indicates whether the TCP socket is currently connected and open.
   *
   * @return true if the socket exists, is connected, and is not closed; false otherwise
   */
  public boolean isConnected() {
    return socket != null && socket.isConnected() && !socket.isClosed();
  }

  /**
   * Registers player name with server.
   *
   * @param name player name
   */
  public void hello(final String name) {
    if (!checkConnection()) {
      return;
    }
    writer.println("HELLO " + name);
    if (Logger.getInstance().isVerboseEnabled()) {
      Logger.getInstance().verbose("Net Out: HELLO {0}", name);
    }
  }

  /**
   * Sends a move to the server using the text format defined by the protocol.
   *
   * @param move move to send
   */
  public void sendMove(final String move) {
    if (!checkConnection()) {
      return;
    }
    writer.println("MOVE " + move);
    if (Logger.getInstance().isVerboseEnabled()) {
      Logger.getInstance().verbose("Net Out: MOVE {0}", move);
    }
  }
}
