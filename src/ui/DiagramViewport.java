package ui;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * DiagramViewport<br>
 * Converts between diagram coordinates and screen coordinates.<br>
 * Keeps zoom anchored to the pointer and limits the viewing scale.<br>
 */
public final class DiagramViewport {
    public static final double MIN_ZOOM = 0.1;
    public static final double MAX_ZOOM = 8;
    private double zoom = 1;
    private double offsetX;
    private double offsetY;

    public double getZoom() { return zoom; }

    public Point2D.Double toScreen(Point2D world) {
        return new Point2D.Double(world.getX() * zoom + offsetX, world.getY() * zoom + offsetY);
    }

    public Point2D.Double toWorld(Point2D screen) {
        return new Point2D.Double((screen.getX() - offsetX) / zoom, (screen.getY() - offsetY) / zoom);
    }

    public void pan(double dx, double dy) {
        requireFinite(dx); requireFinite(dy);
        requireFinite(offsetX + dx); requireFinite(offsetY + dy);
        offsetX += dx;
        offsetY += dy;
    }

    public void zoomAt(double factor, Point2D anchor) {
        requireFinite(factor);
        requireFinite(anchor.getX()); requireFinite(anchor.getY());
        if (factor <= 0) { throw new IllegalArgumentException("Zoom factor must be positive."); }
        Point2D world = toWorld(anchor);
        double nextZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        double nextX = anchor.getX() - world.getX() * nextZoom;
        double nextY = anchor.getY() - world.getY() * nextZoom;
        requireFinite(nextX); requireFinite(nextY);
        zoom = nextZoom;
        offsetX = nextX;
        offsetY = nextY;
    }

    public void reset() { zoom = 1; offsetX = 0; offsetY = 0; }

    public void fit(Rectangle2D bounds, int width, int height) {
        if (width <= 0 || height <= 0) { return; }
        double scaleX = Math.max(1, width - 80) / Math.max(40, bounds.getWidth());
        double scaleY = Math.max(1, height - 80) / Math.max(40, bounds.getHeight());
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.min(scaleX, scaleY)));
        offsetX = width / 2.0 - bounds.getCenterX() * zoom;
        offsetY = height / 2.0 - bounds.getCenterY() * zoom;
    }

    private void requireFinite(double value) {
        if (!Double.isFinite(value)) { throw new IllegalArgumentException("View coordinates must be finite."); }
    }
}
