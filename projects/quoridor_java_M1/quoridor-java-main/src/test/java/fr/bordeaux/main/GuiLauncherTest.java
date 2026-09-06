package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.GameEngine;
import org.junit.jupiter.api.Test;

/** Unit tests for GuiLauncher. */
public class GuiLauncherTest {

  @Test
  public void testGuiLauncherConstructor() {
    assertNotNull(new GuiLauncher());
  }

  @Test
  public void testGuiLauncherBuildEngine() {
    GuiLauncher launcher = new GuiLauncher();
    assertDoesNotThrow(
        () -> {
          GameEngine engine = launcher.buildEngine();
          assertNotNull(engine);
        });
  }
}
