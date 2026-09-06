package fr.bordeaux.core;

import java.util.Objects;

/** Quoridor move: pawn movement or wall placement. */
public final class Move {

  /** Defines the type of move. */
  public enum Type {
    /** Pawn jumping or stepping to a new position. */
    PAWN,
    /** A move representing the placement of a wall on the board. */
    WALL
  }

  private final Type type;
  private final Position from;
  private final Position target;
  private final Orientation orientation;

  /**
   * Private constructor for static factory methods.
   *
   * @param type The type of the move (PAWN or WALL).
   * @param from The starting position (only for PAWN moves).
   * @param target The target position or wall coordinate.
   * @param orientation The orientation of the wall (only for WALL moves).
   */
  private Move(
      final Type type, final Position from, final Position target, final Orientation orientation) {
    this.type = type;
    this.from = from;
    this.target = target;
    this.orientation = orientation;
  }

  /**
   * Creates a pawn movement.
   *
   * @param from The current position of the pawn.
   * @param target The destination position.
   * @return A new {@link Move} instance of type PAWN.
   */
  public static Move pawn(final Position from, final Position target) {
    return new Move(Type.PAWN, from, target, null);
  }

  /**
   * Creates a wall placement.
   *
   * @param target The top-left coordinate where the wall is placed.
   * @param orientation The orientation of the wall (HORIZONTAL or VERTICAL).
   * @return A new {@link Move} instance of type WALL.
   */
  public static Move wall(final Position target, final Orientation orientation) {
    return new Move(Type.WALL, null, target, orientation);
  }

  /**
   * Gets the starting position of the move.
   *
   * @return The starting {@link Position}, or {@code null} if it's a wall placement.
   */
  public Position getFrom() {
    return from;
  }

  /**
   * Gets the destination or target position of the move.
   *
   * @return The target {@link Position}.
   */
  public Position getTo() {
    return target;
  }

  /**
   * Gets the orientation of the wall.
   *
   * @return The {@link Orientation}, or {@code null} if it's a pawn move.
   */
  public Orientation getOrientation() {
    return orientation;
  }

  /**
   * Gets the type of the move.
   *
   * @return The {@link Type} of move.
   */
  public Type getType() {
    return type;
  }

  /**
   * Checks if the move is a wall placement.
   *
   * @return {@code true} if it's a wall, {@code false} otherwise.
   */
  public boolean isWall() {
    return type == Type.WALL;
  }

  /**
   * Checks if the move is a pawn movement.
   *
   * @return {@code true} if it's a pawn move, {@code false} otherwise.
   */
  public boolean isPawn() {
    return type == Type.PAWN;
  }

  /**
   * Returns a string representation of the move.
   *
   * @return A string containing move details.
   */
  @Override
  public String toString() {
    return "Move{"
        + "type="
        + type
        + ", from="
        + from
        + ", target="
        + target
        + ", orientation="
        + orientation
        + '}';
  }

  /**
   * Compares this move with another object for equality.
   *
   * @param other The object to compare with.
   * @return {@code true} if the moves are identical.
   */
  @Override
  public boolean equals(final Object other) {
    if (this == other) return true;
    if (!(other instanceof Move)) return false;
    final Move move = (Move) other;
    return type == move.type
        && ((from == null && move.from == null) || from.equals(move.from))
        && target.equals(move.target)
        && orientation == move.orientation;
  }

  /**
   * Generates a hash code for this move.
   *
   * @return The hash code value.
   */
  @Override
  public int hashCode() {
    return Objects.hash(type, from, target, orientation);
  }
}
