package models;

public class Tag {

    private int id;
    private String text;
    private int color;

    public Tag(int id, String text, int color) {
        this.id = id;
        this.text = text;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\": " + id +
                ", \"text\": \"" + text + '\"' +
                ", \"color\": " + color +
                "}";
    }
}
