package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/** CLI command that displays global or detailed help information. help for a specific command. */
public class HelpCommand implements Command {

  /** Command entered by the user */
  private String[] tokens;

  /** Logger to display informations */
  private static final Logger LOGGER = Logger.getInstance();

  /** Constant for length check when exactly one token is present */
  private static final int UNIQUE_MATCH = 1;

  /** Reference from the CliApp command registry */
  private final Map<String, Function<String[], Command>> registry;

  /** Creates a HelpCommand without specific tokens. */
  public HelpCommand() {
    this(null, null);
  }

  /**
   * Creates a HelpCommand with the given command tokens and the command registry.
   *
   * @param userTokens tokens entered by the user in the CLI
   * @param registryCli mapping of command names to factory functions
   */
  public HelpCommand(
      final String[] userTokens, final Map<String, Function<String[], Command>> registryCli) {
    this.tokens = userTokens;
    this.registry = registryCli;
  }

  /**
   * Executes help command. Displays general or specific help message. depending on the provided
   * tokens.
   *
   * @param engine the current game engine
   * @return the unchanged game engine
   */
  @Override
  public GameEngine execute(final GameEngine engine) {
    if (tokens == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(I18n.get("noCommand"));
      }
    } else if (tokens.length == UNIQUE_MATCH) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(I18n.get("allCommandHelp"));
      }
      helpCli();
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(I18n.get("helpFor"), tokens[1]);
      }
      helpFetch();
    }
    return engine;
  }

  /** Displays a short help description for this command. */
  @Override
  public void helpText() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("=== {0} {1} ===\n", I18n.get("helpHeader"), I18n.get("cliTitle"));
      LOGGER.info(I18n.get("usage"));
      LOGGER.info("  {0}\n", I18n.get("helpUsage"));
      LOGGER.info(I18n.get("description"));
      LOGGER.info("  {0}", I18n.get("helpDesc"));
    }
  }

  /** Displays the global help message listing all available CLI commands. */
  public void helpCli() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{0}\n", I18n.get("cliHelp"));
      LOGGER.info("{0} \n", I18n.get("commands"));
      LOGGER.info("new [ARGS] :          {0}", I18n.get("helpNew"));
      LOGGER.info("help [CMD] :          {0}", I18n.get("helpCmd"));
      LOGGER.info("                      {0}", I18n.get("helpCmd2"));
      LOGGER.info("help move :           {0}", I18n.get("helpMove"));
      LOGGER.info("quit :                {0}", I18n.get("helpQuit"));
      LOGGER.info("load [filename] :     {0}", I18n.get("helpLoad"));
      LOGGER.info("save [filename] :     {0}", I18n.get("helpSave"));
      LOGGER.info("pause :               {0}", I18n.get("helpPause"));
      LOGGER.info("hint :                {0}", I18n.get("helpHint"));
      LOGGER.info("undo [N] :            {0}", I18n.get("helpUndo"));
      LOGGER.info("redo [N] :            {0}", I18n.get("helpRedo"));
      LOGGER.info("show board :          {0}", I18n.get("helpBoard"));
      LOGGER.info("show history :        {0}", I18n.get("helpHistory"));
      LOGGER.info("show time :           {0}", I18n.get("helpTime"));
      LOGGER.info("show configurations : {0}", I18n.get("helpConfigs"));
      LOGGER.info("set PARAM=VALUE :     {0}", I18n.get("helpSet"));
    }
  }

  /**
   * Displays detailed help for a specific command based on user input. determined from the provided
   * tokens.
   */
  public void helpFetch() {
    if (this.tokens.length <= UNIQUE_MATCH) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("{0} {1}", I18n.get("usage"), I18n.get("helpUsage"));
      }
    } else {
      final String commandName = tokens[1];
      if (registry.containsKey(commandName)) {
        // Extract sub-tokens for the command (e.g., ["show", "board"] from ["help", "show",
        // "board"])
        final String[] subTokens = Arrays.copyOfRange(tokens, 1, tokens.length);
        // Temporary instance of the command to use helpText()
        final Command cmd = registry.get(commandName).apply(subTokens);
        cmd.helpText();
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("{0}{1}", I18n.get("unknownCmd"), commandName);
        }
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("{0} {1}", I18n.get("usage"), I18n.get("helpUsage"));
        }
      }
    }
  }
}
