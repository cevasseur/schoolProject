package fr.bordeaux.gui;

import fr.bordeaux.config.ConfigManager;
import fr.bordeaux.i18n.I18n;
import java.io.IOException;
import java.util.Map;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 * Manages keyboard shortcuts. Loads, stores, and persists in config. Triggers actions through an
 * {@link ActionDispatcher}.
 */
public final class ShortcutManager {

  /** Singleton instance of the manager */
  private static final ShortcutManager INSTANCE = new ShortcutManager();

  /** Maps each action to its keyboard shortcut using a thread-safe map */
  private final Map<Action, KeyCombination> shortcuts =
      new java.util.concurrent.ConcurrentHashMap<>();

  /** Dispatcher to trigger actions when shortcuts are pressed */
  private ActionDispatcher dispatcher;

  /** Private constructor. Loads default and saved shortcuts. */
  private ShortcutManager() {
    loadDefaults();
    loadFromConfig();
  }

  /**
   * Returns the unique instance of the shortcut manager.
   *
   * @return the singleton instance of the shortcut manager
   */
  public static ShortcutManager getInstance() {
    return INSTANCE;
  }

  /**
   * Sets action dispatcher for shortcuts.
   *
   * @param dispatcher ActionDispatcher instance
   */
  public void setDispatcher(final ActionDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  /**
   * Retrieves the keyboard shortcut for a given action.
   *
   * @param action The action to query
   * @return The KeyCombination associated with the action
   */
  public KeyCombination getShortcut(final Action action) {
    return shortcuts.get(action);
  }

  /**
   * Updates and persists the shortcut for an action.
   *
   * @param action The action to update
   * @param combination The new KeyCombination
   */
  public void setShortcut(final Action action, final KeyCombination combination) {
    shortcuts.put(action, combination);
    final ConfigManager config = ConfigManager.getInstance();
    config.setOption("shortcut." + action.name(), combination.getName());
    config.save();
  }

  private void loadDefaults() {
    shortcuts.put(Action.NEW_GAME, new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
    shortcuts.put(Action.LOAD_GAME, new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
    shortcuts.put(Action.SAVE_GAME, new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
    shortcuts.put(
        Action.CONFIGURATION, new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN));
    shortcuts.put(Action.INFO, new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN));
    shortcuts.put(Action.QUIT, new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
    shortcuts.put(Action.UNDO, new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN));
    shortcuts.put(Action.REDO, new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
    shortcuts.put(Action.PAUSE, new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN));
    shortcuts.put(Action.HINT, new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
  }

  /** Loads previously saved shortcuts from the ConfigManager. */
  private void loadFromConfig() {
    final ConfigManager config = ConfigManager.getInstance();
    for (final Action action : Action.values()) {
      final String value = config.getOption("shortcut." + action.name(), null);
      if (value != null) {
        try {
          shortcuts.put(action, parseKeyCombination(value));
        } catch (final IllegalArgumentException ignored) {
          // Ignored if invalid
        }
      }
    }
  }

  /**
   * Parses a string (like "Ctrl+Shift+P") into a KeyCodeCombination.
   *
   * @param value The string representation of the shortcut
   * @return A KeyCodeCombination object representing the shortcut
   */
  private KeyCodeCombination parseKeyCombination(final String value) {
    final String[] parts = value.split("\\+");
    final KeyCode code =
        KeyCode.valueOf(parts[parts.length - 1].toUpperCase(java.util.Locale.ENGLISH));

    final java.util.List<KeyCombination.Modifier> modifiers = new java.util.ArrayList<>();

    for (int index = 0; index < parts.length - 1; index++) {
      final String part = parts[index].toUpperCase(java.util.Locale.ENGLISH);
      switch (part) {
        case "CTRL":
          modifiers.add(KeyCombination.CONTROL_DOWN);
          break;
        case "SHIFT":
          modifiers.add(KeyCombination.SHIFT_DOWN);
          break;
        case "ALT":
          modifiers.add(KeyCombination.ALT_DOWN);
          break;
        default:
          break;
      }
    }

    return new KeyCodeCombination(code, modifiers.toArray(new KeyCombination.Modifier[0]));
  }

  /**
   * Registers shortcuts on a JavaFX Scene.
   *
   * @param scene The Scene to register on
   * @throws IllegalStateException if ActionDispatcher is not set
   */
  public void register(final Scene scene) {
    if (dispatcher == null) {
      throw new IllegalStateException(I18n.get("actionDisp"));
    }

    scene.addEventHandler(
        KeyEvent.KEY_PRESSED,
        event -> {
          for (final Map.Entry<Action, KeyCombination> entry : shortcuts.entrySet()) {
            final KeyCombination combination = entry.getValue();
            if (combination.match(event)) {
              try {
                this.dispatcher.dispatch(entry.getKey());
              } catch (final IOException exception) {
                throw new java.io.UncheckedIOException(exception);
              }
              event.consume();
            }
          }
        });
  }
}
