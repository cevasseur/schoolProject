package fr.bordeaux.main;

/** Resolves AppMode from args (-g, -n, -c). Default is CLI. */
public final class AppModeResolver {

  /** Private constructor to prevent instantiation. */
  private AppModeResolver() {}

  /**
   * Resolves the application mode from command-line arguments.
   *
   * @param args command-line arguments
   * @return the resolved AppMode, or CLI if none
   */
  public static AppMode resolve(final String... args) {
    for (final String arg : args) {
      switch (arg) {
        case "--gui":
        case "-g":
          return AppMode.GUI;

        case "--net":
        case "-n":
          return AppMode.NETWORK;

        case "--server":
        case "-s":
          return AppMode.NETWORK;

        case "--cli":
        case "-c":
          return AppMode.CLI;

        default:
          break;
      }
    }
    return AppMode.CLI;
  }
}
