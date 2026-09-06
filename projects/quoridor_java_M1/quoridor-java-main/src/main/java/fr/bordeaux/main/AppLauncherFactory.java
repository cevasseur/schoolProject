package fr.bordeaux.main;

/**
 * Factory for creating the appropriate {@link AppLauncher} based on the selected {@link AppMode}.
 */
public final class AppLauncherFactory {

  /** Default constructor. Constructs a new AppLauncherFactory instance. */
  private AppLauncherFactory() {}

  /**
   * Creates an AppLauncher corresponding to the given mode.
   *
   * @param mode the selected application mode
   * @return the appropriate launcher implementation
   */
  public static AppLauncher create(final AppMode mode) {
    final AppLauncher launcher;
    if (mode == AppMode.GUI) {
      launcher = new GuiLauncher();
    } else if (mode == AppMode.NETWORK) {
      launcher = new NetworkLauncher();
    } else {
      launcher = new CliLauncher();
    }
    return launcher;
  }
}
