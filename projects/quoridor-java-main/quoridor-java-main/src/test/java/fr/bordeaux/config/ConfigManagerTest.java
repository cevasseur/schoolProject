package fr.bordeaux.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.*;

public class ConfigManagerTest {

  private Path configFile;

  @BeforeEach
  void setup() throws IOException {
    Path tempDir = Files.createTempDirectory("test-config");
    configFile = tempDir.resolve(".quoridorrc");
    System.setProperty("quoridor.config.file", configFile.toString());
    resetSingleton();
    deleteConfigFile();
  }

  private void resetSingleton() {
    try {
      var field = ConfigManager.class.getDeclaredField("instance");
      field.setAccessible(true);
      field.set(null, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testLoadCreatesDefaultFile() {
    ConfigManager config = ConfigManager.getInstance();
    config.load();

    assertTrue(Files.exists(configFile));

    assertEquals("true", config.getOption("verbose", "false"));
    assertEquals("false", config.getOption("blitz", "true"));
    // assertEquals("30", config.getOption("timeout", "0"));
  }

  private void deleteConfigFile() throws IOException {
    if (configFile != null) {
      Files.deleteIfExists(configFile);
    }
  }

  @Test
  void testSaveAndReload() {
    ConfigManager config = ConfigManager.getInstance();
    config.load();

    config.setOption("timeout", "60");
    config.save();

    resetSingleton();

    ConfigManager config2 = ConfigManager.getInstance();
    config2.load();

    assertEquals(60, config2.getInt("timeout", 0));
  }

  @Test
  void testGetBoolean() {
    ConfigManager config = ConfigManager.getInstance();
    config.load();

    config.setOption("blitz", "true");

    assertTrue(config.getBoolean("blitz", false));
  }

  @Test
  void testGetIntInvalid() {
    ConfigManager config = ConfigManager.getInstance();
    config.load();

    config.setOption("timeout", "abc");

    assertEquals(42, config.getInt("timeout", 42));
  }

  @Test
  void testLoadExistingFile() {
    ConfigManager config = ConfigManager.getInstance();
    config.load();

    config.setOption("timeout", "99");
    config.save();

    resetSingleton();

    ConfigManager config2 = ConfigManager.getInstance();
    config2.load();

    assertEquals(99, config2.getInt("timeout", 0));
  }

  @Test
  void testLoadCorruptedFile() throws IOException {
    Files.writeString(configFile, "INVALID_CONTENT");

    ConfigManager config = ConfigManager.getInstance();
    config.load();

    assertEquals("true", config.getOption("verbose", "true"));
  }

  @Test
  void testFileDeletedBeforeLoad() throws IOException {
    Files.deleteIfExists(configFile);

    ConfigManager config = ConfigManager.getInstance();
    config.load();

    assertTrue(Files.exists(configFile));
  }

  @Test
  void testSetAndGetOption() {
    ConfigManager config = ConfigManager.getInstance();
    config.load();

    config.setOption("key", "value");

    assertEquals("value", config.getOption("key", "default"));
  }

  @Test
  void testSingleton() {
    ConfigManager a = ConfigManager.getInstance();
    ConfigManager b = ConfigManager.getInstance();

    assertSame(a, b);
  }
}
