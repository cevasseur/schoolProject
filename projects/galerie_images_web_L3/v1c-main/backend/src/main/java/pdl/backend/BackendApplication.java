package pdl.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.nio.file.*;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) throws IOException {
		if(!Files.exists(Paths.get(System.getProperty("user.dir"), "images"))) {
			throw new IOException("\n[ERROR MESSAGE] Le fichier test.jpg n'existe pas dans le répertoire images.\n");
		}
		
		SpringApplication.run(BackendApplication.class, args);
	}

}
