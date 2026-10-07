package settings;

import com.google.gson.Gson;
import model.NodeType;
import model.TrackType;

/** User preferences; defaults preserve the original editor behaviour. */
public final class AppSettings {
    public int windowWidth = 800;
    public int windowHeight = 600;
    public boolean startMaximized;
    public boolean snapToGrid = true;
    public int gridSpacing = 20;
    public boolean continuousDraw;
    public NodeType defaultNodeType = NodeType.REGULAR;
    public TrackType defaultTrackType = TrackType.MAINLINE;
    public int undoLimit = 100;
    public boolean showGrid = true;
    public boolean showDebugNodes;
    public boolean antialiasing = true;
    public String backgroundColor = "#FFFFFF";
    public String gridColor = "#EBEFF3";
    public int wheelZoomPercent = 15;
    public boolean invertWheelZoom;
    public boolean fitOnOpen = true;
    public String textFont = "SansSerif";
    public int textSize = 16;
    public String textColor = "#000000";
    public int platformStartPercent = 20;
    public int platformEndPercent = 80;
    public double platformOffset = 8;
    public boolean backupOnSave;
    public boolean confirmOverwrite = true;

    public AppSettings copy() { return new Gson().fromJson(new Gson().toJson(this), AppSettings.class); }

    public void validate() {
        range(windowWidth, 800, 7680, "Window width");
        range(windowHeight, 400, 4320, "Window height");
        range(gridSpacing, 1, 200, "Grid spacing");
        range(undoLimit, 1, 1000, "Undo limit");
        range(wheelZoomPercent, 1, 100, "Wheel zoom step");
        range(textSize, 1, 512, "Default text size");
        range(platformStartPercent, 0, 99, "Platform start");
        range(platformEndPercent, 1, 100, "Platform end");
        if (platformStartPercent >= platformEndPercent) { throw new IllegalArgumentException("Platform start must be before its end."); }
        if (!Double.isFinite(platformOffset) || platformOffset < 0 || platformOffset > 1000) {
            throw new IllegalArgumentException("Platform offset must be 0–1000 units.");
        }
        if (defaultNodeType == null || defaultTrackType == null) { throw new IllegalArgumentException("Choose node and track types."); }
        if (textFont == null || textFont.isBlank()) { throw new IllegalArgumentException("Choose a default text font."); }
        color(backgroundColor, "Background");
        color(gridColor, "Grid");
        color(textColor, "Text");
    }

    private static void range(int value, int min, int max, String label) {
        if (value < min || value > max) { throw new IllegalArgumentException(label + " must be " + min + "–" + max + "."); }
    }

    private static void color(String value, String label) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(label + " colour must use #RRGGBB.");
        }
    }
}
