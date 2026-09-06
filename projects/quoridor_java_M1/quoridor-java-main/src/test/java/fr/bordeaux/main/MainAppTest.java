package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.i18n.I18n;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for MainApp entry point. */
public class MainAppTest {

  @BeforeEach
  public void setUp() {
    System.setProperty("quoridor.test", "true");
    I18n.setLocale(java.util.Locale.ENGLISH);
  }

  @Test
  public void testMainHelp() {
    // Should not exit because quoridor.test is true
    assertDoesNotThrow(() -> MainApp.main(new String[] {"--help"}));
  }

  @Test
  public void testMainVersion() {
    assertDoesNotThrow(() -> MainApp.main(new String[] {"--version"}));
  }

  @Test
  public void testMainInvalidOption() {
    // Should handle IllegalArgumentException and "exit"
    assertDoesNotThrow(() -> MainApp.main(new String[] {"--unknown-option"}));
  }

  @Test
  public void testMainDaemon() {
    // This will try to resolve mode and launch.
    assertDoesNotThrow(() -> MainApp.main(new String[] {"--daemon"}));
  }
}
