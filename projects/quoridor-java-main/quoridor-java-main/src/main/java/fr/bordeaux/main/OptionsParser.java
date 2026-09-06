package fr.bordeaux.main;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Interprets CLI arguments (GUI, CLI, Network) and game settings. */
public final class OptionsParser {

  private static final Logger LOGGER = Logger.getInstance();

  private OptionsParser() {}

  /**
   * Processes CLI arguments into configuration using Apache Commons CLI.
   *
   * @param args Command-line arguments.
   * @param config Config manager instance.
   */
  public static void parse(final String[] args, final ConfigManager config) {
    Options options = new Options();

    // System Options
    options.addOption(Option.builder("h").longOpt("help").desc("Show help").build());
    options.addOption(Option.builder("V").longOpt("version").desc("Show version").build());
    options.addOption(
        Option.builder("c").longOpt("contest").hasArg().desc("Contest mode file").build());
    options.addOption(Option.builder("v").longOpt("verbose").desc("Verbose mode").build());
    options.addOption(Option.builder("d").longOpt("debug").desc("Debug mode").build());
    options.addOption(Option.builder("D").longOpt("daemon").desc("Daemon mode").build());
    options.addOption(Option.builder("g").longOpt("gui").desc("GUI mode").build());
    options.addOption(Option.builder().longOpt("cli").desc("CLI mode").build());
    options.addOption(Option.builder("n").longOpt("net").desc("Network mode").build());

    // Game Options
    options.addOption(Option.builder("b").longOpt("blitz").desc("Blitz mode").build());
    options.addOption(
        Option.builder("t").longOpt("time").hasArg().desc("Limité time for blitz").build());
    options.addOption(
        Option.builder("s").hasArg().desc("Size of the board or Server port").build());
    options.addOption(Option.builder().longOpt("size").hasArg().desc("Size of the board").build());
    options.addOption(Option.builder().longOpt("server").hasArg().desc("Server mode").build());
    options.addOption(
        Option.builder("p").longOpt("players").hasArg().desc("Number of players").build());
    options.addOption(
        Option.builder("w").longOpt("walls").hasArg().desc("Number of walls").build());

    // AI Options
    options.addOption(
        Option.builder("a").longOpt("ai").hasArg().optionalArg(true).desc("Enable AI").build());
    options.addOption(Option.builder().longOpt("ai-mode").hasArg().desc("AI algorithm").build());
    options.addOption(
        Option.builder().longOpt("ai-minimax-depth").hasArg().desc("AI depth").build());
    options.addOption(Option.builder().longOpt("ai-time").hasArg().desc("AI time").build());
    options.addOption(
        Option.builder().longOpt("ai-minimax-scoring").hasArg().desc("AI scoring").build());
    options.addOption(
        Option.builder().longOpt("ai-mcts-selection").hasArg().desc("AI MCTS selection").build());

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine commandLine = parser.parse(options, args);
      handleSystemOptions(commandLine, config);
      GameOptionsHandler.handle(commandLine, config);
      AiOptionsHandler.handle(commandLine, config);
      AiOptionsHandler.validateAiPlayerCount(config);

      // Parse unresolved arguments as file to load
      String[] leftOverArgs = commandLine.getArgs();
      if (leftOverArgs.length > 0) {
        config.setOption("load-file", leftOverArgs[leftOverArgs.length - 1]);
      }
    } catch (ParseException exception) {
      if (exception instanceof org.apache.commons.cli.MissingArgumentException
          || exception instanceof org.apache.commons.cli.UnrecognizedOptionException) {
        throw new IllegalArgumentException(
            I18n.get("invalidOpt") + " or argument missing: " + exception.getMessage(), exception);
      }
      throw new IllegalArgumentException(exception.getMessage(), exception);
    }
  }

  private static void handleSystemOptions(
      final CommandLine commandLine, final ConfigManager config) {
    if (commandLine.hasOption("h")) {
      printHelp();
      if (System.getProperty("quoridor.test") == null) {
        System.exit(0);
      }
    }
    if (commandLine.hasOption("V")) {
      Logger.getInstance().raw("Quoridor version 1.0.0");
      if (System.getProperty("quoridor.test") == null) {
        System.exit(0);
      }
    }
    if (commandLine.hasOption("c")) {
      String file = commandLine.getOptionValue("c");
      if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException(
            "--contest " + I18n.get("require", I18n.get("filePath")));
      }
      config.enableContest(file);
    }
    if (commandLine.hasOption("v")) {
      config.setOption("verbose", "true");
    }
    if (commandLine.hasOption("d")) {
      config.setOption("debug", "true");
    }
    if (commandLine.hasOption("D")) {
      config.setOption("daemon", "true");
    }
    // g, cli, n are parsed here but affect AppModeResolver (no action needed here)
  }

  /**
   * @brief Logs an informational message if info logging is enabled.
   * @param message Message to log.
   */
  private static void logInfo(final String message) {
    Logger.getInstance().raw(message);
  }

  /** Prints usage and help information to the console. */
  public static void printHelp() {
    logInfo("Usage: quoridor [OPTIONS]");
    logInfo("");
    logInfo(I18n.get("geneOpt"));
    logInfo("  -h, --help        " + I18n.get("showThis"));
    logInfo("  -V, --version     " + I18n.get("displayVersion"));
    logInfo("  -v, --verbose     " + I18n.get("verboseMode"));
    logInfo("  -d, --debug       " + I18n.get("debugMode"));
    logInfo("  -D, --daemon      " + I18n.get("headless"));
    logInfo("");
    logInfo("AI options:");
    logInfo("  -a, --ai [MODE]        Enable AI player(s):");
    logInfo("                         blanc | 1   -> AI for player 1");
    logInfo("                         noir  | 2   -> AI for player 2 (default)");
    logInfo("                         a            -> AI for both players");
    logInfo("                         (no value)   -> AI for player 2");
    logInfo("");
    logInfo("  --ai-mode MODE         AI algorithm: minimax, mcts, iterative");
    logInfo("  --ai-minimax-depth N   Depth for minimax algorithm (>0)");
    logInfo("  --ai-time N            Time limit for AI in seconds (>0)");
    logInfo("  --ai-minimax-scoring S Scoring method: simple, intermediate, advanced");
    logInfo("  --ai-mcts-selection S  MCTS selection: UCT, ML");
    logInfo("");

    logInfo("Modes:");
    logInfo("  --contest, -c <file> " + I18n.get("contest"));
    logInfo("  -g, --gui         " + I18n.get("guiMode"));
    logInfo("  -n, --net         " + I18n.get("networkMode"));
    logInfo("  -p, --players N   " + I18n.get("numberPlayers"));
    logInfo("");
    logInfo(I18n.get("blitzOpt"));
    logInfo("  -b, --blitz       " + I18n.get("enableBlitz"));
    logInfo("  -t, --time N      " + I18n.get("timeout"));
  }
}
