package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.i18n.I18n;
import java.io.IOException;
import java.util.Objects;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MainMenuBarTest {
  @BeforeAll
  static void initJfx() {
    new JFXPanel();
  }

  @Test
  void testMenusAreCreated() {
    ActionDispatcher dispatcher = mock(ActionDispatcher.class);

    MainMenuBar menuBar = new MainMenuBar(dispatcher);

    assertEquals(3, menuBar.getMenus().size());
    assertEquals(I18n.get("file"), menuBar.getMenus().get(0).getText());
    assertEquals(I18n.get("menuGame"), menuBar.getMenus().get(1).getText());
    assertEquals("Network", menuBar.getMenus().get(2).getText());
  }

  @Test
  void testFileMenuItemsExist() {
    ActionDispatcher dispatcher = mock(ActionDispatcher.class);

    MainMenuBar menuBar = new MainMenuBar(dispatcher);

    Menu file = menuBar.getMenus().get(0);

    assertTrue(file.getItems().stream().anyMatch(Objects::nonNull));

    assertTrue(
        file.getItems().stream().anyMatch(item -> I18n.get("newGame").equals(item.getText())));
    assertTrue(
        file.getItems().stream().anyMatch(item -> I18n.get("loadGame").equals(item.getText())));
    assertTrue(
        file.getItems().stream().anyMatch(item -> I18n.get("saveGame").equals(item.getText())));
    assertTrue(
        file.getItems().stream().anyMatch(item -> I18n.get("menuQuit").equals(item.getText())));
  }

  @Test
  void testGameMenuItemsExist() {
    ActionDispatcher dispatcher = mock(ActionDispatcher.class);

    MainMenuBar menuBar = new MainMenuBar(dispatcher);

    Menu game = menuBar.getMenus().get(1);

    assertTrue(
        game.getItems().stream().anyMatch(item -> I18n.get("menuUndo").equals(item.getText())));
    assertTrue(
        game.getItems().stream().anyMatch(item -> I18n.get("menuRedo").equals(item.getText())));
    assertTrue(game.getItems().stream().anyMatch(item -> "Pause".equals(item.getText())));
    assertTrue(
        game.getItems().stream().anyMatch(item -> I18n.get("menuHint").equals(item.getText())));
  }

  @Test
  void testMenuItemDispatchAction() throws IOException {
    ActionDispatcher dispatcher = mock(ActionDispatcher.class);

    MainMenuBar menuBar = new MainMenuBar(dispatcher);

    Menu file = menuBar.getMenus().get(0);

    MenuItem newGameItem =
        file.getItems().stream()
            .filter(i -> I18n.get("newGame").equals(i.getText()))
            .findFirst()
            .orElseThrow();

    newGameItem.getOnAction().handle(null);

    verify(dispatcher).dispatch(Action.NEW_GAME);
  }

  @Test
  void testNetworkMenuItemsExist() {
    ActionDispatcher dispatcher = mock(ActionDispatcher.class);

    MainMenuBar menuBar = new MainMenuBar(dispatcher);

    Menu network = menuBar.getMenus().get(2);

    assertTrue(network.getItems().stream().anyMatch(item -> "Start Server".equals(item.getText())));
    assertTrue(network.getItems().stream().anyMatch(item -> "Stop Server".equals(item.getText())));
    assertTrue(network.getItems().stream().anyMatch(item -> "Server List".equals(item.getText())));
    assertTrue(
        network.getItems().stream().anyMatch(item -> "Server Status".equals(item.getText())));
    assertTrue(network.getItems().stream().anyMatch(item -> "Players".equals(item.getText())));
    assertTrue(network.getItems().stream().anyMatch(item -> "Scoreboard".equals(item.getText())));
    assertTrue(
        network.getItems().stream().anyMatch(item -> "New Network Game".equals(item.getText())));
  }

  @Test
  void testQuitDispatch() throws IOException {
    ActionDispatcher dispatcher = mock(ActionDispatcher.class);

    MainMenuBar menuBar = new MainMenuBar(dispatcher);

    Menu file = menuBar.getMenus().getFirst();

    MenuItem quit =
        file.getItems().stream()
            .filter(i -> I18n.get("menuQuit").equals(i.getText()))
            .findFirst()
            .orElseThrow();

    quit.getOnAction().handle(null);

    verify(dispatcher).dispatch(Action.QUIT);
  }
}
