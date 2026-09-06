package pdl.backend;

public class Score {
    private String name;
    private int score;
    private double time;

    Score(String name, int score, double time) {
        this.name = name;
        this.score = score;
        this.time = time;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String getName() {
        return this.name;
    }

    public int getScore() {
        return this.score;
    }

    public double getTime() {
        return this.time;
    }
}

