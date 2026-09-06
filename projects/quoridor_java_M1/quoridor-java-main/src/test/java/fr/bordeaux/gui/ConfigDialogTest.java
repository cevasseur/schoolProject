package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.i18n.I18n;
import java.util.Optional;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationTest;

class ConfigDialogTest extends ApplicationTest {

  @Override
  public void start(Stage stage) {}

  @Test
  void testHandleKeyPress_modifierKey_returnsEarly() throws Exception {
    KeyEvent event = mock(KeyEvent.class);

    when(event.getCode()).thenReturn(KeyCode.CONTROL);

    Button button = new Button();

    var method =
        ConfigDialog.class.getDeclaredMethod(
            "handleKeyPress", KeyEvent.class, Action.class, Button.class);
    method.setAccessible(true);

    try (MockedStatic<ShortcutManager> mock = mockStatic(ShortcutManager.class)) {

      ShortcutManager manager = mock(ShortcutManager.class);
      when(ShortcutManager.getInstance()).thenReturn(manager);

      method.invoke(null, event, Action.values()[0], button);

      verify(manager, never()).setShortcut(any(), any());
    }
  }

  @Test
  void testHandleKeyPress_validKey_updatesShortcut() throws Exception {
    KeyEvent event = mock(KeyEvent.class);

    when(event.getCode()).thenReturn(KeyCode.A);
    when(event.isControlDown()).thenReturn(true);
    when(event.isShiftDown()).thenReturn(false);
    when(event.isAltDown()).thenReturn(false);

    Button button = new Button();

    var method =
        ConfigDialog.class.getDeclaredMethod(
            "handleKeyPress", KeyEvent.class, Action.class, Button.class);
    method.setAccessible(true);

    try (MockedStatic<ShortcutManager> mock = mockStatic(ShortcutManager.class)) {

      ShortcutManager manager = mock(ShortcutManager.class);
      when(ShortcutManager.getInstance()).thenReturn(manager);

      method.invoke(null, event, Action.values()[0], button);

      verify(manager, times(1)).setShortcut(eq(Action.values()[0]), any());
      assertNotNull(button.getText());
    }
  }

  @Test
  void testIsModifierKey() throws Exception {
    var method = ConfigDialog.class.getDeclaredMethod("isModifierKey", KeyCode.class);
    method.setAccessible(true);

    assertTrue((boolean) method.invoke(null, KeyCode.CONTROL));
    assertTrue((boolean) method.invoke(null, KeyCode.SHIFT));
    assertTrue((boolean) method.invoke(null, KeyCode.ALT));

    assertFalse((boolean) method.invoke(null, KeyCode.A));
  }

  @Test
  void testExtractModifiers() throws Exception {
    var method = ConfigDialog.class.getDeclaredMethod("extractModifiers", KeyEvent.class);
    method.setAccessible(true);

    // Test Control + Shift
    KeyEvent event1 = mock(KeyEvent.class);
    when(event1.isControlDown()).thenReturn(true);
    when(event1.isShiftDown()).thenReturn(true);
    when(event1.isAltDown()).thenReturn(false);
    KeyCodeCombination.Modifier[] mods1 =
        (KeyCodeCombination.Modifier[]) method.invoke(null, event1);
    assertEquals(2, mods1.length);

    // Test Alt only
    KeyEvent event2 = mock(KeyEvent.class);
    when(event2.isControlDown()).thenReturn(false);
    when(event2.isShiftDown()).thenReturn(false);
    when(event2.isAltDown()).thenReturn(true);
    KeyCodeCombination.Modifier[] mods2 =
        (KeyCodeCombination.Modifier[]) method.invoke(null, event2);
    assertEquals(1, mods2.length);

    // Test No Modifiers
    KeyEvent event3 = mock(KeyEvent.class);
    when(event3.isControlDown()).thenReturn(false);
    when(event3.isShiftDown()).thenReturn(false);
    when(event3.isAltDown()).thenReturn(false);
    KeyCodeCombination.Modifier[] mods3 =
        (KeyCodeCombination.Modifier[]) method.invoke(null, event3);
    assertEquals(0, mods3.length);
  }

  @Test
  void testShow() {
    interact(ConfigDialog::show);

    Optional<Window> stageOpt =
        Window.getWindows().stream()
            .filter(window -> window instanceof Stage)
            .filter(window -> I18n.get("shortcut").equals(((Stage) window).getTitle()))
            .findFirst();

    assertTrue(stageOpt.isPresent(), "ConfigDialog stage should be opened");
    Stage stage = (Stage) stageOpt.get();
    assertTrue(stage.isShowing());

    interact(stage::close);
  }
}
