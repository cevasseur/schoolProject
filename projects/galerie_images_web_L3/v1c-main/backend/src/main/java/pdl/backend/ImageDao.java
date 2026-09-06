package pdl.backend;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

@Repository
public class ImageDao implements Dao<Image> {

  private final ImagesRepository imagesRepository;

  private final Map<Long, Image> images = new HashMap<>();

  @Autowired
  public ImageDao(ImagesRepository imagesRepository) {
    this.imagesRepository = imagesRepository;

    Path dirPath = Paths.get(System.getProperty("user.dir"), "/images");
    try {
      Files.walk(dirPath)
      .filter(file -> file.toString().endsWith(".jpeg") || file.toString().endsWith(".png") ||
                      file.toString().endsWith(".jpg") || file.toString().endsWith(".webp") ||
                      file.toString().endsWith(".gif"))
      .forEach(file -> {
        byte[] fileContent;
        List<Map.Entry<String, Long>> allImagesMap = this.imagesRepository.getAllImages();

        Map<String, Long> allImages = allImagesMap.stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    
        List<Long> ids = new ArrayList<>();
        for (Map.Entry<String, Long> entry : allImagesMap) {
            ids.add(entry.getValue());
        }
        try {
          fileContent = Files.readAllBytes(dirPath.resolve(file.toString()));
          ByteArrayInputStream bais = new ByteArrayInputStream(fileContent);
          BufferedImage bufferedImage = ImageIO.read(bais);
          if (bufferedImage == null) {
            System.err.println("\n[NULL BUFFERED IMAGE] The current BufferedImage is null\n");
            return;
          }
          String size = String.valueOf(bufferedImage.getWidth()) + 'X' + String.valueOf(bufferedImage.getHeight());
          String name = file.getFileName().toString();
          String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
          String mediaType = switch (extension) {
            case "jpeg", "jpg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "gif" -> MediaType.IMAGE_GIF_VALUE;
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
          };

          Image img;
          if (allImages.containsKey(name)) {
            Long id = allImages.get(name);
            img = new Image(id, name, fileContent, mediaType, size);
          } else {
            while(ids.contains(Image.count)) {
              Image.count++;
            }
            img = new Image(Image.count, name, fileContent, mediaType, size);
            this.imagesRepository.insertImages(
                img.getId(),
                name,
                GrayLevelProcessing.imgToHisto1DCompressedFromHisto2D(img),
                GrayLevelProcessing.imgToHisto1DCompressedFromHisto3D(img)
            );
          }
          images.put(img.getId(), img);
        } catch (IOException e) {
          e.printStackTrace();
        }

      });

    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Optional<Image> retrieve(final long id) {
    for (Image image : images.values()) {
      if (image.getId() == id) {
        return Optional.of(image);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<Image> retrieveAll() {
    List<Image> new_l = new ArrayList<>();
    for (Image image : images.values()) {
      new_l.add(image);
    }
    return new_l;
  }

  @Override
  public void create(final Image img) {
    this.imagesRepository.insertImages(img.getId(), img.getName(),GrayLevelProcessing.imgToHisto1DCompressedFromHisto2D(img), GrayLevelProcessing.imgToHisto1DCompressedFromHisto3D(img));
    images.put(img.getId(), img);
  }

  @Override
  public void update(final Image img, final String[] params) {
    // Not used
  }

  @Override
  public void delete(final Image img) {
    this.imagesRepository.deleteImages(img.getId());
    this.imagesRepository.deleteAllComments(img.getId());
    images.remove(img.getId());
  }
}
