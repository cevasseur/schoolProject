package fr.bordeaux.core;

import java.util.ArrayList;
import java.util.List;

/** Player in Quoridor with name, position, walls, and score. */
public abstract class Player {

  /** The color assigned to the player for visual representation. */
  private final Color color;

  /** The name of the player. */
  private final String name;

  /** The current position of the player's pawn on the board. */
  private Position position;

  /** The number of walls the player has remaining. */
  private int remainingWalls = 10;

  /** The remaining reflection time for the player in milliseconds. */
  private long remainingTime = 30 * 60 * 1000;

  /** The initial starting position of the player on the board. */
  private final Position startingPosition;

  /** Whether the player is currently disabled (e.g., in Blitz mode elimination). */
  private boolean disabled;

  /**
   * Constructs a new Player with a name and starting position.
   *
   * @param name The name of the player.
   * @param positionStart The initial starting position on the board.
   * @param color The color of the player.
   */
  public Player(final String name, final Position positionStart, final Color color) {
    this.name = name;
    this.position = positionStart;
    this.startingPosition = positionStart;
    this.color = color;
  }

  /**
   * Internal constructor for state restoration.
   *
   * @param name player name
   * @param currentPos current position
   * @param startPos starting position
   * @param walls remaining walls
   * @param color player color
   */
  protected Player(
      final String name,
      final Position currentPos,
      final Position startPos,
      final int walls,
      final Color color) {
    this.name = name;
    this.position = new Position(currentPos.getX(), currentPos.getY());
    this.startingPosition = startPos;
    this.remainingWalls = walls;
    this.color = color;
  }

  /**
   * Creates a default list of players for a standard 2-player game.
   *
   * @return A list containing two {@link HumanPlayer} instances.
   */
  public static List<Player> createDefaultPlayers() {
    final List<Player> players = new ArrayList<>();
    final Player player1 = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    final Player player2 = new HumanPlayer("Bob", new Position(8, 4), Color.BLACK);
    players.add(player1);
    players.add(player2);
    return players;
  }

  /**
   * Gets the name of the player.
   *
   * @return The player's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the initial starting position of the player.
   *
   * @return The starting {@link Position}.
   */
  public Position getStartingPosition() {
    return startingPosition;
  }

  /**
   * Gets the current position of the player's pawn.
   *
   * @return The current {@link Position}.
   */
  public Position getPosition() {
    return position;
  }

  /**
   * Sets the current position of the player's pawn.
   *
   * @param position The new {@link Position} to set.
   */
  public void setPosition(final Position position) {
    this.position = position;
  }

  /**
   * Gets the current disable boolean of the player's pawn.
   *
   * @return The current disable boolean.
   */
  public boolean isDisabled() {
    return disabled;
  }

  /**
   * Sets the current disable boolean of the player's pawn.
   *
   * @param disBoolean The new boolean to set.
   */
  public void setDisabled(final boolean disBoolean) {
    this.disabled = disBoolean;
  }

  /**
   * Gets the number of walls the player has remaining.
   *
   * @return The count of available walls.
   */
  public int getRemainingWalls() {
    return remainingWalls;
  }

  /**
   * Sets the number of walls remaining
   *
   * @param walls The count of wanted walls
   */
  public void setRemainingWalls(final int walls) {
    this.remainingWalls = walls;
  }

  /** Decrements the count of remaining walls if the player has any left. */
  public void useWall() {
    if (remainingWalls > 0) {
      remainingWalls--;
    }
  }

  /** Increments the count of remaining walls (for undo). */
  public void giveBackWall() {
    remainingWalls++;
  }

  /**
   * Retrieves the color assigned to the player.
   *
   * @return The color of the player.
   */
  public Color getColor() {
    return color;
  }

  /**
   * Returns the player's remaining reflection time.
   *
   * @return The remaining time in milliseconds.
   */
  public long getRemainingTime() {
    return remainingTime;
  }

  /**
   * Sets the player's remaining reflection time.
   *
   * @param remainingTime The new remaining time in milliseconds.
   */
  public void setRemainingTime(final long remainingTime) {
    this.remainingTime = remainingTime;
  }

  /**
   * Determines the player's next move.
   *
   * @param state The current state of the game.
   * @return The selected {@link Move}.
   */
  public abstract Move getNextMove(GameState state);

  /**
   * Checks if the player is controlled by AI.
   *
   * @return {@code true} if the player is an AI, {@code false} otherwise.
   */
  public abstract Boolean isAI();

  /**
   * Creates a deep copy of the player.
   *
   * @return A new {@link Player} instance with the same internal state.
   */
  public abstract Player copy();

  /**
   * Returns a string representation of the player.
   *
   * @return A formatted string with player details.
   */
  @Override
  public String toString() {
    return String.format("Player{name='%s', pos=%s, walls=%d}", name, position, remainingWalls);
  }
}
