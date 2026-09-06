package fr.bordeaux.main;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import org.apache.commons.cli.CommandLine;

/** Handles game rules, server settings, and time constraints from CLI. */
public final class GameOptionsHandler {

  private static final Logger LOGGER = Logger.getInstance();

  private GameOptionsHandler() {}

  /**
   * Processes game-related arguments using Commons CLI.
   *
   * @param commandLine The parsed command line.
   * @param config The configuration manager.
   */
  public static void handle(final CommandLine commandLine, final ConfigManager config) {
    if (commandLine.hasOption("b")) {
      config.setOption("blitz", "true");
    }

    if (commandLine.hasOption("t")) {
      final boolean blitz = config.getBoolean("blitz", false);
      if (!blitz) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("--time {0}", I18n.get("needsBlitz"));
        }
      } else {
        final String value = commandLine.getOptionValue("t");
        try {
          if (Integer.parseInt(value) <= 0) {
            throw new IllegalArgumentException("--time " + I18n.get("higher"));
          }
          config.setOption("timeout", value);
        } catch (NumberFormatException exception) {
          throw new IllegalArgumentException("--time " + I18n.get("requireInt"));
        }
      }
    }

    if (commandLine.hasOption("s")) {
      final String value = commandLine.getOptionValue("s");
      try {
        final int argument = Integer.parseInt(value);
        if (argument < 16 && argument > 2) {
          if (argument % 2 == 0) {
            throw new IllegalArgumentException("--size " + I18n.get("sizeOdd"));
          }
          config.setOption("board-size", value);
        } else if (argument >= 20 && argument <= 99999) {
          config.setOption("server", "true");
          config.setOption("server-port", value);
        }
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException("--server " + I18n.get("requiresIntPort"));
      }
    }

    if (commandLine.hasOption("size")) {
      config.setOption("board-size", commandLine.getOptionValue("size"));
    }

    if (commandLine.hasOption("server")) {
      config.setOption("server", "true");
      config.setOption("server-port", commandLine.getOptionValue("server"));
    }

    if (commandLine.hasOption("p")) {
      config.setOption("nb-players", commandLine.getOptionValue("p"));
    }

    if (commandLine.hasOption("w")) {
      final String value = commandLine.getOptionValue("w");
      try {
        if (Integer.parseInt(value) < 0) {
          throw new IllegalArgumentException("--walls " + I18n.get("higher"));
        } else {
          config.setOption("nb-walls", value);
        }
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException("--walls " + I18n.get("requireInt"));
      }
    }
  }
}
