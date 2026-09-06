package fr.bordeaux.gui;

import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import fr.bordeaux.core.RuleChecker;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/** Utility class that handles pawn drag and drop on the game board. */
public final class DragAndDrop {

  /** Data format used to store the dragged pawn position. */
  private static final DataFormat PAWN_FORMAT = new DataFormat("quoridor/pawn");

  /** Base CSS style shared by board cells during drag and drop. */
  private static final String STYLE_BASE = "-fx-border-color: black; -fx-background-color: ";

  /** Default background color of a board cell. */
  private static final String COLOR_NORMAL = "beige;";

  /** Background color used for a legal target cell. */
  private static final String COLOR_LEGAL = "lightgreen;";

  /** Background color used for an illegal target cell. */
  private static final String COLOR_ILLEGAL = "lightgray;";

  /** Utility class constructor. */
  private DragAndDrop() {
    // Prevent instantiation.
  }

  /**
   * Installs drag and drop handlers and reattaches them after each move.
   *
   * @param engine game engine used to validate and play moves
   * @param boardView board view on which handlers are installed
   * @param dispatcher dispatcher used to route moves in local or network mode
   */
  public static void install(
      final GameEngine engine, final BoardView boardView, final ActionDispatcher dispatcher) {
    final Runnable attachHandlers = () -> attachHandlers(engine, boardView, dispatcher);
    final GameState state = engine.getState();

    // Initial attachment
    Platform.runLater(attachHandlers);

    // Re-attach when the game state changes (e.g., after a move)
    state.addListener(() -> Platform.runLater(attachHandlers));

    // Re-attach when the board is redrawn (e.g., on window resize)
    boardView
        .getChildren()
        .addListener((ListChangeListener<Node>) c -> Platform.runLater(attachHandlers));

    // Coverage for gaps and wall slots to avoid barred circle between cells
    boardView.setOnDragOver(
        event -> {
          if (event.getDragboard().hasContent(PAWN_FORMAT)) {
            event.acceptTransferModes(TransferMode.ANY);
          }
          event.consume();
        });
  }

  /**
   * Attaches drag and drop handlers to all board cells and pawns.
   *
   * @param engine game engine used to validate moves
   * @param boardView board view containing the cells
   */
  private static void attachHandlers(
      final GameEngine engine, final BoardView boardView, final ActionDispatcher dispatcher) {
    final var boardNodes = boardView.getChildren();
    for (final Node node : boardNodes) {
      if (!(node instanceof StackPane cell)) {
        continue;
      }
      final int row = getIndex(cell, true);
      final int col = getIndex(cell, false);
      installDropOnCell(engine, dispatcher, cell, row, col);
      for (final Node child : cell.getChildren()) {
        if (child instanceof Rectangle pawn) {
          installDragOnPawn(engine, pawn, row, col);
        }
      }
    }
  }

  /**
   * Installs drag detection on a pawn.
   *
   * @param engine game engine used to read the current player
   * @param pawn pawn node that can be dragged
   * @param row row of the pawn
   * @param col column of the pawn
   */
  private static void installDragOnPawn(
      final GameEngine engine, final Rectangle pawn, final int row, final int col) {
    pawn.setOnDragDetected(
        event -> {
          event.consume();
          final GameState state = engine.getState();
          final Player currentPlayer = state.getCurrentPlayer();

          if (currentPlayer.isAI()) {
            return;
          }

          final Position currentPosition = currentPlayer.getPosition();

          if (currentPosition.getX() != row || currentPosition.getY() != col) {
            return;
          }

          final Dragboard dragboard = pawn.startDragAndDrop(TransferMode.ANY);
          dragboard.setDragView(pawn.snapshot(null, null));
          dragboard.setDragViewOffsetX(pawn.getWidth() / 2.0);
          dragboard.setDragViewOffsetY(pawn.getHeight() / 2.0);
          dragboard.setContent(createPawnDragContent(row, col));
        });
  }

  /**
   * Creates the clipboard content for a pawn move.
   *
   * @param row source row
   * @param col source column
   * @return the clipboard content
   */
  /* default */ static ClipboardContent createPawnDragContent(final int row, final int col) {
    final ClipboardContent content = new ClipboardContent();
    content.put(PAWN_FORMAT, row + "," + col);
    return content;
  }

  /** Installs drop handlers on a target cell. */
  private static void installDropOnCell(
      final GameEngine engine,
      final ActionDispatcher dispatcher,
      final StackPane cell,
      final int toRow,
      final int toCol) {
    cell.setOnDragEntered(
        event -> {
          if (event.getDragboard().hasContent(PAWN_FORMAT)) {
            final boolean wasHighlighted = cell.getStyle().contains(COLOR_LEGAL);
            cell.getProperties().put("wasHighlighted", wasHighlighted);

            if (isMoveLegal(engine, event.getDragboard(), toRow, toCol)) {
              cell.setStyle(STYLE_BASE + COLOR_LEGAL);
            }
            event.acceptTransferModes(TransferMode.ANY);
          }
          event.consume();
        });

    cell.setOnDragExited(
        event -> {
          if (event.getDragboard().hasContent(PAWN_FORMAT)) {
            final boolean wasHighlighted =
                (boolean) cell.getProperties().getOrDefault("wasHighlighted", false);
            cell.setStyle(STYLE_BASE + (wasHighlighted ? COLOR_LEGAL : COLOR_NORMAL));
          }
          event.consume();
        });

    cell.setOnDragOver(
        event -> {
          if (event.getDragboard().hasContent(PAWN_FORMAT)) {
            event.acceptTransferModes(TransferMode.ANY);
          }
          event.consume();
        });

    cell.setOnDragDropped(
        event -> {
          final boolean success =
              handleDrop(engine, dispatcher, event.getDragboard(), toRow, toCol);
          event.setDropCompleted(success);
          event.consume();
        });

    cell.setOnDragDone(
        event -> {
          cell.setStyle(STYLE_BASE + COLOR_NORMAL);
          event.consume();
        });
  }

  /** Plays the dropped move when the dragboard contains a legal pawn move. */
  private static boolean handleDrop(
      final GameEngine engine,
      final ActionDispatcher dispatcher,
      final Dragboard dragboard,
      final int toRow,
      final int toCol) {
    boolean success = false;
    if (dragboard.hasContent(PAWN_FORMAT) && isMoveLegal(engine, dragboard, toRow, toCol)) {
      final Move move = buildMove(dragboard, toRow, toCol);
      if (!dispatcher.isNetworkGame() || dispatcher.isMyTurn()) {
        dispatcher.playBoardMove(move);
        success = true;
      }
    }
    return success;
  }

  /**
   * @return true if move to target cell is legal.
   */
  private static boolean isMoveLegal(
      final GameEngine engine, final Dragboard dragboard, final int toRow, final int toCol) {
    return RuleChecker.isMoveLegal(engine.getState(), buildMove(dragboard, toRow, toCol));
  }

  /**
   * Builds a move from the dragboard origin and the target coordinates.
   *
   * @param dragboard dragboard containing the origin position
   * @param toRow target row
   * @param toCol target column
   * @return the constructed pawn move
   */
  private static Move buildMove(final Dragboard dragboard, final int toRow, final int toCol) {
    final int[] from = parsePosition((String) dragboard.getContent(PAWN_FORMAT));
    final Position origin = new Position(from[0], from[1]);
    final Position destination = new Position(toRow, toCol);
    return Move.pawn(origin, destination);
  }

  /**
   * Parses a position stored as "row,col".
   *
   * @param text text representation of the position
   * @return an array containing row and column
   */
  private static int[] parsePosition(final String text) {
    final String[] parts = text.split(",");
    return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
  }

  /**
   * Returns the row or column index of a node in the grid.
   *
   * @param node node placed in the grid
   * @param isRow true to get the row index, false for the column index
   * @return the requested index, or 0 if it is null
   */
  private static int getIndex(final Node node, final boolean isRow) {
    final Integer index = isRow ? GridPane.getRowIndex(node) : GridPane.getColumnIndex(node);
    return index == null ? 0 : index;
  }
}
