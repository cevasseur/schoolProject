package pdl.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImageControllerTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ImagesRepository imagesRepository;

	private long firstImageId;
	private long secondImageId;
	private long thirdImageId;
	
	@BeforeAll
	@Order(1)
	public void reset() throws Exception {
		// reset Image class static counter
		imagesRepository.afterPropertiesSet();
		
	}

	@BeforeAll
	@Order(2)
	public void initialize() throws Exception {
		Path path = Paths.get(System.getProperty("user.dir"), "test");
		byte[] test1 = Files.readAllBytes(path.resolve("test1.jpg"));
		byte[] test2 = Files.readAllBytes(path.resolve("test2.jpg"));
		byte[] test3 = Files.readAllBytes(path.resolve("test3.png"));
		MockMultipartFile filetest1 = new MockMultipartFile(
			"file",
			"test1.jpg",
			MediaType.IMAGE_JPEG_VALUE,
			test1);

		MockMultipartFile filetest2 = new MockMultipartFile(
			"file",
			"test2.jpg",
			MediaType.IMAGE_JPEG_VALUE,
			test2);

		MockMultipartFile filetest3 = new MockMultipartFile(
			"file",
			"test3.png",
			MediaType.IMAGE_PNG_VALUE,
			test3);

		mockMvc.perform(MockMvcRequestBuilders.multipart("/images").file(filetest1))
		.andExpectAll(
				status().isCreated());
		firstImageId = Image.count;
		mockMvc.perform(MockMvcRequestBuilders.multipart("/images").file(filetest2))
		.andExpectAll(
				status().isCreated());
		secondImageId = Image.count;
		mockMvc.perform(MockMvcRequestBuilders.multipart("/images").file(filetest3))
		.andExpectAll(
				status().isCreated());
		thirdImageId = Image.count;
	}

	@AfterAll
	public void deleteTemporaryImages() throws Exception {
		mockMvc.perform(delete(String.format("/images/%d",firstImageId)));

		mockMvc.perform(delete(String.format("/images/%d",secondImageId)));

		mockMvc.perform(delete(String.format("/images/%d",thirdImageId)));
	}

	@Test
	@Order(1)
	public void getImageListShouldReturnSuccess() throws Exception {

		MvcResult result = (mockMvc.perform(get("/images"))
				.andExpectAll(
						status().isOk(),
						content().contentType("application/json; charset=UTF-8"))
						.andReturn());
						
		String responseContent = result.getResponse().getContentAsString();

		ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
		int firstImageIndex = -1;
		int secondImageIndex = -1;
		int thirdImageIndex = -1;

		for (int i = 0; i < jsonNode.size(); i++) {
			long id = jsonNode.get(i).get("Id").asLong();
			if (id == firstImageId) {
				firstImageIndex = i;
			} else if (id == secondImageId) {
				secondImageIndex = i;
			} else if (id == thirdImageId) {
				thirdImageIndex = i;
			}
		}
		assertEquals(
			jsonNode.get(firstImageIndex).toString(),
			String.format("{\"Id\":%d,\"Name\":\"test1.jpg\",\"Type\":\"image/jpeg\",\"Size\":\"6927X4618\"}", firstImageId)
		);
		
		assertEquals(
			jsonNode.get(secondImageIndex).toString(),
			String.format("{\"Id\":%d,\"Name\":\"test2.jpg\",\"Type\":\"image/jpeg\",\"Size\":\"4000X6000\"}", secondImageId)
		);
		
		assertEquals(
			jsonNode.get(thirdImageIndex).toString(),
			String.format("{\"Id\":%d,\"Name\":\"test3.png\",\"Type\":\"image/png\",\"Size\":\"404X316\"}", thirdImageId)
		);
	}

	@Test
	@Order(2)
	public void getImageShouldReturnNotFound() throws Exception {
		mockMvc.perform(get(String.format("/images/%d",Image.count + 1)))
				.andExpectAll(
						status().isNotFound(),
						content().contentType("application/json"));
	}

	@Test
	@Order(3)
	public void getImageShouldReturnSuccess() throws Exception {
		mockMvc.perform(get(String.format("/images/%d",Image.count)))
				.andExpectAll(
						status().isOk());
	}

	@Test
	@Order(4)
	public void deleteImagesShouldReturnMethodNotAllowed() throws Exception {
		mockMvc.perform(delete("/images"))
				.andExpectAll(
						status().isMethodNotAllowed());
	}

	@Test
	@Order(5)
	public void deleteImageShouldReturnNotFound() throws Exception {
		mockMvc.perform(delete((String.format("/images/%d",Image.count + 1))))
				.andExpectAll(
						status().isNotFound(),
						content().contentType("application/json"));
	}

	@Test
	@Order(6)
	public void deleteImageShouldReturnSuccess() throws Exception {
		mockMvc.perform(delete((String.format("/images/%d",Image.count))))
				.andExpectAll(
						status().isNoContent());
	}

	@Test
	@Order(7)
	public void getSimilarImagesShouldReturnNotFound() throws Exception {
		mockMvc.perform(get((String.format("/images/%d/similar?number=2&descriptor=histo4D",Image.count + 1))))
		.andExpectAll(
				status().isNotFound());
	}

	@Test
	@Order(8)
	public void createImageShouldReturnSuccess() throws Exception {
		byte[] fileContent = Files.readAllBytes(Paths.get(System.getProperty("user.dir"), "/test/test3.png"));
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test3.png",
				MediaType.IMAGE_PNG_VALUE,
				fileContent);
		mockMvc.perform(MockMvcRequestBuilders.multipart("/images").file(file))
				.andExpectAll(
						status().isCreated());
	}

	@Test
	@Order(9)
	public void createImageShouldReturnUnsupportedMediaType() throws Exception {
		byte[] fileContent = Files.readAllBytes(Paths.get(System.getProperty("user.dir"), "/test/text.txt"));
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"text.txt",
				MediaType.TEXT_PLAIN_VALUE,
				fileContent);
		mockMvc.perform(MockMvcRequestBuilders.multipart("/images").file(file))
				.andExpectAll(
						status().isUnsupportedMediaType());
	}

	@Test
	@Order(10)
	public void getSimilarImagesShouldReturnSuccess() throws Exception {
		mockMvc.perform(get((String.format("/images/%d/similar?number=2&descriptor=histo_2D_T/S",Image.count))))
		.andExpectAll(
				status().isOk(),
				content().contentType("application/json; charset=UTF-8"));
	}

	@Test
	@Order(11)
	public void getSimilarImagesShouldReturnBadRequest() throws Exception {
		mockMvc.perform(get((String.format("/images/%d/similar?number=2&descriptor=histo4D",Image.count))))
		.andExpectAll(
				status().isBadRequest());
	}



}
