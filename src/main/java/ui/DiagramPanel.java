package ui;

import editor.DiagramEditor;
import editor.PlatformGeometry;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Objects;
import javax.swing.*;
import model.*;
import settings.AppSettings;

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
    public enum Tool { SELECT, ADD_NODE, ADD_TRACK, ADD_STATION, ADD_TEXT, ADD_PLATFORM, PAN }

    private static final int HIT_RADIUS = 10;
    private AppSettings settings = new AppSettings();
    private final DiagramEditor editor;
    private final Network network;
    private final DiagramViewport viewport = new DiagramViewport();
    private boolean showDebugNodes;
    private boolean snapToGrid = true;
    private boolean continuousDraw;
    private Tool tool = Tool.SELECT;
    private NodeType nodeType = NodeType.REGULAR;
    private TrackType trackType = TrackType.MAINLINE;
    private Node selectedNode;
    private Object selectedLabel;
    private Platform selectedPlatform;
    private Object draggedLabel;
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
    private String statusMessage = "Select an object or drag it to move it. Double-click labels to edit. Mouse wheel zooms.";
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
            @Override public void mouseWheelMoved(MouseWheelEvent event){
                double direction = settings.invertWheelZoom ? 1 : -1;
                zoomAt(Math.pow(1 + settings.wheelZoomPercent / 100.0,
                        direction * event.getPreciseWheelRotation()), event.getPoint());
            }
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
        bindToolShortcut("S", Tool.ADD_STATION);
        bindToolShortcut("P", Tool.ADD_PLATFORM);
        bindToolShortcut("X", Tool.ADD_TEXT);
        editor.getHistory().addListener(() -> firePropertyChange("history", null, editor.getHistory()));
    }

    // Selection and editing tools
    public Tool getTool(){ return tool; }
    public NodeType getNodeType(){ return nodeType; }
    public TrackType getTrackType(){ return trackType; }
    public int getGridSpacing(){ return settings.gridSpacing; }

    public void applySettings(AppSettings preferences) {
        preferences.validate();
        settings = preferences.copy();
        clearPendingEdit();
        setSnapToGrid(settings.snapToGrid);
        setContinuousDraw(settings.continuousDraw);
        setNodeType(settings.defaultNodeType);
        setTrackType(settings.defaultTrackType);
        setShowDebugNodes(settings.showDebugNodes);
        setBackground(Color.decode(settings.backgroundColor));
        editor.getHistory().setLimit(settings.undoLimit);
        firePropertyChange("preferences", null, settings.copy());
        repaint();
    }

    public void undo(){ changeHistory(false); }
    public void redo(){ changeHistory(true); }

    private void changeHistory(boolean redo) {
        clearPendingEdit();
        selectedNode = null; selectedLabel = null; selectedPlatform = null; hoveredNode = null;
        String name = redo ? editor.getHistory().redoName() : editor.getHistory().undoName();
        if (redo) { editor.getHistory().redo(); } else { editor.getHistory().undo(); }
        showStatus(name.isEmpty() ? "No edit to " + (redo ? "redo." : "undo.")
                : (redo ? "Redid: " : "Undid: ") + name + ".", false);
        repaint();
    }
    public Node getSelectedNode(){ return selectedNode; }
    public Platform getSelectedPlatform(){ return selectedPlatform; }
    public Station getSelectedStation(){ return selectedLabel instanceof Station ? (Station) selectedLabel : null; }
    public CustomText getSelectedCustomText(){ return selectedLabel instanceof CustomText ? (CustomText) selectedLabel : null; }

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
            case ADD_TRACK: return "Drag to draw a track, or click endpoints; empty clicks add nodes. Escape cancels.";
            case ADD_STATION: return "Click to place a named station. Select and double-click to rename it.";
            case ADD_PLATFORM: return "Click a track to add a side or island platform to a station.";
            case ADD_TEXT: return "Click to place custom text. Select and double-click to edit it.";
            case PAN: return "Drag to pan. Mouse wheel zooms at the pointer. Fit shows the whole diagram.";
            default: return "Select an object or drag it to move it. Double-click labels to edit. Wheel: zoom. Middle drag: pan.";
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
        clearPendingEdit();
        Rectangle2D bounds = null;
        for (Node node : network.getAllNodes()) {
            Rectangle2D item = new Rectangle2D.Double(node.getX() - 12.0, node.getY() - 12.0, 24, 24);
            bounds = bounds == null ? item : bounds.createUnion(item);
        }
        for (Station station : network.getAllStations()) {
            Rectangle2D item = labelBounds(station);
            bounds = bounds == null ? item : bounds.createUnion(item);
        }
        for (CustomText text : network.getAllCustomTexts()) {
            Rectangle2D item = labelBounds(text);
            bounds = bounds == null ? item : bounds.createUnion(item);
        }
        for (Station station : network.getAllStations()) {
            for (Platform platform : station.getPlatforms()) {
                Rectangle2D item = platformBounds(platform);
                if (item != null) { bounds = bounds == null ? item : bounds.createUnion(item); }
            }
        }
        if (bounds == null) { resetView(); return; }
        viewport.fit(bounds, getWidth(), getHeight());
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

    // Continuous track drawing
    public boolean isContinuousDraw(){ return continuousDraw; }

    public void setContinuousDraw(boolean enabled) {
        boolean previous = continuousDraw;
        continuousDraw = enabled;
        if (!enabled) { clearPendingEdit(); }
        firePropertyChange("continuousDraw", previous, enabled);
        showStatus(enabled ? "Continuous Draw enabled. Each track end starts the next track. Escape ends the chain."
                : "Continuous Draw disabled. " + toolHint(), false);
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
        draggedLabel = null;
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
        selectedLabel = null;
        selectedPlatform = null;
        if (tool == Tool.ADD_STATION || tool == Tool.ADD_TEXT) { selectedNode = null; }
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
                    showStatus("Drag or click the other endpoint; empty space creates a node. Escape cancels.", false);
                    break;
                case ADD_PLATFORM: {
                    selectedNode = null;
                    if (network.stationCount() == 0 || network.segmentCount() == 0) {
                        showStatus("Add a station and at least one track before creating a platform.", true);
                        break;
                    }
                    applyPlatformDialog(null, trackAt(pointer));
                    break;
                }
                case ADD_STATION: {
                    Point at = placement(screenToWorld(pointer));
                    String name = requestStationName(null);
                    if (name != null) {
                        selectedLabel = editor.createStation(name, at.x, at.y);
                        showStatus("Added station. Select and double-click to rename it.", false);
                    } else { showStatus("Station placement cancelled.", false); }
                    break;
                }
                case ADD_TEXT: {
                    Point at = placement(screenToWorld(pointer));
                    CustomText draft = requestCustomText(null);
                    if (draft != null) {
                        selectedLabel = editor.createCustomText(draft.getText(), at.x, at.y,
                                draft.getSize(), draft.getFont(), draft.getColor());
                        showStatus("Added text. Select and double-click to edit it.", false);
                    } else { showStatus("Text placement cancelled.", false); }
                    break;
                }
                default:
                    selectedLabel = labelAt(pointer);
                    if (selectedLabel != null) {
                        selectedNode = null;
                        if (event.getClickCount() >= 2) { editLabel(); break; }
                        draggedLabel = selectedLabel;
                        Point at = labelPosition(selectedLabel);
                        Point2D world = screenToWorld(pointer);
                        dragOffset = new Point2D.Double(world.getX() - at.x, world.getY() - at.y);
                        preview = at;
                        dragMoved = false;
                        showStatus("Drag to move the label; double-click to edit it.", false);
                        break;
                    }
                    // Node handles remain reachable when a platform touches a track.
                    selectedPlatform = hit == null ? platformAt(pointer) : null;
                    if (selectedPlatform != null) {
                        selectedNode = null;
                        if (event.getClickCount() >= 2) { applyPlatformDialog(selectedPlatform, null); }
                        else { showStatus("Platform " + selectedPlatform.getNumber()
                                + ": double-click to edit placement, number, or delete.", false); }
                        break;
                    }
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
            } else if (draggedNode != null || draggedLabel != null) {
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
        if (draggedLabel != null) {
            try {
                if (dragMoved) {
                    Point at = movePosition(pointer);
                    if (draggedLabel instanceof Station) {
                        editor.moveStation(((Station) draggedLabel).getId(), at.x, at.y);
                    } else { editor.moveCustomText(((CustomText) draggedLabel).getId(), at.x, at.y); }
                    showStatus("Moved label.", false);
                }
            } catch (IllegalArgumentException failure) { showStatus(failure.getMessage(), true); }
            finally { clearPendingEdit(); repaint(); }
            return;
        }
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
            draggedLabel = null;
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
            if (trackDragged || !startedThisPress) {
                Point finish = end == null ? placement(screenToWorld(pointer)) : positionOf(end);
                boolean parallel = trackStart != null && end != null
                        && network.neighborsOf(trackStart.getId()).contains(end);
                TrackSegment segment = editor.createTrackBetween(
                        trackStart == null ? null : trackStart.getId(), connectionStart.x, connectionStart.y,
                        end == null ? null : end.getId(), finish.x, finish.y, nodeType, trackType);
                selectedNode = segment.getEnd();
                clearPendingEdit();
                if (continuousDraw) {
                    trackStart = segment.getEnd();
                    connectionStart = positionOf(trackStart);
                }
                showStatus("Added track " + segment.getId()
                        + (parallel ? ". Parallel tracks overlap until their layout is adjusted." : ".")
                        + (continuousDraw ? " Click or drag to continue from this endpoint. Escape ends the chain."
                                : " Draw another track or choose a different tool."), false);
            } else if (trackStart == null) {
                trackStart = nodeAt(worldToScreen(connectionStart));
                if (trackStart == null) {
                    trackStart = editor.createNode(connectionStart.x, connectionStart.y, nodeType);
                }
                connectionStart = positionOf(trackStart);
                selectedNode = trackStart;
                showStatus("Added starting node. Click or drag to the other endpoint. Escape cancels the connection.", false);
            }
        } catch (IllegalArgumentException failure) {
            if (trackStart == null) { clearPendingEdit(); }
            showStatus(failure.getMessage(), true);
        }
        repaint();
    }

    // Platform editing and drawing
    protected PlatformDialog.Result requestPlatform(Platform existing, TrackSegment track) {
        return new PlatformDialog(editor, existing, track, settings).showDialog(this);
    }

    private void applyPlatformDialog(Platform existing, TrackSegment track) {
        PlatformDialog.Result result = requestPlatform(existing, track);
        if (result == null) { showStatus("Platform edit cancelled.", false); return; }
        if (result.delete) {
            if (existing == null) { throw new IllegalArgumentException("Select a platform to delete."); }
            editor.deletePlatform(existing.getStation().getId(), existing.getNumber());
            selectedPlatform = null;
            showStatus("Deleted platform; its tracks remain in the diagram.", false);
            return;
        }
        Platform replacement = result.platform;
        PlatformEdge[] edges = replacement.getEdges().toArray(new PlatformEdge[0]);
        if (existing == null) {
            selectedPlatform = editor.createPlatform(result.station.getId(), replacement.getNumber(), edges);
        } else {
            editor.editPlatform(existing.getStation().getId(), existing.getNumber(), replacement.getNumber(), edges);
            selectedPlatform = existing;
        }
        showStatus("Platform " + selectedPlatform.getNumber() + " updated. Double-click to edit it.", false);
    }

    private Path2D platformShape(Platform platform) {
        return PlatformGeometry.outline(platform, node -> displayPosition(node));
    }

    private Rectangle2D platformBounds(Platform platform) {
        try {
            Rectangle2D shape = platformShape(platform).getBounds2D();
            CustomText text = platformNumber(platform, shape);
            return shape.createUnion(labelBounds(text));
        } catch (IllegalArgumentException invalid) { return null; }
    }

    private CustomText platformNumber(Platform platform, Rectangle2D shape) {
        String number = "" + platform.getNumber();
        FontMetrics metrics = getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        return new CustomText(number, (int) Math.round(shape.getCenterX() - metrics.stringWidth(number) / 2.0),
                (int) Math.round(shape.getCenterY() - metrics.getHeight() / 2.0), 12,
                Font.SANS_SERIF, "#333333", 0);
    }

    private Platform platformAt(Point2D screen) {
        Point2D world = screenToWorld(screen);
        Platform hit = null;
        for (Station station : network.getAllStations()) {
            for (Platform platform : station.getPlatforms()) {
                try {
                    Path2D shape = platformShape(platform);
                    if (shape.contains(world) || labelBounds(platformNumber(platform, shape.getBounds2D())).contains(world)) {
                        hit = platform;
                    }
                } catch (IllegalArgumentException invalid) { /* Invalid legacy geometry has no selectable surface. */ }
            }
        }
        return hit;
    }

    private TrackSegment trackAt(Point2D screen) {
        TrackSegment hit = null;
        double nearest = HIT_RADIUS;
        for (TrackSegment track : network.getAllTrackSegments()) {
            double distance = new Line2D.Double(worldToScreen(positionOf(track.getStart())),
                    worldToScreen(positionOf(track.getEnd()))).ptSegDist(screen);
            if (distance <= nearest) { hit = track; nearest = distance; }
        }
        return hit;
    }

    private void drawPlatform(Graphics2D g, Platform platform) {
        try {
            Path2D shape = platformShape(platform);
            g.setColor(new Color(222, 226, 230));
            g.fill(shape);
            g.setColor(platform == selectedPlatform ? new Color(30, 115, 210) : new Color(90, 95, 100));
            g.setStroke(new BasicStroke((float) ((platform == selectedPlatform ? 2 : 1) / getZoom())));
            g.draw(shape);
            drawLabel(g, platformNumber(platform, shape.getBounds2D()));
        } catch (IllegalArgumentException invalid) { /* A zero-length preview cannot define a platform surface. */ }
    }

    // Label dialogs and validated edits
    protected String requestStationName(Station station) {
        return (String) JOptionPane.showInputDialog(this, "Station name:", "Station",
                JOptionPane.PLAIN_MESSAGE, null, null, station == null ? "" : station.getName());
    }

    protected CustomText requestCustomText(CustomText existing) {
        JTextArea text = new JTextArea(existing == null ? "" : existing.getText(), 4, 28);
        JComboBox<String> font = new JComboBox<>(GraphicsEnvironment
                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        font.setEditable(true);
        font.setSelectedItem(existing == null ? settings.textFont : existing.getFont());
        JTextField size = new JTextField(existing == null ? "" + settings.textSize : "" + existing.getSize());
        JTextField color = new JTextField(existing == null ? settings.textColor : existing.getColor());
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Text (multiple lines supported):"));
        form.add(new JScrollPane(text));
        form.add(new JLabel("Font:")); form.add(font);
        form.add(new JLabel("Size in diagram units:")); form.add(size);
        form.add(new JLabel("Colour (#RRGGBB):")); form.add(color);
        while (JOptionPane.showConfirmDialog(this, form, "Custom text",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                return new CustomText(text.getText(), 0, 0, Integer.parseInt(size.getText().trim()),
                        String.valueOf(font.getSelectedItem()), color.getText().trim(), 0);
            } catch (IllegalArgumentException failure) {
                JOptionPane.showMessageDialog(this, failure.getMessage(), "Invalid text", JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private void editLabel() {
        if (selectedLabel instanceof Station) {
            Station station = (Station) selectedLabel;
            String name = requestStationName(station);
            if (name != null) { editor.renameStation(station.getId(), name); }
        } else {
            CustomText text = (CustomText) selectedLabel;
            CustomText draft = requestCustomText(text);
            if (draft != null) {
                editor.editCustomText(text.getId(), draft.getText(), draft.getSize(), draft.getFont(), draft.getColor());
            }
        }
        showStatus("Label editing finished.", false);
    }

    // Label geometry and drawing
    private Point labelPosition(Object label) {
        if (label == draggedLabel && preview != null) { return preview; }
        if (label instanceof Station) { return new Point(((Station) label).getX(), ((Station) label).getY()); }
        CustomText text = (CustomText) label;
        return new Point(text.getX(), text.getY());
    }

    private CustomText labelStyle(Object label) {
        if (label instanceof CustomText) { return (CustomText) label; }
        Station station = (Station) label;
        return new CustomText(station.getName(), station.getX(), station.getY(), 16,
                Font.SANS_SERIF, "#000000", station.getId());
    }

    private Font labelFont(Object label) {
        CustomText text = labelStyle(label);
        return new Font(text.getFont(), label instanceof Station ? Font.BOLD : Font.PLAIN, text.getSize());
    }

    private Rectangle2D labelBounds(Object label) {
        FontMetrics metrics = getFontMetrics(labelFont(label));
        String[] lines = labelStyle(label).getText().split("\\R", -1);
        double width = 1;
        for (String line : lines) { width = Math.max(width, metrics.stringWidth(line)); }
        Point at = labelPosition(label);
        return new Rectangle2D.Double(at.x, at.y,
                width + (label instanceof Station ? 20 : 0), (double) metrics.getHeight() * lines.length);
    }

    private Object labelAt(Point2D screen) {
        Point2D world = screenToWorld(screen);
        Object hit = null;
        for (Station station : network.getAllStations()) {
            if (labelBounds(station).contains(world)) { hit = station; }
        }
        for (CustomText text : network.getAllCustomTexts()) {
            if (labelBounds(text).contains(world)) { hit = text; }
        }
        return hit;
    }

    private void drawLabel(Graphics2D g, Object label) {
        CustomText text = labelStyle(label);
        Point at = labelPosition(label);
        g.setFont(labelFont(label));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(Color.decode(text.getColor()));
        int offset = label instanceof Station ? 20 : 0;
        if (label instanceof Station) {
            g.setStroke(new BasicStroke(2));
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Double(at.x + 2.0, at.y + 4.0, 12, 12));
            g.setColor(Color.BLACK);
            g.draw(new Rectangle2D.Double(at.x + 2.0, at.y + 4.0, 12, 12));
        }
        float baseline = (float) at.y + metrics.getAscent();
        for (String line : text.getText().split("\\R", -1)) {
            g.drawString(line, (float) at.x + offset, baseline);
            baseline += metrics.getHeight();
        }
        if (label == selectedLabel) {
            g.setColor(new Color(30, 115, 210));
            g.setStroke(new BasicStroke((float) (1 / getZoom())));
            g.draw(labelBounds(label));
        }
    }

    // Placement and hit testing
    private Point movePosition(Point screen) {
        Point2D world = screenToWorld(screen);
        return placement(new Point2D.Double(world.getX() - dragOffset.x, world.getY() - dragOffset.y));
    }

    private Point placement(Point2D point) {
        double x = point.getX(), y = point.getY();
        if (snapToGrid) {
            x = Math.round(x / settings.gridSpacing) * (double) settings.gridSpacing;
            y = Math.round(y / settings.gridSpacing) * (double) settings.gridSpacing;
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
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, settings.antialiasing
                    ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, settings.antialiasing
                    ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            if (settings.showGrid) { drawGrid(g); }
            Point2D origin = worldToScreen(new Point(0, 0));
            g.translate(origin.getX(), origin.getY());
            g.scale(getZoom(), getZoom());
            for (Station station : network.getAllStations()) {
                for (Platform platform : station.getPlatforms()) { drawPlatform(g, platform); }
            }
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Color.BLACK);
            for (TrackSegment segment : network.getAllTrackSegments()) {
                g.draw(new Line2D.Double(displayPosition(segment.getStart()), displayPosition(segment.getEnd())));
            }
            if (tool == Tool.ADD_PLATFORM && pointer != null) {
                TrackSegment hovered = trackAt(pointer);
                if (hovered != null) {
                    g.setColor(new Color(195, 120, 20));
                    g.setStroke(new BasicStroke((float) (3 / getZoom())));
                    g.draw(new Line2D.Double(displayPosition(hovered.getStart()), displayPosition(hovered.getEnd())));
                }
            }
            for (Node node : network.getAllNodes()) { drawNode(g, node); }
            for (Station station : network.getAllStations()) { drawLabel(g, station); }
            for (CustomText text : network.getAllCustomTexts()) { drawLabel(g, text); }
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
        double step = settings.gridSpacing * getZoom();
        while (step < 8) { step *= 2; }
        Point2D origin = worldToScreen(new Point());
        double firstX = origin.getX() - Math.ceil(origin.getX() / step) * step;
        double firstY = origin.getY() - Math.ceil(origin.getY() / step) * step;
        g.setColor(Color.decode(settings.gridColor));
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
