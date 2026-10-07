package ui;

import editor.DiagramEditor;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Objects;
import javax.swing.*;
import model.*;

/**
 * DiagramPanel<br>
 * Draws and edits tracks through a pannable, zoomable diagram view.<br>
 * Previews node moves and track gestures before committing edits.<br>
 */
public class DiagramPanel extends JPanel {
    /**
     * Tool<br>
     * Identifies the current canvas editing or navigation action.<br>
     */
    public enum Tool { SELECT, ADD_NODE, ADD_TRACK, PAN }

    private static final int HIT_RADIUS = 10;
    private static final int GRID_SIZE = 20;
    private final DiagramEditor editor;
    private final Network network;
    private final DiagramViewport viewport = new DiagramViewport();
    private boolean showDebugNodes;
    private boolean snapToGrid = true;
    private Tool tool = Tool.SELECT;
    private NodeType nodeType = NodeType.REGULAR;
    private TrackType trackType = TrackType.MAINLINE;
    private Node selectedNode;
    private Node trackStart;
    private Point connectionStart;
    private boolean trackGesture;
    private boolean trackDragged;
    private boolean startedThisPress;
    private Point pressPoint;
    private Node draggedNode;
    private Node hoveredNode;
    private Point pointer;
    private Point2D.Double dragOffset;
    private Point preview;
    private boolean dragMoved;
    private Point panPoint;
    private int panButton;
    private String statusMessage = "Select a node or drag it to move it. Mouse wheel zooms.";
    private boolean statusError;

    public DiagramPanel(Network network){ this(new DiagramEditor(network)); }

    public DiagramPanel(DiagramEditor editor) {
        this.editor = Objects.requireNonNull(editor, "An editor is required.");
        network = editor.getNetwork();
        setBackground(Color.WHITE);
        setFocusable(true);
        setToolTipText("Wheel: zoom. Middle drag or Pan tool: move the view. Escape: cancel.");
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event){ press(event); }
            @Override public void mouseDragged(MouseEvent event){ drag(event); }
            @Override public void mouseReleased(MouseEvent event){ release(event); }
            @Override public void mouseWheelMoved(MouseWheelEvent event){ zoomAt(Math.pow(1.15, -event.getPreciseWheelRotation()), event.getPoint()); }
            @Override public void mouseMoved(MouseEvent event) {
                pointer = event.getPoint();
                hoveredNode = nodeAt(pointer);
                repaint();
            }
            @Override public void mouseExited(MouseEvent event) {
                hoveredNode = null;
                if (draggedNode == null && !trackGesture) { pointer = null; }
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event){ cancelInteraction(); }
        });
        bindToolShortcut("N", Tool.ADD_NODE);
        bindToolShortcut("T", Tool.ADD_TRACK);
    }

    // Selection and editing tools
    public Tool getTool(){ return tool; }
    public Node getSelectedNode(){ return selectedNode; }

    public void setTool(Tool tool) {
        Tool previous = this.tool;
        this.tool = Objects.requireNonNull(tool, "A tool is required.");
        clearPendingEdit();
        int cursor = tool == Tool.PAN ? Cursor.HAND_CURSOR
                : tool == Tool.SELECT ? Cursor.DEFAULT_CURSOR : Cursor.CROSSHAIR_CURSOR;
        setCursor(Cursor.getPredefinedCursor(cursor));
        firePropertyChange("tool", previous, tool);
        showStatus(toolHint(), false);
        repaint();
    }

    public void setNodeType(NodeType type){ nodeType = Objects.requireNonNull(type, "A node type is required."); }
    public void setTrackType(TrackType type){ trackType = Objects.requireNonNull(type, "A track type is required."); }

    private void bindToolShortcut(String key, Tool tool) {
        String action = "tool-" + key;
        InputMap keys = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        keys.put(KeyStroke.getKeyStroke(key), action);
        keys.put(KeyStroke.getKeyStroke("shift " + key), action);
        getActionMap().put(action, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (!(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                        instanceof javax.swing.text.JTextComponent)) {
                    setTool(tool);
                }
            }
        });
    }

    private String toolHint() {
        switch (tool) {
            case ADD_NODE: return "Click empty space to add a node. Choose its type above.";
            case ADD_TRACK: return "Drag to draw a track, or click two existing nodes. Escape cancels.";
            case PAN: return "Drag to pan. Mouse wheel zooms at the pointer. Fit shows the whole diagram.";
            default: return "Select a node or drag it to move it. Wheel: zoom. Middle drag: pan.";
        }
    }

    // Status feedback
    public String getStatusMessage(){ return statusMessage; }
    public boolean isStatusError(){ return statusError; }

    private void showStatus(String message, boolean error) {
        statusMessage = message;
        statusError = error;
        firePropertyChange("status", null, message);
    }

    // View navigation and coordinates
    public double getZoom(){ return viewport.getZoom(); }
    public Point2D.Double worldToScreen(Point2D world){ return viewport.toScreen(world); }
    public Point2D.Double screenToWorld(Point2D screen){ return viewport.toWorld(screen); }

    public void zoomAt(double factor, Point2D anchor) {
        viewport.zoomAt(factor, anchor);
        viewChanged();
    }

    public void zoomIn(){ zoomAt(1.25, new Point(getWidth() / 2, getHeight() / 2)); }
    public void zoomOut(){ zoomAt(0.8, new Point(getWidth() / 2, getHeight() / 2)); }
    public void resetView(){ viewport.reset(); viewChanged(); }

    public void fitToDiagram() {
        if (network.nodeCount() == 0) { resetView(); return; }
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Node node : network.getAllNodes()) {
            minX = Math.min(minX, node.getX()); minY = Math.min(minY, node.getY());
            maxX = Math.max(maxX, node.getX()); maxY = Math.max(maxY, node.getY());
        }
        viewport.fit(new Rectangle2D.Double(minX - 12, minY - 12,
                maxX - minX + 24, maxY - minY + 24), getWidth(), getHeight());
        viewChanged();
    }

    private void viewChanged() {
        clearPendingEdit();
        hoveredNode = null;
        showStatus(toolHint(), false);
        firePropertyChange("view", null, getZoom());
        repaint();
    }

    // Display and snapping preferences
    public boolean isShowDebugNodes(){ return showDebugNodes; }
    public void setShowDebugNodes(boolean enabled){ showDebugNodes = enabled; repaint(); }
    public boolean isSnapToGrid(){ return snapToGrid; }

    public void setSnapToGrid(boolean enabled) {
        snapToGrid = enabled;
        clearPendingEdit();
        showStatus(toolHint(), false);
        repaint();
    }

    // Pending edits and cancellation
    public void cancelInteraction() {
        clearPendingEdit();
        showStatus("Edit cancelled. " + toolHint(), false);
        repaint();
    }

    private void clearPendingEdit() {
        trackStart = null;
        connectionStart = null;
        trackGesture = false;
        trackDragged = false;
        draggedNode = null;
        preview = null;
        dragOffset = null;
        dragMoved = false;
        panPoint = null;
    }

    // Mouse interactions
    private void press(MouseEvent event) {
        if (SwingUtilities.isRightMouseButton(event)) { cancelInteraction(); return; }
        if (SwingUtilities.isMiddleMouseButton(event)
                || (tool == Tool.PAN && SwingUtilities.isLeftMouseButton(event))) {
            clearPendingEdit();
            panPoint = event.getPoint();
            panButton = event.getButton();
            showStatus("Panning the view. Release to finish; the diagram stays unchanged.", false);
            return;
        }
        if (!SwingUtilities.isLeftMouseButton(event)) { return; }
        requestFocusInWindow();
        pointer = event.getPoint();
        Node hit = nodeAt(pointer);
        try {
            switch (tool) {
                case ADD_NODE:
                    Point position = placement(screenToWorld(pointer));
                    Node existing = hit != null ? hit : nodeAt(worldToScreen(position));
                    if (existing != null) {
                        selectedNode = existing;
                        showStatus("A node is already here. Selected node " + existing.getId() + ".", false);
                    } else {
                        selectedNode = editor.createNode(position.x, position.y, nodeType);
                        showStatus("Added node " + selectedNode.getId() + ".", false);
                    }
                    break;
                case ADD_TRACK:
                    startedThisPress = connectionStart == null;
                    if (startedThisPress) {
                        trackStart = hit;
                        connectionStart = hit == null ? placement(screenToWorld(pointer)) : positionOf(hit);
                    }
                    trackGesture = true;
                    trackDragged = false;
                    pressPoint = event.getPoint();
                    if (hit != null) { selectedNode = hit; }
                    showStatus("Drag to an endpoint, or click another existing node. Escape cancels.", false);
                    break;
                default:
                    selectedNode = hit;
                    if (hit != null) {
                        draggedNode = hit;
                        Point2D world = screenToWorld(pointer);
                        dragOffset = new Point2D.Double(world.getX() - hit.getX(), world.getY() - hit.getY());
                        preview = positionOf(hit);
                        dragMoved = false;
                        showStatus("Selected node " + hit.getId() + ". Drag to move it.", false);
                    } else {
                        showStatus(toolHint(), false);
                    }
                    break;
            }
        } catch (IllegalArgumentException failure) { showStatus(failure.getMessage(), true); }
        repaint();
    }

    private void drag(MouseEvent event) {
        if (panPoint != null) {
            viewport.pan(event.getX() - panPoint.x, event.getY() - panPoint.y);
            panPoint = event.getPoint();
            firePropertyChange("view", null, getZoom());
            repaint();
            return;
        }
        if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) { return; }
        pointer = event.getPoint();
        try {
            if (trackGesture) {
                trackDragged |= pressPoint.distance(pointer) >= 4;
                hoveredNode = nodeAt(pointer);
            } else if (draggedNode != null) {
                preview = movePosition(pointer);
                dragMoved = true;
            }
        } catch (IllegalArgumentException failure) { showStatus(failure.getMessage(), true); }
        repaint();
    }

    private void release(MouseEvent event) {
        if (panPoint != null) {
            if (event.getButton() == panButton) {
                viewport.pan(event.getX() - panPoint.x, event.getY() - panPoint.y);
                panPoint = null;
                firePropertyChange("view", null, getZoom());
                repaint();
                showStatus(toolHint(), false);
            }
            return;
        }
        if (!SwingUtilities.isLeftMouseButton(event)) { return; }
        pointer = event.getPoint();
        if (trackGesture) { releaseTrack(); return; }
        if (draggedNode == null) { return; }
        try {
            if (dragMoved) {
                Point position = movePosition(pointer);
                editor.moveNode(draggedNode.getId(), position.x, position.y);
                showStatus("Moved node " + draggedNode.getId() + " to (" + position.x + ", " + position.y + ").", false);
            }
        } catch (IllegalArgumentException failure) { showStatus(failure.getMessage(), true); }
        finally {
            draggedNode = null;
            preview = null;
            dragOffset = null;
            dragMoved = false;
            repaint();
        }
    }

    private void releaseTrack() {
        trackGesture = false;
        trackDragged |= pressPoint.distance(pointer) >= 4;
        Node end = nodeAt(pointer);
        try {
            if (trackDragged || (!startedThisPress && end != null)) {
                Point finish = end == null ? placement(screenToWorld(pointer)) : positionOf(end);
                boolean parallel = trackStart != null && end != null
                        && network.neighborsOf(trackStart.getId()).contains(end);
                TrackSegment segment = editor.createTrackBetween(
                        trackStart == null ? null : trackStart.getId(), connectionStart.x, connectionStart.y,
                        end == null ? null : end.getId(), finish.x, finish.y, nodeType, trackType);
                selectedNode = segment.getEnd();
                clearPendingEdit();
                showStatus("Added track " + segment.getId() + (parallel
                        ? ". Parallel tracks overlap until their layout is adjusted."
                        : ". Draw another track or choose a different tool."), false);
            } else if (trackStart == null) {
                clearPendingEdit();
                showStatus("Drag across empty space to draw a track, or click an existing node.", true);
            } else if (!startedThisPress) {
                showStatus("Choose an existing end node, or drag to create an endpoint.", true);
            }
        } catch (IllegalArgumentException failure) {
            if (trackStart == null) { clearPendingEdit(); }
            showStatus(failure.getMessage(), true);
        }
        repaint();
    }

    // Placement and hit testing
    private Point movePosition(Point screen) {
        Point2D world = screenToWorld(screen);
        return placement(new Point2D.Double(world.getX() - dragOffset.x, world.getY() - dragOffset.y));
    }

    private Point placement(Point2D point) {
        double x = point.getX(), y = point.getY();
        if (snapToGrid) {
            x = Math.round(x / GRID_SIZE) * (double) GRID_SIZE;
            y = Math.round(y / GRID_SIZE) * (double) GRID_SIZE;
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < Integer.MIN_VALUE
                || x > Integer.MAX_VALUE || y < Integer.MIN_VALUE || y > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("This position is outside the supported diagram coordinates.");
        }
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    private Node nodeAt(Point2D screen) {
        Node nearest = null;
        double distance = HIT_RADIUS * HIT_RADIUS;
        for (Node node : network.getAllNodes()) {
            double candidate = screen.distanceSq(worldToScreen(positionOf(node)));
            if (candidate <= distance) { nearest = node; distance = candidate; }
        }
        return nearest;
    }

    private Point positionOf(Node node){ return new Point(node.getX(), node.getY()); }
    private Point displayPosition(Node node){ return node == draggedNode && preview != null ? preview : positionOf(node); }

    // Diagram rendering
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (snapToGrid) { drawGrid(g); }
            Point2D origin = worldToScreen(new Point(0, 0));
            g.translate(origin.getX(), origin.getY());
            g.scale(getZoom(), getZoom());
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Color.BLACK);
            for (TrackSegment segment : network.getAllTrackSegments()) {
                g.draw(new Line2D.Double(displayPosition(segment.getStart()), displayPosition(segment.getEnd())));
            }
            for (Node node : network.getAllNodes()) { drawNode(g, node); }
            if (connectionStart != null && pointer != null) {
                Node end = nodeAt(pointer);
                Point finish;
                try { finish = end == null ? placement(screenToWorld(pointer)) : positionOf(end); }
                catch (IllegalArgumentException invalid) { return; }
                g.setColor(new Color(40, 110, 190));
                g.setStroke(new BasicStroke((float) (1.5 / getZoom()), BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10, new float[]{(float) (6 / getZoom()), (float) (4 / getZoom())}, 0));
                g.draw(new Line2D.Double(connectionStart, finish));
            }
        } finally { g.dispose(); }
    }

    private void drawGrid(Graphics2D g) {
        double step = GRID_SIZE * getZoom();
        while (step < 8) { step *= 2; }
        Point2D origin = worldToScreen(new Point());
        double firstX = origin.getX() - Math.ceil(origin.getX() / step) * step;
        double firstY = origin.getY() - Math.ceil(origin.getY() / step) * step;
        g.setColor(new Color(235, 239, 243));
        for (double x = firstX; x < getWidth(); x += step) {
            for (double y = firstY; y < getHeight(); y += step) { g.fillRect((int) x, (int) y, 1, 1); }
        }
    }

    private void drawNode(Graphics2D g, Node node) {
        Point position = displayPosition(node);
        double x = position.x, y = position.y;
        g.setStroke(new BasicStroke(1f));
        if (showDebugNodes) {
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
            g.setColor(Color.BLACK);
            g.draw(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
        }
        if (node.getNodeType() == NodeType.STUB_END) { drawStubEnd(g, node, position); }
        // Handles keep a constant screen size, independent of debug symbols.
        double radius = 3 / getZoom();
        g.setColor(new Color(65, 115, 160));
        g.setStroke(new BasicStroke((float) (1 / getZoom())));
        g.draw(new Rectangle2D.Double(x - radius, y - radius, 2 * radius, 2 * radius));
        if (node == selectedNode || node == hoveredNode || node == trackStart) {
            radius = 8 / getZoom();
            g.setColor(node == trackStart ? new Color(195, 120, 20) : new Color(30, 115, 210));
            g.setStroke(new BasicStroke((float) (2 / getZoom())));
            g.draw(new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius));
        }
    }

    private void drawStubEnd(Graphics2D g, Node node, Point end) {
        double ux = 0, uy = -1;
        for (TrackSegment segment : network.segmentsAt(node.getId())) {
            Node other = segment.getStart() == node ? segment.getEnd() : segment.getStart();
            Point start = displayPosition(other);
            double dx = end.getX() - start.getX(), dy = end.getY() - start.getY();
            double length = Math.hypot(dx, dy);
            if (length > 0) { ux = dx / length; uy = dy / length; break; }
        }
        double px = -uy * 7, py = ux * 7;
        Path2D shape = new Path2D.Double();
        shape.moveTo(end.x - px + ux * 7, end.y - py + uy * 7);
        shape.lineTo(end.x - px, end.y - py);
        shape.lineTo(end.x + px, end.y + py);
        shape.lineTo(end.x + px + ux * 7, end.y + py + uy * 7);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.draw(shape);
    }
}
