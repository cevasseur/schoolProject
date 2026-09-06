package fr.bordeaux.ai.mcts;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.core.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MCTSPlayerTest {

  private Board board;
  private List<Player> players;
  private MctsPlayer ai;

  @BeforeEach
  public void setUp() {
    int size = 9;
    board = new Board(size);
    players = new ArrayList<>();

    // AI starts at the top (row 0, middle)
    ai = new MctsPlayer("MCTS_AI", new Position(0, 4), 2000, Color.WHITE);
    // Opponent starts at the bottom (row 8, middle)
    HumanPlayer opponent = new HumanPlayer("Opponent", new Position(8, 4), Color.BLACK);

    players.add(ai);
    players.add(opponent);
  }

  @Test
  public void testMCTSInitialization() {
    assertEquals("MCTS_AI", ai.getName());
    assertEquals(new Position(0, 4), ai.getPosition());
    assertEquals(10, ai.getRemainingWalls(), "AI should start with 10 walls");
  }

  @Test
  public void testMCTSCopy() {
    Player copy = ai.copy();

    assertNotSame(ai, copy, "Copy should be a different object instance");
    assertEquals(ai.getName(), copy.getName());
    assertEquals(ai.getPosition(), copy.getPosition());
    assertEquals(ai.getRemainingWalls(), copy.getRemainingWalls());
    assertInstanceOf(MctsPlayer.class, copy, "Copy should also be an instance of MCTSPlayer");
  }

  @Test
  public void testGetNextMoveReturnsLegalMove() {
    GameState state = new GameState(board, players, 0); // AI's turn

    Move move = ai.getNextMove(state);

    assertNotNull(move, "MCTS should return a move");

    // Verify the move is among legal moves
    List<Move> legalMoves = state.generateLegalMoves();
    assertTrue(
        legalMoves.contains(move), "The chosen move must be legal according to the GameState");
  }

  @Test
  public void testMCTSPlayerSettings() {
    var settings = ai.getSettings();
    assertEquals("mcts", settings.get("mode"), "Settings should correctly identify MCTS mode");
  }

  @Test
  public void testFullConstructor() throws java.io.IOException {
    MctsPlayer mcts = new MctsPlayer("FullAI", new Position(0, 0), 1000, false, Color.WHITE);
    assertEquals("FullAI", mcts.getName());
    assertEquals(1000L, mcts.getSettings().get("reflexion"));
    assertEquals("UCT", mcts.getSettings().get("selection"));
  }

  @Test
  public void testToString() {
    String str = ai.toString();
    assertTrue(
        str.startsWith("AI Player{name='MCTS_AI'"), "toString should start with 'AI Player'");
  }

  @Test
  public void testHint() throws java.io.IOException {
    GameState state = new GameState(board, players, 0);
    Move move = MctsPlayer.hint(ai, state);
    assertNotNull(move, "Hint should return a move");
    assertTrue(state.generateLegalMoves().contains(move), "Hinted move should be legal");
  }

  @Test
  public void testDetailedHint() throws java.io.IOException {
    GameState state = new GameState(board, players, 0);
    Move move = MctsPlayer.hint(ai, state, 500, false);
    assertNotNull(move, "Detailed hint should return a move");
  }

  @Test
  public void testMCTSFindsWinningMove() {
    // Create a scenario where the AI is one step away from winning
    // Goal for Player starting at row 0 is row 8

    // Position AI at row 7
    ai.setPosition(new Position(7, 4));
    // Position Human at row 1
    players.get(1).setPosition(new Position(1, 4));
    ai.setRemainingWalls(0);
    players.get(1).setRemainingWalls(0);

    GameState state = new GameState(board, players, 0); // AI's turn (index 0)

    // The winning move is to go to row 8
    Position winningPos = new Position(8, 4);

    // Standard UCT search (useNN = false)
    Move chosenMove = ai.getNextMove(state);

    assertNotNull(chosenMove, "MCTS should return a move");
    assertEquals(
        winningPos, chosenMove.getTo(), "MCTS should prioritize the move that wins the game");
  }

  @Test
  public void testConstructorWithNN() throws java.io.IOException {
    MctsPlayer mcts = new MctsPlayer("NNAI", new Position(0, 0), 1000, true, Color.WHITE);
    assertEquals("NNAI", mcts.getName());
    assertEquals("ML", mcts.getSettings().get("selection"));
  }

  @Test
  public void testSelectionLoopBreakWhenNoMovesPossible() {
    // We create a scenario where a player is completely blocked by walls.
    // On a 3x3 board, we position the player at (1, 1) and surround them.
    Board tinyBoard = new Board(3);
    List<Player> tinyPlayers = new ArrayList<>();
    Player blockedAi = new MctsPlayer("BlockedAI", new Position(1, 1), 200, Color.WHITE);
    Player opponent = new HumanPlayer("Opponent", new Position(0, 0), Color.BLACK);
    tinyPlayers.add(blockedAi);
    tinyPlayers.add(opponent);

    // Surround (1, 1) with walls:
    tinyBoard.addWall(new Position(0, 0), Orientation.HORIZONTAL);
    tinyBoard.addWall(new Position(1, 0), Orientation.HORIZONTAL);
    tinyBoard.addWall(new Position(0, 0), Orientation.VERTICAL);
    tinyBoard.addWall(new Position(0, 1), Orientation.VERTICAL);

    GameState state = new GameState(tinyBoard, tinyPlayers, 0);
    assertTrue(state.generateLegalMoves().isEmpty(), "Player should have no legal moves");

    MctsNode root = new MctsNode(state);

    assertDoesNotThrow(
        () -> {
          Move best = root.getBestMoveAfterSearch(200, 0, null);
          assertNull(best, "Best move should be null when no moves are possible");
        },
        "MCTS search should not crash or enter infinite loop when no moves are possible");
  }
}
