package fr.bordeaux.ai.random;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class RandomPlayerTest {

  private GameState game;
  private Board board;
  private Player ia1;
  private Player ia2;

  @Test
  public void testRandomDistribution() {
    board = new Board(9);
    ia1 = new RandomPlayer("IA1", new Position(8, 4), Color.WHITE);
    ia2 = new RandomPlayer("IA2", new Position(0, 4), Color.BLACK);

    List<Player> players = new ArrayList<>();
    players.add(ia1);
    players.add(ia2);
    game = new GameState(board, players);

    // KHI2

    List<Move> legalMoves = game.generateLegalMoves();
    int k = legalMoves.size();
    int nbIteration = 1000;
    int e = nbIteration / k;
    Map<Move, Integer> counts = new HashMap<>();

    for (int i = 0; i < nbIteration; i++) {
      Move m = ia1.getNextMove(game);
      counts.put(m, counts.getOrDefault(m, 0) + 1);
    }

    int khi2 = 0;
    for (Move m : legalMoves) {
      int o_i = counts.getOrDefault(m, 0);
      khi2 += Math.pow((o_i - e), 2) / e;
    }

    // Approximation of Wilson-Hilferty to transform KHI2 in a Normal Distribution

    double df = k - 1.0; // degree of freedom

    double z =
        3.89; // According to the Reduced Standard Normal Table, z = 3.89 represents a success rate
    // of 99.99%.

    // Approximation of Wilson-Hilferty
    double term1 = 2.0 / (9.0 * df);
    double threshold = df * Math.pow(1.0 - term1 + z * Math.sqrt(term1), 3);

    assertTrue(
        khi2 < threshold,
        "IA est biaisée -> Khi2 : "
            + khi2
            + " > Seuil : "
            + threshold); // Statistically, there is a 1/10,000 chance that the test will fail.
  }

  @Test
  public void checkToStringAndSettings() {
    RandomPlayer ia1 = new RandomPlayer("IA1", new Position(8, 4), Color.WHITE);
    assertEquals(
        ia1.toString(),
        "Random AI "
            + String.format(
                "Player{name='%s', pos=%s, walls=%d}",
                ia1.getName(), ia1.getPosition(), ia1.getRemainingWalls()));
    Map<String, Object> settings = new HashMap<>();
    settings.put("mode", "random");
    assertEquals(ia1.getSettings(), settings);
  }
}
