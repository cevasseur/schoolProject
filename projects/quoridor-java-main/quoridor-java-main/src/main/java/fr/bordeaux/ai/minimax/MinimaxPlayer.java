package fr.bordeaux.ai.minimax;

import fr.bordeaux.ai.base.AiPlayer;
import fr.bordeaux.ai.base.HeuristicEvaluator;
import fr.bordeaux.ai.heuristics.*;
import fr.bordeaux.ai.random.RandomPlayer;
import fr.bordeaux.core.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** AI player using Minimax with Alpha-Beta pruning and Iterative Deepening. */
public final class MinimaxPlayer extends AiPlayer {

  /** The maximum depth the AI will search if iterative deepening is disabled. */
  private final int searchDepth;

  /** The maximum reflection time allowed for the AI in seconds. */
  private final int reflexionTime;

  /** Flag indicating whether iterative deepening is enabled. */
  private final boolean useIterativeD;

  /** The name of the heuristic algorithm to use. */
  private final String heuristicName;

  /**
   * * The core search engine instance responsible for executing the Minimax and Alpha-Beta pruning
   * algorithms.
   */
  private final MinimaxSearch searchEngine;

  /**
   * Private constructor used by the Builder to instantiate the AI.
   *
   * @param builder The builder containing configuration settings.
   */
  public MinimaxPlayer(final MinimaxBuilder builder) {
    super(builder.getName(), builder.getStartingPosition(), builder.getColor());
    this.searchDepth = builder.getSearchDepth();
    this.reflexionTime = builder.getLimitTime();
    this.useIterativeD = builder.isUseIterativeDeepening();
    this.heuristicName = builder.getHeuristicName();

    final HeuristicEvaluator evaluator = determineHeuristic(this.heuristicName);
    this.searchEngine = new MinimaxSearch(evaluator);
  }

  /** Internal constructor for cloning. */
  private MinimaxPlayer(
      final String name,
      final Position current,
      final Position start,
      final int walls,
      final int depth,
      final int timeLimit,
      final boolean iterativeDeepVal,
      final String heuristicVal,
      final Color color) {
    super(name, current, start, walls, color);
    this.searchDepth = depth;
    this.reflexionTime = timeLimit;
    this.useIterativeD = iterativeDeepVal;
    this.heuristicName = heuristicVal;

    final HeuristicEvaluator evaluator = determineHeuristic(this.heuristicName);
    this.searchEngine = new MinimaxSearch(evaluator);
  }

  /**
   * Static factory method for the builder.
   *
   * @param name The player name.
   * @param pos The starting position.
   * @param color The AI's color on the board.
   * @return A new Builder instance.
   */
  public static MinimaxBuilder builder(final String name, final Position pos, final Color color) {
    return new MinimaxBuilder(name, pos, color);
  }

  /**
   * Sets the heuristic function based on the provided name.
   *
   * @param heuristicNameP The name of the heuristic.
   * @return The corresponding heuristic class.
   */
  private HeuristicEvaluator determineHeuristic(final String heuristicNameP) {
    final HeuristicEvaluator chosenHeuristic;
    switch (heuristicNameP) {
      case "intermediate":
        chosenHeuristic = new MediumHeuristic(this.getColor());
        break;
      case "advanced":
        chosenHeuristic = new AdvancedHeuristic(this.getColor());
        break;
      case "simple":
      default:
        chosenHeuristic = new SimpleHeuristic(this.getColor());
        break;
    }
    return chosenHeuristic;
  }

  /**
   * Computes and returns the best possible move within the allocated time limit.
   *
   * @param state The current game state.
   * @return The best move found.
   */
  @Override
  public Move getNextMove(final GameState state) {

    final long deadlineTime = System.currentTimeMillis() + (this.reflexionTime * 1000L);
    Move bestMove;

    if (this.useIterativeD) {
      bestMove = searchEngine.iterativeDeepeningSearch(state, deadlineTime);
    } else {
      bestMove = searchEngine.search(state, this.searchDepth, deadlineTime);
    }

    if (bestMove == null) {
      final RandomPlayer fallback =
          new RandomPlayer(this.getName(), this.getPosition(), this.getColor());
      bestMove = fallback.getNextMove(state);
    }
    return bestMove;
  }

  /**
   * Get the search depth of the MiniMax Player.
   *
   * @return The depth search value of Minimax
   */
  public int getSearchDepth() {
    return this.searchDepth;
  }

  /**
   * Get the limit time reflexion of the MiniMax Player.
   *
   * @return The reflexion time value of Minimax
   */
  public int getLimitTime() {
    return this.reflexionTime;
  }

  /**
   * Get if the MiniMax Player use iterative deepening.
   *
   * @return The boolean representing if Minimax use Iterative Deepening
   */
  public boolean isUseIterativeDeepening() {
    return this.useIterativeD;
  }

  /**
   * Get the heuristicname of the MiniMax Player.
   *
   * @return The name of the heuristic selected
   */
  public String getHeuristicName() {
    return this.heuristicName;
  }

  @Override
  public String toString() {
    return "MiniMax AI " + super.toString();
  }

  @Override
  public Player copy() {
    return new MinimaxPlayer(
        this.getName(),
        this.getPosition(),
        this.getStartingPosition(),
        this.getRemainingWalls(),
        this.searchDepth,
        this.reflexionTime,
        this.useIterativeD,
        this.heuristicName,
        this.getColor());
  }

  @Override
  public Map<String, Object> getSettings() {
    final Map<String, Object> settings = new ConcurrentHashMap<>();
    if (this.useIterativeD) {
      settings.put("mode", "minimax-iterative-deepening");
    } else {
      settings.put("mode", "minimax");
      settings.put("depth", this.searchDepth);
    }
    settings.put("reflexion", this.reflexionTime);
    settings.put("heuristic", this.heuristicName);
    return settings;
  }
}
