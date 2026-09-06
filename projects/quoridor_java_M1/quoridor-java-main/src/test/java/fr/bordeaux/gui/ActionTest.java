package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ActionTest {

  @Test
  void testValues() {
    Action[] actions = Action.values();

    assertEquals(21, actions.length);
    assertArrayEquals(
        new Action[] {
          Action.NEW_GAME,
          Action.LOAD_GAME,
          Action.SAVE_GAME,
          Action.CONFIGURATION,
          Action.INFO,
          Action.QUIT,
          Action.UNDO,
          Action.REDO,
          Action.PAUSE,
          Action.HINT,
          Action.START_SERVER,
          Action.STOP_SERVER,
          Action.SERVER_STATUS,
          Action.SERVER_LIST,
          Action.PLAYERS,
          Action.SCOREBOARD,
          Action.NEW_NETWORK_GAME,
          Action.JOIN_SERVER,
          Action.SET_NETWORK_NAME,
          Action.PING_SERVER,
          Action.QUIT_SERVER
        },
        actions);
  }

  @Test
  void testValueOf() {
    assertEquals(Action.NEW_GAME, Action.valueOf("NEW_GAME"));
    assertEquals(Action.LOAD_GAME, Action.valueOf("LOAD_GAME"));
    assertEquals(Action.SAVE_GAME, Action.valueOf("SAVE_GAME"));
    assertEquals(Action.CONFIGURATION, Action.valueOf("CONFIGURATION"));
    assertEquals(Action.INFO, Action.valueOf("INFO"));
    assertEquals(Action.QUIT, Action.valueOf("QUIT"));
    assertEquals(Action.UNDO, Action.valueOf("UNDO"));
    assertEquals(Action.REDO, Action.valueOf("REDO"));
    assertEquals(Action.PAUSE, Action.valueOf("PAUSE"));
    assertEquals(Action.HINT, Action.valueOf("HINT"));
    assertEquals(Action.START_SERVER, Action.valueOf("START_SERVER"));
    assertEquals(Action.STOP_SERVER, Action.valueOf("STOP_SERVER"));
    assertEquals(Action.SERVER_STATUS, Action.valueOf("SERVER_STATUS"));
    assertEquals(Action.SERVER_LIST, Action.valueOf("SERVER_LIST"));
    assertEquals(Action.PLAYERS, Action.valueOf("PLAYERS"));
    assertEquals(Action.SCOREBOARD, Action.valueOf("SCOREBOARD"));
    assertEquals(Action.NEW_NETWORK_GAME, Action.valueOf("NEW_NETWORK_GAME"));
    assertEquals(Action.JOIN_SERVER, Action.valueOf("JOIN_SERVER"));
    assertEquals(Action.SET_NETWORK_NAME, Action.valueOf("SET_NETWORK_NAME"));
    assertEquals(Action.PING_SERVER, Action.valueOf("PING_SERVER"));
    assertEquals(Action.QUIT_SERVER, Action.valueOf("QUIT_SERVER"));
  }

  @Test
  void testInvalidValueOf() {
    Executable executable = () -> Action.valueOf("INVALID");
    assertThrows(IllegalArgumentException.class, executable);
  }
}
