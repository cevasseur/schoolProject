package fr.bordeaux.ai.base;

import fr.bordeaux.core.GameState;

/** Functional interface for evaluating a Quoridor game state. */
@FunctionalInterface
public interface HeuristicEvaluator {
  /**
   * Evaluates the game state and returns a numerical score.
   *
   * @param gameState The game state.
   * @return Evaluation score (positive for AI, negative for opponent).
   */
  int evaluate(GameState gameState);
}
