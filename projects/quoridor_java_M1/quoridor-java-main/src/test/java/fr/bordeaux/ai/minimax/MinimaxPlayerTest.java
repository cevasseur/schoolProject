package fr.bordeaux.ai.minimax;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.ai.random.RandomPlayer;
import fr.bordeaux.core.*;
import fr.bordeaux.core.Move.Type;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MinimaxPlayerTest {

  private GameState game;
  private Board board;
  private Player ia1;
  private Player ia2;

  @Test
  public void checkParameters() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ia1 =
              MinimaxPlayer.builder("IA1", new Position(8, 4), Color.WHITE).withDepth(-12).build();
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ia1 =
              MinimaxPlayer.builder("IA1", new Position(8, 4), Color.WHITE)
                  .withReflexion(0)
                  .build();
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ia1 =
              MinimaxPlayer.builder("IA1", new Position(8, 4), Color.WHITE)
                  .withHeuristic("bonjour")
                  .build();
        });
  }

  @Test
  public void checkMiniMaxMoveWall() {
    board = new Board(9);
    ia1 =
        MinimaxPlayer.builder("IA1", new Position(8, 4), Color.WHITE)
            .withDepth(2)
            .withReflexion(5)
            .withHeuristic("advanced")
            .build();
    ia2 = MinimaxPlayer.builder("IA2", new Position(0, 4), Color.BLACK).build();
    ia1.setPosition(new Position(2, 4));
    ia2.setPosition(new Position(7, 2));

    List<Player> players = new ArrayList<>();
    players.add(ia1);
    players.add(ia2);
    game = new GameState(board, players);

    Move res = ia1.getNextMove(game);

    assertTrue(
        res.getTo().equals(new Position(7, 1))
            && res.getType() == Type.WALL
            && res.getOrientation() == Orientation.HORIZONTAL);
  }

  @Test
  public void checkMiniMaxMovePawn() {
    board = new Board(3);
    ia1 = MinimaxPlayer.builder("IA1", new Position(2, 1), Color.WHITE).withDepth(2).build();
    ia2 = new RandomPlayer("IA2", new Position(0, 1), Color.BLACK);
    ia1.setPosition(new Position(1, 1));
    ia2.setPosition(new Position(0, 2));

    List<Player> players = new ArrayList<>();
    players.add(ia1);
    players.add(ia2);
    game = new GameState(board, players);

    Move res = ia1.getNextMove(game);
    assertTrue(
        res.getTo().equals(new Position(0, 1))
            && res.getType() == Type.PAWN
            && res.getOrientation() == null
            && res.getFrom().equals(new Position(1, 1)));
  }

  @Test
  public void checkTimer() {
    board = new Board(9);
    ia1 =
        MinimaxPlayer.builder("IA1", new Position(0, 4), Color.WHITE)
            .withDepth(10)
            .withHeuristic("intermediate")
            .build();
    ia2 = new RandomPlayer("IA2", new Position(8, 4), Color.BLACK);
    List<Player> players = new ArrayList<>();
    players.add(ia1);
    players.add(ia2);
    game = new GameState(board, players);

    long startTime = System.currentTimeMillis();
    Move move = ia1.getNextMove(game);
    long duration = System.currentTimeMillis() - startTime;

    assert (duration >= 5000);
    assertNotNull(move);
  }

  @Test
  public void checkIterativeDeepening() {
    board = new Board(9);
    ia1 =
        MinimaxPlayer.builder("IA1", new Position(8, 4), Color.WHITE)
            .withDepth(2)
            .withHeuristic("intermediate")
            .build();
    ia2 = new RandomPlayer("IA2", new Position(0, 4), Color.BLACK);
    List<Player> players = new ArrayList<>();
    players.add(ia1);
    players.add(ia2);
    game = new GameState(board, players);

    Move move1 = ia1.getNextMove(game);

    ia1 =
        MinimaxPlayer.builder("IA1", new Position(8, 4), Color.WHITE)
            .withIterativeDeepening(true)
            .withHeuristic("intermediate")
            .build();
    List<Player> new_players = new ArrayList<>();
    new_players.add(ia1);
    new_players.add(ia2);
    game = new GameState(board, new_players);

    Move move2 = ia1.getNextMove(game);

    assertTrue(move1.equals(move2));
  }

  @Test
  public void checkToString() {
    ia1 = MinimaxPlayer.builder("IA1", new Position(0, 4), Color.WHITE).build();
    assertEquals(
        ia1.toString(),
        "MiniMax AI "
            + String.format(
                "Player{name='%s', pos=%s, walls=%d}",
                ia1.getName(), ia1.getPosition(), ia1.getRemainingWalls()));
  }
}
