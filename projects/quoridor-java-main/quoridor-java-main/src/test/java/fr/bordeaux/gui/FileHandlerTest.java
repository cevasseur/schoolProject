package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.File;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

/**
 * Unit tests for the {@link FileHandler} class.
 *
 * <p>These tests use Mockito's construction mocking to intercept internal JavaFX {@link
 * FileChooser} instantiation.
 */
class FileHandlerTest {

  /** Constant for a dummy file path used in tests. */
  private static final String TEST_PATH = "/path/to/test.txt";

  /** Mock of the JavaFX stage. */
  private Stage stage;

  /** The instance of the class under test. */
  private FileHandler fileHandler;

  /** Sets up the test environment before each test. */
  @BeforeEach
  void setUp() {
    this.stage = mock(Stage.class);
    this.fileHandler = new FileHandler(this.stage);
  }

  /**
   * Helper method to configure the FileChooser mock to avoid NullPointerExceptions on
   * getExtensionFilters().
   *
   * @param mock the FileChooser mock to configure
   */
  private void configureMock(FileChooser mock) {
    when(mock.getExtensionFilters()).thenReturn(mock(ObservableList.class));
  }

  /** Tests that chooseSaveFile returns the absolute path when a file is selected. */
  @Test
  void testChooseSaveFileWithSelection() {
    final File mockFile = mock(File.class);
    when(mockFile.getAbsolutePath()).thenReturn(TEST_PATH);

    try (MockedConstruction<FileChooser> ignored =
        mockConstruction(
            FileChooser.class,
            (mock, context) -> {
              configureMock(mock);
              when(mock.showSaveDialog(this.stage)).thenReturn(mockFile);
            })) {

      final String result = this.fileHandler.chooseSaveFile();
      assertEquals(TEST_PATH, result, "The path should match the selected file path.");
    }
  }

  /** Tests that chooseSaveFile returns null when the dialog is cancelled. */
  @Test
  void testChooseSaveFileCancelled() {
    try (MockedConstruction<FileChooser> ignored =
        mockConstruction(
            FileChooser.class,
            (mock, context) -> {
              configureMock(mock);
              when(mock.showSaveDialog(this.stage)).thenReturn(null);
            })) {

      final String result = this.fileHandler.chooseSaveFile();
      assertNull(result, "The result should be null when the user cancels the dialog.");
    }
  }

  /** Tests that chooseLoadFile returns the absolute path when a file is selected. */
  @Test
  void testChooseLoadFileWithSelection() {
    final File mockFile = mock(File.class);
    when(mockFile.getAbsolutePath()).thenReturn(TEST_PATH);

    try (MockedConstruction<FileChooser> ignored =
        mockConstruction(
            FileChooser.class,
            (mock, context) -> {
              configureMock(mock);
              when(mock.showOpenDialog(this.stage)).thenReturn(mockFile);
            })) {

      final String result = this.fileHandler.chooseLoadFile();
      assertEquals(TEST_PATH, result, "The path should match the selected file path.");
    }
  }

  /** Tests that chooseLoadFile returns null when the dialog is cancelled. */
  @Test
  void testChooseLoadFileCancelled() {
    try (MockedConstruction<FileChooser> ignored =
        mockConstruction(
            FileChooser.class,
            (mock, context) -> {
              configureMock(mock);
              when(mock.showOpenDialog(this.stage)).thenReturn(null);
            })) {

      final String result = this.fileHandler.chooseLoadFile();
      assertNull(result, "The result should be null when the user cancels the dialog.");
    }
  }
}
