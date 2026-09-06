package fr.bordeaux.gui;

/** Enumerates the actions available in the graphical user interface. */
public enum Action {
  /** Starts a new game. */
  NEW_GAME,

  /** Loads a saved game. */
  LOAD_GAME,

  /** Saves the current game. */
  SAVE_GAME,

  /** Opens the configuration dialog. */
  CONFIGURATION,

  /** Opens the information dialog. */
  INFO,

  /** Quits the application. */
  QUIT,

  /** Undoes the last action. */
  UNDO,

  /** Redoes the last undone action. */
  REDO,

  /** Pauses the current game. */
  PAUSE,

  /** Displays a hint for the current player. */
  HINT,

  /** Starts the network server. */
  START_SERVER,

  /** Stops the network server. */
  STOP_SERVER,

  /** Displays the network server status. */
  SERVER_STATUS,

  /** Discovers available servers on the local network. */
  SERVER_LIST,

  /** Displays the connected players. */
  PLAYERS,

  /** Displays the server scoreboard. */
  SCOREBOARD,

  /** Creates a new network game. */
  NEW_NETWORK_GAME,

  /** Joins a remote network server. */
  JOIN_SERVER,

  /** Sends the local player name to the connected server. */
  SET_NETWORK_NAME,

  /** Sends a ping to the connected server. */
  PING_SERVER,

  /** Disconnects from the connected network server. */
  QUIT_SERVER
}
