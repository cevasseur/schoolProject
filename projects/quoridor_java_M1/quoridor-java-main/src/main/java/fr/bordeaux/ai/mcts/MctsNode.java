package fr.bordeaux.ai.mcts;

import static fr.bordeaux.ai.mcts.MctsPlayer.backpropagate;
import static fr.bordeaux.ai.mcts.MctsPlayer.bestUCT;

import fr.bordeaux.ai.nn.NeuralNetworkEvaluator;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import fr.bordeaux.core.RuleChecker;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Inner class representing a node within the MCTS search tree. */
public class MctsNode {

  /** The game state associated with this node. */
  private final GameState state;

  /** Logger for AI search progress. */
  private static final Logger LOGGER = Logger.getInstance();

  /** The move that led to this node. */
  private final Move move;

  /** The parent node in the tree. */
  private final MctsNode parent;

  /** List of child nodes. */
  private final List<MctsNode> children = new ArrayList<>();

  /** List of legal moves that haven't been explored yet. */
  private final List<Move> untriedMoves;

  /** Total number of wins backpropagated to this node. */
  private int wins;

  /** Total number of times this node has been visited. */
  private int visits;

  /** Constant direction offsets for orthogonal movement. */
  private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

  /** Diagonal direction offsets for jumps only. */
  private static final int[][] DIAG_DIRS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

  /** Maximum number of turns allowed in a single simulation playout. */
  private static final int MAX_TURNS = 200;

  /**
   * Constructor for the root node.
   *
   * @param state The initial game state for the search.
   */
  public MctsNode(final GameState state) {
    this(state, null, null);
  }

  /**
   * Constructor for child nodes.
   *
   * @param state The game state resulting from the applied move.
   * @param move The move that led to this state.
   * @param parent The parent node in the search tree.
   */
  public MctsNode(final GameState state, final Move move, final MctsNode parent) {
    this.state = state;
    this.move = move;
    this.parent = parent;
    this.untriedMoves = new ArrayList<>(state.generatePseudoLegalMoves());
    Collections.shuffle(this.untriedMoves);
  }

  /**
   * Returns true if there is at least one untried legal move for this node.
   *
   * @return true if an untried legal move exists, false otherwise
   */
  public boolean hasUntriedLegalMoves() {
    boolean isMove = false;
    while (!untriedMoves.isEmpty() && !isMove) {
      if (RuleChecker.isMoveLegal(state, untriedMoves.getLast())) {
        isMove = true;
      } else {
        untriedMoves.removeLast();
      }
    }
    return isMove;
  }

  /**
   * Performs MCTS iterations until the reflection time limit is reached.
   *
   * @param limit The time limit in milliseconds for the search.
   * @param myIdx The index of the player for whom we are optimizing.
   * @param evaluator The neural network used to guide or evaluate states.
   * @return The best move found after the search duration.
   */
  public Move getBestMoveAfterSearch(
      final long limit, final int myIdx, final NeuralNetworkEvaluator evaluator) {
    final long start = System.currentTimeMillis();
    int iterations = 0;
    while (System.currentTimeMillis() - start < limit && !Thread.currentThread().isInterrupted()) {
      MctsNode current = this;
      // Selection
      while (!current.state.isGameOver() && !current.hasUntriedLegalMoves()) {
        if (current.children.isEmpty()) {
          break;
        }
        current = bestUCT(current, myIdx, evaluator);
      }
      // Expansion
      if (!current.state.isGameOver() && current.hasUntriedLegalMoves()) {
        current = current.expand();
      }
      // Simulation & Backpropagation
      if (current != null) {
        backpropagate(current, current.simulate(), myIdx);
      }
      iterations++;
    }

    if (Thread.currentThread().isInterrupted()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(I18n.get("mctsInterrupted", limit));
      }
    }

    final Move bestMove = this.extractBestMove();

    MctsNode bestChild = null;
    for (MctsNode child : children) {
      if (child.move.equals(bestMove)) {
        bestChild = child;
        break;
      }
    }
    final long endTime = System.currentTimeMillis();
    Logger.getInstance()
        .verbose(
            I18n.get(
                "mctsSummary",
                iterations,
                (endTime - start),
                bestMove,
                bestChild != null ? bestChild.visits : 0));

    return bestMove;
  }

  /**
   * Extracts the best move from children.
   *
   * @return the most-visited child's move, or null if no children
   */
  private Move extractBestMove() {
    Move result = null;
    if (!children.isEmpty()) {
      MctsNode bestNode = children.getFirst();
      for (final MctsNode child : children) {
        if (child.visits > bestNode.visits) {
          bestNode = child;
        }
      }
      result = bestNode.move;
    }
    return result;
  }

  /**
   * Expands the tree by creating a new child node from an untried move.
   *
   * @return The newly created MctsNode.
   */
  public MctsNode expand() {
    final Move nextMove = untriedMoves.removeLast();
    final GameState nextState = state.copy();
    final Player currentPlayer = nextState.getCurrentPlayer();
    nextState.applyMove(nextMove);
    if (nextState.hasReachedGoal(currentPlayer)) {
      nextState.setGameOver(true);
    }
    final MctsNode newNode = new MctsNode(nextState, nextMove, this);
    children.add(newNode);
    return newNode;
  }

  /**
   * Simulates a random game from the current node's state.
   *
   * @return The index of the winning player, or -1 if the game reaches MAX_TURNS.
   */
  public int simulate() {
    final GameState temp = state.copy();
    int winnerResult;

    if (temp.isGameOver()) {
      winnerResult = temp.getPreviousIndexPlayer();
    } else {
      final List<Player> allPlayers = temp.getPlayers();
      winnerResult = -1;
      int turns = 0;

      while (winnerResult == -1 && turns < MAX_TURNS) {
        winnerResult = simulateOneStep(temp, allPlayers);
        turns++;
      }
    }

    return winnerResult;
  }

  /**
   * Simulates one step of the game by selecting a random legal pawn move.
   *
   * @param temp the current game state being simulated
   * @param allPlayers list of all players in the game
   * @return the index of the winner if the move results in a win, or -1 otherwise
   */
  private int simulateOneStep(final GameState temp, final List<Player> allPlayers) {
    final Position currentPos = temp.getCurrentPlayer().getPosition();
    final int curX = currentPos.getX();
    final int curY = currentPos.getY();

    int result = -1;

    final List<Move> legalMoves = new ArrayList<>(12);

    for (final int[] direction : DIRECTIONS) {
      final Position stepPos = new Position(curX + direction[0], curY + direction[1]);
      final Move stepMove = Move.pawn(currentPos, stepPos);
      if (RuleChecker.isMoveLegal(temp, stepMove)) {
        legalMoves.add(stepMove);
      }

      final Position jumpPos = new Position(curX + (direction[0] * 2), curY + (direction[1] * 2));
      final Move jumpMove = Move.pawn(currentPos, jumpPos);
      if (RuleChecker.isMoveLegal(temp, jumpMove)) {
        legalMoves.add(jumpMove);
      }
    }

    for (final int[] direction : DIAG_DIRS) {
      final Position diagPos = new Position(curX + direction[0], curY + direction[1]);
      final Move diagonalStep = Move.pawn(currentPos, diagPos);
      if (RuleChecker.isMoveLegal(temp, diagonalStep)) {
        legalMoves.add(diagonalStep);
      }
    }

    if (!legalMoves.isEmpty()) {
      final int randomIndex = (int) (Math.random() * legalMoves.size());
      final Move selectedMove = legalMoves.get(randomIndex);
      final int lastIdx = temp.getCurrentIndexPlayer();

      temp.applyMoveWithoutClearingRedo(selectedMove);

      if (temp.hasReachedGoal(allPlayers.get(lastIdx))) {
        result = lastIdx;
      }
    }

    return result;
  }

  /**
   * Returns the index of the player whose turn it is in the current node's state.
   *
   * @return The current player index.
   */
  public int getCurrentIndexPlayer() {
    return state.getCurrentIndexPlayer();
  }

  /**
   * Returns the list of child nodes expanded from this node.
   *
   * @return A list of MctsNode children.
   */
  public List<MctsNode> getChildren() {
    return children;
  }

  /**
   * Returns the parent node of this node.
   *
   * @return The parent MctsNode, or null if this is the root.
   */
  public MctsNode getParent() {
    return parent;
  }

  /** Increments the total visit count for this node. */
  public void incrementVisits() {
    this.visits++;
  }

  /** Increments the total win count backpropagated to this node. */
  public void incrementWins() {
    this.wins++;
  }

  /**
   * Gets the total number of wins recorded for this node.
   *
   * @return The win count.
   */
  public int getWins() {
    return wins;
  }

  /**
   * Gets the total number of times this node has been visited.
   *
   * @return The visit count.
   */
  public int getVisits() {
    return visits;
  }

  /**
   * * Gets the game state represented by this node.
   *
   * @return The current GameState.
   */
  public GameState getState() {
    return state;
  }
}
