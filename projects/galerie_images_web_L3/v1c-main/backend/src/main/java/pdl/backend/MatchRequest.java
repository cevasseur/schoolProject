package pdl.backend;

public class MatchRequest {
    private String firstId;
    private String secondId;
    private Boolean isMatched;
    private String playerId;

    
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getFirstId() { return firstId; }
    public void setFirstId(String firstId) { this.firstId = firstId; }

    public String getSecondId() { return secondId; }
    public void setSecondId(String secondId) { this.secondId = secondId; }

    public Boolean getIsMatched() { return isMatched; }
    public void setIsMatched(Boolean isMatched) {  this.isMatched = isMatched; }
}