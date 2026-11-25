package ui;

import i18n.Messages;

import editor.DiagramEditor;
import editor.PlatformGeometry;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
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
    private boolean exportRendering;
    private boolean snapToGrid = true;
    private boolean continuousDraw;
    private Tool tool = Tool.SELECT;
    private NodeType nodeType = NodeType.REGULAR;
    private TrackType trackType = TrackType.MAINLINE;
    private Node selectedNode;
    private TrackSegment selectedTrack;
    private boolean spaceHeld;
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
    private String statusMessage = Messages.text("diagramPanel.select.an.object.or.drag.it.to.move.it");
    private boolean statusError;

    public DiagramPanel(Network network){ this(new DiagramEditor(network)); }

    public DiagramPanel(DiagramEditor editor) {
        this.editor = Objects.requireNonNull(editor, Messages.text("diagramPanel.an.editor.is.required"));
        network = editor.getNetwork();
        setBackground(Color.WHITE);
        setFocusable(true);
        setToolTipText(Messages.text("diagramPanel.wheel.zoom.space.drag.middle.drag.or.pan.tool"));
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event){ press(event); }
            @Override public void mouseDragged(MouseEvent event){ drag(event); }
            @Override public void mouseReleased(MouseEvent event){ release(event); }
            @Override public void mouseWheelMoved(MouseWheelEvent event){
                double direction = settings.invertWheelZoom ? 1 : -1;
                zoomWithWheel(Math.pow(1 + settings.wheelZoomPercent / 100.0,
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
        InputMap canvasKeys = getInputMap(JComponent.WHEN_FOCUSED);
        canvasKeys.put(KeyStroke.getKeyStroke("DELETE"), "delete-selection");
        canvasKeys.put(KeyStroke.getKeyStroke("BACK_SPACE"), "delete-selection");
        getActionMap().put("delete-selection", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { deleteSelection(); }
        });
        canvasKeys.put(KeyStroke.getKeyStroke("pressed SPACE"), "pan-pressed");
        canvasKeys.put(KeyStroke.getKeyStroke("released SPACE"), "pan-released");
        getActionMap().put("pan-pressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                spaceHeld = true;
                updateCursor();
            }
        });
        getActionMap().put("pan-released", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                spaceHeld = false;
                updateCursor();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) {
                spaceHeld = false;
                clearPendingEdit();
                updateCursor();
                repaint();
            }
        });
        bindToolShortcut("A", Tool.SELECT);
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
        selectedNode = null; selectedTrack = null; selectedLabel = null; selectedPlatform = null; hoveredNode = null;
        selectedNode = null; selectedTrack = null; selectedLabel = null; selectedPlatform = null; hoveredNode = null;
        String name = redo ? editor.getHistory().redoName() : editor.getHistory().undoName();
        if (redo) { editor.getHistory().redo(); } else { editor.getHistory().undo(); }
        showStatus(name.isEmpty() ? Messages.text(redo ? "history.emptyRedo" : "history.emptyUndo")
                : Messages.text(redo ? "history.redone" : "history.undone", name), false);
        repaint();
    }
    public Node getSelectedNode(){ return selectedNode; }
    public TrackSegment getSelectedTrack(){ return selectedTrack; }
    public Platform getSelectedPlatform(){ return selectedPlatform; }
    public Station getSelectedStation(){ return selectedLabel instanceof Station ? (Station) selectedLabel : null; }
    public CustomText getSelectedCustomText(){ return selectedLabel instanceof CustomText ? (CustomText) selectedLabel : null; }

    public void setTool(Tool tool) {
        Tool previous = this.tool;
        this.tool = Objects.requireNonNull(tool, "A tool is required.");
        clearPendingEdit();
        updateCursor();
        firePropertyChange("tool", previous, tool);
        showStatus(toolHint(), false);
        repaint();
    }

    private void updateCursor() {
        int cursor = spaceHeld || tool == Tool.PAN ? Cursor.HAND_CURSOR
                : tool == Tool.SELECT ? Cursor.DEFAULT_CURSOR : Cursor.CROSSHAIR_CURSOR;
        setCursor(Cursor.getPredefinedCursor(cursor));
    }

    public void setNodeType(NodeType type){ nodeType = Objects.requireNonNull(type, "A node type is required."); }
    public void setTrackType(TrackType type){ trackType = Objects.requireNonNull(type, "A track type is required."); }

    private void refreshCursor() {
        setCursor(panPoint != null ? PanCursor.grabbing() : Cursor.getPredefinedCursor(
                tool == Tool.PAN ? Cursor.HAND_CURSOR : tool == Tool.SELECT ? Cursor.DEFAULT_CURSOR : Cursor.CROSSHAIR_CURSOR));
    }

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
            case ADD_NODE: return Messages.text("diagramPanel.click.empty.space.to.add.a.node.choose.its");
            case ADD_TRACK: return Messages.text("diagramPanel.drag.to.draw.a.track.or.click.endpoints.empty");
            case ADD_STATION: return Messages.text("diagramPanel.click.to.place.a.named.station.select.and.double");
            case ADD_PLATFORM: return Messages.text("diagramPanel.click.a.track.to.add.a.side.or.island");
            case ADD_TEXT: return Messages.text("diagramPanel.click.to.place.custom.text.select.and.double.click");
            case PAN: return Messages.text("diagramPanel.drag.to.pan.mouse.wheel.zooms.at.the.pointer");
            default: return Messages.text("diagramPanel.select.an.object.or.drag.it.to.move.it.alternate");
        }
    }

    private void deleteSelection() {
        clearPendingEdit();
        try {
            String name;
            if (selectedNode != null) {
                editor.deleteNode(selectedNode.getId()); name = Messages.text("selection.node");
            } else if (selectedTrack != null) {
                editor.deleteTrack(selectedTrack.getId()); name = Messages.text("selection.track");
            } else if (selectedPlatform != null) {
                editor.deletePlatform(selectedPlatform.getStation().getId(), selectedPlatform.getNumber()); name = Messages.text("selection.platform");
            } else if (selectedLabel instanceof Station) {
                editor.deleteStation(((Station) selectedLabel).getId()); name = Messages.text("selection.station");
            } else if (selectedLabel instanceof CustomText) {
                editor.deleteCustomText(((CustomText) selectedLabel).getId()); name = Messages.text("selection.text");
            } else {
                showStatus(Messages.text("diagramPanel.select.an.object.to.delete"), false);
                return;
            }
            selectedNode = null; selectedTrack = null; selectedPlatform = null; selectedLabel = null;
            hoveredNode = null;
            showStatus(Messages.text("selection.deleted", name), false);
        } catch (IllegalArgumentException failure) { showStatus(failure.getMessage(), true); }
        finally { repaint(); }
    }

    // Status feedback
    public String getStatusMessage(){
        String identity = selectedLabel instanceof Station ? "ID: " + ((Station) selectedLabel).getId()
                : selectedLabel instanceof CustomText ? "ID: " + ((CustomText) selectedLabel).getId()
                : selectedPlatform != null ? "ID: " + selectedPlatform.getNumber() + " (Station ID: " + selectedPlatform.getStation().getId() + ")"
                : selectedNode != null ? "ID: " + selectedNode.getId()
                : selectedTrack != null ? "ID: " + selectedTrack.getId() : "";
        return identity.isEmpty() ? statusMessage : identity + " — " + statusMessage;
    }
    public boolean isStatusError(){ return statusError; }

    private void showStatus(String message, boolean error) {
        statusMessage = message;
        statusError = error;
        firePropertyChange("status", null, getStatusMessage());
    }

    // View navigation and coordinates
    public double getZoom(){ return viewport.getZoom(); }
    public Point2D.Double worldToScreen(Point2D world){ return viewport.toScreen(world); }
    public Point2D.Double screenToWorld(Point2D screen){ return viewport.toWorld(screen); }

    public void zoomAt(double factor, Point2D anchor) {
        viewport.zoomAt(factor, anchor);
        viewChanged();
    }

    private void zoomWithWheel(double factor, Point anchor) {
        if (tool != Tool.ADD_TRACK || connectionStart == null) {
            zoomAt(factor, anchor);
            return;
        }
        // Track endpoints are stored in world coordinates, so zoom can keep the
        // pending click, drag, or continuous chain without moving its start.
        viewport.zoomAt(factor, anchor);
        pointer = anchor;
        hoveredNode = nodeAt(pointer);
        firePropertyChange("view", null, getZoom());
        repaint();
    }

    public void zoomIn(){ zoomAt(1.25, new Point(getWidth() / 2, getHeight() / 2)); }
    public void zoomOut(){ zoomAt(0.8, new Point(getWidth() / 2, getHeight() / 2)); }
    public void resetView(){ viewport.reset(); viewChanged(); }

    public void fitToDiagram() {
        clearPendingEdit();
        Rectangle2D bounds = diagramBounds();
        if (bounds == null) { resetView(); return; }
        viewport.fit(bounds, getWidth(), getHeight());
        viewChanged();
    }

    private Rectangle2D diagramBounds() {
        Rectangle2D bounds = null;
        for (Node node : network.getAllNodes()) {
            Rectangle2D item = new Rectangle2D.Double(node.getX() - 16.0, node.getY() - 16.0, 32, 32);
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
        return bounds;
    }

    /** Renders committed content through a fresh canvas, independent of editor gestures and view. */
    public static BufferedImage renderImage(Network network, double scale, int padding, Color background) {
        DiagramPanel canvas = new DiagramPanel(network);
        canvas.exportRendering = true;
        Rectangle2D bounds = canvas.diagramBounds();
        if (bounds == null) { throw new IllegalArgumentException(Messages.text("diagramPanel.the.diagram.is.empty")); }
        Dimension size = exportSize(bounds, scale, padding);
        BufferedImage image = new BufferedImage(size.width, size.height,
                background == null ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            if (background != null) { g.setColor(background); g.fillRect(0, 0, size.width, size.height); }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(padding + 2 * scale, padding + 2 * scale);
            g.scale(scale, scale);
            g.translate(-bounds.getMinX(), -bounds.getMinY());
            canvas.drawDiagram(g);
        } finally { g.dispose(); }
        return image;
    }

    public static Dimension imageSize(Network network, double scale, int padding) {
        Rectangle2D bounds = new DiagramPanel(network).diagramBounds();
        if (bounds == null) { throw new IllegalArgumentException(Messages.text("diagramPanel.the.diagram.is.empty")); }
        return exportSize(bounds, scale, padding);
    }

    private static Dimension exportSize(Rectangle2D bounds, double scale, int padding) {
        if (!Double.isFinite(scale) || scale <= 0 || scale > 8 || padding < 0 || padding > 1000) {
            throw new IllegalArgumentException(Messages.text("diagramPanel.use.a.scale.above.0.and.up.to.800"));
        }
        double width = Math.ceil((bounds.getWidth() + 4) * scale) + 2.0 * padding;
        double height = Math.ceil((bounds.getHeight() + 4) * scale) + 2.0 * padding;
        if (width > 16384 || height > 16384 || width * height > 40_000_000) {
            throw new IllegalArgumentException(Messages.text("diagramPanel.image.is.too.large.maximum.16.384.pixels.per"));
        }
        return new Dimension(Math.max(1, (int) width), Math.max(1, (int) height));
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
        showStatus(enabled ? Messages.text("diagramPanel.continuous.draw.enabled.each.track.end.starts.the.next")
                : Messages.text("tool.continuousDisabled", toolHint()), false);
        repaint();
    }

    // Pending edits and cancellation
    public void cancelInteraction() {
        clearPendingEdit();
        showStatus(Messages.text("tool.cancelled", toolHint()), false);
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
        refreshCursor();
    }

    // Mouse interactions
    private void press(MouseEvent event) {
        requestFocusInWindow();
        if (SwingUtilities.isRightMouseButton(event)) { cancelInteraction(); return; }
        if (SwingUtilities.isMiddleMouseButton(event)
                || ((spaceHeld || tool == Tool.PAN) && SwingUtilities.isLeftMouseButton(event))) {
            clearPendingEdit();
            panPoint = event.getPoint();
            panButton = event.getButton();
            showStatus(Messages.text("diagramPanel.panning.the.view.release.to.finish.the.diagram.stays"), false);
            return;
        }
        if (!SwingUtilities.isLeftMouseButton(event)) { return; }
        requestFocusInWindow();
        pointer = event.getPoint();
        Node hit = nodeAt(pointer);
        selectedLabel = null;
        selectedPlatform = null;
        selectedTrack = null;
        selectedTrack = null;
        if (tool == Tool.ADD_STATION || tool == Tool.ADD_TEXT) { selectedNode = null; }
        try {
            switch (tool) {
                case ADD_NODE:
                    Point position = placement(screenToWorld(pointer));
                    Node existing = hit != null ? hit : nodeAt(worldToScreen(position));
                    if (existing != null) {
                        selectedNode = existing;
                        showStatus(Messages.text("node.existing", existing.getId()), false);
                    } else {
                        selectedNode = editor.createNode(position.x, position.y, nodeType);
                        showStatus(Messages.text("node.added", selectedNode.getId()), false);
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
                    showStatus(Messages.text("diagramPanel.drag.or.click.the.other.endpoint.empty.space.creates"), false);
                    break;
                case ADD_PLATFORM: {
                    selectedNode = null;
                    if (network.stationCount() == 0 || network.segmentCount() == 0) {
                        showStatus(Messages.text("diagramPanel.add.a.station.and.at.least.one.track.before"), true);
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
                        showStatus(Messages.text("diagramPanel.added.station.select.and.double.click.to.rename.it"), false);
                    } else { showStatus(Messages.text("diagramPanel.station.placement.cancelled"), false); }
                    break;
                }
                case ADD_TEXT: {
                    Point at = placement(screenToWorld(pointer));
                    CustomText draft = requestCustomText(null);
                    if (draft != null) {
                        selectedLabel = editor.createCustomText(draft.getText(), at.x, at.y,
                                draft.getSize(), draft.getFont(), draft.getColor());
                        showStatus(Messages.text("diagramPanel.added.text.select.and.double.click.to.edit.it"), false);
                    } else { showStatus(Messages.text("diagramPanel.text.placement.cancelled"), false); }
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
                        showStatus(Messages.text("diagramPanel.drag.to.move.the.label.double.click.to.edit"), false);
                        break;
                    }
                    // Node handles remain reachable when a platform touches a track.
                    selectedPlatform = hit == null ? platformAt(pointer) : null;
                    if (selectedPlatform != null) {
                        selectedNode = null;
                        if (event.getClickCount() >= 2) { applyPlatformDialog(selectedPlatform, null); }
                        else { showStatus(Messages.text("platform.selected", selectedPlatform.getNumber()), false); }
                        break;
                    }
                    selectedNode = hit;
                    if (hit != null) {
                        draggedNode = hit;
                        Point2D world = screenToWorld(pointer);
                        dragOffset = new Point2D.Double(world.getX() - hit.getX(), world.getY() - hit.getY());
                        preview = positionOf(hit);
                        dragMoved = false;
                        showStatus(Messages.text("node.selected", hit.getId()), false);
                    } else {
                        selectedTrack = trackAt(pointer);
                        showStatus(selectedTrack == null ? toolHint()
                                : Messages.text("track.selected", selectedTrack.getId()), false);
                    }
                    break;
            }
        } catch (IllegalArgumentException failure) { showStatus(failure.getMessage(), true); }
        repaint();
    }

    private void drag(MouseEvent event) {
        if (panPoint != null) {
            viewport.pan(event.getX() - panPoint.x, event.getY() - panPoint.y);
            pointer = event.getPoint();
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
            // Releasing left while panning pauses the track gesture. Its world
            // start remains available for the next click instead of committing.
            if (SwingUtilities.isLeftMouseButton(event) && trackGesture) {
                trackGesture = false;
                trackDragged = false;
            }
            if (event.getButton() == panButton) {
                viewport.pan(event.getX() - panPoint.x, event.getY() - panPoint.y);
                pointer = event.getPoint();
                hoveredNode = nodeAt(pointer);
                panPoint = null;
                refreshCursor();
                firePropertyChange("view", null, getZoom());
                repaint();
                if (connectionStart == null) { showStatus(toolHint(), false); }
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
                    showStatus(Messages.text("diagramPanel.moved.label"), false);
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
                showStatus(Messages.text("node.moved", draggedNode.getId(), position.x, position.y), false);
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
                showStatus(Messages.text("track.added", segment.getId(), parallel ? Messages.text("track.parallel") : "", continuousDraw ? Messages.text("track.continue") : Messages.text("track.another")), false);
            } else if (trackStart == null) {
                trackStart = nodeAt(worldToScreen(connectionStart));
                if (trackStart == null) {
                    trackStart = editor.createNode(connectionStart.x, connectionStart.y, nodeType);
                }
                connectionStart = positionOf(trackStart);
                selectedNode = trackStart;
                showStatus(Messages.text("diagramPanel.added.starting.node.click.or.drag.to.the.other"), false);
            }
        } catch (IllegalArgumentException failure) {
            if (trackStart == null) { clearPendingEdit(); }
            showStatus(failure.getMessage(), true);
        }
        repaint();
    }

    // Platform editing and drawing
    protected PlatformDialog.Result requestPlatform(Platform existing, TrackSegment track) {
        Point2D placement = existing == null && pointer != null ? screenToWorld(pointer) : null;
        return new PlatformDialog(editor, existing, track, settings, placement).showDialog(this);
    }

    private void applyPlatformDialog(Platform existing, TrackSegment track) {
        PlatformDialog.Result result = requestPlatform(existing, track);
        if (result == null) { showStatus(Messages.text("diagramPanel.platform.edit.cancelled"), false); return; }
        if (result.delete) {
            if (existing == null) { throw new IllegalArgumentException(Messages.text("diagramPanel.select.a.platform.to.delete")); }
            editor.deletePlatform(existing.getStation().getId(), existing.getNumber());
            selectedPlatform = null;
            showStatus(Messages.text("diagramPanel.deleted.platform.its.tracks.remain.in.the.diagram"), false);
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
        showStatus(Messages.text("platform.updated", selectedPlatform.getNumber()), false);
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
        return (String) JOptionPane.showInputDialog(this, Messages.text("diagramPanel.station.name"), Messages.text("diagramPanel.station"),
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
        form.add(new JLabel(Messages.text("diagramPanel.text.multiple.lines.supported")));
        form.add(new JScrollPane(text));
        form.add(new JLabel(Messages.text("diagramPanel.font"))); form.add(font);
        form.add(new JLabel(Messages.text("diagramPanel.size.in.diagram.units"))); form.add(size);
        form.add(new JLabel(Messages.text("diagramPanel.colour.rrggbb"))); form.add(color);
        while (JOptionPane.showConfirmDialog(this, form, Messages.text("diagramPanel.custom.text"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                return new CustomText(text.getText(), 0, 0, Messages.integer(size.getText().trim()),
                        String.valueOf(font.getSelectedItem()), color.getText().trim(), 0);
            } catch (IllegalArgumentException failure) {
                JOptionPane.showMessageDialog(this, failure.getMessage(), Messages.text("diagramPanel.invalid.text"), JOptionPane.ERROR_MESSAGE);
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
        showStatus(Messages.text("diagramPanel.label.editing.finished"), false);
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
        if (!exportRendering && label == selectedLabel) {
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
            throw new IllegalArgumentException(Messages.text("diagramPanel.this.position.is.outside.the.supported.diagram.coordinates"));
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
            drawDiagram(g);
        } finally { g.dispose(); }
    }

    private void drawDiagram(Graphics2D g) {
            for (Station station : network.getAllStations()) {
                for (Platform platform : station.getPlatforms()) { drawPlatform(g, platform); }
            }
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Color.BLACK);
            for (TrackSegment segment : network.getAllTrackSegments()) {
                g.draw(new Line2D.Double(displayPosition(segment.getStart()), displayPosition(segment.getEnd())));
            }
            if (!exportRendering && selectedTrack != null) {
                g.setColor(new Color(30, 115, 210));
                g.setStroke(new BasicStroke((float) (3 / getZoom())));
                g.draw(new Line2D.Double(displayPosition(selectedTrack.getStart()), displayPosition(selectedTrack.getEnd())));
            }
            if (!exportRendering && selectedTrack != null) {
                g.setColor(new Color(30, 115, 210));
                g.setStroke(new BasicStroke((float) (3 / getZoom())));
                g.draw(new Line2D.Double(displayPosition(selectedTrack.getStart()), displayPosition(selectedTrack.getEnd())));
            }
            if (!exportRendering && tool == Tool.ADD_PLATFORM && pointer != null) {
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
            if (!exportRendering && connectionStart != null && pointer != null) {
                Node end = nodeAt(pointer);
                Point finish;
                try { finish = end == null ? placement(screenToWorld(pointer)) : positionOf(end); }
                catch (IllegalArgumentException invalid) { return; }
                g.setColor(new Color(40, 110, 190));
                g.setStroke(new BasicStroke((float) (1.5 / getZoom()), BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10, new float[]{(float) (6 / getZoom()), (float) (4 / getZoom())}, 0));
                g.draw(new Line2D.Double(connectionStart, finish));
            }
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
        if (exportRendering) {
            if (node.getNodeType() == NodeType.STUB_END) { drawStubEnd(g, node, position); }
            return;
        }
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
