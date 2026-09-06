package fr.bordeaux.gui;

import fr.bordeaux.i18n.I18n;
import java.io.IOException;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/** Represents the main menu bar of the application. */
public class MainMenuBar extends MenuBar {
  /** Dispatcher used to execute selected menu actions. */
  private final ActionDispatcher dispatcher;

  /**
   * Creates the main menu bar and binds its actions to the dispatcher.
   *
   * @param dispatcher dispatcher used to handle menu actions
   */
  public MainMenuBar(final ActionDispatcher dispatcher) {
    this.dispatcher = dispatcher;
    getMenus().addAll(createFileMenu(), createGameMenu(), createNetworkMenu());
  }

  private Menu createFileMenu() {
    final Menu file = new Menu(I18n.get("file"));
    file.getItems()
        .addAll(
            menuItem(I18n.get("newGame"), Action.NEW_GAME),
            menuItem(I18n.get("loadGame"), Action.LOAD_GAME),
            menuItem(I18n.get("saveGame"), Action.SAVE_GAME),
            new SeparatorMenuItem(),
            menuItem("Configuration", Action.CONFIGURATION),
            menuItem("Info", Action.INFO),
            new SeparatorMenuItem(),
            menuItem(I18n.get("menuQuit"), Action.QUIT));
    return file;
  }

  private Menu createGameMenu() {
    final Menu game = new Menu(I18n.get("menuGame"));
    game.getItems()
        .addAll(
            menuItem(I18n.get("menuUndo"), Action.UNDO),
            menuItem(I18n.get("menuRedo"), Action.REDO),
            new SeparatorMenuItem(),
            menuItem("Pause", Action.PAUSE),
            menuItem(I18n.get("menuHint"), Action.HINT));
    return game;
  }

  private Menu createNetworkMenu() {
    final Menu network = new Menu("Network");
    network
        .getItems()
        .addAll(
            menuItem("Start Server", Action.START_SERVER),
            menuItem("Stop Server", Action.STOP_SERVER),
            new SeparatorMenuItem(),
            menuItem("Server List", Action.SERVER_LIST),
            menuItem("Server Status", Action.SERVER_STATUS),
            menuItem("Players", Action.PLAYERS),
            menuItem("Scoreboard", Action.SCOREBOARD),
            menuItem("New Network Game", Action.NEW_NETWORK_GAME),
            new SeparatorMenuItem(),
            menuItem("Join Server", Action.JOIN_SERVER),
            menuItem("Set Player Name", Action.SET_NETWORK_NAME),
            menuItem("Ping Server", Action.PING_SERVER),
            menuItem("Quit Server", Action.QUIT_SERVER));
    return network;
  }

  private MenuItem menuItem(final String text, final Action action) {
    final MenuItem item = new MenuItem(text);
    item.setOnAction(
        event -> {
          try {
            dispatcher.dispatch(action);
          } catch (final IOException e) {
            throw new java.io.UncheckedIOException(e);
          }
        });

    item.setAccelerator(ShortcutManager.getInstance().getShortcut(action));
    return item;
  }
}
