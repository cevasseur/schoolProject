package fr.bordeaux.cli.command;

import fr.bordeaux.core.GameEngine;
import java.io.IOException;

/**
 * Represents a CLI command that performs an action on the game engine. and can provide help
 * information about its usage.
 */
public interface Command {
  /**
   * Executes the command, potentially modifying the game engine state.
   *
   * @param engine the current game engine
   * @return the updated game engine after executing the command
   * @throws IOException Possible if mcts with NN selection used
   */
  GameEngine execute(GameEngine engine) throws IOException;

  /** Prints command help information, including usage and examples. and examples if applicable. */
  void helpText();
}
