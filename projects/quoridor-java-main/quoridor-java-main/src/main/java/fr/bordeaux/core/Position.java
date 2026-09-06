package fr.bordeaux.core;

import java.util.Objects;

/** Represents a position on the Quoridor board. */
public class Position {

  private final int posX;
  private final int posY;

  /**
   * Constructs a new Position with specified coordinates.
   *
   * @param posX The x-coordinate.
   * @param posY The y-coordinate.
   */
  public Position(final int posX, final int posY) {
    this.posX = posX;
    this.posY = posY;
  }

  /**
   * Gets the x-coordinate of this position.
   *
   * @return The x value.
   */
  public int getX() {
    return posX;
  }

  /**
   * Gets the y-coordinate of this position.
   *
   * @return The y value.
   */
  public int getY() {
    return posY;
  }

  /**
   * Returns a string representation of the position.
   *
   * @return A string in the format "Position: x, y".
   */
  @Override
  public String toString() {
    return "Position: " + this.posX + ", " + this.posY;
  }

  /**
   * Compares this position to another object for equality.
   *
   * @param obj The object to compare with.
   * @return {@code true} if the coordinates are identical, {@code false} otherwise.
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Position position = (Position) obj;
    return this.posX == position.posX && this.posY == position.posY;
  }

  /**
   * Generates a hash code based on the x and y coordinates.
   *
   * @return The hash code value for this position.
   */
  @Override
  public int hashCode() {
    return Objects.hash(posX, posY);
  }
}
