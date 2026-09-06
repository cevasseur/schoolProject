package pdl.backend;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Repository
public class ImagesRepository implements InitializingBean {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Override
  public void afterPropertiesSet() throws Exception {

    // Create table with vector type for desc_1 and desc_2 using pgvector
    this.jdbcTemplate.execute(
      "CREATE TABLE IF NOT EXISTS images (id bigint PRIMARY KEY, name TEXT UNIQUE, desc_1 vector(3636), desc_2 vector(16000))"
    );
    this.jdbcTemplate.execute(
      "CREATE TABLE IF NOT EXISTS comments (id SERIAL PRIMARY KEY, image_id bigint REFERENCES images(id), content TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
    );
  }

  public void insertImages(Long id, String name, float[] descriptor_1, float[] descriptor_2) {
    // If the id is already existing then the descriptors are updated
    String desc1 = Arrays.toString(descriptor_1);
    String desc2 = Arrays.toString(descriptor_2);
    jdbcTemplate.update("INSERT INTO images (id, name, desc_1, desc_2) VALUES (?, ?, ?::vector, ?::vector) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, desc_1 = EXCLUDED.desc_1, desc_2 = EXCLUDED.desc_2", id, name, desc1, desc2);
  }

  public void deleteImages(Long id) {
    jdbcTemplate.update("DELETE FROM images WHERE id = ?", id);
  }

  public int getNbImages() {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM images", Integer.class);
  }

  public List<Map.Entry<String, Long>> getAllImages() {
    String sql = "SELECT id, name FROM images";
    
    return jdbcTemplate.query(sql, (rs, rowNum) -> 
        new AbstractMap.SimpleEntry<>(
            rs.getString("name"),
            rs.getLong("id")
        )
    );
}

  public List<Map.Entry<Long, Double>> getSimilarImagePG(Long id, int N, String DESCR) throws Exception {
    String descriptor;
    // Check if the descriptor in the arguments is valid
    if (DESCR.equals("histo_2D_T/S")) descriptor = "desc_1";
    else if (DESCR.equals("histo_3D_RGB")) descriptor = "desc_2";
    else throw new Exception("\n[ERROR MESSAGE] The given descriptor is invalid.\n");
    // This SQL request sorts the N most similar images in descending order except the image we use to compare others
    String sql = """
        SELECT id, %s <-> (SELECT %s FROM images WHERE id = ?) AS similarity
        FROM images 
        WHERE id != ?
        ORDER BY similarity
        LIMIT ?
    """.formatted(descriptor, descriptor);
    
    List<Map.Entry<Long, Double>> similarImages = jdbcTemplate.query(
      sql,
      new RowMapper<Map.Entry<Long, Double>>() {
        @Override
        public Map.Entry<Long, Double> mapRow(ResultSet rs, int rowNum) throws SQLException {
          Long imageId = rs.getLong("id");
          Double distance = rs.getDouble("similarity");  // The calculated similarity (distance)
        
          return new AbstractMap.SimpleEntry<>(imageId, 1.0 / (1.0 + distance));  // Invert distance to get a similarity measure
        }
      },
      id,  // Image's id to compare with others
      id,  // Image's id to not be compared with itself
      N     // Limit the results number
    );
    return similarImages;
  }

  public void addComment(Long imageId, String content) {
    jdbcTemplate.update("INSERT INTO comments (image_id, content) VALUES (?, ?)", imageId, content);
  }

  public Map<String, Object> getComment(Long imageId, Long commId) {
    return jdbcTemplate.queryForMap("SELECT content FROM comments WHERE commId = ? & image_id = ?", commId, imageId);
  }

  public List<Map<String, Object>> getAllComments(Long imageId) {
    return jdbcTemplate.queryForList("SELECT id, image_id, content, created_at FROM comments WHERE image_id = ?", imageId);
  }

  public void deleteComment(Long commentId) {
    jdbcTemplate.update("DELETE FROM comments WHERE id = ?", commentId);
  }

  public void deleteAllComments(Long imageId) {
    jdbcTemplate.update("DELETE FROM comments WHERE image_id = ?", imageId);
  }
}