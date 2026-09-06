package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.bordeaux.core.Move;
import fr.bordeaux.core.Position;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ServerGameSessionTest {

  @Test
  void constructorInitializesGameWithTwoPlayers() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");

    ServerGameSession session = new ServerGameSession("game-1", List.of(alice, bob));

    assertEquals("game-1", session.getGameId());
    assertEquals(List.of(alice, bob), session.getPlayers());
    assertNotNull(session.getGameState());
    assertNotNull(session.getGameEngine());
    assertEquals(2, session.getGameState().getPlayers().size());
    assertEquals("Alice", session.getGameState().getPlayers().get(0).getName());
    assertEquals("Bob", session.getGameState().getPlayers().get(1).getName());
    assertEquals(new Position(0, 4), session.getGameState().getPlayers().get(0).getPosition());
    assertEquals(new Position(8, 4), session.getGameState().getPlayers().get(1).getPosition());
  }

  @Test
  void constructorInitializesGameWithFourPlayers() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ClientSession chloe = client("id-3", "Chloe");
    ClientSession david = client("id-4", "David");

    ServerGameSession session = new ServerGameSession("game-4", List.of(alice, bob, chloe, david));

    assertEquals(4, session.getGameState().getPlayers().size());
    assertEquals(new Position(0, 4), session.getGameState().getPlayers().get(0).getPosition());
    assertEquals(new Position(8, 4), session.getGameState().getPlayers().get(1).getPosition());
    assertEquals(new Position(4, 0), session.getGameState().getPlayers().get(2).getPosition());
    assertEquals(new Position(4, 8), session.getGameState().getPlayers().get(3).getPosition());
  }

  @Test
  void constructorRejectsSinglePlayerSession() {
    ClientSession alice = client("id-1", "Alice");

    assertThrows(
        IllegalArgumentException.class, () -> new ServerGameSession("game-1", List.of(alice)));
  }

  @Test
  void containsPlayerAndOpponentWorkAsExpected() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ServerGameSession session = new ServerGameSession("game-1", List.of(alice, bob));

    assertTrue(session.containsPlayer("id-1"));
    assertTrue(session.containsPlayer("id-2"));
    assertFalse(session.containsPlayer("missing"));
    assertSame(bob, session.getOpponent("id-1"));
    assertSame(alice, session.getOpponent("id-2"));
    assertSame(alice, session.getOpponent("missing"));
  }

  @Test
  void getOtherPlayersReturnsEveryoneExceptRequestedPlayer() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ClientSession chloe = client("id-3", "Chloe");
    ClientSession david = client("id-4", "David");
    ServerGameSession session = new ServerGameSession("game-4", List.of(alice, bob, chloe, david));

    assertEquals(List.of(bob, chloe, david), session.getOtherPlayers("id-1"));
    assertEquals(List.of(alice, chloe, david), session.getOtherPlayers("id-2"));
  }

  @Test
  void currentPlayerSessionFollowsGameStateTurn() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ServerGameSession session = new ServerGameSession("game-1", List.of(alice, bob));

    assertSame(alice, session.getCurrentPlayerSession());
    assertTrue(session.isPlayerTurn("id-1"));
    assertFalse(session.isPlayerTurn("id-2"));

    session.getGameState().nextPlayer();

    assertSame(bob, session.getCurrentPlayerSession());
    assertTrue(session.isPlayerTurn("id-2"));
    assertFalse(session.isPlayerTurn("id-1"));
  }

  @Test
  void getCurrentPlayerSessionReturnsNullWhenIndexIsInvalid() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ServerGameSession session = new ServerGameSession("game-1", List.of(alice, bob));

    session
        .getGameState()
        .loadState(
            new fr.bordeaux.core.GameState(
                session.getGameState().getBoard(), session.getGameState().getPlayers(), 2));

    assertNull(session.getCurrentPlayerSession());
    assertFalse(session.isPlayerTurn("id-1"));
  }

  @Test
  void getWinnerSessionReturnsMatchingClientWhenGoalReached() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ServerGameSession session = new ServerGameSession("game-1", List.of(alice, bob));

    session.getGameState().getPlayers().get(0).setPosition(new Position(8, 4));

    assertSame(alice, session.getWinnerSession());
  }

  @Test
  void getWinnerSessionReturnsNullWhenNobodyHasWon() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ServerGameSession session = new ServerGameSession("game-1", List.of(alice, bob));

    assertNull(session.getWinnerSession());
  }

  @Test
  void applyMoveDelegatesToGameEngineAndUpdatesState() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ServerGameSession session = new ServerGameSession("game-1", List.of(alice, bob));
    Move move = Move.pawn(new Position(0, 4), new Position(1, 4));

    boolean applied = session.applyMove(move);

    assertTrue(applied);
    assertEquals(new Position(1, 4), session.getGameState().getPlayers().get(0).getPosition());
    assertSame(bob, session.getCurrentPlayerSession());
  }

  @Test
  void isFinishedReflectsGameState() {
    ClientSession alice = client("id-1", "Alice");
    ClientSession bob = client("id-2", "Bob");
    ServerGameSession session = new ServerGameSession("game-1", List.of(alice, bob));

    assertFalse(session.isFinished());

    session.getGameState().setGameOver(true);

    assertTrue(session.isFinished());
  }

  private ClientSession client(String id, String name) {
    ClientSession client =
        new ClientSession(
            id,
            new Socket(),
            new BufferedReader(new StringReader("")),
            new PrintWriter(new StringWriter(), true));
    client.setName(name);
    return client;
  }
}
