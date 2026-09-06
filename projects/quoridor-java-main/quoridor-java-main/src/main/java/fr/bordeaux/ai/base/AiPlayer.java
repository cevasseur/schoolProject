package fr.bordeaux.ai.base;

import fr.bordeaux.core.*;
import java.util.Map;

/** Base class for AI players. */
public abstract class AiPlayer extends Player {

  /**
   * Constructs a new AI player with a given name and starting position.
   *
   * @param name The name of the AI player.
   * @param positionStart The starting position of the AI on the board.
   * @param color The color of the player.
   */
  public AiPlayer(final String name, final Position positionStart, final Color color) {
    super(name, positionStart, color);
  }

  /**
   * Internal constructor for state restoration.
   *
   * @param name player name
   * @param current current position
   * @param start starting position
   * @param walls remaining walls
   * @param color player color
   */
  protected AiPlayer(
      final String name,
      final Position current,
      final Position start,
      final int walls,
      final Color color) {
    super(name, current, start, walls, color);
  }

  /**
   * Indicates whether this player is controlled by an AI.
   *
   * @return {@code true} since this class represents an AI player.
   */
  @Override
  public Boolean isAI() {
    return true;
  }

  /**
   * Creates and returns a deep copy of this AI player.
   *
   * @return A new instance of {@link Player} with the same state.
   */
  @Override
  public abstract Player copy();

  @Override
  public abstract Move getNextMove(GameState state);

  /**
   * Gets AI configuration settings.
   *
   * @return Map of settings.
   */
  public abstract Map<String, Object> getSettings();
}
