package fr.bordeaux.core;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.ai.minimax.MinimaxPlayer;
import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.persistence.GameLoader;
import fr.bordeaux.persistence.GameSaver;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GameEngineTest {

  private GameEngine engine;
  private GameState state;
  private Player p1, p2;
  private Board board;
  private ConfigManager configManager;

  @BeforeEach
  public void setup() {
    board = new Board(9);
    p1 = new HumanPlayer("Human", new Position(0, 2), Color.WHITE);
    p2 = MinimaxPlayer.builder("Bot", new Position(4, 2), Color.BLACK).build();
    state = new GameState(board, List.of(p1, p2));
    engine = new GameEngine(state, false, false, false, false, 10);
    configManager = ConfigManager.getInstance();
  }

  @Test
  public void play_pawn_move_valid() {
    Move move = Move.pawn(p1.getPosition(), new Position(1, 2));
    assertTrue(engine.play(move));
    assertEquals(new Position(1, 2), p1.getPosition());
  }

  @Test
  public void play_pawn_move_illegal() {
    Move move = Move.pawn(p1.getPosition(), new Position(3, 2));
    assertFalse(engine.play(move));
  }

  @Test
  public void play_wall_move_valid() {
    Move wall = Move.wall(new Position(2, 2), Orientation.HORIZONTAL);
    assertTrue(engine.play(wall));
  }

  @Test
  public void play_wall_move_illegal_collision() {
    Move wall1 = Move.wall(new Position(2, 2), Orientation.HORIZONTAL);
    engine.play(wall1);
    Move wall2 = Move.wall(new Position(2, 2), Orientation.HORIZONTAL);
    assertFalse(engine.play(wall2));
  }

  @Test
  public void undo_reverts_pawn_move() {
    Position from = p1.getPosition();
    Position to = new Position(1, 2);
    Move move = Move.pawn(from, to);
    engine.play(move);

    engine.undo();
    assertEquals(from, p1.getPosition());
  }

  @Test
  public void redo_reapplies_pawn_move() {
    Position from = p1.getPosition();
    Position to = new Position(1, 2);
    Move move = Move.pawn(from, to);
    engine.play(move);
    engine.undo();
    engine.redo();
    assertEquals(to, p1.getPosition());
  }

  @Test
  public void undo_redo_wall_moves() {
    Move wall = Move.wall(new Position(2, 2), Orientation.HORIZONTAL);
    engine.play(wall);
    engine.undo();
    engine.redo();
    assertTrue(board.isWallCollision(new Position(2, 2), Orientation.HORIZONTAL));
  }

  @Test
  public void pause_blocks_play() {
    engine.pause();
    Move move = Move.pawn(p1.getPosition(), new Position(1, 2));
    assertFalse(engine.play(move));
    assertEquals(new Position(0, 2), p1.getPosition());
  }

  @Test
  public void newGame_resets_state() {
    final GameState old = engine.getState();
    engine.newGame(9, fr.bordeaux.main.PlayerFactory.createPlayers(configManager), null);
    final GameState fresh = engine.getState();
    assertSame(old, fresh);
    assertEquals(2, fresh.getPlayers().size());
    assertEquals(9, fresh.getBoard().getSize());
  }

  @Test
  public void save_and_load_game() throws Exception {
    final String file = "engine_test.json";
    GameSaver.saveGame(engine, file);
    engine.newGame(9, fr.bordeaux.main.PlayerFactory.createPlayers(configManager), null);
    final GameState loadedState = GameLoader.loadGame(engine, file);
    engine.getState().loadState(loadedState);
    engine.initializeBlitz();
    assertEquals(2, engine.getState().getPlayers().size());
    new File(file).delete();
  }

  @Test
  public void testPlay_Winner_WithBlitz() {
    engine = new GameEngine(state, true, false, false, false, 10);
    // p1 starts at X=0 in setup(), so goal is X=8
    p1.setPosition(new Position(7, 2));
    Move move = Move.pawn(new Position(7, 2), new Position(8, 2));

    assertTrue(engine.play(move));
    assertTrue(state.isGameOver());
    assertFalse(engine.getBlitzManager().isRunning());
  }

  @Test
  public void testNewGame_WithBlitz() {
    engine = new GameEngine(state, true, false, false, false, 10);
    assertTrue(engine.getBlitzManager().isRunning());
    engine.newGame(9, List.of(p1, p2), null);
    // Since newGame calls initializeBlitz, it should still be running or re-created
    assertNotNull(engine.getBlitzManager());
  }

  @Test
  public void testLoadGame_WithBlitz() throws Exception {
    engine = new GameEngine(state, true, false, false, false, 10);
    final String file = "engine_load_blitz.json";
    GameSaver.saveGame(engine, file);
    final GameState loadedState = GameLoader.loadGame(engine, file);
    engine.getState().loadState(loadedState);
    engine.initializeBlitz();
    assertNotNull(engine.getBlitzManager());
    new File(file).delete();
  }

  @Test
  public void testPause_AI() {
    state.nextPlayer(); // Now it's p2 (AI)
    engine.pause();
    assertFalse(state.isPaused());
  }

  @Test
  public void testPause_Toggle_WithBlitz() {
    engine = new GameEngine(state, true, false, false, false, 10);
    assertTrue(engine.getBlitzManager().isRunning());

    engine.pause(); // Pause
    assertTrue(state.isPaused());
    assertFalse(engine.getBlitzManager().isRunning());

    engine.pause(); // Resume
    assertFalse(state.isPaused());
    assertTrue(engine.getBlitzManager().isRunning());
  }

  @Test
  public void testSetInitialWalls() {
    engine.setInitialWalls(15);
    assertEquals(15, engine.getInitialWalls());
  }
}
