package fr.bordeaux.ai.nn;

import static fr.bordeaux.ai.nn.NeuralNetworkEvaluator.NUM_FEATURES;

import fr.bordeaux.ai.minimax.MinimaxPlayer;
import fr.bordeaux.core.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.tensorflow.Operand;
import org.tensorflow.framework.losses.BinaryCrossentropy;
import org.tensorflow.framework.optimizers.GradientDescent;
import org.tensorflow.ndarray.StdArrays;
import org.tensorflow.op.Op;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Placeholder;
import org.tensorflow.types.TFloat32;

/** Trainer class for the NeuralNetworkEvaluator. */
public class NeuralNetworkTrainer {

  /** The learning rate for the gradient descent optimizer. */
  private static final float LEARNING_RATE = 0.1f;

  /** The probability of choosing a random move (epsilon-greedy). */
  private static final double EXPLORATION_PROB = 0.1;

  /** Default constructor for the trainer. */
  /* package */ NeuralNetworkTrainer() {
    // Empty constructor to satisfy PMD
  }

  /**
   * Trains the neural network model.
   *
   * @param evaluator the neural network evaluator
   * @param numGames the number of games to simulate
   * @param epochs the number of epochs to train for
   */
  public void trainModel(
      final NeuralNetworkEvaluator evaluator, final int numGames, final int epochs) {
    final Ops ops = Ops.create(evaluator.getGraph());

    final Placeholder<TFloat32> labelPlaceholder =
        ops.placeholder(TFloat32.class, Placeholder.shape(org.tensorflow.ndarray.Shape.of(-1, 1)));

    final BinaryCrossentropy bce = new BinaryCrossentropy(false);

    final Operand<TFloat32> loss = bce.call(ops, labelPlaceholder, evaluator.getPrediction());

    // Optimizer
    final Op trainOp = new GradientDescent(evaluator.getGraph(), LEARNING_RATE).minimize(loss);

    // 2. Génération des données
    final List<TrainingData> dataList = generateTrainingData(evaluator, numGames);
    if (dataList.isEmpty()) {
      return;
    }

    try (TFloat32 inputTensor = prepareInputTensor(dataList);
        TFloat32 labelTensor = prepareLabelTensor(dataList)) {

      for (int epoch = 0; epoch < epochs; epoch++) {
        evaluator
            .getSession()
            .runner()
            .feed(evaluator.getInputPlaceholder(), inputTensor)
            .feed(labelPlaceholder, labelTensor)
            .addTarget(trainOp)
            .run();
      }
    }
  }

  /**
   * Prepares a 2D float tensor representing the batch input features for training.
   *
   * @param data the list of {@link TrainingData} generated from simulations
   * @return a {@link TFloat32} tensor containing the input data
   */
  private TFloat32 prepareInputTensor(final List<TrainingData> data) {
    final float[][] matrix = new float[data.size()][NUM_FEATURES];

    for (int i = 0; i < data.size(); i++) {
      final TrainingData sample = data.get(i);
      final double[] featArray = sample.features();

      for (int j = 0; j < NUM_FEATURES; j++) {
        matrix[i][j] = (float) featArray[j];
      }
    }
    return TFloat32.tensorOf(StdArrays.ndCopyOf(matrix));
  }

  /**
   * Prepares a 2D float tensor representing the batch labels for training.
   *
   * @param data the list of {@link TrainingData} generated from simulations
   * @return a {@link TFloat32} tensor containing the target labels
   */
  private TFloat32 prepareLabelTensor(final List<TrainingData> data) {
    final float[][] matrix = new float[data.size()][1];
    for (int i = 0; i < data.size(); i++) {
      matrix[i][0] = (float) data.get(i).outcome;
    }
    return TFloat32.tensorOf(StdArrays.ndCopyOf(matrix));
  }

  /**
   * Simulates random games to build a training dataset.
   *
   * @return a list of {@link TrainingData}.
   */
  private List<TrainingData> generateTrainingData(
      final NeuralNetworkEvaluator evaluator, final int numGames) {
    final List<TrainingData> trainingData = new ArrayList<>();
    final Random random = new Random(42);
    for (int game = 0; game < numGames; game++) {
      simulateAndRecordGame(evaluator, trainingData, random);
    }
    Collections.shuffle(trainingData);
    return trainingData;
  }

  /**
   * Simulates a single game and adds the resulting training data.
   *
   * @param trainingData the list to append training samples to
   * @param random the random number generator for exploration
   */
  /* package */ void simulateAndRecordGame(
      final NeuralNetworkEvaluator evaluator,
      final List<TrainingData> trainingData,
      final Random random) {
    final Board board = new Board(9);
    final Player player1 =
        MinimaxPlayer.builder("P1", new Position(0, 4), Color.WHITE)
            .withDepth(1)
            .withReflexion(1)
            .build();
    final Player player2 =
        MinimaxPlayer.builder("P2", new Position(8, 4), Color.BLACK)
            .withDepth(1)
            .withReflexion(1)
            .build();

    player1.setPosition(new Position(random.nextInt(8), random.nextInt(9)));
    player2.setPosition(new Position(random.nextInt(1, 9), random.nextInt(9)));

    player1.setRemainingWalls(random.nextInt(11));
    player2.setRemainingWalls(random.nextInt(11));
    final List<Player> players = List.of(player1, player2);
    final GameState state = new GameState(board, players);

    final List<double[]> p1History = new ArrayList<>();
    final List<double[]> p2History = new ArrayList<>();

    int turn = 0;
    while (turn < 250 && !state.isGameOver()) {
      final float[] features = evaluator.getFeatures(state);
      final double[] featuresD = {features[0], features[1], features[2], features[3]};

      if (state.getCurrentPlayer().equals(player1)) {
        p1History.add(featuresD);
      } else {
        p2History.add(featuresD);
      }

      final Move nextMove = selectExploratoryMove(state, random);
      final Player movingPlayer = state.getCurrentPlayer();
      state.applyMove(nextMove);

      if (state.hasReachedGoal(movingPlayer)) {
        state.setGameOver(true);
      }
      turn++;
    }

    if (state.hasReachedGoal(player1)) {
      p1History.forEach(f -> trainingData.add(new TrainingData(f, 1.0)));
      p2History.forEach(f -> trainingData.add(new TrainingData(f, 0.0)));
    } else if (state.hasReachedGoal(player2)) {
      p1History.forEach(f -> trainingData.add(new TrainingData(f, 0.0)));
      p2History.forEach(f -> trainingData.add(new TrainingData(f, 1.0)));
    }
  }

  /** Selects a move using epsilon-greedy exploration. */
  /* package */ Move selectExploratoryMove(final GameState state, final Random random) {
    final Move selectedMove;

    if (random.nextDouble() < EXPLORATION_PROB) {
      final List<Move> legalMoves = state.generateLegalMoves();
      final int randomIndex = random.nextInt(legalMoves.size());
      selectedMove = legalMoves.get(randomIndex);
    } else {
      final Player curr = state.getCurrentPlayer();
      selectedMove = curr.getNextMove(state);
    }

    return selectedMove;
  }

  /**
   * Internal data structure representing a single training example.
   *
   * @param features The feature array corresponding to a specific game state.
   * @param outcome The observed outcome of the simulated game.
   */
  /* package */ record TrainingData(double[] features, double outcome) {
    /**
     * Constructs a new TrainingData instance.
     *
     * @param features an array of numerical features evaluating the position
     * @param outcome the float outcome representing win/loss
     */
    /* package */ TrainingData {}
  }

  /**
   * Loads the model weights from the provided InputStream.
   *
   * @param evaluator the evaluator instance
   * @param inputStream the stream containing the model weights
   * @throws IOException if an error occurs while reading
   */
  public static void loadModel(
      final NeuralNetworkEvaluator evaluator, final InputStream inputStream) throws IOException {
    try (DataInputStream dis = new DataInputStream(inputStream)) {
      final float[][] wArray = new float[NUM_FEATURES][1];
      for (int i = 0; i < NUM_FEATURES; i++) {
        wArray[i][0] = dis.readFloat();
      }
      final float bVal = dis.readFloat();

      try (TFloat32 wTensor = TFloat32.tensorOf(StdArrays.ndCopyOf(wArray));
          TFloat32 bTensor = TFloat32.tensorOf(StdArrays.ndCopyOf(new float[] {bVal}))) {
        evaluator
            .getSession()
            .runner()
            .feed(evaluator.wPlaceholder, wTensor)
            .feed(evaluator.bPlaceholder, bTensor)
            .addTarget(evaluator.weightsAssign)
            .addTarget(evaluator.biasAssign)
            .run();
      }
    }
  }
}
