package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.*;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.persistence.GameLoader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

class QuoridorAppTest extends ApplicationTest {

  @BeforeEach
  void setupI18n() {
    I18n.initialize();
  }

  @Override
  public void start(Stage stage) throws IOException {
    QuoridorApp app = new QuoridorApp();
    app.start(stage);
  }

  @Test
  void testApplicationStarts() {
    Stage stage =
        (Stage)
            javafx.stage.Window.getWindows().stream()
                .filter(javafx.stage.Window::isShowing)
                .findFirst()
                .orElseThrow();

    assertNotNull(stage);
    assertEquals("Quoridor", stage.getTitle());
    assertTrue(stage.isShowing());
  }

  @Test
  void testSceneIsSet() {
    Stage stage =
        (Stage)
            javafx.stage.Window.getWindows().stream()
                .filter(javafx.stage.Window::isShowing)
                .findFirst()
                .orElseThrow();

    Scene scene = stage.getScene();

    assertNotNull(scene);
    assertTrue(scene.getWidth() > 0);
    assertTrue(scene.getHeight() > 0);
  }

  /** Tests that the game-over overlay is correctly displayed and can be closed. */
  @Test
  void testShowGameOverOverlay() {
    StackPane container = new StackPane();
    QuoridorApp app = new QuoridorApp();
    AtomicBoolean popupShown = new AtomicBoolean(true);

    interact(() -> app.showGameOverOverlay(container, "TestWinner", popupShown));

    VBox overlay = (VBox) container.getChildren().get(0);
    assertNotNull(overlay);

    Label winLabel = (Label) overlay.getChildren().get(1);
    assertEquals("TestWinner won the game!", winLabel.getText());

    Button closeBtn = (Button) overlay.getChildren().get(2);
    interact(() -> closeBtn.fire());

    assertTrue(container.getChildren().isEmpty());
    assertFalse(popupShown.get());
  }

  /** Tests that the game-over listener correctly triggers the overlay when the game ends. */
  @Test
  void testSetupGameOverListener() {
    GameEngine engine = mock(GameEngine.class);
    GameState state = mock(GameState.class);
    when(engine.getState()).thenReturn(state);
    when(state.isGameOver()).thenReturn(true);

    Player winner = mock(Player.class);
    when(winner.getName()).thenReturn("Winner");
    when(state.getWinner()).thenReturn(java.util.Optional.of(winner));

    StackPane container = new StackPane();
    QuoridorApp app = new QuoridorApp();

    app.setupGameOverListener(engine, container);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(state).addListener(captor.capture());

    interact(() -> captor.getValue().run());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(container.getChildren().isEmpty());
    VBox overlay = (VBox) container.getChildren().get(0);
    Label winLabel = (Label) overlay.getChildren().get(1);
    assertEquals("Winner won the game!", winLabel.getText());
  }

  private static final Move DEFAULT_MOVE = Move.pawn(new Position(0, 0), new Position(0, 1));

  /** Helper to verify AI turn branches after the thinking phase. */
  private void verifyAiTurnBranch(
      boolean isNullMove,
      boolean gameOverAfter,
      boolean pausedAfter,
      boolean turnChanged,
      boolean playSuccess,
      boolean expectedPlay,
      boolean expectedDraw)
      throws Exception {
    GameEngine engine = mock(GameEngine.class);
    GameState state = mock(GameState.class);
    BoardView boardView = mock(BoardView.class);
    when(engine.getState()).thenReturn(state);

    Player aiPlayer = mock(Player.class);
    when(aiPlayer.isAI()).thenReturn(true);
    Player otherPlayer = mock(Player.class);

    // Initial state setup using an AtomicBoolean to switch states after thinking
    AtomicBoolean thinkingFinished = new AtomicBoolean(false);

    when(state.isGameOver()).thenAnswer(inv -> thinkingFinished.get() ? gameOverAfter : false);
    when(state.isPaused()).thenAnswer(inv -> thinkingFinished.get() ? pausedAfter : false);
    when(state.getCurrentPlayer())
        .thenAnswer(inv -> thinkingFinished.get() && turnChanged ? otherPlayer : aiPlayer);

    Move finalMove = isNullMove ? null : DEFAULT_MOVE;
    when(aiPlayer.getNextMove(state))
        .thenAnswer(
            inv -> {
              Thread.sleep(100); // Simulate thinking time
              thinkingFinished.set(true);
              return finalMove;
            });

    when(engine.play(any())).thenReturn(playSuccess);

    QuoridorApp app = new QuoridorApp();
    app.setupAITurnListener(engine, boardView);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(state, atLeastOnce()).addListener(captor.capture());

    // Trigger the task
    interact(() -> captor.getValue().run());

    // Wait for AI thread and Platform.runLater
    waitForAiExecution(engine, expectedPlay);

    if (expectedPlay) {
      verify(engine, atLeastOnce()).play(DEFAULT_MOVE);
    } else {
      verify(engine, never()).play(any());
    }

    if (expectedDraw) {
      verify(boardView, atLeastOnce()).drawBoard();
    } else {
      verify(boardView, never()).drawBoard();
    }
  }

  private void waitForAiExecution(GameEngine engine, boolean expectedPlay) throws Exception {
    long timeout = 2000;
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < timeout) {
      WaitForAsyncUtils.waitForFxEvents();
      try {
        if (expectedPlay) {
          verify(engine, atLeastOnce()).play(any());
          return;
        } else {
          // If not expected, we wait a bit and if still not called, we assume success
          if (System.currentTimeMillis() - start > 1000) return;
        }
      } catch (AssertionError e) {
        // Continue waiting
      }
      Thread.sleep(50);
    }
    if (expectedPlay) {
      fail("AI move was expected but engine.play() was never called");
    }
  }

  @Test
  void testSetupAiTurnListener_Success() throws Exception {
    verifyAiTurnBranch(false, false, false, false, true, true, true);
  }

  @Test
  void testSetupAiTurnListener_NullMove() throws Exception {
    verifyAiTurnBranch(true, false, false, false, true, false, false);
  }

  @Test
  void testSetupAiTurnListener_GameOverDuringThink() throws Exception {
    verifyAiTurnBranch(false, true, false, false, true, false, false);
  }

  @Test
  void testSetupAiTurnListener_PausedDuringThink() throws Exception {
    verifyAiTurnBranch(false, false, true, false, true, false, false);
  }

  @Test
  void testSetupAiTurnListener_TurnChangedDuringThink() throws Exception {
    verifyAiTurnBranch(false, false, false, true, true, false, false);
  }

  @Test
  void testSetupAiTurnListener_PlayFailed() throws Exception {
    verifyAiTurnBranch(false, false, false, false, false, true, false);
  }

  @Test
  void testBuildRootLayoutVariations() {
    ActionDispatcher dispatcher = mock(ActionDispatcher.class);
    StackPane board = new StackPane();

    GameState state = mock(GameState.class);
    Player player = mock(Player.class);
    when(state.getPlayers()).thenReturn(List.of(player, player, player, player));
    when(player.getColor()).thenReturn(fr.bordeaux.core.Color.WHITE);
    when(player.getRemainingWalls()).thenReturn(10);

    interact(
        () -> {
          MainMenuBar menu = new MainMenuBar(dispatcher);
          // 2 players
          PlayerPanel[] p2 = {new PlayerPanel("P1", state, 0), new PlayerPanel("P2", state, 1)};
          BorderPane r2 = QuoridorApp.buildRootLayout(menu, board, p2);
          assertEquals(1, ((VBox) r2.getLeft()).getChildren().size());
          assertEquals(1, ((VBox) r2.getRight()).getChildren().size());

          // 3 players
          PlayerPanel[] p3 = {
            new PlayerPanel("P1", state, 0),
            new PlayerPanel("P2", state, 1),
            new PlayerPanel("P3", state, 2)
          };
          BorderPane r3 = QuoridorApp.buildRootLayout(menu, board, p3);
          assertEquals(2, ((VBox) r3.getLeft()).getChildren().size()); // 0 and 2
          assertEquals(1, ((VBox) r3.getRight()).getChildren().size()); // 1

          // 4 players
          PlayerPanel[] p4 = {
            new PlayerPanel("P1", state, 0),
            new PlayerPanel("P2", state, 1),
            new PlayerPanel("P3", state, 2),
            new PlayerPanel("P4", state, 3)
          };
          BorderPane r4 = QuoridorApp.buildRootLayout(menu, board, p4);
          assertEquals(2, ((VBox) r4.getLeft()).getChildren().size());
          assertEquals(2, ((VBox) r4.getRight()).getChildren().size());
        });
  }

  @Test
  void testSetupGameOverListener_SinglePopup() {
    GameEngine engine = mock(GameEngine.class);
    GameState state = mock(GameState.class);
    when(engine.getState()).thenReturn(state);
    when(state.isGameOver()).thenReturn(true);
    when(state.getWinner()).thenReturn(Optional.of(mock(Player.class)));

    StackPane container = new StackPane();
    // Wrap in scene to avoid NPE in focus properties
    interact(() -> new Scene(container));

    QuoridorApp app = new QuoridorApp();
    app.setupGameOverListener(engine, container);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(state).addListener(captor.capture());

    // Call twice
    interact(
        () -> {
          captor.getValue().run();
          captor.getValue().run();
        });
    WaitForAsyncUtils.waitForFxEvents();

    // Only one overlay
    assertEquals(1, container.getChildren().size());
  }

  @Test
  void testSetupGameOverListener_ResetPopupShown() {
    GameEngine engine = mock(GameEngine.class);
    GameState state = mock(GameState.class);
    when(engine.getState()).thenReturn(state);

    // First call: gameOver=true
    when(state.isGameOver()).thenReturn(true);
    when(state.getWinner()).thenReturn(Optional.of(mock(Player.class)));

    StackPane container = new StackPane();
    QuoridorApp app = new QuoridorApp();
    app.setupGameOverListener(engine, container);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(state).addListener(captor.capture());

    interact(() -> captor.getValue().run());
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(1, container.getChildren().size());

    // Reset overlay
    interact(() -> container.getChildren().clear());

    // Second call: gameOver=false (resets popupShown)
    when(state.isGameOver()).thenReturn(false);
    interact(() -> captor.getValue().run());

    // Third call: gameOver=true (should show again)
    when(state.isGameOver()).thenReturn(true);
    interact(() -> captor.getValue().run());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(1, container.getChildren().size());
  }

  @Test
  void testSetupAiTurnListener_AiComputingGuard() throws Exception {
    GameEngine engine = mock(GameEngine.class);
    GameState state = mock(GameState.class);
    BoardView boardView = mock(BoardView.class);
    when(engine.getState()).thenReturn(state);

    Player ai = mock(Player.class);
    when(ai.isAI()).thenReturn(true);
    when(state.getCurrentPlayer()).thenReturn(ai);
    when(state.isGameOver()).thenReturn(false);
    when(state.isPaused()).thenReturn(false);

    // AI will block to simulate long computation
    when(ai.getNextMove(any()))
        .thenAnswer(
            inv -> {
              Thread.sleep(500);
              return DEFAULT_MOVE;
            });

    QuoridorApp app = new QuoridorApp();
    app.setupAITurnListener(engine, boardView);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(state, atLeastOnce()).addListener(captor.capture());

    // Trigger manually.
    // Atomic compareAndSet should ensure only one is actually computing.
    interact(
        () -> {
          captor.getValue().run();
          captor.getValue().run();
        });

    // Check that getNextMove was called exactly once despite multiple task triggers
    Thread.sleep(100);
    verify(ai, times(1)).getNextMove(any());
  }

  @Test
  void testSetupAiTurnListener_AiException() throws Exception {
    GameEngine engine = mock(GameEngine.class);
    GameState state = mock(GameState.class);
    BoardView boardView = mock(BoardView.class);
    when(engine.getState()).thenReturn(state);

    Player ai = mock(Player.class);
    when(ai.isAI()).thenReturn(true);
    when(state.getCurrentPlayer()).thenReturn(ai);
    when(state.isGameOver()).thenReturn(false);
    when(state.isPaused()).thenReturn(false);

    when(ai.getNextMove(any())).thenThrow(new RuntimeException("AI Error"));

    QuoridorApp app = new QuoridorApp();
    app.setupAITurnListener(engine, boardView);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(state, atLeastOnce()).addListener(captor.capture());

    interact(() -> captor.getValue().run());

    // Give it time to run the thread
    Thread.sleep(200);

    // Ensure it didn't crash the app and didn't call play
    verify(engine, never()).play(any());
  }

  @Test
  void testStop() {
    QuoridorApp app = new QuoridorApp();
    assertDoesNotThrow(() -> app.stop());
  }

  /** Tests the Hint button interaction and feedback. */
  @Test
  void testHintActionShowsThinkingMessage() throws IOException {
    GameEngine engine = mock(GameEngine.class);
    BoardView boardView = mock(BoardView.class);
    Move hintMove = Move.pawn(new Position(4, 4), new Position(4, 5));

    final AtomicReference<Button> btnRef = new AtomicReference<>();
    final AtomicReference<Label> labelRef = new AtomicReference<>();

    interact(
        () -> {
          // Use a subclass to override computeHint and avoid MockedStatic issues
          QuoridorApp appWithMockHint =
              new QuoridorApp() {
                @Override
                protected Move computeHint(GameEngine eng) {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  return hintMove;
                }
              };

          javafx.scene.Node hintArea = appWithMockHint.createHintArea(engine, boardView);
          HBox container = (HBox) hintArea;
          Button btn = (Button) container.getChildren().get(0);
          Label label = (Label) container.getChildren().get(1);
          btnRef.set(btn);
          labelRef.set(label);

          assertEquals("", label.getText());
          assertFalse(btn.isDisable());

          btn.fire();

          assertTrue(btn.isDisable());
          assertEquals("AI is thinking...", label.getText());
        });

    // Wait for the async task and Platform.runLater
    WaitForAsyncUtils.waitForFxEvents();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    WaitForAsyncUtils.waitForFxEvents();

    interact(
        () -> {
          assertFalse(btnRef.get().isDisable());
          assertEquals("", labelRef.get().getText());
          verify(boardView).highlightMove(hintMove);
        });
  }

  /** Tests the initial game loading branch in start() with a missing file. */
  @Test
  void testStartWithLoadFile_Failure() throws IOException {
    ConfigManager config = ConfigManager.getInstance();
    String originalLoadFile = config.getOption("load-file", null);
    try {
      config.setOption("load-file", "non_existent_file.quoridor");
      Stage mockStage = mock(Stage.class);
      QuoridorApp app = new QuoridorApp();

      interact(
          () -> {
            try {
              app.start(mockStage);
            } catch (IOException e) {
              fail("start should catch internal loading exceptions: " + e.getMessage());
            }
          });

      // Verification is done via coverage, but we ensure no crash occurred.
      assertNotNull(app);
    } finally {
      if (originalLoadFile != null) {
        config.setOption("load-file", originalLoadFile);
      } else {
        config.removeOption("load-file");
      }
    }
  }

  /** Tests the initial game loading branch in start() with a mock success. */
  @Test
  void testStartWithLoadFile_Success() throws IOException {
    ConfigManager config = ConfigManager.getInstance();
    String originalLoadFile = config.getOption("load-file", null);

    try (MockedStatic<GameLoader> mockedLoader = mockStatic(GameLoader.class)) {
      config.setOption("load-file", "dummy.quoridor");

      // Use a real but minimal state
      List<Player> players = new ArrayList<>();
      players.add(new HumanPlayer("P1", new Position(0, 4), Color.WHITE));
      players.add(new HumanPlayer("P2", new Position(8, 4), Color.BLACK));
      GameState dummyState = new GameState(new Board(9), players);
      mockedLoader
          .when(() -> GameLoader.loadGame(any(), eq("dummy.quoridor")))
          .thenReturn(dummyState);

      Stage mockStage = mock(Stage.class);
      QuoridorApp app = new QuoridorApp();

      interact(
          () -> {
            try {
              app.start(mockStage);
            } catch (IOException e) {
              fail("start should successfully load when GameLoader succeeds");
            }
          });
      assertNotNull(app);
    } finally {
      if (originalLoadFile != null) {
        config.setOption("load-file", originalLoadFile);
      } else {
        config.removeOption("load-file");
      }
    }
  }
}
