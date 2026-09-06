package fr.bordeaux.core;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.ai.minimax.MinimaxPlayer;
import fr.bordeaux.config.ConfigManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GameStateTest {

  private GameState game;
  private Board board;
  private Player human;
  private Player ia;

  @BeforeEach
  public void setup() {
    board = new Board(9);
    human = new HumanPlayer("Human", new Position(0, 4), Color.WHITE);
    ia = MinimaxPlayer.builder("IA", new Position(8, 4), Color.BLACK).build();

    List<Player> players = new ArrayList<>();
    players.add(human);
    players.add(ia);

    game = new GameState(board, players);
  }

  @Test
  public void constructorOk2Players() {
    assertNotNull(game);
    assertEquals(human, game.getCurrentPlayer());
  }

  @Test
  public void testPlayersNumberValidThree() {
    Board board = new Board(9);
    Player human2 = new HumanPlayer("Human2", new Position(4, 4), Color.BLUE);
    assertDoesNotThrow(
        () -> {
          new GameState(board, List.of(human, human2, ia));
        });
  }

  @Test
  public void testConstructorInvalidPlayerCount() {
    Board board = new Board(9);
    // 1 player
    assertThrows(IllegalArgumentException.class, () -> new GameState(board, List.of(human)));
    // 5 players
    Player p3 = new HumanPlayer("P3", new Position(0, 0), Color.BLUE);
    Player p4 = new HumanPlayer("P4", new Position(0, 0), Color.RED);
    Player p5 = new HumanPlayer("P5", new Position(0, 0), Color.WHITE);
    assertThrows(
        IllegalArgumentException.class, () -> new GameState(board, List.of(human, ia, p3, p4, p5)));
  }

  @Test
  public void listenerIsCalledOnApplyMove() {
    final boolean[] called = {false};

    game.addListener(() -> called[0] = true);

    Position pos = game.getCurrentPlayer().getPosition();
    Move move = Move.pawn(pos, new Position(1, 4));
    game.applyMove(move);

    assertTrue(called[0]);
  }

  @Test
  public void applyMovePawnChangesPositionAndSwitchPlayer() {
    Position newPos = new Position(1, 4);
    Position pos = game.getCurrentPlayer().getPosition();

    Move move = Move.pawn(pos, newPos);
    game.applyMove(move);

    assertEquals(newPos, human.getPosition());
    assertEquals(ia, game.getCurrentPlayer());
  }

  @Test
  public void applyMoveWallPlacesWallAndSwitchPlayer() {
    Move move = Move.wall(new Position(4, 4), Orientation.HORIZONTAL);

    game.applyMove(move);

    assertEquals(ia, game.getCurrentPlayer());
  }

  @Test
  public void undoRevertsMove() {
    Position p1 = new Position(1, 4);
    Player player = game.getCurrentPlayer();
    game.applyMove(Move.pawn(player.getPosition(), p1));

    game.undo();

    assertEquals(new Position(0, 4), human.getPosition());
    assertEquals(human, game.getCurrentPlayer());
  }

  @Test
  public void redoReappliesMove() {
    Position p1 = new Position(1, 4);
    Position pos = game.getCurrentPlayer().getPosition();

    Move move = Move.pawn(pos, p1);

    game.applyMove(move);
    game.undo();
    game.redo();

    assertEquals(p1, human.getPosition());
    assertEquals(ia, game.getCurrentPlayer());
  }

  @Test
  public void undoDoesNothingIfStackEmpty() {
    game.undo();
    assertEquals(human, game.getCurrentPlayer());
  }

  @Test
  public void redoDoesNothingIfStackEmpty() {
    game.redo();
    assertEquals(human, game.getCurrentPlayer());
  }

  @Test
  public void undoRedoBlockedWhenPausedOrGameOver() {
    game.applyMove(Move.pawn(human.getPosition(), new Position(1, 4)));

    // Paused
    game.setPaused(true);
    game.undo();
    assertEquals(ia, game.getCurrentPlayer()); // No undo
    game.redo(); // No redo

    // Game Over
    game.setPaused(false);
    game.setGameOver(true);
    game.undo();
    assertEquals(ia, game.getCurrentPlayer()); // No undo
  }

  @Test
  public void testUndoWithAI() {
    // Current: Human
    game.applyMove(Move.pawn(human.getPosition(), new Position(1, 4)));
    // Current: AI
    game.applyMove(Move.pawn(ia.getPosition(), new Position(7, 4)));
    // Current: Human

    // Undo should revert BOTH (AI then Human)
    game.undo();

    assertEquals(new Position(0, 4), human.getPosition());
    assertEquals(new Position(8, 4), ia.getPosition());
    assertEquals(human, game.getCurrentPlayer());
  }

  @Test
  public void testRedoWithAI() {
    game.applyMove(Move.pawn(human.getPosition(), new Position(1, 4)));
    game.applyMove(Move.pawn(ia.getPosition(), new Position(7, 4)));
    game.undo(); // Both reverted

    game.redo(); // Both reapplied

    assertEquals(new Position(1, 4), human.getPosition());
    assertEquals(new Position(7, 4), ia.getPosition());
    assertEquals(human, game.getCurrentPlayer()); // It switch twice
  }

  @Test
  public void victoryIsDetected() {
    human.setPosition(new Position(8, 4));
    Position pos = game.getCurrentPlayer().getPosition();
    game.applyMove(Move.pawn(pos, new Position(8, 4)));

    assertFalse(game.isGameOver());
  }

  @Test
  public void winConditionFromTopRow() {
    Predicate<Integer> predicate = game.getWinCondition(human);
    int getCellId = board.getCellId(8, 4);
    assertTrue(predicate.test(getCellId));
  }

  @Test
  public void winConditionFromBottomRow() {
    Player p = new HumanPlayer("P", new Position(8, 4), Color.BLACK);
    GameState g = new GameState(board, List.of(p, ia));
    Predicate<Integer> predicate = g.getWinCondition(p);
    int getCellId = board.getCellId(0, 4);
    assertTrue(predicate.test(getCellId));
  }

  @Test
  public void winConditionFromLeftCol() {
    Player p = new HumanPlayer("P", new Position(4, 0), Color.BLUE);
    GameState g = new GameState(board, List.of(p, ia));
    Predicate<Integer> predicate = g.getWinCondition(p);
    int getCellId = board.getCellId(4, 8);
    assertTrue(predicate.test(getCellId));
  }

  @Test
  public void winConditionFromRightCol() {
    Player p = new HumanPlayer("P", new Position(4, 8), Color.RED);
    GameState g = new GameState(board, List.of(p, ia));
    Predicate<Integer> predicate = g.getWinCondition(p);
    int getCellId = board.getCellId(4, 0);
    assertTrue(predicate.test(getCellId));
  }

  @Test
  public void resetCreatesNewGame() {
    final ConfigManager config = ConfigManager.getInstance();
    game.reset(9, fr.bordeaux.main.PlayerFactory.createPlayers(config), null);
    assertEquals(2, game.getPlayers().size());
    assertEquals(9, game.getBoard().getSize());
  }

  @Test
  public void testResetWithBlitz() {
    final ConfigManager config = ConfigManager.getInstance();
    final String oldBlitz = config.getOption("blitz", "false");
    final String oldTimeout = config.getOption("timeout", "30");

    try {
      config.setOption("blitz", "true");
      config.setOption("timeout", "10"); // 10 minutes

      // 10 minutes = 10 * 60 * 1000 = 600,000 ms
      game.reset(9, fr.bordeaux.main.PlayerFactory.createPlayers(config), 600000L);

      for (final Player p : game.getPlayers()) {
        assertEquals(600000L, p.getRemainingTime());
      }
    } finally {
      // Restore config to avoid side effects on other tests
      config.setOption("blitz", oldBlitz);
      config.setOption("timeout", oldTimeout);
    }
  }

  @Test
  public void testGetLegalMoves() {
    // Starting position (0,4) for WHITE player
    List<Position> legalPositions = game.getLegalMoves(human);

    // Possible moves from (0,4) on a standard board are (1,4), (0,3), (0,5)
    assertTrue(legalPositions.contains(new Position(1, 4)));
    assertTrue(legalPositions.contains(new Position(0, 3)));
    assertTrue(legalPositions.contains(new Position(0, 5)));
    assertEquals(3, legalPositions.size());
  }

  @Test
  public void testGenerateLegalPawnMoves() {
    List<Move> legalPawnMoves = game.generateLegalPawnMoves(human);

    // Should contain moves to the same positions as getLegalMoves
    assertTrue(legalPawnMoves.stream().anyMatch(m -> m.getTo().equals(new Position(1, 4))));
    assertTrue(legalPawnMoves.stream().anyMatch(m -> m.getTo().equals(new Position(0, 3))));
    assertTrue(legalPawnMoves.stream().anyMatch(m -> m.getTo().equals(new Position(0, 5))));
    assertEquals(3, legalPawnMoves.size());
    assertTrue(legalPawnMoves.stream().allMatch(Move::isPawn));
  }

  @Test
  public void generateLegalMovesNoWallsLeftExcludesWallMoves() {
    // Use all walls
    for (int i = 0; i < 10; i++) {
      human.useWall();
    }

    List<Move> legalMoves = game.generateLegalMoves();

    // Should not contain any wall moves
    boolean hasWallMove = legalMoves.stream().anyMatch(Move::isWall);
    assertFalse(hasWallMove);

    // But should still have pawn moves
    Move pawnMove = Move.pawn(new Position(0, 4), new Position(1, 4));
    assertTrue(legalMoves.contains(pawnMove));
  }

  @Test
  public void generateLegalMovesWithOpponentBlockingIncludesJump() {
    // Move human to position where IA is blocking
    human.setPosition(new Position(6, 4));
    ia.setPosition(new Position(7, 4));

    List<Move> legalMoves = game.generateLegalMoves();

    // Should include jump over IA
    Move jumpMove = Move.pawn(new Position(6, 4), new Position(8, 4));
    assertTrue(legalMoves.contains(jumpMove));

    game.getBoard().addWall(new Position(7, 4), Orientation.HORIZONTAL);
    legalMoves = game.generateLegalMoves();
    Move diagJumpMove1 = Move.pawn(new Position(6, 4), new Position(7, 5));
    Move diagJumpMove2 = Move.pawn(new Position(6, 4), new Position(7, 3));
    assertTrue(legalMoves.contains(diagJumpMove1));
    assertTrue(legalMoves.contains(diagJumpMove2));
    assertFalse(legalMoves.contains(jumpMove));
  }

  @Test
  public void checkGenerateLegalMoves() {
    board = new Board(3);
    human = new HumanPlayer("Human", new Position(0, 1), Color.WHITE);
    ia = MinimaxPlayer.builder("IA", new Position(2, 1), Color.BLACK).build();
    ia.setPosition(new Position(1, 1));
    List<Player> players = new ArrayList<>();
    players.add(human);
    players.add(ia);

    game = new GameState(board, players);
    Position p = human.getPosition();
    List<Move> moves =
        new ArrayList<>(
            List.of(
                Move.pawn(p, new Position(0, 0)),
                Move.pawn(p, new Position(0, 2)),
                Move.pawn(p, new Position(2, 1))));
    for (int x = 0; x < board.getSize() - 1; x++) {
      for (int y = 0; y < board.getSize() - 1; y++) {
        Position pos_wall = new Position(x, y);

        moves.add(Move.wall(pos_wall, Orientation.HORIZONTAL));

        moves.add(Move.wall(pos_wall, Orientation.VERTICAL));
      }
    }
    assertTrue(moves.containsAll(game.generateLegalMoves()));

    game.applyMove(Move.wall(new Position(0, 0), Orientation.HORIZONTAL));
    p = game.getCurrentPlayer().getPosition();
    List<Move> moves2 =
        new ArrayList<>(
            List.of(
                Move.pawn(p, new Position(1, 0)),
                Move.pawn(p, new Position(1, 2)),
                Move.pawn(p, new Position(2, 1))));

    moves2.add(Move.wall(new Position(1, 0), Orientation.HORIZONTAL));
    moves2.add(Move.wall(new Position(1, 0), Orientation.VERTICAL));
    moves2.add(Move.wall(new Position(1, 1), Orientation.HORIZONTAL));

    assertTrue(moves2.containsAll(game.generateLegalMoves()));
  }

  @Test
  public void testThreePlayerInitialization() {
    Board board = new Board(9);
    List<Player> players = new ArrayList<>();
    players.add(new HumanPlayer("P1", new Position(0, 4), Color.WHITE));
    players.add(new HumanPlayer("P2", new Position(8, 4), Color.BLACK));
    players.add(new HumanPlayer("P3", new Position(4, 0), Color.BLUE));

    GameState state = new GameState(board, players);
    assertEquals(3, state.getPlayers().size());
    assertEquals(0, state.getCurrentIndexPlayer());

    // Test turn cycling
    state.nextPlayer();
    assertEquals(1, state.getCurrentIndexPlayer());
    state.nextPlayer();
    assertEquals(2, state.getCurrentIndexPlayer());
    state.nextPlayer();
    assertEquals(0, state.getCurrentIndexPlayer());
  }

  @Test
  public void testGetWinnerScenarios() {
    // 1. Standard Win
    human.setPosition(new Position(8, 4));
    assertTrue(game.getWinner().isPresent());
    assertEquals(human, game.getWinner().get());

    // 2. Blitz Victory (all but one disabled)
    human.setPosition(new Position(0, 4)); // Reset position
    ia.setDisabled(true);
    assertTrue(game.getWinner().isPresent());
    assertEquals(human, game.getWinner().get());

    // 3. No winner
    ia.setDisabled(false);
    assertFalse(game.getWinner().isPresent());
  }

  @Test
  public void testPlayerCyclingWithDisabled() {
    // 3 players: human1(0), human2(1), ia(2)
    Player human2 = new HumanPlayer("H2", new Position(4, 0), Color.BLUE);
    GameState state = new GameState(board, List.of(human, human2, ia));

    // Disable middle player
    human2.setDisabled(true);

    // nextPlayer from 0 should skip 1 and go to 2
    state.nextPlayer();
    assertEquals(2, state.getCurrentIndexPlayer());

    // nextPlayer from 2 should skip to 0 (skipping 1 again)
    state.nextPlayer();
    assertEquals(0, state.getCurrentIndexPlayer());

    // previousPlayer from 0 should skip 1 and go to 2
    state.previousPlayer();
    assertEquals(2, state.getCurrentIndexPlayer());

    // getPreviousIndexPlayer from 2 should skip 1 and return 0
    assertEquals(0, state.getPreviousIndexPlayer());
  }
}
