package fr.bordeaux.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlitzManagerTest {

  private GameEngine engine;
  private GameState state;
  private BlitzManager blitz;

  @BeforeEach
  public void setUp() {
    Board board = new Board(9);
    List<Player> players = new ArrayList<>();
    players.add(new HumanPlayer("Human 1", new Position(8, 4), Color.WHITE));
    players.add(new HumanPlayer("Human 2", new Position(0, 4), Color.BLACK));

    state = new GameState(board, players);
    engine = new GameEngine(state, true, false, false, false, 10);
    blitz = engine.getBlitzManager();
  }

  @AfterEach
  public void tearDown() {
    if (blitz != null) {
      blitz.pause();
    }
  }

  @Test
  public void testDecreaseTime() throws InterruptedException {
    state.setLimitTimePlayer(65000); // 65 seconds
    Player player = state.getCurrentPlayer();
    long initialTime = player.getRemainingTime();

    blitz.start();
    assertTrue(blitz.isRunning(), "Le BlitzManager devrait être actif");

    Thread.sleep(1500);

    long afterSleep = player.getRemainingTime();

    assertTrue(afterSleep < initialTime, "Le temps devrait avoir diminué");
    assertEquals(64000, afterSleep, "Le temps restant devrait être exactement de 64000 ms");
  }

  @Test
  public void testLooseByTime() throws InterruptedException {
    state.setLimitTimePlayer(1000); // 1 second
    Player player = state.getCurrentPlayer();

    blitz.start();
    assertTrue(blitz.isRunning());

    Thread.sleep(2500);

    assertEquals(0, player.getRemainingTime(), "The remaining time should be 0");
    assertTrue(state.isGameOver(), "The game should be over (Game Over)");
    assertFalse(blitz.isRunning(), "BlitzManager should have paused automatically");
  }

  @Test
  public void testPauseTimer() throws InterruptedException {
    state.setLimitTimePlayer(5000);
    Player player = state.getCurrentPlayer();

    blitz.start();
    blitz.pause();
    assertFalse(blitz.isRunning(), "BlitzManager should no longer be active");

    Thread.sleep(1500);

    assertEquals(
        5000, player.getRemainingTime(), "The time shouldn't have decreased during the break");
  }

  /** Tests that the tick rate switches from slow (1s) to fast (10ms) below 60s. */
  @Test
  public void testSwitchToFastTick() throws InterruptedException {
    state.setLimitTimePlayer(60200); // 60.2 seconds
    Player player = state.getCurrentPlayer();

    blitz.start();

    Thread.sleep(1500);

    long timeAfterSwitch = player.getRemainingTime();
    assertTrue(timeAfterSwitch < 60000, "Time should have crossed 60s");

    Thread.sleep(500);
    assertTrue(
        player.getRemainingTime() < timeAfterSwitch - 100,
        "Time should decrease rapidly after switch");
  }

  /** Tests that exceptions in the task are caught and don't crash the manager. */
  @Test
  public void testTaskExceptionHandling() {
    GameEngine mockedEngine = mock(GameEngine.class);
    GameState mockedState = mock(GameState.class);
    when(mockedEngine.getState()).thenReturn(mockedState);

    Player player = mock(Player.class);
    when(player.getRemainingTime()).thenReturn(70000L);
    when(mockedState.getCurrentPlayer())
        .thenReturn(player)
        .thenThrow(new RuntimeException("Injected task error"));

    BlitzManager faultyBlitz = new BlitzManager(mockedEngine);

    assertDoesNotThrow(() -> faultyBlitz.start());

    try {
      Thread.sleep(1200); // Wait for task to execute and catch exception
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    assertDoesNotThrow(() -> faultyBlitz.pause());
  }
}
