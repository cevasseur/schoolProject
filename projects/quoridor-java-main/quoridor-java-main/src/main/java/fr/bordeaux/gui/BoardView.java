package fr.bordeaux.gui;

import fr.bordeaux.core.Board;
import fr.bordeaux.core.GameEngine;
import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Orientation;
import fr.bordeaux.core.Player;
import fr.bordeaux.core.Position;
import fr.bordeaux.core.RuleChecker;
import fr.bordeaux.graph.Graph;
import java.util.List;
import java.util.Optional;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Graphical Quoridor board. Draws cells, pawns, walls and handles interactions. Board redraws
 * automatically on resize or game state change.
 */
public final class BoardView extends GridPane {

  /** Game engine used to retrieve the game state. */
  private final GameEngine engine;

  /** Dispatcher used to forward GUI moves in local or network mode. */
  private final ActionDispatcher dispatcher;

  /** References to all cell containers on the board. */
  private final StackPane[][] cells;

  /** The highlighted move suggested by the AI, if any. */
  private Optional<Move> highlightedMove = Optional.empty();

  /** Size factor for pawns relative to cell size. */
  private static final double PAWN_SIZE_FACTOR = 0.6;

  /** Arc radius for rounded wall corners. */
  private static final double WALL_ARC = 4.0;

  /** Arc radius for rounded pawn corners. */
  private static final double PAWN_ARC = 15.0;

  /** Width of the pawn's border stroke. */
  private static final double PAWN_STROKE_WIDTH = 5.0;

  /** Radius for the pawn's drop shadow effect. */
  private static final double SHADOW_RADIUS = 5.0;

  /** Duration of the pulse animation in seconds. */
  private static final double PULSE_DURATION = 0.5;

  /** Minimum opacity during the pulse animation. */
  private static final double PULSE_MIN_OPACITY = 0.2;

  /** Opacity for the wall preview highlight. */
  private static final double PREVIEW_OPACITY = 0.4;

  /** Semi-transparent turquoise color for the pawn hint highlight. */
  private static final Color HINT_PAWN_COLOR = Color.rgb(64, 224, 208, 0.4);

  /** Semi-transparent turquoise color for the wall hint highlight. */
  private static final Color HINT_WALL_COLOR = Color.rgb(64, 224, 208, 0.8);

  /** Stroke color for hint highlight over pawns. */
  private static final Color HINT_STROKE_COLOR = Color.TURQUOISE;

  /** Stroke width for the hint highlight over pawns. */
  private static final double HINT_STROKE_WIDTH = 3.0;

  /** Default size of a cell if dimensions are unknown. */
  private static final int DEFAULT_CELL_SIZE = 100;

  /** Minimum thickness of walls in pixels. */
  private static final double MIN_WALL_WIDTH = 4.0;

  /** Ratio of cell size to wall thickness. */
  private static final double WALL_WIDTH_RATIO = 8.0;

  /** Minimum visible size threshold before using dynamic board sizing. */
  private static final double MIN_SIZE = 0.0;

  /** Color used for placed walls. */
  private static final Color WALL_COLOR = Color.rgb(101, 67, 33);

  /** semi-transparent color for wall preview. */
  private static final Color PREVIEW_COLOR = Color.rgb(139, 69, 19, PREVIEW_OPACITY);

  /**
   * Creates board view.
   *
   * @param engine game engine
   * @param dispatcher dispatcher used for local and network moves
   */
  public BoardView(final GameEngine engine, final ActionDispatcher dispatcher) {
    super();
    this.engine = engine;
    this.dispatcher = dispatcher;
    final GameState state = engine.getState();
    final Board board = state.getBoard();
    final int size = board.getSize();
    cells = new StackPane[size][size];
    this.widthProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              drawBoard();
            });
    this.heightProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              drawBoard();
            });
    state.addListener(
        () -> {
          this.highlightedMove = Optional.empty();
          Platform.runLater(this::drawBoard);
        });
    drawBoard();
  }

  /** Redraws board. Clears children/constraints and rebuilds all components. */
  public void drawBoard() {
    getChildren().clear();
    getColumnConstraints().clear();
    getRowConstraints().clear();
    final GameState state = engine.getState();
    final Board board = state.getBoard();
    final int size = board.getSize();
    final double percent = 100.0 / size;
    for (int index = 0; index < size; index++) {
      final ColumnConstraints columnConstraints = new ColumnConstraints();
      columnConstraints.setPercentWidth(percent);
      columnConstraints.setHalignment(HPos.CENTER);
      getColumnConstraints().add(columnConstraints);

      final RowConstraints rowConstraints = new RowConstraints();
      rowConstraints.setPercentHeight(percent);
      rowConstraints.setValignment(VPos.CENTER);
      getRowConstraints().add(rowConstraints);
    }
    for (int row = 0; row < size; row++) {
      for (int col = 0; col < size; col++) {
        final StackPane cell = new StackPane();
        cell.setStyle("-fx-border-color: black; -fx-background-color: beige;");
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cells[row][col] = cell;
        final int targetRow = row;
        final int targetCol = col;
        cell.setOnMouseClicked(
            event -> {
              if (event.getButton() == MouseButton.PRIMARY) {
                handleCellClick(targetRow, targetCol);
              } else {
                handleWallClick(targetRow, targetCol, event.getButton());
              }
            });
        add(cell, col, row);
      }
    }
    drawWallSlots();
    drawWalls();
    drawPlayers();
    drawHighlightedMove();
    highlightCurrentPlayer();
  }

  /**
   * Highlights a specific move on the board (used for hints).
   *
   * @param move The move to highlight.
   */
  public void highlightMove(final Move move) {
    this.highlightedMove = Optional.of(move);
    Platform.runLater(this::drawBoard);
  }

  /** Draws the highlighted move if one is active. */
  private void drawHighlightedMove() {
    if (highlightedMove.isEmpty()) {
      return;
    }
    final Move move = highlightedMove.orElseThrow();
    if (move.isPawn()) {
      drawHighlightedPawnMove(move);
    } else {
      drawHighlightedWallMove(move);
    }
  }

  /** Draws placed walls by checking missing graph edges. Rendered as brown rectangles. */
  public void drawWalls() {
    final GameState state = engine.getState();
    final Board board = state.getBoard();
    final Graph graph = board.getGraph();
    final double cellSize = getCellSize();
    final double wallThickness = getWallThickness();
    final int size = board.getSize();
    for (int row = 0; row < size - 1; row++) {
      for (int col = 0; col < size - 1; col++) {
        final int cellU1 = board.getCellId(row, col);
        final int cellU2 = board.getCellId(row, col + 1);
        final int cellV1 = board.getCellId(row + 1, col);
        final int cellV2 = board.getCellId(row + 1, col + 1);

        if (!graph.hasEdge(cellU1, cellV1) && !graph.hasEdge(cellU2, cellV2)) {
          final Rectangle wall = new Rectangle(cellSize * 2.0, wallThickness);
          wall.setFill(WALL_COLOR);
          wall.setArcWidth(WALL_ARC);
          wall.setArcHeight(WALL_ARC);
          wall.setMouseTransparent(true);
          add(wall, col, row, 2, 1);
          setValignment(wall, VPos.BOTTOM);
          wall.setTranslateY(wallThickness / 2.0);
        }
        if (!graph.hasEdge(cellU1, cellU2) && !graph.hasEdge(cellV1, cellV2)) {
          final Rectangle wall = new Rectangle(wallThickness, cellSize * 2.0);
          wall.setFill(WALL_COLOR);
          wall.setArcWidth(WALL_ARC);
          wall.setArcHeight(WALL_ARC);
          wall.setMouseTransparent(true);
          add(wall, col, row, 1, 2);
          setHalignment(wall, HPos.RIGHT);
          wall.setTranslateX(wallThickness / 2.0);
        }
      }
    }
  }

  /** Draws player pawns. Pawns listen for left click to show highlights. */
  public void drawPlayers() {
    final double cellSize = getCellSize();
    final GameState state = engine.getState();
    final List<Player> players = state.getPlayers();
    for (final Player player : players) {
      if (player.isDisabled()) {
        continue;
      }
      final Position pos = player.getPosition();
      final Rectangle pawn =
          new Rectangle(cellSize * PAWN_SIZE_FACTOR, cellSize * PAWN_SIZE_FACTOR);
      pawn.setArcWidth(PAWN_ARC);
      pawn.setArcHeight(PAWN_ARC);
      pawn.setStroke(Color.LIGHTGRAY);
      pawn.setStrokeWidth(PAWN_STROKE_WIDTH);
      pawn.setEffect(new javafx.scene.effect.DropShadow(SHADOW_RADIUS, Color.GRAY));

      final fr.bordeaux.core.Color playerColor = player.getColor();
      final Color fxColor =
          switch (playerColor) {
            case WHITE -> Color.WHITE;
            case BLACK -> Color.BLACK;
            case BLUE -> Color.BLUE;
            case RED -> Color.RED;
          };
      pawn.setFill(fxColor);
      final int posX = pos.getX();
      final int posY = pos.getY();
      cells[posX][posY].getChildren().add(pawn);
      pawn.setOnMouseClicked(
          event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
              highlightAccessibleCells(player);
            }
          });
    }
  }

  /** Poulse animation for the current player's pawn. */
  public void highlightCurrentPlayer() {
    final GameState state = engine.getState();
    if (!state.isGameOver()) {
      final Player current = state.getCurrentPlayer();
      final Position pos = current.getPosition();
      final int posX = pos.getX();
      final int posY = pos.getY();
      final StackPane cell = cells[posX][posY];
      final Rectangle pawn =
          cell.getChildren().stream()
              .filter(n -> n instanceof Rectangle)
              .map(n -> (Rectangle) n)
              .findFirst()
              .orElse(null);
      if (pawn != null) {
        final FadeTransition fadeTransition =
            new FadeTransition(Duration.seconds(PULSE_DURATION), pawn);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(PULSE_MIN_OPACITY);
        fadeTransition.setCycleCount(FadeTransition.INDEFINITE);
        fadeTransition.setAutoReverse(true);
        fadeTransition.play();
      }
    }
  }

  /**
   * Highlights cells reachable by player.
   *
   * @param player player to check
   */
  public void highlightAccessibleCells(final Player player) {
    final GameState state = engine.getState();
    final Player currentPlayer = state.getCurrentPlayer();
    if (currentPlayer.equals(player) && !player.isAI()) {
      for (final StackPane[] row : cells) {
        for (final StackPane cell : row) {
          cell.setStyle("-fx-border-color: black; -fx-background-color: beige;");
        }
      }
      for (final Position movePos : state.getLegalMoves(player)) {
        final int posX = movePos.getX();
        final int posY = movePos.getY();
        cells[posX][posY].setStyle("-fx-border-color: black; -fx-background-color: lightgreen;");
      }
    }
  }

  /**
   * Moves pawn on left click.
   *
   * @param row row index
   * @param col column index
   */
  public void handleCellClick(final int row, final int col) {
    final GameState state = engine.getState();
    final Player current = state.getCurrentPlayer();
    if (!current.isAI()) {
      if (dispatcher.isNetworkGame() && !dispatcher.isMyTurn()) {
        return;
      }
      final Position from = current.getPosition();
      final Position destination = new Position(row, col);
      final Move move = Move.pawn(from, destination);
      dispatcher.playBoardMove(move);
    }
  }

  /**
   * Places wall on right/other click.
   *
   * @param row row index
   * @param col column index
   * @param button clicked button
   */
  public void handleWallClick(final int row, final int col, final MouseButton button) {
    final GameState state = engine.getState();
    final Player current = state.getCurrentPlayer();
    if (!current.isAI()) {
      if (dispatcher.isNetworkGame() && !dispatcher.isMyTurn()) {
        return;
      }
      final Orientation orientation =
          button == MouseButton.SECONDARY ? Orientation.HORIZONTAL : Orientation.VERTICAL;
      final Move wallMove = Move.wall(new Position(row, col), orientation);
      if (RuleChecker.isMoveLegal(state, wallMove)) {
        dispatcher.playBoardMove(wallMove);
      }
    }
  }

  /** Creates interactive wall slots between cells. */
  private void drawWallSlots() {
    final double cellSize = getCellSize();
    final double wallThickness = getWallThickness();
    final int size = engine.getState().getBoard().getSize();
    for (int row = 0; row < size - 1; row++) {
      for (int col = 0; col < size - 1; col++) {
        final Rectangle hSlot = new Rectangle(cellSize * 2.0, wallThickness);
        hSlot.setFill(Color.TRANSPARENT);
        setupWallInteractions(hSlot, row, col, Orientation.HORIZONTAL);
        add(hSlot, col, row, 2, 1);
        setValignment(hSlot, VPos.BOTTOM);
        hSlot.setTranslateY(wallThickness / 2.0);
        final Rectangle vSlot = new Rectangle(wallThickness, cellSize * 2.0);
        vSlot.setFill(Color.TRANSPARENT);
        setupWallInteractions(vSlot, row, col, Orientation.VERTICAL);
        add(vSlot, col, row, 1, 2);
        setHalignment(vSlot, HPos.RIGHT);
        vSlot.setTranslateX(wallThickness / 2.0);
      }
    }
  }

  /**
   * Sets wall slot handlers (hover, click).
   *
   * @param slot interactive slot
   * @param row row index
   * @param col column index
   * @param orientation wall orientation
   */
  private void setupWallInteractions(
      final Rectangle slot, final int row, final int col, final Orientation orientation) {
    slot.setOnMouseEntered(
        event -> {
          final GameState state = engine.getState();
          final Player current = state.getCurrentPlayer();
          if (!current.isAI()) {
            slot.setFill(PREVIEW_COLOR);
          }
        });
    slot.setOnMouseExited(
        event -> {
          slot.setFill(Color.TRANSPARENT);
        });
    slot.setOnMouseClicked(
        event -> {
          final GameState state = engine.getState();
          final Player current = state.getCurrentPlayer();
          if (!current.isAI() && event.getButton() == MouseButton.SECONDARY) {
            if (dispatcher.isNetworkGame() && !dispatcher.isMyTurn()) {
              return;
            }
            final Move wallMove = Move.wall(new Position(row, col), orientation);
            if (RuleChecker.isMoveLegal(state, wallMove)) {
              dispatcher.playBoardMove(wallMove);
            }
          }
        });
  }

  /**
   * @return cell size in pixels.
   */
  private double getCellSize() {
    final Board board = engine.getState().getBoard();
    final int size = board.getSize();
    final double available = Math.min(getWidth(), getHeight());
    double cellSize = DEFAULT_CELL_SIZE;
    if (available > MIN_SIZE) {
      cellSize = available / size;
    }
    return cellSize;
  }

  /**
   * @return wall thickness in pixels.
   */
  private double getWallThickness() {
    return Math.max(MIN_WALL_WIDTH, getCellSize() / WALL_WIDTH_RATIO);
  }

  private void addToCell(final int row, final int col, final Rectangle rectangle) {
    cells[row][col].getChildren().add(rectangle);
  }

  /** Draws the pawn overlay used to highlight a suggested move. */
  private void drawHighlightedPawnMove(final Move move) {
    final Position target = move.getTo();
    final int targetRow = target.getX();
    final int targetCol = target.getY();
    final double pawnSize = getCellSize() * PAWN_SIZE_FACTOR;
    final Rectangle highlight = new Rectangle(pawnSize, pawnSize, HINT_PAWN_COLOR);
    highlight.setArcWidth(PAWN_ARC);
    highlight.setArcHeight(PAWN_ARC);
    highlight.setStroke(HINT_STROKE_COLOR);
    highlight.setStrokeWidth(HINT_STROKE_WIDTH);
    highlight.setMouseTransparent(false);
    highlight.setOnMouseEntered(
        event -> {
          highlight.setFill(HINT_PAWN_COLOR.darker());
          highlight.setStroke(HINT_STROKE_COLOR.darker());
        });
    highlight.setOnMouseExited(
        event -> {
          highlight.setFill(HINT_PAWN_COLOR);
          highlight.setStroke(HINT_STROKE_COLOR);
        });
    highlight.setOnMouseClicked(
        event -> {
          if (event.getButton() == MouseButton.PRIMARY) {
            handleCellClick(targetRow, targetCol);
          }
        });
    addToCell(targetRow, targetCol, highlight);
  }

  /** Draws the wall overlay used to highlight a suggested move. */
  private void drawHighlightedWallMove(final Move move) {
    final Position target = move.getTo();
    final int targetRow = target.getX();
    final int targetCol = target.getY();
    final Orientation orientation = move.getOrientation();
    final double cellSize = getCellSize();
    final double wallThickness = getWallThickness();
    final Rectangle wall = createHintWallRectangle(orientation, cellSize, wallThickness);
    wall.setOnMouseClicked(event -> playHighlightedWallMove(target, orientation));
    placeHighlightedWall(wall, orientation, targetRow, targetCol, wallThickness);
  }

  /** Creates the highlighted wall rectangle with hover styling. */
  private Rectangle createHintWallRectangle(
      final Orientation orientation, final double cellSize, final double wallThickness) {
    final Rectangle wall =
        orientation == Orientation.HORIZONTAL
            ? new Rectangle(cellSize * 2.0, wallThickness)
            : new Rectangle(wallThickness, cellSize * 2.0);
    wall.setFill(HINT_WALL_COLOR);
    wall.setArcWidth(WALL_ARC);
    wall.setArcHeight(WALL_ARC);
    wall.setMouseTransparent(false);
    wall.setOnMouseEntered(event -> wall.setFill(HINT_WALL_COLOR.darker()));
    wall.setOnMouseExited(event -> wall.setFill(HINT_WALL_COLOR));
    return wall;
  }

  /** Plays the highlighted wall move when it is legal for the current player. */
  private void playHighlightedWallMove(final Position target, final Orientation orientation) {
    final GameState state = engine.getState();
    final Player current = state.getCurrentPlayer();
    if (!current.isAI()) {
      if (dispatcher.isNetworkGame() && !dispatcher.isMyTurn()) {
        return;
      }
      final Move wallMove = Move.wall(target, orientation);
      if (RuleChecker.isMoveLegal(state, wallMove)) {
        dispatcher.playBoardMove(wallMove);
      }
    }
  }

  /** Places the highlighted wall rectangle at the expected grid location. */
  private void placeHighlightedWall(
      final Rectangle wall,
      final Orientation orientation,
      final int targetRow,
      final int targetCol,
      final double wallThickness) {
    if (orientation == Orientation.HORIZONTAL) {
      add(wall, targetCol, targetRow, 2, 1);
      setValignment(wall, VPos.BOTTOM);
      wall.setTranslateY(wallThickness / 2.0);
    } else {
      add(wall, targetCol, targetRow, 1, 2);
      setHalignment(wall, HPos.RIGHT);
      wall.setTranslateX(wallThickness / 2.0);
    }
  }
}
