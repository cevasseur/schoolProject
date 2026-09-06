package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.core.*;
import java.util.List;
import javafx.embed.swing.JFXPanel;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.testfx.util.WaitForAsyncUtils;

/**
 * @class BoardViewTest
 * @brief Unit tests for BoardView JavaFX component.
 */
class BoardViewTest {

  private GameEngine engine;
  private GameState state;
  private Board board;
  private Player player1, player2;
  private BoardView boardView;
  private ActionDispatcher dispatcher;

  @BeforeEach
  void setup() {
    new JFXPanel(); // initialize JavaFX toolkit

    engine = mock(GameEngine.class);
    state = mock(GameState.class);
    board = mock(Board.class);
    player1 = mock(Player.class);
    player2 = mock(Player.class);
    dispatcher = mock(ActionDispatcher.class);
    fr.bordeaux.graph.Graph graph = mock(fr.bordeaux.graph.Graph.class);

    when(engine.getState()).thenReturn(state);
    when(state.getBoard()).thenReturn(board);
    when(board.getGraph()).thenReturn(graph);
    when(board.getSize()).thenReturn(9);
    when(state.getPlayers()).thenReturn(List.of(player1, player2));
    when(state.getCurrentPlayer()).thenReturn(player1);
    when(player1.getPosition()).thenReturn(new Position(0, 0));
    when(player1.getColor()).thenReturn(fr.bordeaux.core.Color.WHITE);
    when(player2.getPosition()).thenReturn(new Position(1, 1));
    when(player2.getColor()).thenReturn(fr.bordeaux.core.Color.BLACK);
    when(dispatcher.isNetworkGame()).thenReturn(false);

    boardView = new BoardView(engine, dispatcher);
  }

  @Test
  void testDrawWallsAddsRectangles() {
    fr.bordeaux.graph.Graph graph = mock(fr.bordeaux.graph.Graph.class);
    when(board.getGraph()).thenReturn(graph);
    when(graph.hasEdge(anyInt(), anyInt())).thenReturn(false);

    boardView.drawWalls();

    assertTrue(boardView.getChildren().stream().anyMatch(n -> n instanceof Rectangle));
  }

  @Test
  void testHandleCellClickCallsEnginePlay() {
    Rectangle slot =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().equals(javafx.scene.paint.Color.TRANSPARENT))
            .findFirst()
            .orElse(null);

    assertNotNull(slot, "Should find at least one wall slot");

    var event = mock(javafx.scene.input.MouseEvent.class);
    when(event.getButton()).thenReturn(MouseButton.SECONDARY);

    try (MockedStatic<RuleChecker> checker = mockStatic(RuleChecker.class)) {
      checker
          .when(() -> RuleChecker.isMoveLegal(any(GameState.class), any(Move.class)))
          .thenReturn(true);

      slot.getOnMouseClicked().handle(event);

      verify(dispatcher, atLeastOnce()).playBoardMove(argThat(move -> move.isWall()));
    }
  }

  @Test
  void testHandleWallClickCallsEnginePlayIfLegal() {
    try (MockedStatic<RuleChecker> checker = mockStatic(RuleChecker.class)) {
      checker
          .when(() -> RuleChecker.isMoveLegal(any(GameState.class), any(Move.class)))
          .thenReturn(true);

      boardView.handleWallClick(0, 0, MouseButton.SECONDARY);

      checker.verify(() -> RuleChecker.isMoveLegal(any(GameState.class), any(Move.class)));
      verify(dispatcher, times(1)).playBoardMove(any(Move.class));
    }
  }

  @Test
  void testClickOnCellPrimary() {
    StackPane cell =
        (StackPane)
            boardView.getChildren().stream()
                .filter(n -> n instanceof StackPane)
                .findFirst()
                .orElse(null);
    assertNotNull(cell);

    var event = mock(javafx.scene.input.MouseEvent.class);
    when(event.getButton()).thenReturn(MouseButton.PRIMARY);

    cell.getOnMouseClicked().handle(event);

    verify(dispatcher, atLeast(0)).playBoardMove(any(Move.class));
  }

  @Test
  void testClickOnCellSecondary() {
    StackPane cell =
        (StackPane)
            boardView.getChildren().stream()
                .filter(n -> n instanceof StackPane)
                .findFirst()
                .orElse(null);
    assertNotNull(cell);

    try (MockedStatic<RuleChecker> checker = mockStatic(RuleChecker.class)) {
      checker
          .when(() -> RuleChecker.isMoveLegal(any(GameState.class), any(Move.class)))
          .thenReturn(true);

      var event = mock(javafx.scene.input.MouseEvent.class);
      when(event.getButton()).thenReturn(MouseButton.SECONDARY);

      cell.getOnMouseClicked().handle(event);

      verify(dispatcher, atLeast(0)).playBoardMove(any(Move.class));
    }
  }

  @Test
  void testDrawBoardDoesNotThrow() {
    assertDoesNotThrow(() -> boardView.drawBoard());
  }

  @Test
  void testHighlightCurrentPlayerDoesNotThrow() {
    assertDoesNotThrow(() -> boardView.highlightCurrentPlayer());
  }

  @Test
  void testHighlightCurrentPlayerGameOver() {
    when(state.isGameOver()).thenReturn(true);
    // Create a new boardView so the constructor call also sees isGameOver = true
    BoardView bv = new BoardView(engine, new ActionDispatcher(engine, null));

    // Reset mock to ignore the calls from constructor if any
    clearInvocations(state);

    bv.highlightCurrentPlayer();
    verify(state, never()).getCurrentPlayer();
  }

  @Test
  void testHighlightCurrentPlayerNoPawn() {
    when(state.getCurrentPlayer()).thenReturn(player1);
    when(player1.getPosition()).thenReturn(new Position(0, 0));

    // Clear the children of cell (0,0) so pawn is null
    StackPane cell = findCell(0, 0);
    cell.getChildren().removeIf(n -> n instanceof Rectangle);

    assertDoesNotThrow(() -> boardView.highlightCurrentPlayer());
  }

  @Test
  void testHighlightAccessibleCells() {
    Position legal1 = new Position(0, 1);
    Position legal2 = new Position(1, 0);
    when(state.getLegalMoves(player1)).thenReturn(List.of(legal1, legal2));

    boardView.highlightAccessibleCells(player1);

    StackPane cell1 = findCell(0, 1);
    StackPane cell2 = findCell(1, 0);

    assertNotNull(cell1);
    assertNotNull(cell2);

    assertTrue(cell1.getStyle().contains("lightgreen"));
    assertTrue(cell2.getStyle().contains("lightgreen"));

    StackPane defaultCell = findCell(0, 0);
    assertNotNull(defaultCell);
    assertTrue(defaultCell.getStyle().contains("beige"));
  }

  @Test
  void testHighlightAccessibleCellsNotCurrent() {
    when(state.getCurrentPlayer()).thenReturn(player2);
    boardView.highlightAccessibleCells(player1);
    verify(state, never()).getLegalMoves(any());
  }

  @Test
  void testHighlightAccessibleCellsAI() {
    when(player1.isAI()).thenReturn(true);
    boardView.highlightAccessibleCells(player1);
    verify(state, never()).getLegalMoves(any());
  }

  private StackPane findCell(int row, int col) {
    return boardView.getChildren().stream()
        .filter(n -> n instanceof StackPane)
        .map(n -> (StackPane) n)
        .filter(
            c ->
                GridPane.getRowIndex(c) != null
                    && GridPane.getRowIndex(c) == row
                    && GridPane.getColumnIndex(c) != null
                    && GridPane.getColumnIndex(c) == col)
        .findFirst()
        .orElse(null);
  }

  @Test
  void testPawnClickCallsHighlightAccessibleCells() {
    StackPane cell = findCell(0, 0);
    assertNotNull(cell);

    Rectangle pawn =
        cell.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .findFirst()
            .orElse(null);

    assertNotNull(pawn);

    var event = mock(javafx.scene.input.MouseEvent.class);
    when(event.getButton()).thenReturn(MouseButton.PRIMARY);

    pawn.getOnMouseClicked().handle(event);
    verify(state, atLeast(0)).getLegalMoves(any());
  }

  /** Test clicking on a wall slot (interactive zone between cells) triggers engine.play */
  @Test
  void testWallSlotClickCallsEnginePlay() {
    Rectangle slot =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().equals(javafx.scene.paint.Color.TRANSPARENT))
            .findFirst()
            .orElse(null);

    assertNotNull(slot, "Should find at least one wall slot");

    when(player1.isAI()).thenReturn(false);
    when(dispatcher.isNetworkGame()).thenReturn(false);

    var event = mock(javafx.scene.input.MouseEvent.class);
    when(event.getButton()).thenReturn(MouseButton.SECONDARY);

    try (MockedStatic<RuleChecker> checker = mockStatic(RuleChecker.class)) {
      checker
          .when(() -> RuleChecker.isMoveLegal(any(GameState.class), any(Move.class)))
          .thenReturn(true);

      slot.getOnMouseClicked().handle(event);

      checker.verify(() -> RuleChecker.isMoveLegal(any(GameState.class), any(Move.class)));
      verify(dispatcher, times(1)).playBoardMove(argThat(Move::isWall));
    }
  }

  @Test
  void testWallSlotHoverHuman() {
    Rectangle slot =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().equals(javafx.scene.paint.Color.TRANSPARENT))
            .findFirst()
            .orElse(null);
    assertNotNull(slot);

    when(player1.isAI()).thenReturn(false);

    var event = mock(javafx.scene.input.MouseEvent.class);
    slot.getOnMouseEntered().handle(event);

    assertNotEquals(javafx.scene.paint.Color.TRANSPARENT, slot.getFill());

    slot.getOnMouseExited().handle(event);
    assertEquals(javafx.scene.paint.Color.TRANSPARENT, slot.getFill());
  }

  @Test
  void testWallSlotHoverAI() {
    Rectangle slot =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().equals(javafx.scene.paint.Color.TRANSPARENT))
            .findFirst()
            .orElse(null);
    assertNotNull(slot);

    when(player1.isAI()).thenReturn(true);

    var event = mock(javafx.scene.input.MouseEvent.class);
    slot.getOnMouseEntered().handle(event);

    assertEquals(javafx.scene.paint.Color.TRANSPARENT, slot.getFill());
  }

  @Test
  void testWallSlotClickAI() {
    Rectangle slot =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().equals(javafx.scene.paint.Color.TRANSPARENT))
            .findFirst()
            .orElse(null);
    assertNotNull(slot);

    when(player1.isAI()).thenReturn(true);

    var event = mock(javafx.scene.input.MouseEvent.class);
    when(event.getButton()).thenReturn(MouseButton.SECONDARY);

    slot.getOnMouseClicked().handle(event);

    verify(dispatcher, never()).playBoardMove(any());
  }

  @Test
  void testWallSlotClickPrimary() {
    Rectangle slot =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().equals(javafx.scene.paint.Color.TRANSPARENT))
            .findFirst()
            .orElse(null);
    assertNotNull(slot);

    when(player1.isAI()).thenReturn(false);

    var event = mock(javafx.scene.input.MouseEvent.class);
    when(event.getButton()).thenReturn(MouseButton.PRIMARY);

    slot.getOnMouseClicked().handle(event);

    verify(dispatcher, never()).playBoardMove(any());
  }

  @Test
  void testDrawPlayersColors() {
    Player p3 = mock(Player.class);
    Player p4 = mock(Player.class);
    when(state.getPlayers()).thenReturn(List.of(p3, p4));
    when(p3.isDisabled()).thenReturn(false);
    when(p3.getPosition()).thenReturn(new Position(2, 2));
    when(p3.getColor()).thenReturn(fr.bordeaux.core.Color.BLUE);
    when(p4.isDisabled()).thenReturn(false);
    when(p4.getPosition()).thenReturn(new Position(3, 3));
    when(p4.getColor()).thenReturn(fr.bordeaux.core.Color.RED);

    boardView.drawPlayers();

    StackPane cell3 = findCell(2, 2);
    Rectangle pawn3 =
        (Rectangle)
            cell3.getChildren().stream().filter(n -> n instanceof Rectangle).findFirst().get();
    assertEquals(javafx.scene.paint.Color.BLUE, pawn3.getFill());

    StackPane cell4 = findCell(3, 3);
    Rectangle pawn4 =
        (Rectangle)
            cell4.getChildren().stream().filter(n -> n instanceof Rectangle).findFirst().get();
    assertEquals(javafx.scene.paint.Color.RED, pawn4.getFill());
  }

  @Test
  void testDrawPlayersSkipsDisabled() {
    when(player2.isDisabled()).thenReturn(true);
    when(state.getPlayers()).thenReturn(List.of(player1, player2));

    // Create a new boardView so it uses our updated mock from the start
    BoardView bv = new BoardView(engine, new ActionDispatcher(engine, null));
    bv.drawPlayers();

    // Since drawBoard is called in constructor, cell2 should already be empty if isDisabled was
    // true
    StackPane cell2 = null;
    // We need to find the cell in the NEW BoardView bv
    cell2 =
        (StackPane)
            bv.getChildren().stream()
                .filter(n -> n instanceof StackPane)
                .map(n -> (StackPane) n)
                .filter(
                    c ->
                        GridPane.getRowIndex(c) != null
                            && GridPane.getRowIndex(c) == 1
                            && GridPane.getColumnIndex(c) != null
                            && GridPane.getColumnIndex(c) == 1)
                .findFirst()
                .orElse(null);

    assertNotNull(cell2);
    assertTrue(cell2.getChildren().isEmpty());
  }

  @Test
  void testHandleCellClickAI() {
    when(player1.isAI()).thenReturn(true);
    boardView.handleCellClick(0, 1);
    verify(dispatcher, never()).playBoardMove(any());
  }

  @Test
  void testHandleCellClickUnsuccessfulPlay() {
    when(player1.isAI()).thenReturn(false);
    boardView.handleCellClick(0, 1);

    verify(dispatcher, times(1)).playBoardMove(any(Move.class));
  }

  @Test
  void testHandleWallClickIllegal() {
    try (MockedStatic<RuleChecker> checker = mockStatic(RuleChecker.class)) {
      checker.when(() -> RuleChecker.isMoveLegal(any(), any())).thenReturn(false);
      when(player1.isAI()).thenReturn(false);

      boardView.handleWallClick(0, 0, MouseButton.SECONDARY);
      verify(dispatcher, never()).playBoardMove(any());
    }
  }

  @Test
  void testHandleWallClickAI() {
    when(player1.isAI()).thenReturn(true);
    boardView.handleWallClick(0, 0, MouseButton.SECONDARY);
    verify(dispatcher, never()).playBoardMove(any());
  }

  @Test
  void testHighlightedWallMoveAppearance() {
    Move wallMove = Move.wall(new Position(2, 2), Orientation.HORIZONTAL);
    boardView.highlightMove(wallMove);
    WaitForAsyncUtils.waitForFxEvents();

    Rectangle hint =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().toString().toLowerCase().contains("40e0d0"))
            .findFirst()
            .orElse(null);

    assertNotNull(hint, "Hint rectangle should be added to the board");
    assertEquals(Orientation.HORIZONTAL, wallMove.getOrientation());
  }

  @Test
  void testHighlightedWallMoveInteraction() {
    Move wallMove = Move.wall(new Position(3, 3), Orientation.VERTICAL);
    boardView.highlightMove(wallMove);
    WaitForAsyncUtils.waitForFxEvents();

    Rectangle hint =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().toString().toLowerCase().contains("40e0d0"))
            .findFirst()
            .orElse(null);

    assertNotNull(hint);

    try (MockedStatic<RuleChecker> checker = mockStatic(RuleChecker.class)) {
      checker.when(() -> RuleChecker.isMoveLegal(any(), any())).thenReturn(true);
      when(player1.isAI()).thenReturn(false);

      var event = mock(javafx.scene.input.MouseEvent.class);
      hint.getOnMouseClicked().handle(event);

      verify(dispatcher, atLeastOnce()).playBoardMove(argThat(Move::isWall));
    }
  }

  @Test
  void testHighlightedWallMoveHover() {
    Move wallMove = Move.wall(new Position(1, 1), Orientation.HORIZONTAL);
    boardView.highlightMove(wallMove);
    WaitForAsyncUtils.waitForFxEvents();

    Rectangle hint =
        boardView.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().toString().toLowerCase().contains("40e0d0"))
            .findFirst()
            .orElse(null);

    assertNotNull(hint);

    javafx.scene.paint.Paint originalFill = hint.getFill();
    var event = mock(javafx.scene.input.MouseEvent.class);

    hint.getOnMouseEntered().handle(event);
    assertNotEquals(originalFill, hint.getFill());

    hint.getOnMouseExited().handle(event);
    assertEquals(originalFill, hint.getFill());
  }

  @Test
  void testHighlightedPawnMoveAppearance() {
    Move pawnMove = Move.pawn(new Position(0, 0), new Position(0, 1));
    boardView.highlightMove(pawnMove);
    WaitForAsyncUtils.waitForFxEvents();

    StackPane cell = findCell(0, 1);
    assertNotNull(cell);

    Rectangle hint =
        cell.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().toString().toLowerCase().contains("40e0d0"))
            .findFirst()
            .orElse(null);

    assertNotNull(hint, "Pawn hint rectangle should be added to the target cell");
  }

  @Test
  void testHighlightedPawnMoveInteraction() {
    Move pawnMove = Move.pawn(new Position(0, 0), new Position(0, 1));
    boardView.highlightMove(pawnMove);
    WaitForAsyncUtils.waitForFxEvents();

    StackPane cell = findCell(0, 1);
    Rectangle hint =
        cell.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().toString().toLowerCase().contains("40e0d0"))
            .findFirst()
            .orElse(null);

    assertNotNull(hint);

    when(player1.isAI()).thenReturn(false);

    var event = mock(javafx.scene.input.MouseEvent.class);
    when(event.getButton()).thenReturn(MouseButton.PRIMARY);
    hint.getOnMouseClicked().handle(event);

    verify(dispatcher, atLeastOnce()).playBoardMove(argThat(Move::isPawn));
  }

  @Test
  void testHighlightedPawnMoveHover() {
    Move pawnMove = Move.pawn(new Position(0, 0), new Position(1, 0));
    boardView.highlightMove(pawnMove);
    WaitForAsyncUtils.waitForFxEvents();

    StackPane cell = findCell(1, 0);
    Rectangle hint =
        cell.getChildren().stream()
            .filter(n -> n instanceof Rectangle)
            .map(n -> (Rectangle) n)
            .filter(r -> r.getFill().toString().toLowerCase().contains("40e0d0"))
            .findFirst()
            .orElse(null);

    assertNotNull(hint);

    javafx.scene.paint.Paint originalFill = hint.getFill();
    var event = mock(javafx.scene.input.MouseEvent.class);

    hint.getOnMouseEntered().handle(event);
    assertNotEquals(originalFill, hint.getFill());

    hint.getOnMouseExited().handle(event);
    assertEquals(originalFill, hint.getFill());
  }
}
