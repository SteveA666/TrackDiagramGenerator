package editor;

import java.awt.geom.*;
import java.util.function.Function;
import model.*;

/**
 * PlatformGeometry<br>
 * Builds side strips and island surfaces from track-relative edges.<br>
 * Shares geometry between validation, drawing, hit testing, and Fit.<br>
 * Accepts preview node positions without modifying the diagram.<br>
 */
public final class PlatformGeometry {
    public static final double SIDE_WIDTH = 12;

    private PlatformGeometry(){}

    // Platform outline
    public static Path2D outline(Platform platform) {
        return outline(platform, node -> new Point2D.Double(node.getX(), node.getY()));
    }

    public static Path2D outline(Platform platform, Function<Node, Point2D> position) {
        PlatformEdge first = platform.getEdges().get(0);
        Point2D[] a = edgePoints(first, position);
        Point2D[] b;
        if (platform.isSidePlatform()) {
            TrackSegment track = first.getTrackSegment();
            Point2D start = position.apply(track.getStart()), end = position.apply(track.getEnd());
            double dx = end.getX() - start.getX(), dy = end.getY() - start.getY();
            double scale = SIDE_WIDTH / Math.hypot(dx, dy) * (first.getSide() == TrackSide.LEFT ? 1 : -1);
            b = new Point2D[]{new Point2D.Double(a[0].getX() + dy * scale, a[0].getY() - dx * scale),
                    new Point2D.Double(a[1].getX() + dy * scale, a[1].getY() - dx * scale)};
        } else {
            b = edgePoints(platform.getEdges().get(1), position);
            // Match physical ends even when the second track runs backwards.
            if (a[0].distanceSq(b[0]) + a[1].distanceSq(b[1])
                    > a[0].distanceSq(b[1]) + a[1].distanceSq(b[0])) {
                Point2D swap = b[0]; b[0] = b[1]; b[1] = swap;
            }
        }
        Point2D[] corners = {a[0], a[1], b[1], b[0]};
        for (Point2D corner : corners) {
            if (!Double.isFinite(corner.getX()) || !Double.isFinite(corner.getY())
                    || corner.getX() < Integer.MIN_VALUE || corner.getX() > Integer.MAX_VALUE
                    || corner.getY() < Integer.MIN_VALUE || corner.getY() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Platform placement is outside supported diagram coordinates.");
            }
        }
        double area = 0;
        for (int i = 0; i < corners.length; i++) {
            Point2D p = corners[i], q = corners[(i + 1) % corners.length];
            area += (p.getX() - a[0].getX()) * (q.getY() - a[0].getY())
                    - (q.getX() - a[0].getX()) * (p.getY() - a[0].getY());
        }
        if (!Double.isFinite(area) || Math.abs(area) < 0.000001
                || intersects(a[0], a[1], b[0], b[1]) || intersects(a[0], b[0], a[1], b[1])) {
            throw new IllegalArgumentException("Platform edges must form a visible surface without crossing.");
        }
        Path2D shape = new Path2D.Double();
        shape.moveTo(corners[0].getX(), corners[0].getY());
        for (int i = 1; i < corners.length; i++) { shape.lineTo(corners[i].getX(), corners[i].getY()); }
        shape.closePath();
        return shape;
    }

    // Track-relative edge coordinates
    public static Point2D[] edgePoints(PlatformEdge edge, Function<Node, Point2D> position) {
        TrackSegment track = edge.getTrackSegment();
        Point2D start = position.apply(track.getStart()), end = position.apply(track.getEnd());
        double dx = end.getX() - start.getX(), dy = end.getY() - start.getY();
        double length = Math.hypot(dx, dy);
        if (length == 0) { throw new IllegalArgumentException("Platform tracks must have visible length."); }
        double offset = edge.getOffset() * (edge.getSide() == TrackSide.LEFT ? 1 : -1);
        Point2D[] points = new Point2D[2];
        double[] fractions = {edge.getStartFraction(), edge.getEndFraction()};
        for (int i = 0; i < points.length; i++) {
            points[i] = new Point2D.Double(start.getX() + dx * fractions[i] + dy / length * offset,
                    start.getY() + dy * fractions[i] - dx / length * offset);
        }
        return points;
    }

    private static boolean intersects(Point2D a, Point2D b, Point2D c, Point2D d) {
        return Line2D.linesIntersect(a.getX(), a.getY(), b.getX(), b.getY(),
                c.getX(), c.getY(), d.getX(), d.getY());
    }
}
