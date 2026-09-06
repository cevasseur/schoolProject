package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.bordeaux.core.Move;
import fr.bordeaux.core.Orientation;
import fr.bordeaux.core.Position;
import org.junit.jupiter.api.Test;

public class NetworkMoveParserTest {

  @Test
  void parsePositionConvertsCoordinateToBoardPosition() {
    Position position = NetworkMoveParser.parsePosition("e2");

    assertEquals(new Position(1, 4), position);
  }

  @Test
  void parsePositionRejectsNullOrTooShortValue() {
    assertThrows(IllegalArgumentException.class, () -> NetworkMoveParser.parsePosition(null));
    assertThrows(IllegalArgumentException.class, () -> NetworkMoveParser.parsePosition("a"));
  }

  @Test
  void parseMoveParsesPawnMove() {
    Move move = NetworkMoveParser.parseMove("e2-e3");

    assertTrue(move.isPawn());
    assertEquals(new Position(1, 4), move.getFrom());
    assertEquals(new Position(2, 4), move.getTo());
  }

  @Test
  void parseMoveTrimsInputBeforeParsing() {
    Move move = NetworkMoveParser.parseMove("  e2-e3  ");

    assertEquals(Move.pawn(new Position(1, 4), new Position(2, 4)), move);
  }

  @Test
  void parseMoveParsesHorizontalWall() {
    Move move = NetworkMoveParser.parseMove("c4h");

    assertTrue(move.isWall());
    assertEquals(new Position(3, 2), move.getTo());
    assertEquals(Orientation.HORIZONTAL, move.getOrientation());
  }

  @Test
  void parseMoveParsesVerticalWall() {
    Move move = NetworkMoveParser.parseMove("c4v");

    assertTrue(move.isWall());
    assertEquals(new Position(3, 2), move.getTo());
    assertEquals(Orientation.VERTICAL, move.getOrientation());
  }

  @Test
  void parseMoveRejectsEmptyMove() {
    assertThrows(IllegalArgumentException.class, () -> NetworkMoveParser.parseMove(null));
    assertThrows(IllegalArgumentException.class, () -> NetworkMoveParser.parseMove("   "));
  }

  @Test
  void parseMoveRejectsInvalidPawnMoveFormat() {
    assertThrows(IllegalArgumentException.class, () -> NetworkMoveParser.parseMove("e2-e3-e4"));
  }

  @Test
  void parseMoveRejectsUnknownFormat() {
    assertThrows(IllegalArgumentException.class, () -> NetworkMoveParser.parseMove("hello"));
  }
}
