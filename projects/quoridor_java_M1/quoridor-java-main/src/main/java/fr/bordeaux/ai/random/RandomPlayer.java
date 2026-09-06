package fr.bordeaux.ai.random;

import fr.bordeaux.ai.base.AiPlayer;
import fr.bordeaux.core.*;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/** Implementation of an AI player that selects a random legal move */
public class RandomPlayer extends AiPlayer {

  /**
   * Constructs a RandomPlayer with a name and a starting position.
   *
   * @param name The name of the player.
   * @param positionStart The initial starting position on the board.
   * @param color The AI's color on the board.
   */
  public RandomPlayer(final String name, final Position positionStart, final Color color) {
    super(name, positionStart, color);
  }

  /**
   * Determines the next move by picking one at random from all legal moves.
   *
   * @param state The current state of the game.
   * @return A randomly selected legal {@link Move}.
   */
  @Override
  public Move getNextMove(final GameState state) {
    final List<Move> moveList = state.generateLegalMoves();
    final Random random = new Random();
    return moveList.get(random.nextInt(moveList.size()));
  }

  /**
   * Returns a string representation of the Random AI player.
   *
   * @return A string containing the AI type and player details.
   */
  @Override
  public String toString() {
    return "Random AI " + super.toString();
  }

  /**
   * Creates a deep copy of this RandomPlayer.
   *
   * @return A new instance of {@link RandomPlayer} with the same state.
   */
  @Override
  public Player copy() {
    return new RandomPlayer(
        this.getName(),
        this.getPosition(),
        this.getStartingPosition(),
        this.getRemainingWalls(),
        this.getColor());
  }

  /** Internal constructor for state management and cloning. */
  private RandomPlayer(
      final String name,
      final Position current,
      final Position start,
      final int walls,
      final Color color) {
    super(name, current, start, walls, color);
  }

  /**
   * Indicates that this player is controlled by an AI.
   *
   * @return {@code true} since this is a RandomPlayer AI.
   */
  @Override
  public Boolean isAI() {
    return true;
  }

  /**
   * Retrieves the specific settings for this AI player.
   *
   * @return A map containing the AI mode configuration.
   */
  @Override
  public Map<String, Object> getSettings() {
    final Map<String, Object> settings = new ConcurrentHashMap<>();
    settings.put("mode", "random");
    return settings;
  }
}
