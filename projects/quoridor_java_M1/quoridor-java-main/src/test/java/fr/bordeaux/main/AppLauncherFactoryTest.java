package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AppLauncherFactoryTest {

  @Test
  public void testCreateGuiLauncher() {
    AppLauncher launcher = AppLauncherFactory.create(AppMode.GUI);
    assertTrue(launcher instanceof GuiLauncher, "Should return a GuiLauncher");
  }

  @Test
  public void testCreateNetworkLauncher() {
    AppLauncher launcher = AppLauncherFactory.create(AppMode.NETWORK);
    assertTrue(launcher instanceof NetworkLauncher, "Should return a NetworkLauncher");
  }

  @Test
  public void testCreateCliLauncher() {
    AppLauncher launcher = AppLauncherFactory.create(AppMode.CLI);
    assertTrue(launcher instanceof CliLauncher, "Should return a CliLauncher");
  }
}
