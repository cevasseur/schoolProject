package fr.bordeaux.core;

/** Possible orientations for wall placement. A wall can be horizontal or vertical. */
public enum Orientation {

  /** Represents a wall placed horizontally between rows. */
  HORIZONTAL,

  /** Represents a wall placed vertically between columns. */
  VERTICAL;

  /**
   * Returns a short string representation of the orientation.
   *
   * @return {@code "h"} for HORIZONTAL or {@code "v"} for VERTICAL.
   */
  @Override
  public String toString() {
    return (this == HORIZONTAL) ? "h" : "v";
  }
}
