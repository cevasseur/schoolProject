package pdl.backend;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void dirImagesExisting() {
		Path path = Paths.get(System.getProperty("user.dir"), "images");
		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
			mockedFiles.when(() -> Files.exists(path)).thenReturn(false);
			assertThrows(IOException.class, () -> {
				BackendApplication.main(null);
			});
		}
	}
}
