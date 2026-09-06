package fr.bordeaux.gui;

import fr.bordeaux.i18n.I18n;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/** Utility class used to display the shortcut configuration dialog. */
public final class ConfigDialog {

  private ConfigDialog() {}

  /** Shows the configuration dialog for keyboard shortcuts. */
  public static void show() {

    final Stage stage = new Stage();
    stage.setTitle(I18n.get("shortcut"));
    stage.setResizable(true);

    final GridPane grid = new GridPane();
    grid.setVgap(10);
    grid.setHgap(10);

    int row = 0;

    for (final Action action : Action.values()) {
      final Label actionLabel = new Label(action.name());

      final KeyCombination currentShortcut = ShortcutManager.getInstance().getShortcut(action);
      final String shortcutName = currentShortcut != null ? currentShortcut.getName() : "None";

      final Button keyButton = new Button(shortcutName);

      keyButton.setFocusTraversable(true);

      keyButton.setOnKeyPressed(ke -> handleKeyPress(ke, action, keyButton));

      grid.add(actionLabel, 0, row);
      grid.add(keyButton, 1, row);
      row++;
    }

    final Button saveButton = new Button(I18n.get("saveButton"));
    saveButton.setOnAction(e -> stage.close());

    final HBox saveBox = new HBox(saveButton);
    saveBox.setSpacing(10);

    grid.add(saveBox, 0, row, 2, 1);

    final Scene scene = new Scene(grid, 400, 50 + row * 40);
    stage.setScene(scene);
    stage.show();
  }

  /** Handles keyboard shortcut editing. */
  private static void handleKeyPress(
      final KeyEvent ke, final Action action, final Button keyButton) {

    final KeyCode code = ke.getCode();

    // Ignore modifier-only keys
    if (isModifierKey(code)) {
      return;
    }

    final KeyCombination.Modifier[] modifiers = extractModifiers(ke);

    final KeyCodeCombination newShortcut = new KeyCodeCombination(code, modifiers);

    ShortcutManager.getInstance().setShortcut(action, newShortcut);

    keyButton.setText(newShortcut.getName());
  }

  /** Returns true if the key is only a modifier key. */
  private static boolean isModifierKey(final KeyCode code) {
    return code == KeyCode.CONTROL || code == KeyCode.SHIFT || code == KeyCode.ALT;
  }

  /** Extracts active modifiers from the key event. */
  private static KeyCombination.Modifier[] extractModifiers(final KeyEvent ke) {
    final List<KeyCombination.Modifier> mods = new ArrayList<>();

    if (ke.isControlDown()) {
      mods.add(KeyCombination.CONTROL_DOWN);
    }
    if (ke.isShiftDown()) {
      mods.add(KeyCombination.SHIFT_DOWN);
    }
    if (ke.isAltDown()) {
      mods.add(KeyCombination.ALT_DOWN);
    }

    return mods.toArray(new KeyCombination.Modifier[0]);
  }
}
