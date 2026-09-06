package fr.bordeaux.main;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.i18n.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.cli.CommandLine;

/** Handles AI-specific command line arguments and validation. */
public final class AiOptionsHandler {

  /** Configuration key enabling AI for player one. */
  private static final String AI_1 = "ai1";

  /** Configuration key enabling AI for player two. */
  private static final String AI_2 = "ai2";

  /** String value used to enable boolean configuration flags. */
  private static final String TRUE_VALUE = "true";

  private AiOptionsHandler() {}

  /**
   * Processes AI-related arguments using Commons CLI.
   *
   * @param commandLine The parsed command line.
   * @param config The configuration manager.
   */
  public static void handle(final CommandLine commandLine, final ConfigManager config) {
    if (commandLine.hasOption("a")) {
      String aiOption = commandLine.getOptionValue("a");
      if (aiOption == null || aiOption.isBlank()) {
        config.setOption(AI_2, TRUE_VALUE);
      } else {
        aiOption = aiOption.toLowerCase(Locale.ROOT);
        if ("a".equals(aiOption)) {
          config.setOption(AI_1, TRUE_VALUE);
          config.setOption(AI_2, TRUE_VALUE);
        } else if ("blanc".equals(aiOption) || "1".equals(aiOption)) {
          config.setOption(AI_1, TRUE_VALUE);
        } else if ("noir".equals(aiOption) || "2".equals(aiOption)) {
          config.setOption(AI_2, TRUE_VALUE);
        } else {
          throw new IllegalArgumentException(I18n.get("aiValueErr"));
        }
      }
    }

    if (commandLine.hasOption("ai-mode")) {
      final String value = commandLine.getOptionValue("ai-mode");
      checkAiString(value, "mode", new ArrayList<>(List.of("minimax", "mcts", "iterative")));
      applyAiOption(value, "mode", config);
    }

    if (commandLine.hasOption("ai-minimax-depth")) {
      final String value = commandLine.getOptionValue("ai-minimax-depth");
      checkAiInt(value, "minimax-depth");
      applyAiOption(value, "depth", config);
    }

    if (commandLine.hasOption("ai-time")) {
      final String value = commandLine.getOptionValue("ai-time");
      checkAiInt(value, "time");
      applyAiOption(value, "time", config);
    }

    if (commandLine.hasOption("ai-minimax-scoring")) {
      final String value = commandLine.getOptionValue("ai-minimax-scoring");
      checkAiString(
          value, "scoring", new ArrayList<>(List.of("simple", "intermediate", "advanced")));
      applyAiOption(value, "scoring", config);
    }

    if (commandLine.hasOption("ai-mcts-selection")) {
      final String value = commandLine.getOptionValue("ai-mcts-selection");
      applyMctsOption(value, config);
    }
  }

  /**
   * Validates that AI is only enabled for 2-player games.
   *
   * @param config The configuration manager instance.
   * @throws IllegalArgumentException If AI is active with more than 2 players.
   */
  public static void validateAiPlayerCount(final ConfigManager config) {
    final int nbPlayers = Integer.parseInt(config.getOption("nb-players", "2"));
    final boolean hasAi = config.getBoolean("ai1", false) || config.getBoolean("ai2", false);

    if (hasAi && nbPlayers > 2) {
      throw new IllegalArgumentException(I18n.get("notSupported", String.valueOf(nbPlayers)));
    }
  }

  /**
   * Validates that a comma-separated string contains only positive integers.
   *
   * @param value The raw string to validate (e.g., "3,5").
   * @param parameter The parameter name for the error message.
   * @throws IllegalArgumentException If value is null or not a positive integer.
   */
  private static void checkAiInt(final String value, final String parameter) {
    if (value == null) {
      throw new IllegalArgumentException("AI " + parameter + " " + I18n.get("requireInt"));
    }
    final String[] values = value.split(",");
    for (final String val : values) {
      if (Integer.parseInt(val) <= 0) {
        throw new IllegalArgumentException("--ai-" + parameter + " " + I18n.get("higher"));
      }
    }
  }

  /**
   * Validates that a comma-separated string contains only allowed keywords.
   *
   * @param value The raw string to validate (e.g., "minimax,mcts,iterative").
   * @param parameter The parameter name for the error message.
   * @param accpetedMode List of permitted string values.
   * @throws IllegalArgumentException If value is null or contains an invalid mode.
   */
  private static void checkAiString(
      final String value, final String parameter, final List<String> accpetedMode) {
    if (value == null) {
      throw new IllegalArgumentException(
          I18n.get("aiArgRequired", parameter, accpetedMode.toString()));
    }
    final String[] values = value.split(",");
    for (final String val : values) {
      if (!accpetedMode.contains(val.toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException(
            I18n.get("aiValidOptions", parameter, accpetedMode.toString()));
      }
    }
  }

  /** Sets AI options only for players currently flagged as AI. */
  private static void applyAiOption(
      final String value, final String suffix, final ConfigManager config) {
    final String[] values = value.split(",");

    // Apply to Player 1 if AI is enabled
    if (config.getBoolean(AI_1, false)) {
      config.setOption("ai1." + suffix, values[0].trim());
    }

    // Apply to Player 2 if AI is enabled
    if (config.getBoolean(AI_2, false)) {
      // Use the second value if provided, otherwise fallback to the first
      final String valToSet = (values.length > 1) ? values[1] : values[0];
      config.setOption("ai2." + suffix, valToSet.trim());
    }
  }

  /** Specialized helper for MCTS selection with active AI check. */
  private static void applyMctsOption(final String value, final ConfigManager config) {
    final String[] values = value.split(",");
    for (final String selectionItem : values) {
      final String trimmed = selectionItem.trim();
      if (!"UCT".equals(trimmed) && !"ML".equals(trimmed)) {
        throw new IllegalArgumentException(I18n.get("invalidOptionMcts", trimmed));
      }
    }

    if (config.getBoolean(AI_1, false)) {
      config.setOption("ai1.selection", values[0].trim());
    }
    if (config.getBoolean(AI_2, false)) {
      final String valToSet = (values.length > 1) ? values[1] : values[0];
      config.setOption("ai2.selection", valToSet.trim());
    }
  }
}
