package fr.bordeaux.main;

/** Execution modes: {@link #GUI}, {@link #CLI}, or {@link #NETWORK}. */
public enum AppMode {

  /** Graphical User Interface mode */
  GUI,

  /** Command Line Interface mode */
  CLI,

  /** Network mode (server/client interactions) */
  NETWORK,
}
