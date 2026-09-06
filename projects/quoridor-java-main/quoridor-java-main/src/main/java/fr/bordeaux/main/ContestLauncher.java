package fr.bordeaux.main;

import fr.bordeaux.ai.mcts.MctsPlayer;
import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import fr.bordeaux.persistence.GameLoadException;
import fr.bordeaux.persistence.GameLoader;
import fr.bordeaux.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Launcher for contest mode. Reads a save file, computes best move via MCTS, and prints it in
 * algebraic notation.
 */
public final class ContestLauncher implements AppLauncher {

  ContestLauncher() {
    // Hidden constructor
  }

  @Override
  public void launch(final String[] args, final ConfigManager config) {
    final String filePath = config.getContestFile();

    if (filePath == null || filePath.isBlank()) {
      Logger.getInstance().error("[contest] No position file specified. Use: --contest <file>");
    }

    assert filePath != null;
    if (!Files.exists(Path.of(filePath))) {
      Logger.getInstance().error("[contest] File not found: {0}", filePath);
    }

    try {
      final GameEngine engine = createDummyEngine(config);
      final GameState state = GameLoader.loadGame(engine, filePath);
      final Move best = computeBestMove(state, config);
      Logger.getInstance().raw(moveToString(best));
    } catch (final IOException exception) {
      if (Logger.getInstance().isErrorEnabled()) {
        Logger.getInstance().error("[contest] I/O error reading file: {0}", exception.getMessage());
      }
      System.exit(1);
    } catch (final GameLoadException exception) {
      if (Logger.getInstance().isErrorEnabled()) {
        Logger.getInstance()
            .error(
                "[contest] Invalid save file [{0}] at line {1}: {2}",
                exception.getErrorType(), exception.getLineNumber(), exception.getMessage());
      }
    } catch (final Exception exception) {
      if (Logger.getInstance().isErrorEnabled()) {
        Logger.getInstance().error("[contest] Unexpected error: {0}", exception.getMessage());
      }
    }
  }

  /**
   * @return placeholder engine for GameLoader.
   */
  private static GameEngine createDummyEngine(final ConfigManager config) {
    final Board dummyBoard = new Board(9);
    final List<Player> dummyPlayers =
        List.of(
            new HumanPlayer("P1", new Position(0, 4), Color.WHITE),
            new HumanPlayer("P2", new Position(8, 4), Color.BLACK));
    final GameState dummyState = new GameState(dummyBoard, dummyPlayers, 0);

    return new GameEngine(
        dummyState, false, false, true, false, config.getInt("initial-walls", 10));
  }

  private static Move computeBestMove(final GameState state, final ConfigManager config) {
    final long timeBudgetMs = config.getInt("ai1.time", 3) * 1000L;

    try {
      return MctsPlayer.hint(state.getCurrentPlayer(), state, timeBudgetMs, true);
    } catch (final Exception exception) {
      if (Logger.getInstance().isErrorEnabled()) {
        Logger.getInstance()
            .error(
                "[contest] MCTS failed ({0}), falling back to first legal move.",
                exception.getMessage());
      }
      final List<Move> legal = state.generateLegalMoves();
      if (legal.isEmpty()) {
        throw new IllegalStateException("No legal moves available.", exception);
      }
      return legal.get(0);
    }
  }

  private static String moveToString(final Move move) {
    if (move.getType() == Move.Type.PAWN) {
      return posToString(move.getFrom()) + "-" + posToString(move.getTo());
    }
    final char orientation = move.getOrientation() == Orientation.HORIZONTAL ? 'h' : 'v';
    return posToString(move.getTo()) + orientation;
  }

  private static String posToString(final Position position) {
    final char letter = (char) ('a' + position.getX());
    return String.valueOf(letter) + position.getY();
  }
}
