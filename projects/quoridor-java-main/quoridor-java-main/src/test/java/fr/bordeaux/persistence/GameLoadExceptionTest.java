package fr.bordeaux.persistence;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class GameLoadExceptionTest {

  @Test
  public void testConstructorAndGetters() {
    GameLoadException exception = new GameLoadException("ERREUR_ENTETE", 5, "Entete invalide");

    assertEquals("ERREUR_ENTETE", exception.getErrorType());
    assertEquals(5, exception.getLineNumber());
    assertEquals("Entete invalide", exception.getMessage());
  }
}
