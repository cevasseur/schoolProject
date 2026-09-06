package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AppModeTest {

  @Test
  public void testEnumValues() {
    AppMode[] modes = AppMode.values();
    assertEquals(3, modes.length, "There should be exactly three application modes");

    assertDoesNotThrow(() -> AppMode.valueOf("GUI"));
    assertDoesNotThrow(() -> AppMode.valueOf("CLI"));
    assertDoesNotThrow(() -> AppMode.valueOf("NETWORK"));
  }
}
