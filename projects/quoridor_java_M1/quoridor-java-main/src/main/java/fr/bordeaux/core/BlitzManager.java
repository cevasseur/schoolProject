package fr.bordeaux.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Manager for the Blitz mode in Quoridor. */
public class BlitzManager {

  /** Threshold below which the manager switches to the fast tick interval. */
  private static final long THRESHOLD_MS = 60_000;

  /** Tick interval used while plenty of time remains. */
  private static final int TICK_SLOW_MS = 1000;

  /** Tick interval used near the end of the countdown. */
  private static final int TICK_FAST_MS = 10;

  /** Game engine whose current player timer is managed by this component. */
  private final GameEngine gameEngine;

  /** Scheduler used to run periodic blitz countdown tasks. */
  private ScheduledExecutorService scheduler;

  /** Indicates whether the blitz timer is currently active. */
  private boolean isRunning;

  /** Lock protecting scheduler lifecycle and timer state. */
  private final Object lock = new Object();

  /**
   * Constructs a BlitzManager associated with a game engine.
   *
   * @param gameEngine The game engine to monitor.
   */
  public BlitzManager(final GameEngine gameEngine) {
    this.gameEngine = gameEngine;
  }

  /** Starts or resumes the countdown. */
  public void start() {
    synchronized (lock) {
      if (isRunning) {
        return;
      }
      isRunning = true;
      final long remaining = gameEngine.getState().getCurrentPlayer().getRemainingTime();
      final int tick = remaining > THRESHOLD_MS ? TICK_SLOW_MS : TICK_FAST_MS;
      startWithTick(tick);
    }
  }

  /**
   * Creates a new scheduler and starts it with the given tick interval.
   *
   * @param tickMs the interval in milliseconds between each timer tick
   */
  private void startWithTick(final int tickMs) {
    synchronized (lock) {
      if (!isRunning) return;
      if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdownNow();
      }
      scheduler = Executors.newSingleThreadScheduledExecutor();
      scheduler.scheduleAtFixedRate(() -> task(tickMs), tickMs, tickMs, TimeUnit.MILLISECONDS);
    }
  }

  /** Temporarily stops the countdown (Pause). decrementation. */
  public void pause() {
    synchronized (lock) {
      if (!isRunning) {
        return;
      }
      isRunning = false;
      if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdownNow();
        scheduler = null;
      }
    }
  }

  /**
   * Internal task executed at each timer tick. decrements their time, and checks for loss
   * conditions.
   */
  private void task(final int tickMs) {
    synchronized (lock) {
      if (!isRunning) return;
      try {
        final GameState state = gameEngine.getState();
        if (state.isPaused() || state.isGameOver()) return;

        state.decrementCurrentPlayerTime(tickMs);
        if (state.isGameOver()) {
          pause();
          return;
        }

        if (tickMs == TICK_SLOW_MS && state.getCurrentPlayer().getRemainingTime() <= THRESHOLD_MS) {
          startWithTick(TICK_FAST_MS);
        }

      } catch (Exception e) {
        if (CoreServiceProvider.getLogger().isErrorEnabled()) {
          CoreServiceProvider.getLogger()
              .error(CoreServiceProvider.getTranslator().translate("blitzTaskFailed"), e);
        }
      }
    }
  }

  /**
   * Indicates whether the time manager is currently active.
   *
   * @return true if the timer is running, false otherwise.
   */
  public boolean isRunning() {
    return isRunning;
  }
}
