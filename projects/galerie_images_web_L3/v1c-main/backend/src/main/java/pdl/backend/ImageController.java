package pdl.backend;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
public class ImageController {

  @Autowired
  private ObjectMapper mapper;

  private final ImageDao imageDao;

  @Autowired
  private ImagesRepository imagesRepository;

  @Autowired
  private MemoryRepository memoryRepository;

  @Autowired
  public ImageController(ImageDao imageDao) {
    this.imageDao = imageDao;
  }

            /**
     * Get an image on the server.
     * @param id The id corresponding to the image to get.
     * @return A response corresponding to the result of the function.
     */
  @RequestMapping(value = "/images/{id}", method = RequestMethod.GET)
  public ResponseEntity<?> getImage(@PathVariable("id") long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);
    if (imageOpt.isPresent()) {
      Image image = imageOpt.get();
      byte[] bytes = image.getData();
      MediaType mediaType;
      switch (image.getType()) {
        case "image/png":
          mediaType = MediaType.IMAGE_PNG;
          break;
        case "image/webp":
          mediaType = MediaType.valueOf("image/webp");
          break;
        case "image/gif":
          mediaType = MediaType.IMAGE_GIF;
          break;
        default:
          mediaType = MediaType.IMAGE_JPEG;
          break;
      }

      return ResponseEntity
          .ok()
          .contentType(mediaType)
          .body(bytes);
    } else {
      String errorMessage = "L'image est introuvable";
      return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
          .body(errorMessage.getBytes());
    }

  }

          /**
     * Delete an image on the server.
     * @param id The id corresponding to the image to delete.
     * @return A response corresponding to the result of the function.
     */
  @RequestMapping(value = "/images/{id}", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteImage(@PathVariable("id") long id) {
    Optional<Image> imageOpt = imageDao.retrieve(id);
    if (imageOpt.isPresent()) {
      Image image = imageOpt.get();
      imageDao.delete(image);
      String fileName = image.getName();

      Path path = Paths.get(System.getProperty("user.dir"), "images");
      Path filePath = path.resolve(fileName);
      try {
        Files.delete(filePath);
      } catch (IOException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
        .body("Image not found");
      }
      
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    } else {
      String errorMessage = "Image not found";
      return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
          .body(errorMessage.getBytes());
    }

  }

            /**
     * Delete all image on the server.
     * @param id The id corresponding to the image to delete.
     * @return A response corresponding to the result of the function.
     */
  @RequestMapping(value = "/images", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteImages() {
      return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
              .body("Deleting all images is not allowed.");
  }

        /**
     * Request POST to add an image to the server.
     * @param file The file corresponding to the image to add.
     * @return A response corresponding to the result of the function.
     */
  @RequestMapping(value = "/images", method = RequestMethod.POST)
  public ResponseEntity<?> addImage(@RequestParam("file") MultipartFile file,
      RedirectAttributes redirectAttributes) {
    try {
      if (!(file.getContentType().equals(MediaType.IMAGE_JPEG_VALUE) || file.getContentType().equals(MediaType.IMAGE_PNG_VALUE) ||
            file.getContentType().equals("image/webp") || file.getContentType().equals("image/gif"))) {
        return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      }
      byte[] fileContent = file.getBytes();
      String fileName = file.getOriginalFilename();

      ByteArrayInputStream bais = new ByteArrayInputStream(fileContent);
      BufferedImage bufferedImage = ImageIO.read(bais);
      if (bufferedImage == null) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                 .body("The uploaded file is not a valid image.");
      }
      String size = String.valueOf(bufferedImage.getWidth()) + 'X' + String.valueOf(bufferedImage.getHeight());

      while((imageDao.retrieve(Image.count).isPresent())) {
        Image.count++;
      }
      Image newImage = new Image(Image.count, fileName, fileContent,file.getContentType(), size);
      imageDao.create(newImage); //L'image est enregistrée sur le serveur


      //L'image est enregistrée dans le dossier images.
      Path path = Paths.get(System.getProperty("user.dir"), "images");
      Path filePath = path.resolve(fileName);
      Files.write(filePath, fileContent);

      return ResponseEntity.status(HttpStatus.CREATED).build();
    } catch (IOException e) {
      String errorMessage = "Une erreur est survenue lors de l'ajout de l'image.";
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(errorMessage);
    }

  }

        /**
     * Return the list composed of all the images in the server.
     * @return A JSON array containing metadata of all the images.
     */
  @RequestMapping(value = "/images", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ResponseEntity<ArrayNode> getImageList() {
    ArrayNode nodes = mapper.createArrayNode();
    List<Image> images = imageDao.retrieveAll();

    for (Image image : images) {
      ObjectNode node = mapper.createObjectNode();
      node.put("Id", image.getId());
      node.put("Name", image.getName());
      node.put("Type", image.getType());
      node.put("Size", image.getSize());
      nodes.add(node);
    }

    return ResponseEntity.ok(nodes);
  }


      /**
     * Endpoint to find similar images based on a given image ID and descriptor.
     * The method retrieves all images, computes histograms, and finds the most similar ones.
     *
     * @param id The ID of the image to compare against.
     * @param N The number of similar images to retrieve.
     * @param DESCR The descriptor type ("histo_2D_T/S" or "histo_3D_RGB").
     * @return A JSON array containing metadata of similar images.
            * @throws Exception 
          */
  @RequestMapping(value = "/images/{id}/similar", method=RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ResponseEntity<ArrayNode> getSimilarImageList(@PathVariable("id") long id, @RequestParam("number") int N, @RequestParam("descriptor") String DESCR) throws Exception {
    ArrayNode tab = mapper.createArrayNode();
    Optional<Image> optionalImg = imageDao.retrieve(id);
    if (!optionalImg.isPresent()) {
      System.err.println("No existing image with this ID.");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
    Image originImg = optionalImg.get();

    List<Map.Entry<Long, Double>> list = new ArrayList<>();
    if (DESCR.equals("histo_2D_T/S") || DESCR.equals("histo_3D_RGB")) {
      list = imagesRepository.getSimilarImagePG(originImg.getId(),N,DESCR);
    }

    else {
      System.err.println("Invalid descriptor.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    //System.out.println(list); To see the different similarity values for each IDs compared to the original image selected in the URL.
    for (int i = 0; i < N; i++) {
      if (i == list.size()) {
        break;
      }
      long index = list.get(i).getKey();
      Optional<Image> imgOpt = imageDao.retrieve(index);
      if (imgOpt.isPresent()) {
        Image img = imgOpt.get();
        ObjectNode node = mapper.createObjectNode();
        node.put("Name", img.getName());
        node.put("Id", img.getId());
        node.put("Type", img.getType());
        node.put("Size", img.getSize());
        node.put("Similarity", list.get(i).getValue());
        tab.add(node);
      }
    }
    return ResponseEntity.ok(tab); // Return the JSON response
  }

  @RequestMapping(value = "/memory/add", method = RequestMethod.POST)
  public ResponseEntity<?> addScore(@RequestParam("name") String name,@RequestParam("score") int score, @RequestParam("time") double time) {
    memoryRepository.addScore(name, score, time);
    return ResponseEntity.ok(null);
  }

  @RequestMapping(value = "/memory/scores", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ResponseEntity<ArrayNode> getScoreList() {
    ArrayNode nodes = mapper.createArrayNode();
    List<Score> scores = memoryRepository.getScores();

    for (Score score : scores) {
      ObjectNode node = mapper.createObjectNode();
      node.put("name", score.getName());
      node.put("score", score.getScore());
      node.put("time", score.getTime());
      nodes.add(node);
    }

    return ResponseEntity.ok(nodes);
  }

  
  public byte[] convertGifToPng(byte[] gifData) throws IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream(gifData);
    BufferedImage bufferedImage = ImageIO.read(bais);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "png", baos);

    return baos.toByteArray();
  }

  @RequestMapping(value = "/images/{id}/comments", method = RequestMethod.POST)
  public ResponseEntity<?> addComment(@PathVariable("id") long id, @RequestParam("content") String content) {
    Optional<Image> imageOpt = imageDao.retrieve(id);
    if (!imageOpt.isPresent()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found");
    }
    imagesRepository.addComment(id, content);
    return ResponseEntity.status(HttpStatus.CREATED).body("Comment added");
  }

  @RequestMapping(value = "/images/{id}/comments", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
  @ResponseBody
  public ResponseEntity<ArrayNode> getAllComments(@PathVariable("id") long id) {
    ArrayNode nodes = mapper.createArrayNode();
    List<Map<String, Object>> comments = imagesRepository.getAllComments(id);

    for (Map<String, Object> comment : comments) {
      ObjectNode node = mapper.createObjectNode();
      node.put("id", (Integer) comment.get("id"));
      node.put("image_id", (Long) comment.get("image_id"));
      node.put("content", (String) comment.get("content"));
      node.put("created_at", comment.get("created_at").toString());
      nodes.add(node);
    }
    return ResponseEntity.ok(nodes);
  }

  @RequestMapping(value = "/comments/{commentId}", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteComment(@PathVariable("commentId") long commentId) {
    imagesRepository.deleteComment(commentId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}



