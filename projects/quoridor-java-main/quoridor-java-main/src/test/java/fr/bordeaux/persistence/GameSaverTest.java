package fr.bordeaux.persistence;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.ai.base.*;
import fr.bordeaux.ai.mcts.*;
import fr.bordeaux.ai.minimax.*;
import fr.bordeaux.ai.random.*;
import fr.bordeaux.core.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.jupiter.api.Test;

public class GameSaverTest {

  @Test
  public void testSaveGame() throws IOException {
    Board board = new Board(9);
    board.addWall(new Position(2, 3), Orientation.HORIZONTAL);
    board.addWall(new Position(4, 5), Orientation.VERTICAL);

    List<Player> players = new ArrayList<>();
    players.add(new HumanPlayer("Joueur1", new Position(0, 4), Color.WHITE));
    players.add(new HumanPlayer("Joueur2", new Position(8, 4), Color.BLACK));

    GameState state = new GameState(board, players, 1);
    String filename = "test.txt";
    GameSaver.saveGame(new GameEngine(state, false, false, false, false, 10), filename);

    File file = new File(filename);
    assertTrue(file.exists(), "Le fichier doit être créé");

    List<String> lines = Files.readAllLines(file.toPath());
    assertTrue(lines.contains("[settings]"));
    assertTrue(lines.contains("[game]"));
    assertTrue(lines.contains("2  # Current player color"));
    assertTrue(lines.contains("walls: 10 10"));
    assertTrue(lines.stream().anyMatch(line -> line.contains("_")));
    assertTrue(lines.stream().anyMatch(line -> line.contains("|")));
  }

  @Test
  public void testSaveGameWithHistory() throws IOException {
    // Setup board and players
    Board board = new Board(9);
    List<Player> players = new ArrayList<>();
    players.add(new HumanPlayer("Player1", new Position(0, 4), Color.WHITE)); // e1
    players.add(new HumanPlayer("Player2", new Position(8, 4), Color.BLACK)); // e9
    GameState state = new GameState(board, players, 0);

    // Simulate moves
    // Player 1 moves from e1 (0,4) to e2 (1,4)
    state.applyMove(Move.pawn(new Position(0, 4), new Position(1, 4)));
    // Player 2 places a horizontal wall at c3 (2,2)
    state.applyMove(Move.wall(new Position(2, 2), Orientation.HORIZONTAL));
    // Player 1 moves from e2 (1,4) to f2 (1,5)
    state.applyMove(Move.pawn(new Position(1, 4), new Position(1, 5)));

    // Save the game to a file
    String filename = "test_history.txt";
    GameSaver.saveGame(new GameEngine(state, false, false, false, false, 10), filename);

    // Verify file existence and content
    File file = new File(filename);
    assertTrue(file.exists(), "The save file should be created");

    // Read file and verify the [History] section
    List<String> lines = Files.readAllLines(file.toPath());
    boolean historyFound = false;
    List<String> historyLines = new ArrayList<>();

    for (String line : lines) {
      if (line.trim().equals("[History]")) {
        historyFound = true;
        continue;
      }
      if (historyFound) {
        if (line.startsWith("[") || line.trim().isEmpty()) break;
        historyLines.add(line.trim());
      }
    }

    assertTrue(historyFound, "The [History] section header is missing");
    assertFalse(historyLines.isEmpty(), "The [History] section should not be empty");

    // Assert specific move notation (Chronological order)
    // Format expected: "1 e1-e2; 2 c3h;" (Turn 1) and "1 e2-f2;" (Turn 2)
    String fullHistory = String.join(" ", historyLines);

    assertTrue(
        fullHistory.contains("1 e1-e2;"), "First move (Player 1 pawn) is missing or incorrect");
    assertTrue(
        fullHistory.contains("2 c3h;"), "Second move (Player 2 wall) is missing or incorrect");
    assertTrue(
        fullHistory.contains("1 e2-f2;"), "Third move (Player 1 pawn) is missing or incorrect");
  }

  @Test
  public void testSaveGameParameters() throws IOException {
    // Setup board and players
    Board board = new Board(9);
    List<Player> players = new ArrayList<>();
    players.add(new HumanPlayer("Player1", new Position(0, 4), Color.WHITE)); // e1
    players.add(
        MinimaxPlayer.builder("Ai1", new Position(8, 4), Color.BLACK).withDepth(3).build()); // e9
    GameState state = new GameState(board, players, 0);

    fr.bordeaux.config.ConfigManager.getInstance().setOption("blitz", "true");
    fr.bordeaux.config.ConfigManager.getInstance().setOption("debug", "true");
    // Save the game to a file
    String filename = "test_parameters.txt";
    GameSaver.saveGame(new GameEngine(state, true, false, false, true, 10), filename);

    // Verify file existence and content
    File file = new File(filename);
    assertTrue(file.exists(), "The save file should be created");

    // Read file and verify the [settings] section
    List<String> lines = Files.readAllLines(file.toPath());
    boolean settingsFound = false;
    List<String> settingsLines = new ArrayList<>();

    for (String line : lines) {
      if (line.trim().equals("[settings]")) {
        settingsFound = true;
        continue;
      }
      if (settingsFound) {
        if (line.startsWith("[") || line.trim().isEmpty()) break;
        settingsLines.add(line.trim());
      }
    }

    assertTrue(settingsFound, "The [settings] section header is missing");
    assertFalse(settingsLines.isEmpty(), "The [settings] section should not be empty");

    assertTrue(settingsLines.contains("blitz=true"), "Mode blitz incorrect");
    assertTrue(settingsLines.contains("debug=true"), "Mode debug incorrect");
    assertTrue(
        settingsLines.contains("player-1-name=Player1"), "Incorrect name for the first Player");
    assertTrue(settingsLines.contains("ai-2-name=Ai1"), "Incorrect name for the second Player");
    assertTrue(settingsLines.contains("ai-2-depth=3"), "Incorrect depth for the second Player");
    assertTrue(settingsLines.contains("ai-2-mode=minimax"), "Incorrect mode for Minimax");
    assertTrue(settingsLines.contains("ai-2-reflexion=5"), "Incorrect reflexion time for Minimax");
    assertTrue(settingsLines.contains("ai-2-heuristic=simple"), "Incorrect heuristic for Minimax");
  }
}
