package fr.bordeaux.ai.heuristics;

import fr.bordeaux.ai.base.HeuristicEvaluator;
import fr.bordeaux.core.Board;
import fr.bordeaux.core.Color;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import java.util.List;

/** Advanced heuristic using pathfinding, resources, and tactical detours. */
public class AdvancedHeuristic implements HeuristicEvaluator {

  /** Maximum possible score for a winning state. */
  private static final int MAX_SCORE = 10_000;

  /** Minimum possible score for a losing state. */
  private static final int MIN_SCORE = -10_000;

  /** Weight for the shortest path distance difference. */
  private static final int PATH_WEIGHT = 40;

  /** Bonus for placing a wall on the opponent's path. */
  private static final int WALL_BONUS = 80;

  /** Base value of a single wall in resource scoring. */
  private static final int WALL_BASE_VALUE = 8;

  /** Multiplier for the progress bonus based on distance from start. */
  private static final int MULTIPLIER = 5;

  /** Score used for neutral or error states. */
  private static final int NEUTRAL_SCORE = 0;

  /** Distance threshold to consider a goal reached. */
  private static final int WIN_THRESHOLD = 1;

  /** Color of the ai player for whom the heuristic is evaluated. */
  private final Color aiColor;

  /**
   * Constructs AdvancedHeuristic with AI color.
   *
   * @param aiColor The AI color identifier.
   */
  public AdvancedHeuristic(final Color aiColor) {
    this.aiColor = aiColor;
  }

  /**
   * Evaluates state from AI perspective.
   *
   * @param state Current state.
   * @return Advantage score.
   */
  @Override
  public int evaluate(final GameState state) {
    int evaluationResult = NEUTRAL_SCORE;
    final Player player = findByColor(state, true);
    final Player enemy = findByColor(state, false);

    if (player != null && enemy != null) {
      final int playerDistance = bfsPath(state, player).size();
      final int enemyDistance = bfsPath(state, enemy).size();

      if (playerDistance <= WIN_THRESHOLD) {
        evaluationResult = MAX_SCORE;
      } else if (enemyDistance <= WIN_THRESHOLD) {
        evaluationResult = MIN_SCORE;
      } else {
        evaluationResult = computeDynamicScore(state, player, enemy, playerDistance, enemyDistance);
      }
    }

    return evaluationResult;
  }

  private int computeDynamicScore(
      final GameState state,
      final Player player,
      final Player enemy,
      final int playerDistance,
      final int enemyDistance) {

    final int score;
    final int totalWalls = player.getRemainingWalls() + enemy.getRemainingWalls();

    if (totalWalls == NEUTRAL_SCORE) {
      score = computeEndgameScore(playerDistance, enemyDistance);
    } else {
      score = computeMidgameScore(state, player, enemy, playerDistance, enemyDistance);
    }

    return score;
  }

  private int computeEndgameScore(final int playerDistance, final int enemyDistance) {
    int endgameResult = NEUTRAL_SCORE;
    if (enemyDistance > playerDistance) {
      endgameResult = MAX_SCORE - playerDistance;
    } else if (playerDistance > enemyDistance) {
      endgameResult = MIN_SCORE + enemyDistance;
    }
    return endgameResult;
  }

  private int computeMidgameScore(
      final GameState state,
      final Player player,
      final Player enemy,
      final int playerDistance,
      final int enemyDistance) {

    final int boardSize = state.getBoard().getSize();
    final double progress = 1.0 - (double) playerDistance / boardSize;
    final double wallCoeff = 1.0 - 0.5 * progress;

    final int scorePath = (enemyDistance - playerDistance) * PATH_WEIGHT;
    final int scoreWalls =
        (int)
            ((player.getRemainingWalls() - enemy.getRemainingWalls())
                * WALL_BASE_VALUE
                * wallCoeff);
    final int scoreDetour = (enemyDistance - manhattanToGoal(state, enemy)) * WALL_BONUS;
    final int progressBonus = getDistanceFromStart(state, player) * MULTIPLIER;

    return scorePath + scoreWalls + scoreDetour + progressBonus;
  }

  private Player findByColor(final GameState state, final boolean isAi) {
    return state.getPlayers().stream()
        .filter(p -> p.getColor() == aiColor == isAi)
        .findFirst()
        .orElse(null);
  }

  /**
   * Finds shortest path with BFS.
   *
   * @param state State.
   * @param player Player.
   * @return Path node IDs.
   */
  private List<Integer> bfsPath(final GameState state, final Player player) {
    final Board board = state.getBoard();
    final Position pos = player.getPosition();
    final int startId = board.getCellId(pos.getX(), pos.getY());
    return board.getGraph().findPathBFS(startId, state.getWinCondition(player));
  }

  /**
   * Calculates Manhattan distance baseline (ignoring walls).
   *
   * @param state State.
   * @param player Player.
   * @return Distance to goal.
   */
  private int manhattanToGoal(final GameState state, final Player player) {
    final Board board = state.getBoard();
    final int boardSize = board.getSize();
    final Position currentPos = player.getPosition();
    final Position startPos = player.getStartingPosition();
    final int posX = currentPos.getX();
    final int posY = currentPos.getY();
    final int startX = startPos.getX();
    final int startY = startPos.getY();

    final int result;
    if (startX == 0) {
      result = boardSize - 1 - posX;
    } else if (startX == boardSize - 1) {
      result = posX;
    } else if (startY == 0) {
      result = boardSize - 1 - posY;
    } else {
      result = posY;
    }
    return result;
  }

  /**
   * Calculates physical progress from start line.
   *
   * @param state State.
   * @param player Player.
   * @return Steps taken.
   */
  private int getDistanceFromStart(final GameState state, final Player player) {
    final Board board = state.getBoard();
    final int boardSize = board.getSize();
    final Position currentPos = player.getPosition();
    final Position startPos = player.getStartingPosition();
    final int posX = currentPos.getX();
    final int posY = currentPos.getY();
    final int startX = startPos.getX();
    final int startY = startPos.getY();

    final int result;
    if (startX == 0) {
      result = posX;
    } else if (startX == boardSize - 1) {
      result = boardSize - 1 - posX;
    } else if (startY == 0) {
      result = posY;
    } else {
      result = boardSize - 1 - posY;
    }
    return result;
  }
}
