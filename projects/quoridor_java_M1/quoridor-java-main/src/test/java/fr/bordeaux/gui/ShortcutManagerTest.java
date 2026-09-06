package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.config.ConfigManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

class ShortcutManagerTest {

  @BeforeAll
  static void initJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
    }
  }

  private Path tempConfigFile;

  @BeforeEach
  void setUp() throws IOException {
    Path tempDir = Files.createTempDirectory("shortcut-test-config");
    tempConfigFile = tempDir.resolve(".quoridorrc");
    System.setProperty("quoridor.config.file", tempConfigFile.toString());
  }

  @AfterEach
  void tearDown() throws IOException {
    System.clearProperty("quoridor.config.file");
    Files.deleteIfExists(tempConfigFile);
  }

  @Test
  void testSingletonAndDefaultsLoaded() {
    ShortcutManager a = ShortcutManager.getInstance();
    ShortcutManager b = ShortcutManager.getInstance();

    assertSame(a, b);

    assertNotNull(a.getShortcut(Action.NEW_GAME));
    assertNotNull(a.getShortcut(Action.LOAD_GAME));
    assertNotNull(a.getShortcut(Action.SAVE_GAME));
    assertNotNull(a.getShortcut(Action.CONFIGURATION));
    assertNotNull(a.getShortcut(Action.INFO));
    assertNotNull(a.getShortcut(Action.QUIT));
    assertNotNull(a.getShortcut(Action.UNDO));
    assertNotNull(a.getShortcut(Action.REDO));
    assertNotNull(a.getShortcut(Action.PAUSE));
    assertNotNull(a.getShortcut(Action.HINT));
  }

  @Test
  void testSetDispatcher() throws Exception {
    ShortcutManager manager = ShortcutManager.getInstance();
    ActionDispatcher dispatcher = mock(ActionDispatcher.class);
    manager.setDispatcher(dispatcher);
    Field field = ShortcutManager.class.getDeclaredField("dispatcher");
    field.setAccessible(true);
    assertSame(dispatcher, field.get(manager));
  }

  @Test
  void testSetShortcutStoresAndSaves() {
    ShortcutManager manager = ShortcutManager.getInstance();
    ConfigManager configMock = mock(ConfigManager.class);
    try (MockedStatic<ConfigManager> mocked = mockStatic(ConfigManager.class)) {
      mocked.when(ConfigManager::getInstance).thenReturn(configMock);
      KeyCombination combo = new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);
      manager.setShortcut(Action.NEW_GAME, combo);
      assertEquals(combo, manager.getShortcut(Action.NEW_GAME));
      verify(configMock).setOption(eq("shortcut.NEW_GAME"), eq(combo.getName()));
      verify(configMock).save();
    }
  }

  @Test
  void testLoadFromConfigValidValue() {
    ConfigManager configMock = mock(ConfigManager.class);
    try (MockedStatic<ConfigManager> mocked = mockStatic(ConfigManager.class)) {
      mocked.when(ConfigManager::getInstance).thenReturn(configMock);
      when(configMock.getOption(anyString(), any())).thenReturn("CTRL+B");
      ShortcutManager manager = ShortcutManager.getInstance();
      assertNotNull(manager.getShortcut(Action.NEW_GAME));
    }
  }

  @Test
  void testLoadFromConfigNullIgnored() {
    ConfigManager configMock = mock(ConfigManager.class);
    try (MockedStatic<ConfigManager> mocked = mockStatic(ConfigManager.class)) {
      mocked.when(ConfigManager::getInstance).thenReturn(configMock);
      when(configMock.getOption(anyString(), any())).thenReturn(null);
      ShortcutManager manager = ShortcutManager.getInstance();
      assertNotNull(manager.getShortcut(Action.NEW_GAME));
    }
  }

  @Test
  void testGetShortcutReturnsDefault() {
    ShortcutManager manager = ShortcutManager.getInstance();

    KeyCombination combo = manager.getShortcut(Action.SAVE_GAME);

    assertNotNull(combo);
  }

  @Test
  void testOverrideShortcut() {
    ShortcutManager manager = ShortcutManager.getInstance();

    KeyCombination first = new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);

    KeyCombination second = new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN);

    manager.setShortcut(Action.NEW_GAME, first);
    manager.setShortcut(Action.NEW_GAME, second);

    assertEquals(second, manager.getShortcut(Action.NEW_GAME));
  }

  private KeyCodeCombination callParse(String value) throws Exception {
    ShortcutManager manager = ShortcutManager.getInstance();

    Method method = ShortcutManager.class.getDeclaredMethod("parseKeyCombination", String.class);
    method.setAccessible(true);

    return (KeyCodeCombination) method.invoke(manager, value);
  }

  @Test
  void testParseNoModifier() throws Exception {
    KeyCodeCombination result = callParse("A");
    assertNotNull(result);
  }

  @Test
  void testParseCtrlOnly() throws Exception {
    KeyCodeCombination result = callParse("CTRL+A");

    assertNotNull(result);
  }

  @Test
  void testParseShiftOnly() throws Exception {
    KeyCodeCombination result = callParse("SHIFT+B");

    assertNotNull(result);
  }

  @Test
  void testParseAltOnly() throws Exception {
    KeyCodeCombination result = callParse("ALT+C");

    assertNotNull(result);
  }

  @Test
  void testParseCtrlShift() throws Exception {
    KeyCodeCombination result = callParse("CTRL+SHIFT+D");

    assertNotNull(result);
  }

  @Test
  void testParseCtrlAlt() throws Exception {
    KeyCodeCombination result = callParse("CTRL+ALT+E");

    assertNotNull(result);
  }

  @Test
  void testParseShiftAlt() throws Exception {
    KeyCodeCombination result = callParse("SHIFT+ALT+F");

    assertNotNull(result);
  }

  @Test
  void testParseCtrlShiftAlt() throws Exception {
    KeyCodeCombination result = callParse("CTRL+SHIFT+ALT+G");

    assertNotNull(result);
  }

  @Test
  void testParseLowerCaseInput() throws Exception {
    KeyCodeCombination result = callParse("ctrl+shift+h");

    assertNotNull(result);
  }

  @Test
  void testRegisterAddsEventHandler() {
    ShortcutManager manager = ShortcutManager.getInstance();

    ActionDispatcher dispatcher = mock(ActionDispatcher.class);
    manager.setDispatcher(dispatcher);

    Scene scene = mock(Scene.class);

    manager.register(scene);

    verify(scene, times(1)).addEventHandler(eq(KeyEvent.KEY_PRESSED), any());
  }

  @Test
  void testRegisterDoesNotDispatchIfNoMatch() throws IOException {
    ShortcutManager manager = ShortcutManager.getInstance();

    ActionDispatcher dispatcher = mock(ActionDispatcher.class);
    manager.setDispatcher(dispatcher);

    KeyCodeCombination combo = new KeyCodeCombination(KeyCode.B, KeyCodeCombination.CONTROL_DOWN);

    manager.setShortcut(Action.NEW_GAME, combo);

    Scene scene = new Scene(new Pane());
    manager.register(scene);

    KeyEvent event =
        new KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            KeyCode.A, // différent → pas de match
            false,
            false,
            false,
            false);

    scene.getRoot().fireEvent(event);

    verify(dispatcher, never()).dispatch(any());
  }

  @Test
  void testRegisterDispatchesActionOnMatch() throws IOException {
    ShortcutManager manager = ShortcutManager.getInstance();

    ActionDispatcher dispatcher = mock(ActionDispatcher.class);
    manager.setDispatcher(dispatcher);

    KeyCodeCombination combo = new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);

    manager.setShortcut(Action.NEW_GAME, combo);

    Scene scene = mock(Scene.class);

    doAnswer(
            invocation -> {
              javafx.event.EventHandler<KeyEvent> handler = invocation.getArgument(1);

              KeyEvent event =
                  new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A, false, false, false, false);

              KeyEvent ctrlEvent =
                  new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A, false, true, false, false);

              handler.handle(ctrlEvent);

              return null;
            })
        .when(scene)
        .addEventHandler(eq(KeyEvent.KEY_PRESSED), any());

    manager.register(scene);

    verify(dispatcher).dispatch(Action.NEW_GAME);
  }
}
