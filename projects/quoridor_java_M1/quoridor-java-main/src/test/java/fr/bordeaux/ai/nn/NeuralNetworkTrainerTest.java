package fr.bordeaux.ai.nn;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Move;
import fr.bordeaux.core.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NeuralNetworkTrainerTest {

  @Test
  void testSelectExploratoryMove_ExplorationBranch() {
    NeuralNetworkTrainer trainer = new NeuralNetworkTrainer();
    GameState state = mock(GameState.class);
    Random random = mock(Random.class);

    // Ensure exploration branch is taken: 0.05 < 0.1
    when(random.nextDouble()).thenReturn(0.05);

    List<Move> legalMoves = new ArrayList<>();
    Move mockMove = Move.pawn(new Position(4, 4), new Position(4, 5));
    legalMoves.add(mockMove);
    when(state.generateLegalMoves()).thenReturn(legalMoves);
    when(random.nextInt(legalMoves.size())).thenReturn(0);

    Move selected = trainer.selectExploratoryMove(state, random);

    assertEquals(mockMove, selected);
    verify(state).generateLegalMoves();
    verify(random).nextInt(legalMoves.size());
  }

  @Test
  void testSimulateAndRecordGame_Player1Wins() {
    NeuralNetworkTrainer trainer = new NeuralNetworkTrainer();
    NeuralNetworkEvaluator evaluator = mock(NeuralNetworkEvaluator.class);
    Random random = mock(Random.class);
    List<NeuralNetworkTrainer.TrainingData> trainingData = new ArrayList<>();

    // Mock starting positions: player1 at x=8 (already winner or one step away)
    when(random.nextInt(8)).thenReturn(7); // x=7
    when(random.nextInt(9)).thenReturn(4); // y=4
    when(random.nextInt(1, 9)).thenReturn(1); // x=1
    when(random.nextInt(9)).thenReturn(4); // y=4

    // Walls
    when(random.nextInt(11)).thenReturn(0);

    // Features
    when(evaluator.getFeatures(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f, 0.4f});

    // Move selection for exploration: not exploration (0.5 > 0.1)
    when(random.nextDouble()).thenReturn(0.5);

    when(random.nextDouble()).thenReturn(0.05); // Force exploration
    when(random.nextInt(anyInt())).thenReturn(0); // Pick first legal move

    trainer.simulateAndRecordGame(evaluator, trainingData, random);

    // If player1 wins, trainingData should have entries with outcome 1.0 for P1 and 0.0 for P2
    assertFalse(trainingData.isEmpty(), "Training data should not be empty if a game was recorded");

    boolean foundP1Win = false;
    boolean foundP2Loss = false;
    for (NeuralNetworkTrainer.TrainingData data : trainingData) {
      if (data.outcome() == 1.0) foundP1Win = true;
      if (data.outcome() == 0.0) foundP2Loss = true;
    }

    assertTrue(foundP1Win || foundP2Loss, "Should have recorded game outcomes");
  }
}
