package fr.bordeaux.main;

import fr.bordeaux.cli.CliApp;
import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import fr.bordeaux.persistence.GameLoader;
import fr.bordeaux.util.Logger;

/** Initializes board, state, engine, and starts CLI interface. */
public class CliLauncher implements AppLauncher {

  /** Default board size for Quoridor (9x9) */
  private static final int DEFAULT_BOARD_SIZE = 9;

  /** Default constructor. */
  public CliLauncher() {
    Logger.getInstance().debug("Instantiating CliLauncher");
  }

  /** Starts the CLI interface. Exceptions are logged via Logger. */
  @Override
  public void launch(final String[] args, final ConfigManager config) {
    try {
      final GameEngine engine = GameEngineFactory.createGameEngine(config);
      final String loadFile = config.getOption("load-file", null);
      if (loadFile != null) {
        final GameState loadedState = GameLoader.loadGame(engine, loadFile);
        engine.getState().loadState(loadedState);
        engine.initializeBlitz();
      }
      final CliApp cli = new CliApp(engine);
      if (System.getProperty("quoridor.test") == null) {
        cli.run();
      }
    } catch (final Exception e) {
      if (Logger.getInstance().isErrorEnabled()) {
        Logger.getInstance().error("CLI Error: {0}", e.getMessage());
      }
    }
  }
}
