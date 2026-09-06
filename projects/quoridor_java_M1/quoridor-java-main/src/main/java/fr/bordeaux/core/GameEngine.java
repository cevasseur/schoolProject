package fr.bordeaux.core;

/** Core engine for Quoridor game logic, player turns, and AI hints. */
public class GameEngine {

  /** The current state of the game. */
  private GameState state;

  /** The manager for blitz mode (timed games). */
  private BlitzManager blitzManager;

  /** Whether Blitz mode is enabled. */
  private boolean blitz;

  /** Whether verbose logging is enabled. */
  private boolean verbose;

  /** Whether contest mode is enabled. */
  private boolean contest;

  /** Whether debug mode is enabled. */
  private boolean debug;

  /** The initial number of walls for each player. */
  private int initialWalls;

  /**
   * Constructs GameEngine.
   *
   * @param state initial game state
   * @param blitz blitz mode flag
   * @param verbose verbose flag
   * @param contest contest mode flag
   * @param debug debug flag
   * @param initialWalls initial wall count
   */
  public GameEngine(
      final GameState state,
      final boolean blitz,
      final boolean verbose,
      final boolean contest,
      final boolean debug,
      final int initialWalls) {
    this.state = state;
    this.blitz = blitz;
    this.verbose = verbose;
    this.contest = contest;
    this.debug = debug;
    this.initialWalls = initialWalls;
    initializeBlitz();
  }

  /** Helper method to initialize the BlitzManager based on the current state. */
  public final void initializeBlitz() {
    if (this.blitz) {
      this.blitzManager = new BlitzManager(this);
      this.blitzManager.start();
    }
  }

  /**
   * Executes a game move.
   *
   * @param move move to execute
   * @return true if success
   */
  public boolean play(final Move move) {
    final GameState currentState = this.state;
    if (currentState.isGameOver() || currentState.isPaused()) {
      if (CoreServiceProvider.getLogger().isInfoEnabled()) {
        CoreServiceProvider.getLogger()
            .info(CoreServiceProvider.getTranslator().translate("overPause"));
      }
      return false;
    }

    if (!RuleChecker.isMoveLegal(currentState, move)) {
      if (CoreServiceProvider.getLogger().isDebugEnabled()) {
        CoreServiceProvider.getLogger()
            .debug(
                CoreServiceProvider.getTranslator()
                    .translate(
                        "engineIllegalMoveAttempt",
                        currentState.getCurrentPlayer().getName(),
                        move.toString()));
      }
      return false;
    }

    final Player currentPlayer = currentState.getCurrentPlayer();
    CoreServiceProvider.getLogger()
        .verbose("Player {0} playing move: {1}", currentPlayer.getName(), move);
    currentState.applyMove(move);

    if (currentState.hasReachedGoal(currentPlayer)) {
      currentState.setGameOver(true);
      if (blitzManager != null) {
        blitzManager.pause();
      }
      if (CoreServiceProvider.getLogger().isInfoEnabled()) {
        CoreServiceProvider.getLogger()
            .info(
                "{0} {1}",
                currentPlayer.getName(), CoreServiceProvider.getTranslator().translate("won"));
      }
    }
    return true;
  }

  /**
   * Resets the current game to a specified initial state.
   *
   * @param boardSize the board size
   * @param players the list of players
   * @param blitzTimeoutMs the blitz timeout in milliseconds, or null if not blitz
   */
  public void newGame(
      final int boardSize, final java.util.List<Player> players, final Long blitzTimeoutMs) {
    if (blitzManager != null) {
      blitzManager.pause();
    }
    state.reset(boardSize, players, blitzTimeoutMs);
    if (CoreServiceProvider.getLogger().isInfoEnabled()) {
      CoreServiceProvider.getLogger()
          .info(CoreServiceProvider.getTranslator().translate("engineNewGame", players.size()));
    }
    initializeBlitz();
  }

  /**
   * Saves the current game state to a file.
   *
   * <p>/** Reverts the last move made in the game.
   */
  public void undo() {
    state.undo();
  }

  /** Re-applies the last move that was undone. */
  public void redo() {
    state.redo();
  }

  /** Toggles the pause state of the game and synchronization with the Blitz timer. */
  public void pause() {
    if (state.getCurrentPlayer().isAI()) {
      if (CoreServiceProvider.getLogger().isInfoEnabled()) {
        CoreServiceProvider.getLogger()
            .info(CoreServiceProvider.getTranslator().translate("aiCannotPause"));
      }
      return;
    }
    final boolean newPauseState = !state.isPaused();
    state.setPaused(newPauseState);

    if (blitzManager != null) {
      if (newPauseState) {
        blitzManager.pause();
        if (CoreServiceProvider.getLogger().isInfoEnabled()) {
          CoreServiceProvider.getLogger()
              .info(CoreServiceProvider.getTranslator().translate("enginePause"));
        }
      } else {
        blitzManager.start();
        if (CoreServiceProvider.getLogger().isInfoEnabled()) {
          CoreServiceProvider.getLogger()
              .info(CoreServiceProvider.getTranslator().translate("engineResume"));
        }
      }
    }
  }

  /**
   * Returns the current state of the game.
   *
   * @return The {@link GameState} managed by this engine.
   */
  public GameState getState() {
    return state;
  }

  /**
   * Returns the current blitz manager of the game.
   *
   * @return The {@link BlitzManager} managed by this engine.
   */
  public BlitzManager getBlitzManager() {
    return blitzManager;
  }

  /**
   * Returns whether blitz mode is enabled.
   *
   * @return {@code true} if blitz is enabled, {@code false} otherwise.
   */
  public boolean isBlitz() {
    return blitz;
  }

  /**
   * Sets whether blitz mode is enabled.
   *
   * @param blitz {@code true} to enable blitz, {@code false} to disable.
   */
  public void setBlitz(final boolean blitz) {
    this.blitz = blitz;
  }

  /**
   * Returns whether verbose mode is enabled.
   *
   * @return {@code true} if verbose is enabled, {@code false} otherwise.
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Sets whether verbose mode is enabled.
   *
   * @param verbose {@code true} to enable verbose, {@code false} to disable.
   */
  public void setVerbose(final boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * Returns whether contest mode is enabled.
   *
   * @return {@code true} if contest is enabled, {@code false} otherwise.
   */
  public boolean isContest() {
    return contest;
  }

  /**
   * Sets whether contest mode is enabled.
   *
   * @param contest {@code true} to enable contest, {@code false} to disable.
   */
  public void setContest(final boolean contest) {
    this.contest = contest;
  }

  /**
   * Returns whether debug mode is enabled.
   *
   * @return {@code true} if debug is enabled, {@code false} otherwise.
   */
  public boolean isDebug() {
    return debug;
  }

  /**
   * Sets whether debug mode is enabled.
   *
   * @param debug {@code true} to enable debug, {@code false} to disable.
   */
  public void setDebug(final boolean debug) {
    this.debug = debug;
  }

  /**
   * Returns the initial number of walls per player.
   *
   * @return The initial wall count.
   */
  public int getInitialWalls() {
    return initialWalls;
  }

  /**
   * Sets the initial number of walls per player.
   *
   * @param initialWalls The new initial wall count.
   */
  public void setInitialWalls(final int initialWalls) {
    this.initialWalls = initialWalls;
  }
}
