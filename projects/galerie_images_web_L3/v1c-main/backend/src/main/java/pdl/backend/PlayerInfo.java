package pdl.backend;

public class PlayerInfo {
    private String playerId;
    private String name;
    private int score;

    public PlayerInfo(String playerId, String name, int score) {
        this.playerId = playerId;
        this.name = name;
        this.score = score;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
