package fr.bordeaux.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PlayerStatusTest {

  @Test
  void enumContainsExpectedStatuses() {
    assertArrayEquals(
        new PlayerStatus[] {PlayerStatus.IDLE, PlayerStatus.INGAME}, PlayerStatus.values());
  }

  @Test
  void valueOfResolvesEachStatusByName() {
    assertEquals(PlayerStatus.IDLE, PlayerStatus.valueOf("IDLE"));
    assertEquals(PlayerStatus.INGAME, PlayerStatus.valueOf("INGAME"));
  }
}
