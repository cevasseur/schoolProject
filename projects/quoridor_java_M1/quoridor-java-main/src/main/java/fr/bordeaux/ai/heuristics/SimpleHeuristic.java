package fr.bordeaux.ai.heuristics;

import fr.bordeaux.ai.base.HeuristicEvaluator;
import fr.bordeaux.core.Color;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;

/** Heuristic focusing on direct forward progress. */
public class SimpleHeuristic implements HeuristicEvaluator {

  /** AI player color identifier. */
  private final Color aiColor;

  /**
   * Constructs SimpleHeuristic with AI color.
   *
   * @param aiColorParameter The AI color.
   */
  public SimpleHeuristic(final Color aiColorParameter) {
    this.aiColor = aiColorParameter;
  }

  /**
   * Evaluates progress from starting position.
   *
   * @param gameState Current state.
   * @return Steps taken.
   * @throws IllegalStateException If AI player not found.
   */
  @Override
  public int evaluate(final GameState gameState) {

    Player player = null;
    for (final Player otherPlayer : gameState.getPlayers()) {
      if (otherPlayer.getColor() == this.aiColor) {
        player = otherPlayer;
        break;
      }
    }
    if (player == null) {
      throw new IllegalStateException("AI player not found");
    }

    final Position startPos = player.getStartingPosition();
    final Position actualPos = player.getPosition();
    final int evaluateHeuristic;
    if (startPos.getX() == 0) {
      evaluateHeuristic = actualPos.getX() - startPos.getX();
    } else {
      evaluateHeuristic = startPos.getX() - actualPos.getX();
    }
    return evaluateHeuristic;
  }
}
