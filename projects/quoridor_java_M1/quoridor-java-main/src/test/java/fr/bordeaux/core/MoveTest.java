package fr.bordeaux.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoveTest {

  private Position from;
  private Position to;

  @BeforeEach
  public void setUp() {
    from = new Position(0, 1);
    to = new Position(1, 1);
    Move pawnMove = Move.pawn(from, to);
    Move hWallMove = Move.wall(new Position(2, 2), Orientation.HORIZONTAL);
    Move vWallMove = Move.wall(new Position(3, 3), Orientation.VERTICAL);
  }

  @Test
  public void createPawnMove_hasCorrectFields() {
    Move move = Move.pawn(from, to);

    assertEquals(Move.Type.PAWN, move.getType());
    assertEquals(from, move.getFrom());
    assertEquals(to, move.getTo());
    assertNull(move.getOrientation());
    assertTrue(move.isPawn());
    assertFalse(move.isWall());
  }

  @Test
  public void createWallMove_hasCorrectFields() {
    Move move = Move.wall(to, Orientation.HORIZONTAL);

    assertEquals(Move.Type.WALL, move.getType());
    assertNull(move.getFrom());
    assertEquals(to, move.getTo());
    assertEquals(Orientation.HORIZONTAL, move.getOrientation());
    assertTrue(move.isWall());
    assertFalse(move.isPawn());
  }

  @Test
  public void pawnMove_fromTo_notNull() {
    Move move = Move.pawn(from, to);
    assertNotNull(move.getFrom());
    assertNotNull(move.getTo());
  }

  @Test
  public void wallMove_fromIsNull() {
    Move move = Move.wall(to, Orientation.VERTICAL);
    assertNull(move.getFrom());
    assertEquals(to, move.getTo());
    assertEquals(Orientation.VERTICAL, move.getOrientation());
  }
}
