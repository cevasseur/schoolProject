package fr.bordeaux.ai.mcts;

import fr.bordeaux.ai.base.AiPlayer;
import fr.bordeaux.ai.nn.NeuralNetworkEvaluator;
import fr.bordeaux.core.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Represents an AI player using the Monte Carlo Tree Search (MCTS) algorithm. */
public class MctsPlayer extends AiPlayer {

  /** Reflection time allowed for the AI in milliseconds. */
  private final long reflectionTime;

  /** Neural Network evaluator for selection (null for standard UCT). */
  private final NeuralNetworkEvaluator evaluator;

  /** UCT constant for exploration vs exploitation balance. */
  private static final double UCT_CONSTANT = 1.41;

  /**
   * Constructs MctsPlayer.
   *
   * @param name player name
   * @param positionStart starting position
   * @param reflectionTime search time limit
   * @param color player color
   */
  public MctsPlayer(
      final String name,
      final Position positionStart,
      final long reflectionTime,
      final Color color) {
    super(name, positionStart, color);
    this.reflectionTime = reflectionTime;
    this.evaluator = null;
  }

  /**
   * Default constructor with 2000ms reflection time.
   *
   * @param name The name of the AI player.
   * @param positionStart The starting position on the board.
   * @param color The player's assigned color.
   */
  public MctsPlayer(final String name, final Position positionStart, final Color color) {
    this(name, positionStart, 2000, color);
  }

  /**
   * Constructor with reflection time and neural network evaluator.
   *
   * @param name AI name
   * @param positionStart starting position
   * @param reflectionTime search time
   * @param useNN use NN evaluator
   * @param color player color
   * @throws IOException if NN evaluator fails to load
   */
  public MctsPlayer(
      final String name,
      final Position positionStart,
      final long reflectionTime,
      final boolean useNN,
      final Color color)
      throws IOException {
    super(name, positionStart, color);
    this.reflectionTime = reflectionTime;
    this.evaluator = useNN ? new NeuralNetworkEvaluator() : null;
  }

  /**
   * Creates a deep copy of this MctsPlayer.
   *
   * @return A new instance of MctsPlayer with the same state.
   */
  @Override
  public Player copy() {
    return new MctsPlayer(
        this.getName(),
        this.getPosition(),
        this.getStartingPosition(),
        this.getRemainingWalls(),
        this.reflectionTime,
        this.getColor());
  }

  /** Private constructor used for copying the player state. */
  private MctsPlayer(
      final String name,
      final Position current,
      final Position start,
      final int walls,
      final long reflectionTime,
      final Color color) {
    super(name, current, start, walls, color);
    this.reflectionTime = reflectionTime;
    this.evaluator = null;
  }

  /**
   * Determines the next move by initiating an MCTS search from the current state.
   *
   * @param state The current game state.
   * @return The best move calculated within the time limit.
   */
  @Override
  public Move getNextMove(final GameState state) {
    final int myIdx = state.getPlayers().indexOf(this);
    return new MctsNode(state.copy())
        .getBestMoveAfterSearch(this.reflectionTime, myIdx, this.evaluator);
  }

  /**
   * Provides a hint (2000ms, no evaluator).
   *
   * @param player the player for whom to find the best move.
   * @param state the current game state.
   * @return the best move for the player, or null.
   * @throws IOException Can't happen because useNN is false here
   */
  public static Move hint(final Player player, final GameState state) throws IOException {
    return hint(player, state, 2000, false);
  }

  /**
   * Hint for best move.
   *
   * @param player player to hint for
   * @param state current game state
   * @param reflectionTimeMs search time
   * @param useNN use NN evaluator
   * @return best move found
   * @throws IOException if NN evaluator fails to load
   */
  public static Move hint(
      final Player player, final GameState state, final long reflectionTimeMs, final boolean useNN)
      throws IOException {
    final int playerIdx = state.getPlayers().indexOf(player);
    NeuralNetworkEvaluator evalToUse = null;
    if (useNN) {
      evalToUse = new NeuralNetworkEvaluator();
    }
    return new MctsNode(state.copy())
        .getBestMoveAfterSearch(reflectionTimeMs, playerIdx, evalToUse);
  }

  /**
   * Selects the best child node based on the available strategy.
   *
   * @param node The current node in the MCTS tree.
   * @param myIdx The index of the AI player.
   * @param evaluator The NN evaluator (can be null).
   * @return The selected child node.
   */
  protected static MctsNode bestUCT(
      final MctsNode node, final int myIdx, final NeuralNetworkEvaluator evaluator) {
    return evaluator != null
        ? selectBestChildUsingNN(node, evaluator)
        : selectBestChildUsingUCT(node, myIdx);
  }

  /**
   * Selects the best child using a neural network to evaluate game states.
   *
   * @param node The current parent node.
   * @param evaluator The NN to score child states.
   * @return The child node with the highest evaluation.
   */
  private static MctsNode selectBestChildUsingNN(
      final MctsNode node, final NeuralNetworkEvaluator evaluator) {
    MctsNode bestChild = null;
    double bestValue = Double.NEGATIVE_INFINITY;

    for (final MctsNode child : node.getChildren()) {
      final double value = -evaluator.evaluate(child.getState());
      if (value > bestValue) {
        bestValue = value;
        bestChild = child;
      }
    }
    return bestChild;
  }

  /**
   * Selects the best child using the Upper Confidence Bound applied to Trees (UCT).
   *
   * @param node The current parent node.
   * @param myIdx The AI player's index to determine perspective.
   * @return The child node maximizing the UCT value.
   */
  private static MctsNode selectBestChildUsingUCT(final MctsNode node, final int myIdx) {
    MctsNode bestChild = null;
    double maxUct = Double.NEGATIVE_INFINITY;
    final int currentPlayerIdx = node.getCurrentIndexPlayer();
    final boolean isAiTurn = currentPlayerIdx == myIdx;

    for (final MctsNode child : node.getChildren()) {
      final double rawWinRate = (double) child.getWins() / child.getVisits();
      final double winRate = isAiTurn ? rawWinRate : 1.0 - rawWinRate;
      final double uctValue =
          winRate + UCT_CONSTANT * Math.sqrt(Math.log(node.getVisits()) / child.getVisits());
      if (uctValue > maxUct) {
        maxUct = uctValue;
        bestChild = child;
      }
    }
    return bestChild;
  }

  /**
   * Backpropagates the simulation result from a leaf node up to the root.
   *
   * @param node The node where the simulation started.
   * @param winnerIdx The index of the player who won the simulation.
   * @param myIdx The index of the AI player.
   */
  protected static void backpropagate(final MctsNode node, final int winnerIdx, final int myIdx) {
    MctsNode current = node;
    while (current != null) {
      current.incrementVisits();
      if (winnerIdx == myIdx) {
        current.incrementWins();
      }
      current = current.getParent();
    }
  }

  /**
   * Returns a string representation of the AI player.
   *
   * @return A string starting with "AI" followed by the player's name.
   */
  @Override
  public String toString() {
    return "AI " + super.toString();
  }

  /**
   * Retrieves the AI settings, specifically the operational mode.
   *
   * @return A map containing the AI's settings.
   */
  @Override
  public Map<String, Object> getSettings() {
    final Map<String, Object> settings = new ConcurrentHashMap<>();
    settings.put("mode", "mcts");
    settings.put("selection", this.evaluator == null ? "UCT" : "ML");
    settings.put("reflexion", this.reflectionTime);
    return settings;
  }
}
