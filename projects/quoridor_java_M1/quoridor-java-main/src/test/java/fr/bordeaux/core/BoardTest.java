package fr.bordeaux.core;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.graph.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BoardTest {

  private Board board;
  private final int SIZE = 9;

  @BeforeEach
  public void setUp() {
    board = new Board(SIZE);
  }

  @Test
  public void testBoardInitialization() {
    assertEquals(SIZE, board.getSize());
    assertNotNull(board.getGraph());
  }

  @Test
  public void testInvalidBoardSizeTooSmall() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Board(1);
        }); // Too small (less than 3)
  }

  @Test
  public void testInvalidBoardSizeTooLarge() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Board(17);
        }); // Too large (greater than 15)
  }

  @Test
  public void testInvalidBoardSizeEven() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Board(8);
        }); // Even number (must be odd)
  }

  @Test
  public void testIsInside() {
    // Valid positions
    assertTrue(board.isInside(new Position(0, 0))); // Top Left
    assertTrue(board.isInside(new Position(0, SIZE - 1))); // Top Right
    assertTrue(board.isInside(new Position(SIZE - 1, 0))); // Bottom Left
    assertTrue(board.isInside(new Position(SIZE - 1, SIZE - 1))); // Bottom Right
    assertTrue(board.isInside(new Position(SIZE / 2, SIZE / 2))); // Center

    // Invalid positions
    assertFalse(board.isInside(new Position(-1, 4))); // Outside Top
    assertFalse(board.isInside(new Position(SIZE, 4))); // Outside Bottom
    assertFalse(board.isInside(new Position(4, -1))); // Outside Left
    assertFalse(board.isInside(new Position(4, SIZE))); // Outside Right
    assertFalse(board.isInside(new Position(-1, -1))); // Outside Top Left diagonal
    assertFalse(board.isInside(new Position(SIZE, SIZE))); // Outside Bottom Right diagonal
  }

  @Test
  public void testIdConversion() {
    assertEquals(0, board.getCellId(0, 0));
    assertEquals(SIZE + 1, board.getCellId(1, 1));
    assertEquals((SIZE * SIZE) - 1, board.getCellId(8, 8)); // 63
  }

  @Test
  public void testAddHorizontalWallRemovesEdges() {
    Position position = new Position(2, 2);
    board.addWall(position, Orientation.HORIZONTAL);

    Graph g = board.getGraph();

    assertFalse(g.getNeighbors(board.getCellId(2, 2)).contains(board.getCellId(3, 2)));
    assertFalse(g.getNeighbors(board.getCellId(2, 3)).contains(board.getCellId(3, 3)));
    assertTrue(g.getNeighbors(board.getCellId(2, 2)).contains(board.getCellId(2, 3)));
  }

  @Test
  public void testAddVerticalWallRemovesEdges() {
    Position position = new Position(4, 1);
    board.addWall(position, Orientation.VERTICAL);

    Graph g = board.getGraph();

    assertFalse(g.getNeighbors(board.getCellId(4, 1)).contains(board.getCellId(4, 2)));
    assertFalse(g.getNeighbors(board.getCellId(5, 1)).contains(board.getCellId(5, 2)));
    assertTrue(g.getNeighbors(board.getCellId(4, 1)).contains(board.getCellId(5, 1)));
  }

  @Test
  public void testHorizontalWallCollision() {
    Position position = new Position(2, 2);
    board.addWall(position, Orientation.HORIZONTAL);

    // Overlap
    assertTrue(board.isWallCollision(new Position(2, 2), Orientation.HORIZONTAL));

    // Partial overlap
    assertTrue(board.isWallCollision(new Position(2, 1), Orientation.HORIZONTAL));
    assertTrue(board.isWallCollision(new Position(2, 3), Orientation.HORIZONTAL));

    // Cross
    assertTrue(board.isWallCollision(new Position(2, 2), Orientation.VERTICAL));

    // Valid cases
    assertFalse(board.isWallCollision(new Position(2, 4), Orientation.HORIZONTAL));
    assertFalse(board.isWallCollision(new Position(0, 2), Orientation.HORIZONTAL));
  }

  @Test
  public void testVerticalWallCollision() {
    Position position = new Position(4, 4);
    board.addWall(position, Orientation.VERTICAL);

    // Overlap
    assertTrue(board.isWallCollision(new Position(4, 4), Orientation.VERTICAL));

    // Partial overlap
    assertTrue(board.isWallCollision(new Position(3, 4), Orientation.VERTICAL));
    assertTrue(board.isWallCollision(new Position(5, 4), Orientation.VERTICAL));

    // Cross
    assertTrue(board.isWallCollision(new Position(4, 4), Orientation.HORIZONTAL));

    // Valid cases
    assertFalse(board.isWallCollision(new Position(3, 3), Orientation.HORIZONTAL));
    assertFalse(board.isWallCollision(new Position(5, 4), Orientation.HORIZONTAL));
  }

  @Test
  public void testNoCollisionWhenWallsAreParallelAdjacent() {
    Position position = new Position(1, 1);
    board.addWall(position, Orientation.HORIZONTAL);

    // Valid cases
    assertFalse(board.isWallCollision(new Position(0, 1), Orientation.HORIZONTAL));
    assertFalse(board.isWallCollision(new Position(2, 1), Orientation.HORIZONTAL));
  }

  @Test
  public void testRemoveHorizontalWallRestoresEdges() {
    Position pos = new Position(2, 2);
    board.addWall(pos, Orientation.HORIZONTAL);

    assertFalse(board.getGraph().hasEdge(board.getCellId(2, 2), board.getCellId(3, 2)));
    assertFalse(board.getGraph().hasEdge(board.getCellId(2, 3), board.getCellId(3, 3)));

    board.removeWall(pos, Orientation.HORIZONTAL);

    assertTrue(board.getGraph().hasEdge(board.getCellId(2, 2), board.getCellId(3, 2)));
    assertTrue(board.getGraph().hasEdge(board.getCellId(2, 3), board.getCellId(3, 3)));
  }

  @Test
  public void testRemoveVerticalWallRestoresEdges() {
    Position pos = new Position(4, 1);
    board.addWall(pos, Orientation.VERTICAL);

    assertFalse(board.getGraph().hasEdge(board.getCellId(4, 1), board.getCellId(4, 2)));
    assertFalse(board.getGraph().hasEdge(board.getCellId(5, 1), board.getCellId(5, 2)));

    board.removeWall(pos, Orientation.VERTICAL);

    assertTrue(board.getGraph().hasEdge(board.getCellId(4, 1), board.getCellId(4, 2)));
    assertTrue(board.getGraph().hasEdge(board.getCellId(5, 1), board.getCellId(5, 2)));
  }
}
