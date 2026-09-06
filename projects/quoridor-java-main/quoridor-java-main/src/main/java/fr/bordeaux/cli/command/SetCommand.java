package fr.bordeaux.cli.command;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.GameEngine;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;

/** CLI command that modifies a game configuration parameter. */
public class SetCommand implements Command {

  /** Command entered by the user */
  private String[] tokens;

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /** Creates a SetCommand. Currently not implemented. */
  public SetCommand() {
    this.tokens = null;
  }

  /**
   * Creates a SetCommand. Currently not implemented.
   *
   * @param setting name of the command
   */
  public SetCommand(final String setting) {
    this(new String[] {"set", setting});
  }

  /**
   * Creates a SetCommand with the provided CLI tokens.
   *
   * @param tokens the command tokens (e.g., ["set", "debug=true"])
   */
  public SetCommand(final String[] tokens) {
    this.tokens = tokens;
  }

  /**
   * Executes set command. Updates a configuration option in the engine.
   *
   * @param engine the current game engine
   * @return the unchanged game engine
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    if (tokens == null || tokens.length < 2) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0}: set PARAM=VALUE", I18n.get("usage"));
      }
      return engine;
    }

    final String argument = tokens[1];
    if (!argument.contains("=")) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("setUsageExample", I18n.get("usage")));
      }
      return engine;
    }

    // Splitting the argument into key and value
    final String[] parts = argument.split("=", 2);
    final String key = parts[0].trim();
    final String value = parts[1].trim();

    if (key.isEmpty() || value.isEmpty()) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("setEmpty"));
      }
      return engine;
    }

    try {
      final ConfigManager config = ConfigManager.getInstance();

      // Update the option in memory
      config.setOption(key, value);

      // Update the engine in-memory state
      switch (key) {
        case "blitz":
          engine.setBlitz(Boolean.parseBoolean(value));
          break;
        case "verbose":
          engine.setVerbose(Boolean.parseBoolean(value));
          LOGGER.refreshConfig();
          break;
        case "contest":
          engine.setContest(Boolean.parseBoolean(value));
          break;
        case "debug":
          engine.setDebug(Boolean.parseBoolean(value));
          LOGGER.refreshConfig();
          break;
        case "walls":
        case "wall":
        case "nb-walls":
          engine.setInitialWalls(Integer.parseInt(value));
          break;
        default:
          break;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(I18n.get("setUpdate", key, I18n.get("setTo"), value));
      }
    } catch (final Exception e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(I18n.get("setFailed", key), e);
      }
    }

    return engine;
  }

  /** Displays help information for the set command. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("setHeader"), I18n.get("cliTitle"));
      LOGGER.info("{0}:", I18n.get("usage"));
      LOGGER.info("  {0}\n", I18n.get("setUsage"));
      LOGGER.info("{0}:", I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("setDesc"));
    }
    LOGGER.verbose(I18n.get("arguments"));
    LOGGER.verbose("  PARAM   : {0}", I18n.get("setParam"));
    LOGGER.verbose("  VALUE   : {0}", I18n.get("setValue"));
    LOGGER.verbose(I18n.get("loadEx"));
    LOGGER.verbose("  {0}\n", I18n.get("setExampleCmd"));
  }
}
