package fr.bordeaux.ai.nn;

import static org.junit.jupiter.api.Assertions.*;

import fr.bordeaux.ai.random.RandomPlayer;
import fr.bordeaux.core.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

// Share a single trained model across all tests for speed.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NeuralNetworkEvaluatorTest {

  // 5000 games and 750 epochs
  private static final int TEST_NUM_TRAINING_GAMES = 5000;
  private static final int TEST_EPOCHS = 750;
  private static final String MODEL_PATH = "src/main/resources/nnmodels/default_quoridor_model.bin";
  private NeuralNetworkEvaluator evaluator;
  private GameState state;

  /** Trains the model once before any test runs. */
  @BeforeAll
  public void setUp() throws IOException {
    File modelFile = new File(MODEL_PATH);

    if (modelFile.exists()) {
      System.out.println("--- Chargement du modèle pré-entraîné ---");
      evaluator = new NeuralNetworkEvaluator();

    } else {
      System.out.println("--- Aucun modèle trouvé. Entraînement en cours... ---");
      evaluator = new NeuralNetworkEvaluator(TEST_NUM_TRAINING_GAMES, TEST_EPOCHS);
      evaluator.saveModel(MODEL_PATH);
      System.out.println("--- Modèle sauvegardé pour les futurs tests ---");
    }
    Board board = new Board(9);
    Player p1 = new RandomPlayer("P1", new Position(0, 4), Color.WHITE);
    Player p2 = new RandomPlayer("P2", new Position(8, 4), Color.BLACK);
    List<Player> players = Arrays.asList(p1, p2);
    state = new GameState(board, players);
  }

  /** Releases TensorFlow resources after all tests have run. */
  @AfterAll
  public void tearDown() {
    evaluator.close();
  }

  /**
   * The evaluator must initialize without throwing any exception. This implicitly tests that
   * training completes successfully.
   */
  @Test
  public void testInitializationSucceeds() {
    assertNotNull(evaluator, "Evaluator should be initialized correctly");
  }

  /**
   * The score returned by evaluate() must always lie within [-1000, 1000], since it is derived from
   * a sigmoid output (probability in [0, 1]).
   */
  @Test
  public void testEvaluateReturnsScoreInValidRange() {
    int score = evaluator.evaluate(state);
    assertTrue(score >= -1000 && score <= 1000, "Score should be in [-1000, 1000], got: " + score);
  }

  /**
   * The evaluator must return the same score for the same game state (the model is deterministic
   * after training).
   */
  @Test
  public void testEvaluateIsConsistent() {
    int score1 = evaluator.evaluate(state);
    int score2 = evaluator.evaluate(state);
    assertEquals(score1, score2, "Evaluate should return the same score for the same state");
  }

  /**
   * A player positioned one row away from their goal should be evaluated more favorably than when
   * they are far from it. This is a sanity check that the model captures proximity-to-win.
   */
  @Test
  public void testEvaluateFavorsWinningPosition() {
    // nearState: p1 is moved close to its goal (row 8), p2 stays at its start
    Board boardNear = new Board(9);
    Player p1Near = new RandomPlayer("P1Near", new Position(0, 4), Color.WHITE);
    Player p2Near = new RandomPlayer("P2Near", new Position(8, 4), Color.BLACK);
    p1Near.setRemainingWalls(0);
    p2Near.setRemainingWalls(0);
    p1Near.setPosition(new Position(7, 3)); // 1 step from goal row 8
    GameState nearState = new GameState(boardNear, Arrays.asList(p1Near, p2Near));

    // farState: both players stay at their canonical starting positions
    Board boardFar = new Board(9);
    Player p1Far = new RandomPlayer("P1Far", new Position(0, 4), Color.WHITE);
    Player p2Far = new RandomPlayer("P2Far", new Position(8, 4), Color.BLACK);
    p1Far.setRemainingWalls(0);
    p2Far.setRemainingWalls(0);
    GameState farState = new GameState(boardFar, Arrays.asList(p1Far, p2Far));
    int scoreNear = evaluator.evaluate(nearState);
    int scoreFar = evaluator.evaluate(farState);
    int gap = scoreNear - scoreFar;
    assertTrue(
        gap >= 50,
        "A player closer to the goal should score clearly higher (gap >= 50). Near: "
            + scoreNear
            + ", Far: "
            + scoreFar
            + ", Gap: "
            + gap);
  }

  /**
   * Tests that a model saved to a file can be reloaded to yield consistent scores.
   *
   * @throws IOException If the temporary file cannot be created or model saving fails.
   */
  @Test
  public void testSaveModel() throws IOException {
    File tempFile = File.createTempFile("test_nn_model", ".bin");
    tempFile.deleteOnExit();

    // 1. Save the model from the primary evaluator
    evaluator.saveModel(tempFile.getAbsolutePath());
    assertTrue(tempFile.exists(), "Model file should be created");
    assertTrue(
        tempFile.length() >= 20,
        "Model file should have weights for "
            + NeuralNetworkEvaluator.NUM_FEATURES
            + " features and a bias.");

    // 2. Create a secondary evaluator and load the saved file into it.
    try (NeuralNetworkEvaluator reloaded = new NeuralNetworkEvaluator(1, 1)) {
      try (java.io.InputStream is = new java.io.FileInputStream(tempFile)) {
        NeuralNetworkTrainer.loadModel(reloaded, is);
      }

      // 3. Both evaluators must now return identical scores for any given state
      int originalScore = evaluator.evaluate(state);
      int reloadedScore = reloaded.evaluate(state);
      assertEquals(
          originalScore,
          reloadedScore,
          "Evaluating with a reloaded model should yield the original score");
    }
  }

  /**
   * Tests that a model can be loaded via a String path constructor.
   *
   * @throws IOException If the path is invalid.
   */
  @Test
  public void testConstructorWithPath() throws IOException {
    File modelFile = new File(MODEL_PATH);
    if (!modelFile.exists()) {
      // If the default model file doesn't exist yet, save it first.
      evaluator.saveModel(MODEL_PATH);
    }

    try (NeuralNetworkEvaluator pathEvaluator = new NeuralNetworkEvaluator(MODEL_PATH)) {
      assertNotNull(pathEvaluator, "Path-based evaluator should be initialized");
      assertEquals(
          evaluator.evaluate(state),
          pathEvaluator.evaluate(state),
          "Path-based evaluator should produce the same results as the primary one");
    }
  }
}
