package fr.bordeaux.ai.heuristics;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdvancedHeuristicTest {

  private AdvancedHeuristic heuristic;
  private Color aiColor = Color.WHITE;

  @BeforeEach
  public void setUp() {
    heuristic = new AdvancedHeuristic(aiColor);
  }

  @Test
  public void testEvaluatePureRaceExactValues() {
    Board board = new Board(9);

    // AI starts at Bottom (8,4), goal is Row 0
    Player ai = new HumanPlayer("AI", new Position(8, 4), aiColor);
    // Enemy starts at Top (0,4), goal is Row 8
    Player enemy = new HumanPlayer("Enemy", new Position(0, 4), Color.BLACK);

    ai.setRemainingWalls(0);
    enemy.setRemainingWalls(0);

    List<Player> players = new ArrayList<>();
    players.add(ai);
    players.add(enemy);
    GameState state = new GameState(board, players, 0);

    ai.setPosition(new Position(1, 4));
    enemy.setPosition(new Position(1, 4));
    assertEquals(
        9998,
        heuristic.evaluate(state),
        "AI winning pure race should return MAX_SCORE - playerDist");

    ai.setPosition(new Position(7, 4));
    enemy.setPosition(new Position(7, 4));
    assertEquals(
        -9998,
        heuristic.evaluate(state),
        "Enemy winning pure race should return MIN_SCORE + enemyDist");

    ai.setPosition(new Position(4, 4));
    enemy.setPosition(new Position(4, 4));
    assertEquals(0, heuristic.evaluate(state), "Tied pure race should return 0");
  }

  @Test
  public void testOrientationsManhattanAndProgress() {
    Board board = new Board(9);

    // 1. AI starts at Top (0, 4), goal row 8. Enemy starts at Bottom (8, 4), goal row 0.
    Player aiTop = new HumanPlayer("AI", new Position(0, 4), aiColor);
    aiTop.setPosition(
        new Position(2, 4)); // x=2. manhattan = 8-2 = 6. distFromStart = 2. playerDist = 7.
    Player enemyBottom = new HumanPlayer("Enemy", new Position(8, 4), Color.BLACK);
    enemyBottom.setPosition(new Position(4, 4)); // x=4. manhattan = 4. enemyDist = 5.
    GameState state1 = new GameState(board, List.of(aiTop, enemyBottom), 0);
    assertEquals(
        10, heuristic.evaluate(state1), "Should cover AI:startX=0 and Enemy:startX=size-1");

    // 2. AI starts at Bottom (8, 4), goal row 0. Enemy starts at Top (0, 4), goal row 8.
    Player aiBottom = new HumanPlayer("AI", new Position(8, 4), aiColor);
    aiBottom.setPosition(
        new Position(6, 4)); // x=6. manhattan = 6. distFromStart = 2. playerDist = 7.
    Player enemyTop = new HumanPlayer("Enemy", new Position(0, 4), Color.BLACK);
    enemyTop.setPosition(new Position(4, 4)); // x=4. manhattan = 8-4 = 4. enemyDist = 5.
    GameState state2 = new GameState(board, List.of(aiBottom, enemyTop), 0);
    assertEquals(
        10, heuristic.evaluate(state2), "Should cover AI:startX=size-1 and Enemy:startX=0");

    // 3. AI starts at Left (4, 0), goal col 8. Enemy starts at Right (4, 8), goal col 0.
    Player aiLeft = new HumanPlayer("AI", new Position(4, 0), aiColor);
    aiLeft.setPosition(
        new Position(4, 2)); // y=2. manhattan = 8-2 = 6. distFromStart = 2. playerDist = 7.
    Player enemyRight = new HumanPlayer("Enemy", new Position(4, 8), Color.BLACK);
    enemyRight.setPosition(new Position(4, 4)); // y=4. manhattan = 4. enemyDist = 5.
    GameState state3 = new GameState(board, List.of(aiLeft, enemyRight), 0);
    assertEquals(
        10, heuristic.evaluate(state3), "Should cover AI:startY=0 and Enemy:startY=size-1");

    // 4. AI starts at Right (4, 8), goal col 0. Enemy starts at Left (4, 0), goal col 8.
    Player aiRight = new HumanPlayer("AI", new Position(4, 8), aiColor);
    aiRight.setPosition(
        new Position(4, 6)); // y=6. manhattan = 6. distFromStart = 2. playerDist = 7.
    Player enemyLeft = new HumanPlayer("Enemy", new Position(4, 0), Color.BLACK);
    enemyLeft.setPosition(new Position(4, 4)); // y=4. manhattan = 8-4 = 4. enemyDist = 5.
    GameState state4 = new GameState(board, List.of(aiRight, enemyLeft), 0);
    assertEquals(10, heuristic.evaluate(state4), "Should cover AI:else and Enemy:startY=0");
  }
}
