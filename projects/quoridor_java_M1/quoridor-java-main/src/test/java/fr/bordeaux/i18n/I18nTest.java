package fr.bordeaux.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

public class I18nTest {

  @Test
  public void testGetEnglish() {
    I18n.setLocale(Locale.ENGLISH);
    assertEquals("Quoridor version", I18n.get("quoridorVersion"));
  }

  @Test
  public void testGetFrench() {
    I18n.setLocale(Locale.FRENCH);
    assertEquals("Version de Quoridor", I18n.get("quoridorVersion"));
  }

  @Test
  public void testDynamicMessage() {
    I18n.setLocale(Locale.ENGLISH);
    String msg = I18n.get("loading", "game.txt");
    assertEquals("Loading game state from file: game.txt", msg);
  }

  @Test
  public void testEnvLocaleDetection() {
    java.util.Map<String, String> env = new java.util.HashMap<>();

    // Test LC_ALL priority
    env.put("LC_ALL", "fr_FR.UTF-8");
    env.put("LANG", "en_US.UTF-8");
    assertEquals(Locale.of("fr", "FR"), I18n.getLocaleFromEnv(env));

    // Test LANG fallback
    env.clear();
    env.put("LANG", "de_DE");
    assertEquals(Locale.of("de", "DE"), I18n.getLocaleFromEnv(env));

    // Test simple language
    env.clear();
    env.put("LANG", "es");
    assertEquals(Locale.of("es"), I18n.getLocaleFromEnv(env));

    // Test empty env fallback
    env.clear();
    assertEquals(Locale.getDefault(), I18n.getLocaleFromEnv(env));
  }
}
