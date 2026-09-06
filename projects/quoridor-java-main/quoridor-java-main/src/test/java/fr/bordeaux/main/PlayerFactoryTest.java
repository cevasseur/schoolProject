package fr.bordeaux.main;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.ai.mcts.*;
import fr.bordeaux.ai.minimax.*;
import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.core.Color;
import fr.bordeaux.core.HumanPlayer;
import fr.bordeaux.core.Player;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlayerFactoryTest {

  private ConfigManager config;

  @BeforeEach
  public void setUp() {
    config = ConfigManager.getInstance();
    config.setOption("ai1", "false");
    config.setOption("ai2", "false");
    config.setOption("nb-players", "2");
  }

  @Test
  public void testCreateDefaultHumanPlayers() {
    List<Player> players = PlayerFactory.createPlayers(config);

    assertEquals(2, players.size(), "Should create 2 players by default");
    assertTrue(players.get(0) instanceof HumanPlayer);
    assertEquals(Color.WHITE, players.get(0).getColor());

    assertTrue(players.get(1) instanceof HumanPlayer);
    assertEquals(Color.BLACK, players.get(1).getColor());
  }

  @Test
  public void testCreateAiPlayers() {
    config.setOption("ai1", "true");
    config.setOption("ai1.mode", "iterative");
    config.setOption("ai1.time", "5");

    config.setOption("ai2", "true");
    config.setOption("ai2.mode", "mcts");

    List<Player> players = PlayerFactory.createPlayers(config);

    assertEquals(2, players.size());

    assertTrue(players.get(0) instanceof MinimaxPlayer);
    MinimaxPlayer ai1 = (MinimaxPlayer) players.get(0);
    assertTrue(ai1.isUseIterativeDeepening(), "Should use iterative deepening");

    assertTrue(players.get(1) instanceof MctsPlayer);
  }

  @Test
  public void testCreateFourPlayers() {
    config.setOption("nb-players", "4");
    List<Player> players = PlayerFactory.createPlayers(config);

    assertEquals(4, players.size());
    assertEquals(Color.WHITE, players.get(0).getColor());
    assertEquals(Color.BLACK, players.get(1).getColor());
    assertEquals(Color.BLUE, players.get(2).getColor());
    assertEquals(Color.RED, players.get(3).getColor());
  }

  @Test
  public void testDepthBasedOnTime() {
    // Time <= 2 -> 3
    assertEquals(3, PlayerFactory.depthBasedOnTime(1));
    assertEquals(3, PlayerFactory.depthBasedOnTime(2));

    // Time <= 5 -> 4
    assertEquals(4, PlayerFactory.depthBasedOnTime(3));
    assertEquals(4, PlayerFactory.depthBasedOnTime(5));

    // Time <= 8 -> 5
    assertEquals(5, PlayerFactory.depthBasedOnTime(6));
    assertEquals(5, PlayerFactory.depthBasedOnTime(8));

    // Time <= 12 -> 6
    assertEquals(6, PlayerFactory.depthBasedOnTime(9));
    assertEquals(6, PlayerFactory.depthBasedOnTime(12));

    // Time > 12 -> 7
    assertEquals(7, PlayerFactory.depthBasedOnTime(13));
    assertEquals(7, PlayerFactory.depthBasedOnTime(100));
  }
}
