package pdl.backend;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryRepository implements InitializingBean {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
      //this.jdbcTemplate.execute("DROP TABLE memory");
      // Create table
      this.jdbcTemplate.execute(
        "CREATE TABLE IF NOT EXISTS memory (name text NOT NULL, score bigint NOT NULL, time DOUBLE PRECISION NOT NULL)"
      );
    }

    public void addScore(String name, int score, double time) {
        jdbcTemplate.update(
            "INSERT INTO memory (name, score, time) VALUES (?, ?, ?)",
            name, score, time
        );
    }
    

    public void deleteScore(String name) {
        jdbcTemplate.update("DELETE FROM memory WHERE name = ?", name);
    }

    public List<Score> getScores() {
        return jdbcTemplate.query(
            "SELECT * FROM memory ORDER BY score DESC, time ASC",
            new RowMapper<Score>() {
                @Override
                public Score mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Score score = new Score(rs.getString("name"), rs.getInt("score"), rs.getDouble("time"));
                    return score;
                }
            }
        );
    }


}
