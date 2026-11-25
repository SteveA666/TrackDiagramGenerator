package ui;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Closed-hand cursor for an active pan, cached independently of the canvas. */
final class PanCursor {
    private PanCursor() {}

    private static final class Holder {
        static final Cursor CURSOR = create();
    }

    static Cursor grabbing() { return Holder.CURSOR; }

    private static Cursor create() {
        Cursor fallback = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        if (GraphicsEnvironment.isHeadless()) { return fallback; }
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension size = toolkit.getBestCursorSize(32, 32);
        if (size.width == 0 || size.height == 0) { return fallback; }
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.scale(size.width / 32.0, size.height / 32.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Path2D hand = new Path2D.Double();
            hand.moveTo(9, 25);
            hand.lineTo(9, 22);
            hand.curveTo(6, 20, 4, 17, 4, 14);
            hand.curveTo(4, 11, 7, 11, 9, 14);
            hand.lineTo(9, 9);
            hand.curveTo(9, 5, 13, 5, 13, 9);
            hand.curveTo(13, 4, 17, 4, 17, 9);
            hand.curveTo(17, 5, 21, 5, 21, 10);
            hand.curveTo(21, 7, 25, 7, 25, 11);
            hand.lineTo(25, 17);
            hand.curveTo(25, 20, 23, 22, 22, 23);
            hand.lineTo(22, 25);
            hand.closePath();
            g.setColor(Color.WHITE);
            g.fill(hand);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(hand);
            g.drawLine(13, 9, 13, 13);
            g.drawLine(17, 9, 17, 13);
            g.drawLine(21, 10, 21, 14);
            g.drawLine(9, 14, 12, 17);
        } finally { g.dispose(); }
        return toolkit.createCustomCursor(image, new Point(size.width / 2, size.height / 2), "Grabbing");
    }
}
