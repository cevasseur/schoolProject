package fr.bordeaux.config;

import fr.bordeaux.i18n.I18n;
import fr.bordeaux.util.Logger;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Singleton managed configuration for loading and saving settings. */
public final class ConfigManager {

  /** Logger used while loading and saving configuration values. */
  private static final Logger LOGGER = Logger.getInstance();

  /** Configuration properties and shortcuts. */
  private final Properties props = new Properties();

  /** Singleton instance of the configuration manager. */
  private static ConfigManager instance;

  /** Private constructor for singleton pattern */
  private ConfigManager() {}

  /**
   * Returns the singleton instance of ConfigManager. Lazily initializes it on first access.
   *
   * @return the unique {@link ConfigManager} instance
   */
  public static ConfigManager getInstance() {
    synchronized (ConfigManager.class) {
      if (instance == null) {
        instance = new ConfigManager();
      }
    }
    return instance;
  }

  /**
   * Returns the dynamic path to the config file
   *
   * @return Path to the config file
   */
  private Path getFilePath() {
    final String customPath = System.getProperty("quoridor.config.file");
    final Path filePath;
    if (customPath != null) {
      filePath = Paths.get(customPath);
    } else {
      filePath = Paths.get(System.getProperty("user.home"), ".quoridorrc");
    }
    return filePath;
  }

  /** Loads config if it exists. Otherwise saves defaults. */
  public void load() {
    props.clear();
    final Path path = getFilePath();
    if (Files.exists(path)) {

      try (InputStream inputStream = Files.newInputStream(path)) {

        props.load(inputStream);

      } catch (final IOException e) {
        if (LOGGER.isWarnEnabled()) {
          final String msg = I18n.get("warning");
          LOGGER.warning(msg);
        }
      }
    } else {
      props.setProperty("verbose", "true");
      props.setProperty("blitz", "false");
      props.setProperty("timeout", "30");
      props.setProperty("debug", "true");
      props.setProperty("ai", "false");
      props.setProperty("nb-walls", "20");
      props.setProperty("board-size", "9");
      props.setProperty("contest", "false");
      props.setProperty("contest-file", "");
      save();
    }
  }

  /** Saves the current properties to the configuration file. */
  public void save() {
    try (OutputStream out = Files.newOutputStream(getFilePath())) {

      props.store(out, "Quoridor configuration");

    } catch (final IOException e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("{0}{1}", I18n.get("configError"), e);
      }
    }
  }

  /**
   * Retrieves a configuration option as a String.
   *
   * @param key The property key.
   * @param defaultValue The value to return if the key is not found.
   * @return The property value or the default value.
   */
  public String getOption(final String key, final String defaultValue) {
    return props.getProperty(key, defaultValue);
  }

  /**
   * Sets a configuration option.
   *
   * @param key The property key.
   * @param value The value to associate with the key.
   */
  public void setOption(final String key, final String value) {
    props.setProperty(key, value);
  }

  /**
   * Removes a configuration option.
   *
   * @param key The property key to remove.
   */
  public void removeOption(final String key) {
    props.remove(key);
  }

  /**
   * Retrieves a configuration option as a boolean.
   *
   * @param key The property key.
   * @param defaultValue The value to return if the key is not found or invalid.
   * @return The boolean value of the property.
   */
  public boolean getBoolean(final String key, final boolean defaultValue) {
    return Boolean.parseBoolean(props.getProperty(key, String.valueOf(defaultValue)));
  }

  /**
   * Retrieves a configuration option as an integer.
   *
   * @param key property key
   * @param defaultValue default value
   * @return integer value of property
   */
  public int getInt(final String key, final int defaultValue) {
    int value = defaultValue;
    try {
      final String property = props.getProperty(key);
      if (property != null) {
        value = Integer.parseInt(property);
      }
    } catch (final NumberFormatException e) {
      value = defaultValue;
    }
    return value;
  }

  /**
   * Returns {@code true} when the application was launched with {@code --contest}.
   *
   * @return whether contest mode is active
   */
  public boolean isContestMode() {
    return getBoolean("contest", false);
  }

  /**
   * Returns the path of the position file.
   *
   * @return contest file path, or ""
   */
  public String getContestFile() {
    return getOption("contest-file", "");
  }

  /**
   * Enables contest mode and stores the associated position-file path.
   *
   * @param filePath absolute or relative path to the position file
   */
  public void enableContest(final String filePath) {
    setOption("contest", "true");
    setOption("contest-file", filePath);
  }
}
