package fr.bordeaux.ai.heuristics;

import fr.bordeaux.ai.base.HeuristicEvaluator;
import fr.bordeaux.core.Color;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import java.util.List;

/** Heuristic evaluating relative progress advantage. */
public class MediumHeuristic implements HeuristicEvaluator {

  /** AI player color. */
  private final Color aiColor;

  /**
   * Constructs MediumHeuristic with AI color.
   *
   * @param aiColorParameter The AI color.
   */
  public MediumHeuristic(final Color aiColorParameter) {
    this.aiColor = aiColorParameter;
  }

  /**
   * Evaluates score by comparing player progress.
   *
   * @param gameState Current state.
   * @return lead or delay score.
   * @throws IllegalStateException if player is missing.
   */
  @Override
  public int evaluate(final GameState gameState) {
    final List<Player> listPlayer = gameState.getPlayers();
    Player enemie = null;
    Player player = null;
    for (final Player otherPlayer : listPlayer) {
      if (otherPlayer.getColor() == this.aiColor) {
        player = otherPlayer;
      } else {
        enemie = otherPlayer;
      }
    }

    if (enemie == null) {
      throw new IllegalStateException("No enemie found !");
    }
    final Position pStartingPosition = player.getStartingPosition();
    final Position pCurrentPosition = player.getPosition();
    final Position eStartingPosition = enemie.getStartingPosition();
    final Position eCurrentPosition = enemie.getPosition();
    final int evaluateHeuristic;

    if (pStartingPosition.getX() == 0) {
      evaluateHeuristic =
          pCurrentPosition.getX()
              - pStartingPosition.getX()
              - eStartingPosition.getX()
              - eCurrentPosition.getX();
    } else {
      evaluateHeuristic =
          pStartingPosition.getX()
              - pCurrentPosition.getX()
              - eCurrentPosition.getX()
              - eStartingPosition.getX();
    }
    return evaluateHeuristic;
  }
}
