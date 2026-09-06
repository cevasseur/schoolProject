package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ScoreEntryTest {

  @Test
  void constructorInitializesDefaultValues() {
    ScoreEntry entry = new ScoreEntry("p1", "Alice");

    assertEquals("p1", entry.getPlayerId());
    assertEquals("Alice", entry.getPlayerName());
    assertEquals(0, entry.getWins());
    assertEquals(0, entry.getLosses());
    assertEquals(0, entry.getGamesPlayed());
  }

  @Test
  void setPlayerNameUpdatesPlayerName() {
    ScoreEntry entry = new ScoreEntry("p1", "Alice");

    entry.setPlayerName("Alicia");

    assertEquals("Alicia", entry.getPlayerName());
  }

  @Test
  void recordWinIncrementsWinsAndGamesPlayed() {
    ScoreEntry entry = new ScoreEntry("p1", "Alice");

    entry.recordWin();

    assertEquals(1, entry.getWins());
    assertEquals(0, entry.getLosses());
    assertEquals(1, entry.getGamesPlayed());
  }

  @Test
  void recordLossIncrementsLossesAndGamesPlayed() {
    ScoreEntry entry = new ScoreEntry("p1", "Alice");

    entry.recordLoss();

    assertEquals(0, entry.getWins());
    assertEquals(1, entry.getLosses());
    assertEquals(1, entry.getGamesPlayed());
  }

  @Test
  void toStringReflectsCurrentStatistics() {
    ScoreEntry entry = new ScoreEntry("p1", "Alice");
    entry.recordWin();
    entry.recordLoss();

    assertEquals("p1 Alice W:1 L:1 G:2", entry.toString());
  }
}
