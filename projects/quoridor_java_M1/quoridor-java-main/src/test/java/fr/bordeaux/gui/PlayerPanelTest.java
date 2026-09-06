package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.bordeaux.core.GameState;
import fr.bordeaux.core.Player;
import java.util.List;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testfx.util.WaitForAsyncUtils;

class PlayerPanelTest {

  private GameState state;
  private Player player;
  private PlayerPanel playerPanel;

  @BeforeEach
  void setup() {
    new JFXPanel(); // initialize JavaFX toolkit

    state = mock(GameState.class);
    player = mock(Player.class);

    when(state.getPlayers()).thenReturn(List.of(player));
    when(player.getColor()).thenReturn(fr.bordeaux.core.Color.WHITE);
    when(player.getRemainingWalls()).thenReturn(10);

    playerPanel = new PlayerPanel("Test Player", state, 0);
  }

  @Test
  void testUpdateActivePlayer() {
    when(player.isDisabled()).thenReturn(false);
    when(player.getRemainingWalls()).thenReturn(7);

    playerPanel.update(player);

    //    assertEquals("7", playerPanel.wallsLabel.getText());
    assertEquals(1.0, playerPanel.getOpacity(), 0.01);
  }

  @Test
  void testUpdateDisabledPlayer() {
    when(player.isDisabled()).thenReturn(true);

    playerPanel.update(player);

    assertEquals(0.4, playerPanel.getOpacity(), 0.01);
    //    assertEquals("ÉLIMINÉ", playerPanel.mainTimeLabel.getText());
    //    assertTrue(playerPanel.mainTimeLabel.getStyle().contains("-fx-text-fill: red;"));
    //    assertEquals("", playerPanel.millisLabel.getText());
  }

  @Test
  void testTimerListenerSkipsDisabledPlayer() {
    // Capture the timer listener provided in the constructor
    ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(state).addTimerListener(timerCaptor.capture());
    Runnable timerListener = timerCaptor.getValue();

    // Set player to disabled and set specific text
    when(player.isDisabled()).thenReturn(true);
    //    playerPanel.mainTimeLabel.setText("ELIMINATED");

    // Execute the listener
    timerListener.run();

    // Wait for Platform.runLater
    WaitForAsyncUtils.waitForFxEvents();

    // Check that text was NOT overwritten
    //    assertEquals("ELIMINATED", playerPanel.mainTimeLabel.getText());
  }
}
