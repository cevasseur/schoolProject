package fr.bordeaux.core;

import java.util.*;
import java.util.function.Predicate;

/** Tracks Quoridor game state: board, players, and turn history. */
public class GameState {

  /** The board used for the game. */
  private Board board;

  /** The list of participants. */
  private List<Player> players;

  /** The index of the current active player. */
  private int playerIndex;

  /** Whether the game is paused. */
  private boolean paused;

  /** Whether the game is over. */
  private boolean gameOver;

  /** The stack for undo functionality. */
  private final Deque<Move> undoStack;

  /** The stack for redo functionality. */
  private final Deque<Move> redoStack;

  /** The list of listeners to be notified of state changes. */
  private final transient List<Runnable> listeners = new ArrayList<>();

  /** The list of listeners to be notified of timer changes. */
  private final transient List<Runnable> timerListeners = new ArrayList<>();

  private static final int MIN_PLAYERS = 2;
  private static final int MAX_PLAYERS = 4;

  /**
   * Constructs a GameState with board, players, and initial index.
   *
   * @param board The board used for the game.
   * @param players The list of participants (must be between 2 and 4).
   * @param playerIndex The index of the player who starts.
   * @throws IllegalArgumentException If the number of players is not between 2 and 4.
   */
  public GameState(final Board board, final List<Player> players, final int playerIndex) {
    if (players.size() < MIN_PLAYERS || players.size() > MAX_PLAYERS) {
      throw new IllegalArgumentException(
          CoreServiceProvider.getTranslator().translate("playersMust") + players.size());
    }

    this.board = board;
    this.players = players;
    this.playerIndex = playerIndex;
    this.paused = false;
    this.gameOver = false;
    this.undoStack = new ArrayDeque<>();
    this.redoStack = new ArrayDeque<>();
  }

  /**
   * Constructs a GameState starting with the first player.
   *
   * @param board The board used for the game.
   * @param players The list of participants.
   */
  public GameState(final Board board, final List<Player> players) {
    this(board, players, 0);
  }

  /**
   * Adds a listener to be notified of state changes.
   *
   * @param listener The {@link Runnable} to execute on change.
   */
  public void addListener(final Runnable listener) {
    listeners.add(listener);
  }

  /**
   * Adds a listener to be notified of timer changes.
   *
   * @param listener The {@link Runnable} to execute on change.
   */
  public void addTimerListener(final Runnable listener) {
    timerListeners.add(listener);
  }

  /**
   * Decrements current player's time, checks for game over, and notifies listeners.
   *
   * @param amountInMillis amount to subtract in milliseconds.
   */
  public synchronized void decrementCurrentPlayerTime(final long amountInMillis) {
    if (playerIndex >= players.size()) {
      return;
    }
    final Player target = getCurrentPlayer();
    final long newTime = target.getRemainingTime() - amountInMillis;
    target.setRemainingTime(newTime);

    if (newTime <= 0) {
      target.setDisabled(true);
      int activePlayersCount = 0;
      for (final Player p : players) {
        if (!p.isDisabled()) {
          activePlayersCount++;
        }
      }

      if (activePlayersCount <= 1) {
        setGameOver(true);
      } else {
        nextPlayer();
        notifyListeners();
      }
    }
    notifyTimerListeners();
  }

  /** Notifies all registered listeners of a state change. */
  private void notifyListeners() {
    for (final Runnable listener : listeners) {
      listener.run();
    }
  }

  /** Notifies all registered listeners of a timer change. */
  private void notifyTimerListeners() {
    for (final Runnable listener : timerListeners) {
      listener.run();
    }
  }

  /**
   * Applies a move, updates state/history, and moves to next turn.
   *
   * @param move The {@link Move} to apply.
   */
  public synchronized void applyMove(final Move move) {
    final Player player = getCurrentPlayer();
    if (move.isPawn()) {
      player.setPosition(move.getTo());
    } else {
      board.addWall(move.getTo(), move.getOrientation());
      player.useWall();
    }

    undoStack.push(move);
    redoStack.clear();
    nextPlayer();
    notifyListeners();
  }

  /**
   * Applies a move without clearing the redo stack.
   *
   * @param move The {@link Move} to apply.
   */
  public synchronized void applyMoveWithoutClearingRedo(final Move move) {
    final Player player = getCurrentPlayer();
    if (move.isPawn()) {
      player.setPosition(move.getTo());
    } else {
      board.addWall(move.getTo(), move.getOrientation());
      player.useWall();
    }
    undoStack.push(move);
    nextPlayer();
  }

  /**
   * Retrieves all moves played so far.
   *
   * @return An array of {@link Move} objects representing the history.
   */
  public Move[] getMovesPlayed() {
    final Object[] objects = undoStack.toArray();
    final Move[] moves = new Move[objects.length];
    for (int index = 0; index < objects.length; index++) {
      moves[index] = (Move) objects[index];
    }
    return moves;
  }

  /** Reverts moves until a human player is reached. */
  public synchronized void undo() {
    if (paused || gameOver || undoStack.isEmpty()) {
      return;
    }

    boolean continueUndo;
    do {
      final Move last = undoStack.pop();
      redoStack.push(last);
      previousPlayer();
      final Player player = getCurrentPlayer();
      if (last.isPawn()) {
        player.setPosition(last.getFrom());
      } else {
        board.removeWall(last.getTo(), last.getOrientation());
        player.giveBackWall();
      }
      gameOver = false;
      continueUndo = !undoStack.isEmpty() && getCurrentPlayer().isAI();
    } while (continueUndo);
    notifyListeners();
  }

  /** Re-applies undone moves. Continues if next player is AI. */
  public synchronized void redo() {
    if (paused || gameOver || redoStack.isEmpty()) {
      return;
    }

    boolean continueRedo;
    do {
      final Move move = redoStack.pop();
      applyMoveWithoutClearingRedo(move);
      continueRedo = !redoStack.isEmpty() && getCurrentPlayer().isAI();
    } while (continueRedo);
    notifyListeners();
  }

  /**
   * Checks if the game is over.
   *
   * @return {@code true} if a player has won or game is terminated.
   */
  public boolean isGameOver() {
    return gameOver;
  }

  /**
   * Checks if the specified player has reached their goal.
   *
   * @param player The player to check.
   * @return {@code true} if the player is on a winning cell.
   */
  public boolean hasReachedGoal(final Player player) {
    final Position playerPos = player.getPosition();
    final int cellId = board.getCellId(playerPos.getX(), playerPos.getY());
    final Predicate<Integer> condition = getWinCondition(player);
    return condition.test(cellId);
  }

  /**
   * Returns the win condition predicate for a player.
   *
   * @param player The player whose win condition is needed.
   * @return A {@link Predicate} that returns true if a cell ID is a winning cell.
   */
  public Predicate<Integer> getWinCondition(final Player player) {
    final Position start = player.getStartingPosition();
    final int boardSize = board.getSize();
    final Predicate<Integer> condition;

    if (start.getX() == 0) {
      condition = id -> (id / boardSize) == boardSize - 1;
    } else if (start.getX() == boardSize - 1) {
      condition = id -> (id / boardSize) == 0;
    } else if (start.getY() == 0) {
      condition = id -> (id % boardSize) == boardSize - 1;
    } else {
      condition = id -> (id % boardSize) == 0;
    }
    return condition;
  }

  /**
   * Finds the winner in the current game state.
   *
   * @return an Optional containing the winning player, or empty if none found
   */
  public Optional<Player> getWinner() {
    // First check if someone actually reached their goal
    Optional<Player> winnerInRange = players.stream().filter(this::hasReachedGoal).findFirst();

    // If not, the winner is the last remaining non-disabled player (Blitz mode victory)
    if (winnerInRange.isEmpty()) {
      final List<Player> activePlayers = players.stream().filter(p -> !p.isDisabled()).toList();
      if (activePlayers.size() == 1) {
        winnerInRange = Optional.of(activePlayers.get(0));
      }
    }
    return winnerInRange;
  }

  /** Moves the turn to the next player. */
  public void nextPlayer() {
    final int start = playerIndex;
    do {
      playerIndex = (playerIndex + 1) % players.size();
    } while (players.get(playerIndex).isDisabled() && playerIndex != start);
  }

  /** Moves the turn to the previous player. */
  public void previousPlayer() {
    final int start = playerIndex;
    do {
      playerIndex = (playerIndex - 1 + players.size()) % players.size();
    } while (players.get(playerIndex).isDisabled() && playerIndex != start);
  }

  /**
   * /** Returns the index of the previous player skipping disabled ones.
   *
   * @return The previous player's index.
   */
  public int getPreviousIndexPlayer() {
    int prev = (playerIndex - 1 + players.size()) % players.size();
    while (players.get(prev).isDisabled() && prev != playerIndex) {
      prev = (prev - 1 + players.size()) % players.size();
    }
    return prev;
  }

  /**
   * Gets the player whose turn it is.
   *
   * @return The current {@link Player}.
   */
  public synchronized Player getCurrentPlayer() {
    if (playerIndex >= players.size()) {
      playerIndex = 0;
    }
    return players.get(playerIndex);
  }

  /**
   * Gets the index of the current player.
   *
   * @return The current index.
   */
  public int getCurrentIndexPlayer() {
    return playerIndex;
  }

  /**
   * Gets the game board.
   *
   * @return The {@link Board} instance.
   */
  public Board getBoard() {
    return board;
  }

  /**
   * Gets the list of players.
   *
   * @return The list of {@link Player} instances.
   */
  public List<Player> getPlayers() {
    return players;
  }

  /**
   * Sets the pause state of the game.
   *
   * @param paused {@code true} to pause, {@code false} to resume.
   */
  public void setPaused(final boolean paused) {
    this.paused = paused;
  }

  /**
   * Checks if the game is currently paused.
   *
   * @return {@code true} if paused.
   */
  public boolean isPaused() {
    return paused;
  }

  /**
   * Copies state from another GameState and notifies listeners.
   *
   * @param loaded the game state to copy from
   */
  public synchronized void loadState(final GameState loaded) {
    this.board = loaded.board;
    this.players = loaded.players;
    this.playerIndex = loaded.playerIndex;
    notifyListeners();
  }

  /**
   * Resets the game to a specified state.
   *
   * @param size the board size
   * @param newPlayers the list of players
   * @param blitzTimeoutMs the blitz timeout in milliseconds, or null if not blitz
   */
  public synchronized void reset(
      final int size, final List<Player> newPlayers, final Long blitzTimeoutMs) {
    this.board = new Board(size);
    this.players = newPlayers;
    this.playerIndex = 0;
    if (blitzTimeoutMs != null) {
      setLimitTimePlayer(blitzTimeoutMs);
    }
    this.paused = false;
    this.gameOver = false;
    this.undoStack.clear();
    this.redoStack.clear();
    notifyListeners();
  }

  /**
   * Creates a deep copy of the current state (Board and Players).
   *
   * @return A cloned {@link GameState}.
   */
  public GameState copy() {
    final Board boardCopy = this.board.copy();
    final List<Player> playersCopy = new ArrayList<>();
    for (final Player p : this.players) {
      playersCopy.add(p.copy());
    }

    final GameState copy = new GameState(boardCopy, playersCopy, this.playerIndex);

    copy.gameOver = this.gameOver;
    copy.paused = this.paused;
    return copy;
  }

  /**
   * Generates all legal moves for the current player.
   *
   * @return A list of legal {@link Move} objects.
   */
  public List<Move> generateLegalMoves() {
    final Player current = getCurrentPlayer();
    final List<Move> legalMoves = new ArrayList<>(generateLegalPawnMoves(current));
    final List<Move> wallMovesCandidate = generatePseudoLegalWallMoves(current);
    for (final Move wallMove : wallMovesCandidate) {
      if (RuleChecker.isMoveLegal(this, wallMove)) {
        legalMoves.add(wallMove);
      }
    }
    return legalMoves;
  }

  /**
   * Generates all potential move candidates without rule validation.
   *
   * @return A list of pseudo-legal {@link Move} objects.
   */
  public List<Move> generatePseudoLegalMoves() {
    final List<Move> allPossibleMoves =
        new ArrayList<>(generatePseudoLegalPawnMoves(getCurrentPlayer()));
    allPossibleMoves.addAll(generatePseudoLegalWallMoves(getCurrentPlayer()));
    return allPossibleMoves;
  }

  /**
   * Generates pawn move candidates without rule validation.
   *
   * @param player The player to generate moves for.
   * @return A list of pseudo-legal pawn {@link Move} objects.
   */
  public List<Move> generatePseudoLegalPawnMoves(final Player player) {
    final List<Move> pawnMoves = new ArrayList<>();
    final Position pos = player.getPosition();
    final int posX = pos.getX();
    final int posY = pos.getY();

    // 4 Cardinal directions (distance 1)
    pawnMoves.add(Move.pawn(pos, new Position(posX + 1, posY)));
    pawnMoves.add(Move.pawn(pos, new Position(posX - 1, posY)));
    pawnMoves.add(Move.pawn(pos, new Position(posX, posY + 1)));
    pawnMoves.add(Move.pawn(pos, new Position(posX, posY - 1)));

    // 4 Jumps (distance 2)
    pawnMoves.add(Move.pawn(pos, new Position(posX + 2, posY)));
    pawnMoves.add(Move.pawn(pos, new Position(posX - 2, posY)));
    pawnMoves.add(Move.pawn(pos, new Position(posX, posY + 2)));
    pawnMoves.add(Move.pawn(pos, new Position(posX, posY - 2)));

    // 4 Diagonals
    pawnMoves.add(Move.pawn(pos, new Position(posX + 1, posY + 1)));
    pawnMoves.add(Move.pawn(pos, new Position(posX - 1, posY - 1)));
    pawnMoves.add(Move.pawn(pos, new Position(posX + 1, posY - 1)));
    pawnMoves.add(Move.pawn(pos, new Position(posX - 1, posY + 1)));

    return pawnMoves;
  }

  /**
   * Generates all potential wall placement candidates for a player.
   *
   * @param player The player to generate wall moves for.
   * @return A list of pseudo-legal wall {@link Move} objects.
   */
  public List<Move> generatePseudoLegalWallMoves(final Player player) {
    final List<Move> wallMoves = new ArrayList<>();
    if (player.getRemainingWalls() > 0) {
      final int boardSize = board.getSize();
      for (int posX = 0; posX < boardSize - 1; posX++) {
        for (int posY = 0; posY < boardSize - 1; posY++) {
          final Position pos = new Position(posX, posY);
          wallMoves.add(Move.wall(pos, Orientation.HORIZONTAL));
          wallMoves.add(Move.wall(pos, Orientation.VERTICAL));
        }
      }
    }
    return wallMoves;
  }

  /**
   * Generates only legal pawn movements for a specific player.
   *
   * @param player The player to check.
   * @return A list of legal pawn {@link Move} objects.
   */
  public List<Move> generateLegalPawnMoves(final Player player) {
    final List<Move> legalPawnMoves = new ArrayList<>();
    for (final Move m : generatePseudoLegalPawnMoves(player)) {
      if (RuleChecker.isMoveLegal(this, m)) {
        legalPawnMoves.add(m);
      }
    }
    return legalPawnMoves;
  }

  /**
   * Gets only legal pawn movements for a specific player.
   *
   * @param player The player to check.
   * @return A list of legal target {@link Position}s.
   */
  public List<Position> getLegalMoves(final Player player) {
    final List<Position> positions = new ArrayList<>();
    final List<Move> legalPawnMoves = generateLegalPawnMoves(player);
    for (final Move move : legalPawnMoves) {
      positions.add(move.getTo());
    }
    return positions;
  }

  /**
   * Initializes reflection time for all players.
   *
   * @param timeLimit The initial time allowed for each player in milliseconds.
   */
  public void setLimitTimePlayer(final long timeLimit) {
    for (final Player player : getPlayers()) {
      player.setRemainingTime(timeLimit);
    }
  }

  /**
   * Populates the history stack without applying moves.
   *
   * @param history The list of {@link Move} objects to load into the history stack.
   */
  public void loadHistoryWithoutApplying(final List<Move> history) {
    this.undoStack.clear();
    for (final Move m : history) {
      this.undoStack.push(m);
    }
  }

  /**
   * Sets the game over status.
   *
   * @param over {@code true} to end the game.
   */
  public void setGameOver(final boolean over) {
    this.gameOver = over;
    notifyListeners();
  }

  /**
   * Sets the game over status without notifying listeners.
   *
   * @param over {@code true} to end the game.
   */
  public void setGameOverSilent(final boolean over) {
    this.gameOver = over;
  }
}
