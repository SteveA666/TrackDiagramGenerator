package model;

public class CustomText {
    private String text;
    private int x;
    private int y;
    private int size;
    private String font;
    private String color;

    public CustomText(String text, int x, int y, int size, String font, String color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.size = size;
        this.font = font;
        this.color = color;
    }

    // Getters and setters
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
    public String getFont() {
        return font;
    }
    public void setFont(String font) {
        this.font = font;
    }
    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        this.color = color;
    }
}
