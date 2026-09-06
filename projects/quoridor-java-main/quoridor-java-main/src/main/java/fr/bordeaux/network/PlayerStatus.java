package fr.bordeaux.network;

/** Represents the current state of a connected player. */
public enum PlayerStatus {
  /** Player is connected but not currently in a game. */
  IDLE,

  /** Player is currently playing a game. */
  INGAME
}
