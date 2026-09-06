package fr.bordeaux.core;

/** Represents player colors in the Quoridor game. */
public enum Color {
  /** White color (ID 1). */
  WHITE(1),
  /** Black color (ID 2). */
  BLACK(2),
  /** Blue color (ID 3). */
  BLUE(3),
  /** Red color (ID 4). */
  RED(4);

  private final int colorId;

  /**
   * Constructs a color with an ID.
   *
   * @param colorId the unique integer ID.
   */
  Color(final int colorId) {
    this.colorId = colorId;
  }

  /**
   * Retrieves the integer ID of the color.
   *
   * @return the integer ID of the color.
   */
  public int getId() {
    return colorId;
  }

  /**
   * Returns the Color associated with the given ID.
   *
   * @param colorId the integer ID to look up.
   * @return the matching Color.
   * @throws IllegalArgumentException if the ID is invalid.
   */
  public static Color fromId(final int colorId) {
    for (final Color color : values()) {
      if (color.colorId == colorId) {
        return color;
      }
    }
    throw new IllegalArgumentException(
        CoreServiceProvider.getTranslator().translate("unknownColor") + colorId);
  }
}
