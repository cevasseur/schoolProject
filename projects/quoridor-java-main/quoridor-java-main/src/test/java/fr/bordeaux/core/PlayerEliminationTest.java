package fr.bordeaux.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlayerEliminationTest {

  private GameState game;
  private Board board;
  private Player p1, p2, p3, p4;

  @BeforeEach
  public void setup() {
    board = new Board(9);
    p1 = new HumanPlayer("P1", new Position(0, 4), Color.WHITE);
    p2 = new HumanPlayer("P2", new Position(8, 4), Color.BLACK);
    p3 = new HumanPlayer("P3", new Position(4, 0), Color.BLUE);
    p4 = new HumanPlayer("P4", new Position(4, 8), Color.RED);

    List<Player> players = new ArrayList<>(List.of(p1, p2, p3, p4));
    game = new GameState(board, players);
  }

  @Test
  public void testEliminationSwitchesTurn() {
    assertEquals(p1, game.getCurrentPlayer());

    // Eliminate P1
    game.decrementCurrentPlayerTime(p1.getRemainingTime());

    assertTrue(p1.isDisabled());
    assertEquals(p2, game.getCurrentPlayer()); // Turn should have passed to P2
  }

  @Test
  public void testEliminatedPlayerIsSkipped() {
    // Eliminate P2
    p2.setDisabled(true);

    assertEquals(p1, game.getCurrentPlayer());
    game.nextPlayer();
    assertEquals(p3, game.getCurrentPlayer()); // Should skip P2 and go to P3

    game.nextPlayer();
    assertEquals(p4, game.getCurrentPlayer());

    game.nextPlayer();
    assertEquals(p1, game.getCurrentPlayer()); // Should skip P2 and go to P1
  }

  @Test
  public void testEliminatedPlayerDoesNotBlock() {
    // Put P2 in front of P1
    p2.setPosition(new Position(1, 4));

    // Without elimination, (1,4) is occupied
    assertTrue(game.getLegalMoves(p1).stream().noneMatch(pos -> pos.equals(new Position(1, 4))));

    // Eliminate P2
    p2.setDisabled(true);

    // Now (1,4) should be free
    assertTrue(game.getLegalMoves(p1).stream().anyMatch(pos -> pos.equals(new Position(1, 4))));
  }

  @Test
  public void testWinByElimination() {
    // 2-player game
    List<Player> players = new ArrayList<>(List.of(p1, p2));
    game = new GameState(board, players);

    // Eliminate P1
    game.decrementCurrentPlayerTime(p1.getRemainingTime());

    assertTrue(game.isGameOver());
  }

  @Test
  public void testPathExistIgnoresDisabled() {
    // P2 goal is row 0. Let's move P2 to row 0.
    p2.setPosition(new Position(0, 0));

    // P1 goal is row 8. Block row 7 completely so P1 cannot reach row 8.
    for (int i = 0; i < 8; i++) {
      board.addWall(new Position(7, i), Orientation.HORIZONTAL);
    }

    // Try a wall move. It should be illegal because P1 is blocked.
    Move wallMove = Move.wall(new Position(4, 4), Orientation.VERTICAL);
    assertFalse(RuleChecker.isMoveLegal(game, wallMove));

    // Eliminate P1
    p1.setDisabled(true);

    // Now the wall move should be legal because P1 (the only one blocked) is disabled.
    assertTrue(RuleChecker.isMoveLegal(game, wallMove));
  }
}
