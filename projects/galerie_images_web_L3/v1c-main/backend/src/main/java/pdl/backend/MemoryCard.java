package pdl.backend;

public class MemoryCard {
    private String id;
    private String src;

    public MemoryCard() {}

    public MemoryCard(String id, String src) {
        this.id = id;
        this.src = src;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}

