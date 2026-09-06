package fr.bordeaux.main;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.util.Logger;

/** Main entry point. Loads config, parses options, and resolves launch mode. */
public final class MainApp {

  /** Default constructor. Constructs a new MainApp instance. */
  private MainApp() {}

  /**
   * Main application entry.
   *
   * @param args command-line args
   */
  public static void main(final String[] args) {
    final ConfigManager config = ConfigManager.getInstance();
    config.load();

    try {
      OptionsParser.parse(args, config);

      if (config.isContestMode()) {
        new ContestLauncher().launch(args, config);
      }
      final AppMode mode = AppModeResolver.resolve(args);

      final AppLauncher launcher = AppLauncherFactory.create(mode);
      launcher.launch(args, config);
      if (System.getProperty("quoridor.test") == null) {
        System.exit(0);
      }

    } catch (IllegalArgumentException exception) {
      if (Logger.getInstance().isErrorEnabled()) {
        Logger.getInstance().error("{0}", exception.getMessage());
      }
      OptionsParser.printHelp();
      if (System.getProperty("quoridor.test") == null) {
        System.exit(1);
      }
    }
  }
}
