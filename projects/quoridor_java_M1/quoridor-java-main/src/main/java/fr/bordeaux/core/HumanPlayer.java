package fr.bordeaux.core;

/** Represents a human player */
public class HumanPlayer extends Player {

  /**
   * Constructs a new HumanPlayer with a name and a starting position.
   *
   * @param name The name of the player.
   * @param positionStart The initial starting position on the board.
   * @param color The color of the player.
   */
  public HumanPlayer(final String name, final Position positionStart, final Color color) {
    super(name, positionStart, color);
  }

  @Override
  public Player copy() {
    return new HumanPlayer(
        this.getName(),
        this.getPosition(),
        this.getStartingPosition(),
        this.getRemainingWalls(),
        this.getColor());
  }

  /** Private constructor for state restoration. */
  private HumanPlayer(
      final String name,
      final Position current,
      final Position start,
      final int walls,
      final Color color) {
    super(name, current, start, walls, color);
  }

  /**
   * @return null as humans decide moves externally.
   */
  @Override
  public Move getNextMove(final GameState state) {
    return null;
  }

  /**
   * Returns a string representation of the human player.
   *
   * @return A string starting with "Human" followed by player details.
   */
  @Override
  public String toString() {
    return "Human" + super.toString();
  }

  /**
   * Indicates that this player is not an AI.
   *
   * @return {@code false} as this is a human player.
   */
  @Override
  public Boolean isAI() {
    return false;
  }
}
