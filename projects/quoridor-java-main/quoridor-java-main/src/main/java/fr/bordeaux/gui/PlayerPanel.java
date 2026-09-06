package fr.bordeaux.gui;

import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Player;
import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.TimeConvertor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Display panel for a player's stats (name, time, walls, score). Updates automatically when its
 * {@link GameState} changes.
 */
public final class PlayerPanel extends VBox {
  /** Label displaying the player's name. */
  private final Label nameLabel;

  /** Label displaying the number of walls remaining for the player. */
  private final Label wallsLabel;

  /** Label displaying main part of time. */
  private final Label mainTimeLabel;

  /** Label displaying milliseconds. */
  private final Label millisLabel;

  /**
   * Constructs panel for player.
   *
   * @param playerName player name
   * @param state game state
   * @param playerIndex player index
   */
  public PlayerPanel(String playerName, GameState state, int playerIndex) {

    final Player player = state.getPlayers().get(playerIndex);
    final String accentColor =
        switch (player.getColor()) {
          case WHITE -> "#95a5a6";
          case BLACK -> "#2c3e50";
          case BLUE -> "#3498db";
          case RED -> "#e74c3c";
        };

    final Region topBar = new Region();
    topBar.setPrefHeight(6);
    topBar.setMaxWidth(Double.MAX_VALUE);
    topBar.setStyle("-fx-background-color: " + accentColor + ";");

    nameLabel = new Label(playerName);
    nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #222222;");

    final Region separator = makeSeparator();

    mainTimeLabel = new Label("00:00");
    mainTimeLabel.setStyle(
        "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

    millisLabel = new Label("");
    millisLabel.setStyle(
        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
            + accentColor
            + "; -fx-opacity: 0.7;");

    final HBox timeContainer = new HBox(0, mainTimeLabel, millisLabel);
    timeContainer.setAlignment(Pos.BASELINE_CENTER);

    final VBox timeBox = makeStatBox(I18n.get("timer"), timeContainer);
    // ---------------------

    final Region separator2 = makeSeparator();

    wallsLabel = new Label(String.valueOf(player.getRemainingWalls()));
    wallsLabel.setStyle(
        "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");
    final VBox wallsBox = makeStatBox(I18n.get("walls"), wallsLabel);

    final VBox content = new VBox(16, nameLabel, separator, timeBox, separator2, wallsBox);
    content.setPadding(new Insets(15));
    content.setAlignment(Pos.TOP_CENTER);
    setVgrow(content, Priority.ALWAYS);

    getChildren().addAll(topBar, content);

    setStyle(
        "-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 6; "
            + "-fx-background-color: #f9f9f9; -fx-background-radius: 6; "
            + "-fx-min-width: 180; -fx-max-width: 250; -fx-pref-width: 200;");

    state.addTimerListener(
        () ->
            Platform.runLater(
                () -> {
                  final Player targetPlayer = state.getPlayers().get(playerIndex);
                  final String[] times =
                      TimeConvertor.formatTime(targetPlayer.getRemainingTime()).split(":");

                  if (times.length > 2) {

                    mainTimeLabel.setText(times[0] + ":" + times[1]);
                    millisLabel.setText(":" + times[2]);
                  } else {

                    mainTimeLabel.setText(times[0] + ":" + times[1]);
                    millisLabel.setText("");
                  }
                }));

    state.addListener(() -> Platform.runLater(() -> update(state.getPlayers().get(playerIndex))));
  }

  /**
   * Creates a vertical block with a title and a centered value.
   *
   * @param title The title of the stat (e.g., "Score").
   * @param valueNode The JavaFX component that displays the value .
   * @return A {@link VBox} containing the title and value.
   */
  private VBox makeStatBox(final String title, final Node valueNode) {
    final Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

    final VBox box = new VBox(4, titleLabel, valueNode);
    box.setAlignment(Pos.CENTER);
    return box;
  }

  /**
   * Creates a thin horizontal separator to visually separate sections of the panel.
   *
   * @return A {@link Region} representing the separator.
   */
  private Region makeSeparator() {
    final Region sep = new Region();
    sep.setPrefHeight(1);
    sep.setMaxWidth(Double.MAX_VALUE);
    sep.setStyle("-fx-background-color: #e0e0e0;");
    return sep;
  }

  /**
   * Updates the panel's information based on the current state of the player.
   *
   * @param player The player whose information should be displayed.
   */
  public void update(final Player player) {
    if (player.isDisabled()) {
      this.setOpacity(0.4);
      mainTimeLabel.setText("ÉLIMINÉ");
      mainTimeLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 20px;");
      millisLabel.setText("");
    } else {
      wallsLabel.setText(String.valueOf(player.getRemainingWalls()));
    }
  }
}
