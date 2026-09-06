package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AppModeResolverTest {

  @Test
  public void testResolveGuiMode() {
    assertEquals(AppMode.GUI, AppModeResolver.resolve(new String[] {"-g"}));
    assertEquals(AppMode.GUI, AppModeResolver.resolve(new String[] {"--gui"}));
  }

  @Test
  public void testResolveNetworkMode() {
    assertEquals(AppMode.NETWORK, AppModeResolver.resolve(new String[] {"-n"}));
    assertEquals(AppMode.NETWORK, AppModeResolver.resolve(new String[] {"--net"}));
  }

  @Test
  public void testResolveCliMode() {
    assertEquals(AppMode.CLI, AppModeResolver.resolve(new String[] {"-c"}));
    assertEquals(AppMode.CLI, AppModeResolver.resolve(new String[] {"--cli"}));
  }

  @Test
  public void testResolveDefaultMode() {
    assertEquals(AppMode.CLI, AppModeResolver.resolve(new String[] {}));
    assertEquals(AppMode.CLI, AppModeResolver.resolve(new String[] {"--verbose", "-b"}));
  }

  @Test
  public void testResolveFirstMatch() {
    assertEquals(AppMode.GUI, AppModeResolver.resolve(new String[] {"-g", "-n"}));
  }
}
