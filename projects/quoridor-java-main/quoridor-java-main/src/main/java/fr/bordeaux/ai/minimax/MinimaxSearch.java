package fr.bordeaux.ai.minimax;

import fr.bordeaux.ai.base.HeuristicEvaluator;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Player;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.util.ArrayDeque;
import java.util.Deque;

/** Engine responsible for executing the Minimax search algorithm. */
public class MinimaxSearch {

  /** The heuristic evaluation strategy. */
  private final HeuristicEvaluator heuristicFunction;

  /** Stack used to keep track of moves for backtracking. */
  private final Deque<Move> aiStack = new ArrayDeque<>();

  /** Logger for AI search. */
  private static final Logger LOGGER = Logger.getInstance();

  /** High score threshold to identify a winning state. */
  private static final int WIN_THRESHOLD = 9000;

  /**
   * Constructs a new Minimax search engine with a specific heuristic.
   *
   * @param heuristicFunction The heuristic evaluator used to score game states.
   */
  public MinimaxSearch(final HeuristicEvaluator heuristicFunction) {
    this.heuristicFunction = heuristicFunction;
  }

  /**
   * Performs a Minimax search to find the best move within a fixed maximum depth.
   *
   * @param state The current state of the game to analyze.
   * @param maxDepth The maximum depth of the search tree.
   * @param endTime The deadline (in milliseconds) at which the search must stop.
   * @return The best {@link Move} found, or {@code null} if no move was determined.
   */
  public Move search(final GameState state, final int maxDepth, final long endTime) {
    return miniMax(state, maxDepth, endTime);
  }

  /**
   * Implements Iterative Deepening search safely within the main thread.
   *
   * @param state The current game state.
   * @param endTime The timestamp when the search must abort.
   * @return The best move found at the maximum depth reached before timeout.
   */
  public Move iterativeDeepeningSearch(final GameState state, final long endTime) {
    int currentDepth = 1;
    Move lastBestMove = null;

    while (System.currentTimeMillis() < endTime) {
      final Move tempMove = miniMax(state, currentDepth, endTime);
      if (System.currentTimeMillis() >= endTime) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(I18n.get("miniInterrupted", currentDepth));
        }
        break;
      }
      if (tempMove != null) {
        lastBestMove = tempMove;
        LOGGER.verbose(I18n.get("miniDepthComp", currentDepth, lastBestMove));
      }
      currentDepth++;
    }
    return lastBestMove;
  }

  /**
   * Entry point for the Minimax algorithm. Explores the first level of legal moves.
   *
   * @param gameState The current game state.
   * @param maxDepth The search depth.
   * @param endTime The deadline for the search.
   * @return The best evaluated move.
   */
  private Move miniMax(final GameState gameState, final int maxDepth, final long endTime) {

    Move bestMove = null;
    int bestScore = Integer.MIN_VALUE;
    final GameState searchState = gameState.copy();
    for (final Move currentMove : searchState.generateLegalMoves()) {
      if (System.currentTimeMillis() >= endTime) {
        bestMove = null;
        break;
      }
      push(searchState, currentMove);
      final int score =
          minMax(searchState, maxDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, endTime);
      if (score > bestScore || bestMove == null) {
        bestScore = score;
        bestMove = currentMove;
      }
      pop(searchState);
    }
    return bestMove;
  }

  /**
   * @param gameState The game state.
   * @param currentDepth The remaining depth.
   * @param alpha The best value guaranteed for MAX.
   * @param beta The best value guaranteed for MIN.
   * @param endTime The search deadline.
   * @return The maximized score.
   */
  private int maxMin(
      final GameState gameState,
      final int currentDepth,
      final int alpha,
      final int beta,
      final long endTime) {
    final int resultScore;
    if (gameState.isGameOver() || currentDepth == 0 || System.currentTimeMillis() >= endTime) {

      final int rawScore = this.heuristicFunction.evaluate(gameState);
      if (rawScore >= WIN_THRESHOLD) {
        resultScore = rawScore + currentDepth;
      } else if (rawScore <= -WIN_THRESHOLD) {
        resultScore = rawScore - currentDepth;
      } else {
        resultScore = rawScore;
      }
    } else {
      int currentAlpha = alpha;
      for (final Move move : gameState.generateLegalMoves()) {
        push(gameState, move);
        currentAlpha =
            Math.max(
                currentAlpha, minMax(gameState, currentDepth - 1, currentAlpha, beta, endTime));
        pop(gameState);

        if (currentAlpha >= beta) {
          currentAlpha = beta;
          break;
        }
      }
      resultScore = currentAlpha;
    }
    return resultScore;
  }

  /**
   * @param gameState The game state.
   * @param currentDepth The remaining depth.
   * @param alpha The best value guaranteed for MAX.
   * @param beta The best value guaranteed for MIN.
   * @param endTime The search deadline.
   * @return The minimized score.
   */
  private int minMax(
      final GameState gameState,
      final int currentDepth,
      final int alpha,
      final int beta,
      final long endTime) {
    final int resultScore;

    if (gameState.isGameOver() || currentDepth == 0 || System.currentTimeMillis() >= endTime) {
      final int rawScore = this.heuristicFunction.evaluate(gameState);
      if (rawScore >= WIN_THRESHOLD) {
        resultScore = rawScore + currentDepth;
      } else if (rawScore <= -WIN_THRESHOLD) {
        resultScore = rawScore - currentDepth;
      } else {
        resultScore = rawScore;
      }
    } else {
      int currentBeta = beta;
      for (final Move move : gameState.generateLegalMoves()) {
        push(gameState, move);
        currentBeta =
            Math.min(currentBeta, maxMin(gameState, currentDepth - 1, alpha, currentBeta, endTime));
        pop(gameState);

        if (alpha >= currentBeta) {
          currentBeta = alpha;
          break;
        }
      }
      resultScore = currentBeta;
    }
    return resultScore;
  }

  /**
   * Simulates a move and updates the game state.
   *
   * @param gameState The current game state.
   * @param move The move to simulate.
   */
  private void push(final GameState gameState, final Move move) {
    final Player currentPlayer = gameState.getCurrentPlayer();
    if (move.isPawn()) {
      currentPlayer.setPosition(move.getTo());
      if (gameState.hasReachedGoal(currentPlayer)) {
        gameState.setGameOverSilent(true);
      }
    } else {
      gameState.getBoard().addWall(move.getTo(), move.getOrientation());
      currentPlayer.useWall();
    }

    this.aiStack.push(move);
    gameState.nextPlayer();
  }

  /**
   * Backtracks the last simulated move by restoring the previous game state.
   *
   * @param gameState The game state to restore.
   */
  private void pop(final GameState gameState) {
    if (!this.aiStack.isEmpty()) {
      final Move lastMove = this.aiStack.pop();
      gameState.previousPlayer();
      gameState.setGameOverSilent(false);
      final Player currentPlayer = gameState.getCurrentPlayer();

      if (lastMove.isPawn()) {
        currentPlayer.setPosition(lastMove.getFrom());
      } else {
        gameState.getBoard().removeWall(lastMove.getTo(), lastMove.getOrientation());
        currentPlayer.giveBackWall();
      }
    }
  }
}
