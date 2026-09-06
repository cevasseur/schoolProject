package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for CliLauncher. */
public class CliLauncherTest {

  @Test
  public void testCliLauncherConstructor() {
    assertNotNull(new CliLauncher());
  }

  @Test
  public void testCliLauncherLaunch() {
    System.setProperty("quoridor.test", "true");
    CliLauncher launcher = new CliLauncher();
    fr.bordeaux.config.ConfigManager config = fr.bordeaux.config.ConfigManager.getInstance();
    config.load();
    assertDoesNotThrow(() -> launcher.launch(new String[] {}, config));
  }
}
