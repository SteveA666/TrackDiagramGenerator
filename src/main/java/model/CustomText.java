package model;

import java.util.Objects;

/**
 * CustomText<br>
 * Stores a diagram label with a stable identity and top-left position.<br>
 * Validates its text, font, size, and hexadecimal RGB colour.<br>
 */
public class CustomText {
    private final int id;
    private String text;
    private int x, y, size;
    private String font, color;
    private Network network;

    public CustomText(String text, int x, int y, int size, String font, String color, int id) {
        this.id = id;
        setAppearance(text, size, font, color);
        setPosition(x, y);
    }

    // Identity and ownership
    public int getId(){ return id; }
    public Network getNetwork(){ return network; }
    void attach(Network network){ this.network = network; }

    // Position
    public int getX(){ return x; }
    public int getY(){ return y; }
    public void setX(int x){ this.x = x; }
    public void setY(int y){ this.y = y; }
    public void setPosition(int x, int y){ this.x = x; this.y = y; }

    // Text and appearance
    public String getText(){ return text; }
    public int getSize(){ return size; }
    public String getFont(){ return font; }
    public String getColor(){ return color; }
    public void setText(String text){ setAppearance(text, size, font, color); }
    public void setSize(int size){ setAppearance(text, size, font, color); }
    public void setFont(String font){ setAppearance(text, size, font, color); }
    public void setColor(String color){ setAppearance(text, size, font, color); }

    public void setAppearance(String text, int size, String font, String color) {
        Objects.requireNonNull(text, "Text cannot be null.");
        if (text.trim().isEmpty()) { throw new IllegalArgumentException("Enter some text."); }
        if (size <= 0) { throw new IllegalArgumentException("Font size must be positive."); }
        if (font == null || font.trim().isEmpty()) {
            throw new IllegalArgumentException("Choose a font.");
        }
        if (color == null || !color.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Use an RGB colour such as #000000.");
        }
        this.text = text;
        this.size = size;
        this.font = font.trim();
        this.color = color.toUpperCase(java.util.Locale.ROOT);
    }
}
