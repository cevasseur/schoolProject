package fr.bordeaux.main;

import fr.bordeaux.config.ConfigManager;

/** Contract for launching different application modes (CLI, GUI, Network). */
public interface AppLauncher {

  /**
   * Launches the application with the given arguments and configuration.
   *
   * @param args command-line arguments passed to the application
   * @param config global configuration manager instance
   */
  void launch(String[] args, ConfigManager config);
}
