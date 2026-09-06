package fr.bordeaux.persistence;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.ai.base.*;
import fr.bordeaux.ai.mcts.*;
import fr.bordeaux.ai.minimax.*;
import fr.bordeaux.ai.random.*;
import fr.bordeaux.core.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GameLoaderTest {

  private static final String TEST_FILE = "load_test.txt";

  private static GameEngine dummyEngine() {
    Board board = new Board(9);
    List<Player> players = new ArrayList<>();
    players.add(new HumanPlayer("P1", new Position(0, 4), Color.WHITE));
    players.add(new HumanPlayer("P2", new Position(8, 4), Color.BLACK));
    return new GameEngine(new GameState(board, players), false, false, false, false, 10);
  }

  @BeforeAll
  public static void setup() throws IOException {
    // Initialize board and players
    Board board = new Board(9);
    List<Player> players = new ArrayList<>();
    players.add(new HumanPlayer("Player1", new Position(0, 4), Color.WHITE));
    players.add(
        MinimaxPlayer.builder("Player2", new Position(8, 4), Color.BLACK)
            .withIterativeDeepening(true)
            .withHeuristic("advanced")
            .build());

    GameState state = new GameState(board, players, 0);

    fr.bordeaux.config.ConfigManager.getInstance().setOption("debug", "true");

    // Apply moves to generate history (undoStack)
    // Player 1 moves pawn from e1 (0,4) to e2 (1,4)
    state.applyMove(Move.pawn(new Position(0, 4), new Position(1, 4)));

    // Player 2 places a horizontal wall at c3 (2,2)
    state.applyMove(Move.wall(new Position(2, 2), Orientation.HORIZONTAL));
    // Save the state to a file
    GameSaver.saveGame(new GameEngine(state, false, false, false, false, 10), TEST_FILE);
  }

  @Test
  public void testLoadGame() throws IOException, GameLoadException {
    GameState loaded = GameLoader.loadGame(dummyEngine(), TEST_FILE);

    assertNotNull(loaded);
    assertEquals(2, loaded.getPlayers().size());
    assertEquals(0, loaded.getCurrentIndexPlayer());
    assertEquals(new Position(1, 4), loaded.getPlayers().get(0).getPosition());
    assertEquals(new Position(8, 4), loaded.getPlayers().get(1).getPosition());
    assertEquals(10, loaded.getPlayers().get(0).getRemainingWalls());
    assertEquals(9, loaded.getPlayers().get(1).getRemainingWalls());
    assertEquals(Color.WHITE, loaded.getPlayers().get(0).getColor());
    assertEquals(Color.BLACK, loaded.getPlayers().get(1).getColor());

    assertFalse(
        loaded
            .getBoard()
            .getGraph()
            .hasEdge(loaded.getBoard().getCellId(2, 2), loaded.getBoard().getCellId(3, 2)));
  }

  @Test
  public void testVerticalWallInCellLine() throws IOException, GameLoadException {
    String filename = "vertical_wall_board.txt";
    try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
      writer.println("[settings]");
      writer.println("verbose=false");
      writer.println("blitz=false");
      writer.println("constest=false");

      writer.println("debug=false");
      writer.println("nb-players=2");
      writer.println("player-1-name=Player1");
      writer.println("player-1-color=WHITE");
      writer.println("player-1-starting-position=Position: 0, 4");
      writer.println("player-1-remaining-time=30");
      writer.println("player-2-name=Player2");
      writer.println("player-2-color=BLACK");
      writer.println("player-2-starting-position=Position: 8, 4");
      writer.println("player-2-remaining-time=30");
      writer.println("[game]");
      writer.println("1  # Current player color");
      writer.println("9  # Board size");
      writer.println("# Board (2 players)");
      writer.println(". . . . 1 . . . .");
      writer.println("                 ");
      writer.println(". . .|. . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . 2 . . . .");
      writer.println("walls: 10 10");
      writer.println("[History]");
    }

    GameState loaded = GameLoader.loadGame(dummyEngine(), filename);
    Board board = loaded.getBoard();

    assertFalse(
        board.getGraph().hasEdge(board.getCellId(1, 2), board.getCellId(1, 3)),
        "Le mur vertical doit supprimer l'arête entre les deux cases");
  }

  @Test
  public void testReadPlayersPositions() throws IOException, GameLoadException {
    String filename = "players_positions_board.txt";
    try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
      writer.println("[settings]");
      writer.println("verbose=false");
      writer.println("blitz=false");
      writer.println("constest=false");

      writer.println("debug=false");
      writer.println("nb-players=2");
      writer.println("player-1-name=Player1");
      writer.println("player-1-color=WHITE");
      writer.println("player-1-starting-position=Position: 0, 4");
      writer.println("player-1-remaining-time=30");
      writer.println("player-2-name=Player2");
      writer.println("player-2-color=BLACK");
      writer.println("player-2-starting-position=Position: 8, 4");
      writer.println("player-2-remaining-time=30");
      writer.println("[game]");
      writer.println("1  # Current player color");
      writer.println("9  # Board size");
      writer.println("# Board (2 players)");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . 1 . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . . . . .");
      writer.println("                 ");
      writer.println(". . . . . 2 . . .");
      writer.println("walls: 10 10");
      writer.println("[History]");
    }

    GameState loaded = GameLoader.loadGame(dummyEngine(), filename);

    assertEquals(new Position(1, 2), loaded.getPlayers().get(0).getPosition());
    assertEquals(new Position(8, 5), loaded.getPlayers().get(1).getPosition());
  }

  @Test
  public void testLoadGameHistory() throws IOException, GameLoadException {
    GameState loaded = GameLoader.loadGame(dummyEngine(), TEST_FILE);
    Move[] history = loaded.getMovesPlayed(); // Reverse order, last move is index 0

    // Verify history stack size
    assertEquals(2, history.length, "History should contain 2 moves");

    // Last move was the wall at c3 (2,2)
    Move lastMove = history[0];
    assertTrue(lastMove.isWall());
    assertEquals(new Position(2, 2), lastMove.getTo());
    assertEquals(Orientation.HORIZONTAL, lastMove.getOrientation());
  }

  @Test
  public void testLoadGameSettings() throws IOException, GameLoadException {
    GameState loaded = GameLoader.loadGame(dummyEngine(), TEST_FILE);

    assertTrue(fr.bordeaux.config.ConfigManager.getInstance().getBoolean("debug", false));
    MinimaxPlayer aiPlayer = (MinimaxPlayer) loaded.getPlayers().get(1);
    assertTrue(aiPlayer.getName().equals("Player2"));
    assertTrue(aiPlayer.getStartingPosition().equals(new Position(8, 4)));
    assertTrue(aiPlayer.isUseIterativeDeepening());
    assertEquals(aiPlayer.getSearchDepth(), 2);
    assertEquals(aiPlayer.getLimitTime(), 5);
    assertTrue(aiPlayer.getHeuristicName().equals("advanced"));
  }
}
