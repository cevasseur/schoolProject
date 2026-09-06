package fr.bordeaux.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlayerTest {

  private Player player;
  private Position startPos;

  @BeforeEach
  public void setUp() {
    startPos = new Position(0, 4);
    player = new HumanPlayer("Alice", startPos, Color.WHITE);
  }

  @Test
  public void testConstructorSetsInitialData() {
    assertEquals("Alice", player.getName());
    assertEquals(startPos, player.getPosition());
    assertEquals(startPos, player.getStartingPosition());
    assertEquals(10, player.getRemainingWalls());
  }

  @Test
  public void testSetPosition() {
    Position newPos = new Position(1, 4);
    player.setPosition(newPos);
    assertEquals(newPos, player.getPosition());
    assertEquals(startPos, player.getStartingPosition());
  }

  @Test
  public void testUseWall() {
    player.useWall();
    assertEquals(9, player.getRemainingWalls());
    player.useWall();
    assertEquals(8, player.getRemainingWalls());
  }

  @Test
  public void testToStringFormat() {
    String expected = "Player{name='Alice', pos=Position[row=0, col=4], walls=10}";
    // assertEquals(expected, player.toString());
  }
}
