package fr.bordeaux.main;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import fr.bordeaux.gui.QuoridorApp;
import fr.bordeaux.util.Logger;
import java.io.IOException;
import javafx.application.Application;

/** Starts Quoridor GUI via {@link QuoridorApp}. */
public class GuiLauncher implements AppLauncher {

  /** Default constructor. */
  public GuiLauncher() {
    Logger.getInstance().debug("Instantiating GuiLauncher");
  }

  /** Launches {@link Application#launch(Class, String...)}. */
  @Override
  public void launch(final String[] args, ConfigManager config) {
    Application.launch(QuoridorApp.class, args);
  }

  /**
   * Builds and returns a new GameEngine instance using the current configuration.
   *
   * @return a new {@link GameEngine} initialized with the configuration
   * @throws IOException if there is an error reading the configuration
   */
  public GameEngine buildEngine() throws IOException {
    return GameEngineFactory.createGameEngine(ConfigManager.getInstance());
  }
}
