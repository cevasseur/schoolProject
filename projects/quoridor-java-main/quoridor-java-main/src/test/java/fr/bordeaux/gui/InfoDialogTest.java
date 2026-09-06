package fr.bordeaux.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import fr.bordeaux.i18n.I18n;
import javafx.scene.control.Alert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class InfoDialogTest {

  @Test
  void testShowCreatesAlert() {

    try (MockedConstruction<Alert> mocked =
        mockConstruction(
            Alert.class,
            (mock, context) -> {
              doNothing().when(mock).show();
            })) {

      InfoDialog.show();

      assertEquals(1, mocked.constructed().size());

      Alert alert = mocked.constructed().get(0);

      verify(alert).setTitle(I18n.get("info"));
      verify(alert).setHeaderText("Quoridor");
      verify(alert).setGraphic(null);
      verify(alert)
          .setContentText(
              "Version : 1.0.0\n"
                  + I18n.get("authors")
                  + "Abdeldjabar Ismail Abdraman \n Cengiz Vasseur \nJoris Douillet \n Khadidja Ahmat Hassan \n Matthieu Pourageaud \n"
                  + I18n.get("students"));

      verify(alert).show();
    }
  }
}
