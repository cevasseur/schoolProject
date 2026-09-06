package fr.bordeaux.ai.minimax;

import fr.bordeaux.core.Color;
import fr.bordeaux.core.Position;
import java.util.ArrayList;
import java.util.List;

/** Builder for MinimaxPlayer. */
public class MinimaxBuilder {

  /** The player's unique name identifier. */
  private final String name;

  /** The starting position of the player on the game board. */
  private final Position positionStart;

  /** The player's color. */
  private final Color color;

  /** The maximum depth the AI will search if iterative deepening is disabled. */
  private int searchDepth = 2;

  /** Default reflection time in seconds for the AI engine. */
  private static final int BASE_TIME_LIMIT = 5;

  /** The maximum reflection time allowed for the AI in seconds. */
  private int reflexionTime = BASE_TIME_LIMIT;

  /** Flag indicating whether iterative deepening is enabled. */
  private boolean useIterativeD;

  /** The name of the heuristic algorithm to use. */
  private String heuristicName = "simple";

  /**
   * Initializes the Builder with mandatory parameters.
   *
   * @param nameParameter The AI's name.
   * @param pos The starting position on the board.
   * @param color The AI's color on the board.
   */
  public MinimaxBuilder(final String nameParameter, final Position pos, final Color color) {
    this.name = nameParameter;
    this.positionStart = pos;
    this.color = color;
  }

  /**
   * Sets the depth of the Minimax search.
   *
   * @param depth The chosen depth.
   * @return The builder instance.
   */
  public MinimaxBuilder withDepth(final int depth) {
    this.searchDepth = depth;
    return this;
  }

  /**
   * Sets the reflection time limit in seconds.
   *
   * @param timeLimit The chosen time limit.
   * @return The builder instance.
   */
  public MinimaxBuilder withReflexion(final int timeLimit) {
    this.reflexionTime = timeLimit;
    return this;
  }

  /**
   * Sets the heuristic name to be used.
   *
   * @param heuristic The name of the heuristic.
   * @return The builder instance.
   */
  public MinimaxBuilder withHeuristic(final String heuristic) {
    this.heuristicName = heuristic;
    return this;
  }

  /**
   * Enables or disables iterative deepening.
   *
   * @param useDeepening The boolean flag.
   * @return The builder instance.
   */
  public MinimaxBuilder withIterativeDeepening(final boolean useDeepening) {
    this.useIterativeD = useDeepening;
    return this;
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
   * Get the limit time reflexiob of the MiniMax Player.
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

  /**
   * Get the Name of the MiniMax Player.
   *
   * @return The name the MiniMax Player
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get the starting position of the MiniMax Player.
   *
   * @return The starting position of the MiniMax Player
   */
  public Position getStartingPosition() {
    return this.positionStart;
  }

  /**
   * Get the color of the MiniMax Player.
   *
   * @return The player's color
   */
  public Color getColor() {
    return this.color;
  }

  /**
   * Validates if the given heuristic name is known to the system.
   *
   * @param heuristic The heuristic name to check.
   * @return True if valid, false otherwise.
   */
  private boolean validHeuristic(final String heuristic) {
    boolean isValid = false;
    final List<String> validNames = new ArrayList<>(List.of("simple", "intermediate", "advanced"));
    for (final String element : validNames) {
      if (heuristic.equals(element)) {
        isValid = true;
        break;
      }
    }
    return isValid;
  }

  /**
   * Builds the final MinimaxPlayer instance.
   *
   * @return A new MinimaxPlayer instance.
   * @throws IllegalArgumentException if the configuration is invalid.
   */
  public MinimaxPlayer build() {
    if (!validHeuristic(heuristicName)) {
      throw new IllegalArgumentException(
          "Incorrect name for the heuristic, the " + heuristicName + " heuristic does not exist.");
    }
    if (searchDepth < 0) {
      throw new IllegalArgumentException(
          "Illegal depth value ! Depth is greater than or equal to 0");
    }

    if (reflexionTime <= 0) {
      throw new IllegalArgumentException(
          "Illegal reflexionTime value ! reflexionTime is grater than 0");
    }
    return new MinimaxPlayer(this);
  }
}
