import editor.DiagramEditor;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;
import model.*;
import ui.*;

/**
 * NavigationTests<br>
 * Checks view transforms, track dragging, and oriented stub markers.<br>
 * Exercises actual canvas events without opening a native window.<br>
 */
public final class NavigationTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            viewport(); compoundEdits(); navigation(); dragTracks(); stubSymbols();
        });
        System.out.println("PASS: " + checks + " navigation and track gesture checks");
    }

    private static void viewport() {
        DiagramViewport view = new DiagramViewport();
        Point2D anchor = new Point2D.Double(170, 230);
        Point2D before = view.toWorld(anchor);
        view.zoomAt(2, anchor);
        near(view.toWorld(anchor).distance(before), 0, "zoom holds pointer anchor");
        Point2D point = new Point2D.Double(-40, 90);
        near(view.toWorld(view.toScreen(point)).distance(point), 0, "coordinate round trip");
        view.pan(15, -30);
        near(view.toScreen(before).getX(), 185, "pan uses screen units");
        near(view.toScreen(before).getY(), 200, "pan moves both axes");
        view.zoomAt(1e10, anchor); near(view.getZoom(), DiagramViewport.MAX_ZOOM, "maximum zoom");
        view.zoomAt(1e-20, anchor); near(view.getZoom(), DiagramViewport.MIN_ZOOM, "minimum zoom");
        rejected(() -> view.zoomAt(0, anchor));
        rejected(() -> view.zoomAt(Double.NaN, anchor));
        rejected(() -> view.pan(Double.POSITIVE_INFINITY, 0));
        view.reset(); near(view.getZoom(), 1, "reset scale");
        near(view.toScreen(point).distance(point), 0, "reset offset");
        view.fit(new Rectangle2D.Double(-100, -50, 400, 200), 800, 500);
        Point2D centre = view.toScreen(new Point(100, 50));
        near(centre.getX(), 400, "fit centres x"); near(centre.getY(), 250, "fit centres y");
    }

    private static void compoundEdits() {
        Network n = new Network(); DiagramEditor editor = new DiagramEditor(n);
        rejected(() -> editor.createTrackBetween(null, 20, 20, null, 20, 20,
                NodeType.REGULAR, TrackType.MAINLINE));
        check(n.nodeCount() == 0 && n.segmentCount() == 0, "invalid gesture creates no partial endpoints");
        rejected(() -> editor.createTrackBetween(null, 0, 0, 99, 100, 0,
                NodeType.REGULAR, TrackType.MAINLINE));
        check(n.nodeCount() == 0, "unknown second endpoint leaves first uncreated");
        rejected(() -> editor.createTrackBetween(null, 0, 0, null, 100, 0, null, TrackType.MAINLINE));
        TrackSegment first = editor.createTrackBetween(null, -20, 40, null, 120, 40,
                NodeType.STUB_END, TrackType.SIDING);
        check(n.nodeCount() == 2 && n.segmentCount() == 1, "whole track gesture committed");
        check(first.getStart().getNodeType() == NodeType.STUB_END && first.getTrackType() == TrackType.SIDING,
                "gesture honours chosen types");
        TrackSegment next = editor.createTrackBetween(null, 120, 40, null, 200, 80,
                NodeType.REGULAR, TrackType.MAINLINE);
        check(next.getStart() == first.getEnd() && n.nodeCount() == 3, "exact existing endpoints reused");
        check(n.segmentsAt(first.getEnd().getId()).size() == 2, "reused endpoint connectivity maintained");
        rejected(() -> editor.createTrackBetween(999, 0, 0, null, 600, 600,
                NodeType.REGULAR, TrackType.MAINLINE));
        check(n.nodeCount() == 3 && n.segmentCount() == 2, "invalid explicit reference is atomic");
    }

    private static void navigation() {
        Network n = new Network(); DiagramEditor editor = new DiagramEditor(n);
        Node a = editor.createNode(100, 100, NodeType.REGULAR);
        Node b = editor.createNode(300, 100, NodeType.REGULAR);
        editor.createTrack(a.getId(), b.getId(), TrackType.MAINLINE);
        DiagramPanel panel = panel(n);
        panel.zoomAt(2, new Point());
        panel.setTool(DiagramPanel.Tool.PAN);
        gesture(panel, new Point(400, 400), new Point(450, 430));
        near(panel.worldToScreen(new Point()).getX(), 50, "Pan tool translates view");
        near(panel.worldToScreen(new Point()).getY(), 30, "Pan tool translates y");
        check(a.getX() == 100 && a.getY() == 100, "pan does not move model nodes");
        panel.setTool(DiagramPanel.Tool.SELECT); panel.setSnapToGrid(false);
        Point screen = screen(panel, 100, 100);
        press(panel, screen); drag(panel, new Point(screen.x + 40, screen.y + 20));
        check(a.getX() == 100, "zoomed drag still previews before commit");
        release(panel, new Point(screen.x + 40, screen.y + 20));
        check(a.getX() == 120 && a.getY() == 110, "zoomed and panned drag uses world coordinates");
        check(panel.getSelectedNode() == a, "selection hit test works after pan and zoom");
        gestureClick(panel, new Point(screen(panel, 120, 110).x + 8, screen(panel, 120, 110).y));
        check(panel.getSelectedNode() == a, "selection tolerance stays constant in screen pixels");
        mouse(panel, MouseEvent.MOUSE_PRESSED, new Point(200, 200), MouseEvent.BUTTON2, MouseEvent.BUTTON2_DOWN_MASK);
        mouse(panel, MouseEvent.MOUSE_DRAGGED, new Point(170, 190), MouseEvent.NOBUTTON, MouseEvent.BUTTON2_DOWN_MASK);
        mouse(panel, MouseEvent.MOUSE_RELEASED, new Point(170, 190), MouseEvent.BUTTON2, 0);
        near(panel.worldToScreen(new Point()).getX(), 20, "middle drag pans from selection tool");
        check(a.getX() == 120 && a.getY() == 110, "middle drag leaves model unchanged");
        Point anchor = new Point(210, 170); Point2D before = panel.screenToWorld(anchor);
        panel.dispatchEvent(new MouseWheelEvent(panel, MouseEvent.MOUSE_WHEEL, 0, 0,
                anchor.x, anchor.y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -1));
        near(panel.screenToWorld(anchor).distance(before), 0, "wheel zoom stays at pointer");
        check(panel.getZoom() > 2, "wheel zoom changes scale");
        NavigationToolbar controls = new NavigationToolbar(panel);
        button(controls, "100%").doClick(); near(panel.getZoom(), 1, "reset button");
        near(panel.worldToScreen(new Point()).distance(new Point()), 0, "reset button clears pan");
        button(controls, "+").doClick(); near(panel.getZoom(), 1.25, "zoom in button");
        button(controls, "-").doClick(); near(panel.getZoom(), 1, "zoom out button");
        button(controls, "Fit").doClick();
        Point2D shown = panel.worldToScreen(new Point(a.getX(), a.getY()));
        check(shown.getX() > 0 && shown.getX() < 800, "fit brings diagram into view");
        panel.resetView(); panel.zoomAt(2, new Point());
        panel.setTool(DiagramPanel.Tool.ADD_NODE);
        gestureClick(panel, screen(panel, 200, 200));
        check(n.getNode(3).getX() == 200 && n.getNode(3).getY() == 200, "node creation uses transformed coordinates");
        panel.setTool(DiagramPanel.Tool.SELECT);
        Point pick = screen(panel, a.getX(), a.getY()); press(panel, pick); drag(panel, new Point(pick.x + 100, pick.y));
        panel.zoomIn(); release(panel, new Point(pick.x + 100, pick.y));
        check(a.getX() == 120, "zoom cancels unfinished model drag");
        DiagramPanel empty = panel(new Network()); empty.fitToDiagram();
        near(empty.getZoom(), 1, "fit on empty diagram is safe");
    }

    private static void dragTracks() {
        Network n = new Network(); DiagramPanel panel = panel(n);
        panel.zoomAt(2, new Point()); panel.setTool(DiagramPanel.Tool.PAN);
        gesture(panel, new Point(0, 0), new Point(40, 20));
        panel.setTool(DiagramPanel.Tool.ADD_TRACK);
        Point start = screen(panel, 20, 40), end = screen(panel, 180, 80);
        press(panel, start); drag(panel, end);
        check(n.nodeCount() == 0 && n.segmentCount() == 0, "new track endpoints are preview only");
        release(panel, end);
        TrackSegment first = n.getTrackSegment(1);
        check(first != null && n.nodeCount() == 2, "drag in empty space creates endpoints and track");
        check(first.getStart().getX() == 20 && first.getEnd().getX() == 180, "track drag respects view transform");
        gesture(panel, end, screen(panel, 260, 160));
        check(n.nodeCount() == 3 && n.segmentCount() == 2, "drag can extend an existing endpoint");
        check(n.getTrackSegment(2).getStart() == first.getEnd(), "drag reuses actual registered node");
        gesture(panel, start, screen(panel, 24, 44));
        check(panel.isStatusError() && n.nodeCount() == 3 && n.segmentCount() == 2,
                "snapping a drag to zero length creates nothing");
        cancel(panel);
        press(panel, screen(panel, 300, 200)); drag(panel, screen(panel, 340, 240)); cancel(panel);
        release(panel, screen(panel, 340, 240));
        check(n.nodeCount() == 3 && n.segmentCount() == 2, "cancelled track drag leaves no endpoints");
        press(panel, start); drag(panel, screen(panel, 350, 100)); panel.zoomIn();
        release(panel, screen(panel, 350, 100));
        check(n.nodeCount() == 3 && n.segmentCount() == 2, "zoom cancels track preview without partial nodes");
        panel.setSnapToGrid(false);
        gesture(panel, screen(panel, 321, 203), screen(panel, 377, 269));
        check(n.nodeCount() == 5 && n.getNode(4).getX() == 321 && n.getNode(5).getY() == 269,
                "track dragging can use exact free coordinates");
        press(panel, screen(panel, 321, 203)); release(panel, screen(panel, 377, 269));
        check(n.nodeCount() == 5 && n.segmentCount() == 4, "release records drag distance if motion events coalesce");
        press(panel, screen(panel, 20, 40)); drag(panel, screen(panel, 390, 300));
        n.removeNode(first.getStart().getId()); release(panel, screen(panel, 390, 300));
        check(panel.isStatusError() && n.nodeCount() == 4, "stale drag reference does not create stray endpoint");
    }

    private static void stubSymbols() {
        Network n = new Network(); DiagramEditor editor = new DiagramEditor(n);
        Node top = editor.createNode(100, 100, NodeType.STUB_END);
        Node bottom = editor.createNode(100, 200, NodeType.REGULAR);
        editor.createTrack(top.getId(), bottom.getId(), TrackType.MAINLINE);
        DiagramPanel panel = panel(n); panel.setSnapToGrid(false);
        BufferedImage image = render(panel);
        check(dark(image, 93, 94) && dark(image, 107, 94), "vertical stub has both outward arms");
        check(dark(image, 94, 100) && dark(image, 106, 100), "vertical stub has a perpendicular crossbar");
        check(!dark(image, 107, 106), "stub no longer draws a diagonal slash");
        bottom.setPosition(200, 100); image = render(panel);
        check(dark(image, 94, 93) && dark(image, 94, 107), "stub rotates for horizontal incoming track");
        check(dark(image, 100, 94) && dark(image, 100, 106), "rotated crossbar stays perpendicular");
        bottom.setPosition(200, 200); render(panel);
        bottom.setPosition(100, 100); render(panel);
        n.removeTrackSegment(1); image = render(panel);
        check(dark(image, 93, 94), "unconnected stub uses safe upright default");
    }

    private static boolean dark(BufferedImage image, int x, int y) {
        Color color = new Color(image.getRGB(x, y));
        return color.getRed() < 70 && color.getGreen() < 70 && color.getBlue() < 70;
    }
    private static BufferedImage render(DiagramPanel panel) {
        BufferedImage image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); panel.paint(graphics); graphics.dispose(); return image;
    }
    private static JButton button(NavigationToolbar toolbar, String text) {
        for (Component component : toolbar.getComponents()) {
            if (component instanceof JButton && ((JButton) component).getText().equals(text)) { return (JButton) component; }
        }
        throw new AssertionError("Missing button: " + text);
    }
    private static DiagramPanel panel(Network n){ DiagramPanel p = new DiagramPanel(n); p.setSize(800, 500); return p; }
    private static Point screen(DiagramPanel p, int x, int y) {
        Point2D point = p.worldToScreen(new Point(x, y)); return new Point((int) Math.round(point.getX()), (int) Math.round(point.getY()));
    }
    private static void cancel(DiagramPanel p){ p.getActionMap().get("cancel").actionPerformed(new ActionEvent(p, 0, "cancel")); }
    private static void gestureClick(DiagramPanel p, Point at){ press(p, at); release(p, at); }
    private static void gesture(DiagramPanel p, Point from, Point to){ press(p, from); drag(p, to); release(p, to); }
    private static void press(DiagramPanel p, Point at){ mouse(p, MouseEvent.MOUSE_PRESSED, at, MouseEvent.BUTTON1, MouseEvent.BUTTON1_DOWN_MASK); }
    private static void drag(DiagramPanel p, Point at){ mouse(p, MouseEvent.MOUSE_DRAGGED, at, MouseEvent.NOBUTTON, MouseEvent.BUTTON1_DOWN_MASK); }
    private static void release(DiagramPanel p, Point at){ mouse(p, MouseEvent.MOUSE_RELEASED, at, MouseEvent.BUTTON1, 0); }
    private static void mouse(DiagramPanel p, int kind, Point at, int button, int modifiers){ p.dispatchEvent(new MouseEvent(p, kind, 0, modifiers, at.x, at.y, 1, false, button)); }
    private static void near(double actual, double expected, String message){ check(Math.abs(actual - expected) < 1e-7, message); }
    private static void check(boolean condition, String message){ checks++; if (!condition) { throw new AssertionError(message); } }
    private static void rejected(Runnable action) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected rejected edit");
    }
}
