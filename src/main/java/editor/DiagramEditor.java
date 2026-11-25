package editor;

import i18n.Messages;

import java.util.*;
import java.awt.geom.Point2D;
import model.*;

/**
 * DiagramEditor<br>
 * Applies validated diagram edits and allocates unused positive object<br>
 * identities.<br>
 * Keeps editing rules independent of the user interface,<br>
 * ready for other editing tools.<br>
 */
public final class DiagramEditor {
    private final Network network;
    private final EditHistory history;

    public DiagramEditor(Network network){
        this.network = Objects.requireNonNull(network, Messages.text("error.DiagramEditor.a.diagram.network.is.required"));
        this.history = new EditHistory(network);
    }

    // Network access
    public Network getNetwork(){ return network; }
    public EditHistory getHistory(){ return history; }

    // Object deletion (including dependent tracks/platforms) is one undoable edit.
    public void deleteNode(int id) {
        history.perform("history.action.delete.node", () -> { requireNode(id); network.removeNode(id); return null; });
    }

    public void deleteTrack(int id) {
        history.perform("history.action.delete.track", () -> {
            if (network.getTrackSegment(id) == null) {
                throw new IllegalArgumentException(Messages.text("error.DiagramEditor.track.is.no.longer.in.this.diagram"));
            }
            network.removeTrackSegment(id);
            return null;
        });
    }

    public void deleteStation(int id) {
        history.perform("history.action.delete.station", () -> { requireStation(id); network.removeStation(id); return null; });
    }

    public void deleteCustomText(int id) {
        history.perform("history.action.delete.text", () -> { requireCustomText(id); network.removeCustomText(id); return null; });
    }

    // Node editing
    public Node createNode(int x, int y, NodeType type) {
        return history.perform("history.action.add.node", () -> createNodeImpl(x, y, type));
    }

    private Node createNodeImpl(int x, int y, NodeType type) {
        if (type == null) { throw new IllegalArgumentException(Messages.text("error.DiagramEditor.choose.a.node.type")); }
        Set<Integer> used = new HashSet<>();
        for (Node node : network.getAllNodes()) { used.add(node.getId()); }
        Node node = new Node(nextId(used), x, y, type);
        network.addNode(node);
        return node;
    }

    /**
     * Validates all affected tracks before committing either<br>
     * coordinate.<br>
     */
    public void moveNode(int nodeId, int x, int y) {
        history.perform("history.action.move.node", () -> { moveNodeImpl(nodeId, x, y); return null; });
    }

    private void moveNodeImpl(int nodeId, int x, int y) {
        Node node = requireNode(nodeId);
        for (TrackSegment segment : network.segmentsAt(nodeId)) {
            Node other = segment.getStart() == node ? segment.getEnd() : segment.getStart();
            if (other.getX() == x && other.getY() == y) {
                throw new IllegalArgumentException(Messages.text("error.DiagramEditor.move.the.node.away.from.its.connected.endpoint.a.track.must.have.a.visible.length"));
            }
        }
        for (Station station : network.getAllStations()) {
            for (Platform platform : station.getPlatforms()) {
                boolean affected = false;
                for (PlatformEdge edge : platform.getEdges()) {
                    TrackSegment track = edge.getTrackSegment();
                    affected |= track.getStart() == node || track.getEnd() == node;
                }
                if (affected) {
                    PlatformGeometry.outline(platform, endpoint -> endpoint == node
                            ? new Point2D.Double(x, y) : new Point2D.Double(endpoint.getX(), endpoint.getY()));
                }
            }
        }
        node.setPosition(x, y);
    }

    // Track editing
    public TrackSegment createTrack(int startId, int endId, TrackType type) {
        return history.perform("history.action.add.track", () -> createTrackImpl(startId, endId, type));
    }

    private TrackSegment createTrackImpl(int startId, int endId, TrackType type) {
        if (type == null) { throw new IllegalArgumentException(Messages.text("error.DiagramEditor.choose.a.track.type")); }
        Node start = requireNode(startId);
        Node end = requireNode(endId);
        if (start == end) {
            throw new IllegalArgumentException(Messages.text("error.DiagramEditor.choose.a.different.node.for.the.other.end.of.the.track"));
        }
        if (start.getX() == end.getX() && start.getY() == end.getY()) {
            throw new IllegalArgumentException(Messages.text("error.DiagramEditor.move.the.endpoints.apart.before.connecting.them"));
        }
        Set<Integer> used = new HashSet<>();
        for (TrackSegment segment : network.getAllTrackSegments()) { used.add(segment.getId()); }
        TrackSegment segment = new TrackSegment(nextId(used), start, end, type);
        network.addTrackSegment(segment);
        return segment;
    }

    /**
     * Creates a track and any missing endpoints as one validated edit.<br>
     * Null identities request endpoints at the supplied coordinates.<br>
     * Existing nodes at those coordinates are reused.<br>
     */
    public TrackSegment createTrackBetween(Integer startId, int startX, int startY,
            Integer endId, int endX, int endY, NodeType nodeType, TrackType trackType) {
        return history.perform("history.action.add.track", () -> createTrackBetweenImpl(startId, startX, startY,
                endId, endX, endY, nodeType, trackType));
    }

    private TrackSegment createTrackBetweenImpl(Integer startId, int startX, int startY,
            Integer endId, int endX, int endY, NodeType nodeType, TrackType trackType) {
        if (nodeType == null || trackType == null) {
            throw new IllegalArgumentException(Messages.text("error.DiagramEditor.choose.node.and.track.types"));
        }
        Set<Integer> nodeIds = new HashSet<>();
        for (Node node : network.getAllNodes()) { nodeIds.add(node.getId()); }
        Node start = startId == null ? nodeAt(startX, startY) : requireNode(startId);
        boolean addStart = start == null;
        if (addStart) {
            start = new Node(nextId(nodeIds), startX, startY, nodeType);
            nodeIds.add(start.getId());
        }
        Node end = endId == null ? nodeAt(endX, endY) : requireNode(endId);
        boolean addEnd = end == null;
        if (addEnd) { end = new Node(nextId(nodeIds), endX, endY, nodeType); }
        if (start == end || (start.getX() == end.getX() && start.getY() == end.getY())) {
            throw new IllegalArgumentException(Messages.text("error.DiagramEditor.choose.distinct.endpoints.with.visible.space.between.them"));
        }
        Set<Integer> trackIds = new HashSet<>();
        for (TrackSegment track : network.getAllTrackSegments()) { trackIds.add(track.getId()); }
        TrackSegment track = new TrackSegment(nextId(trackIds), start, end, trackType);
        // All references, geometry, types, and identities are valid before mutation.
        if (addStart) { network.addNode(start); }
        if (addEnd) { network.addNode(end); }
        network.addTrackSegment(track);
        return track;
    }

    // Station editing
    public Station createStation(String name, int x, int y) {
        return history.perform("history.action.add.station", () -> createStationImpl(name, x, y));
    }

    private Station createStationImpl(String name, int x, int y) {
        Set<Integer> used = new HashSet<>();
        for (Station station : network.getAllStations()) { used.add(station.getId()); }
        Station station = new Station(nextId(used), name, x, y);
        network.addStation(station);
        return station;
    }

    public void moveStation(int id, int x, int y){
        history.perform("history.action.move.station", () -> { requireStation(id).setPosition(x, y); return null; });
    }
    public void renameStation(int id, String name){
        history.perform("history.action.rename.station", () -> { requireStation(id).setName(name); return null; });
    }

    private Station requireStation(int id) {
        Station station = network.getStation(id);
        if (station == null) { throw new IllegalArgumentException(Messages.text("error.DiagramEditor.station.is.no.longer.in.this.diagram")); }
        return station;
    }

    // Platform editing
    public Platform createPlatform(int stationId, int number, PlatformEdge... edges) {
        return history.perform("history.action.add.platform", () -> createPlatformImpl(stationId, number, edges));
    }

    private Platform createPlatformImpl(int stationId, int number, PlatformEdge... edges) {
        Platform platform = new Platform(number, edges);
        validatePlatform(stationId, null, platform);
        requireStation(stationId).addPlatform(platform);
        return platform;
    }

    public void editPlatform(int stationId, int oldNumber, int number, PlatformEdge... edges) {
        history.perform("history.action.edit.platform", () -> { editPlatformImpl(stationId, oldNumber, number, edges); return null; });
    }

    private void editPlatformImpl(int stationId, int oldNumber, int number, PlatformEdge... edges) {
        Platform existing = requirePlatform(stationId, oldNumber);
        Platform replacement = new Platform(number, edges);
        validatePlatform(stationId, existing, replacement);
        existing.setDefinition(number, edges);
    }

    public void deletePlatform(int stationId, int number) {
        history.perform("history.action.delete.platform", () -> { deletePlatformImpl(stationId, number); return null; });
    }

    private void deletePlatformImpl(int stationId, int number) {
        requirePlatform(stationId, number);
        requireStation(stationId).removePlatform(number);
    }

    public void validatePlatform(int stationId, Platform existing, Platform replacement) {
        Station station = requireStation(stationId);
        if (existing != null && station.getPlatform(existing.getNumber()) != existing) {
            throw new IllegalArgumentException(Messages.text("error.DiagramEditor.platform.is.no.longer.in.this.station"));
        }
        Platform conflict = station.getPlatform(replacement.getNumber());
        if (conflict != null && conflict != existing) {
            throw new IllegalArgumentException(Messages.text("error.DiagramEditor.this.station.already.has.that.platform.number"));
        }
        for (PlatformEdge edge : replacement.getEdges()) {
            TrackSegment track = edge.getTrackSegment();
            if (network.getTrackSegment(track.getId()) != track) {
                throw new IllegalArgumentException(Messages.text("error.DiagramEditor.choose.a.track.from.this.diagram"));
            }
        }
        PlatformGeometry.outline(replacement);
    }

    private Platform requirePlatform(int stationId, int number) {
        Platform platform = requireStation(stationId).getPlatform(number);
        if (platform == null) { throw new IllegalArgumentException(Messages.text("error.DiagramEditor.platform.is.no.longer.in.this.station")); }
        return platform;
    }

    // Custom text editing
    public CustomText createCustomText(String text, int x, int y, int size, String font, String color) {
        return history.perform("history.action.add.text", () -> createCustomTextImpl(text, x, y, size, font, color));
    }

    private CustomText createCustomTextImpl(String text, int x, int y, int size, String font, String color) {
        Set<Integer> used = new HashSet<>();
        for (CustomText label : network.getAllCustomTexts()) { used.add(label.getId()); }
        CustomText label = new CustomText(text, x, y, size, font, color, nextId(used));
        network.addCustomText(label);
        return label;
    }

    public void moveCustomText(int id, int x, int y){
        history.perform("history.action.move.text", () -> { requireCustomText(id).setPosition(x, y); return null; });
    }
    public void editCustomText(int id, String text, int size, String font, String color) {
        history.perform("history.action.edit.text", () -> { requireCustomText(id).setAppearance(text, size, font, color); return null; });
    }

    private CustomText requireCustomText(int id) {
        CustomText text = network.getCustomText(id);
        if (text == null) { throw new IllegalArgumentException(Messages.text("error.DiagramEditor.text.is.no.longer.in.this.diagram")); }
        return text;
    }

    // Lookup and identity allocation
    private Node nodeAt(int x, int y) {
        for (Node node : network.getAllNodes()) {
            if (node.getX() == x && node.getY() == y) { return node; }
        }
        return null;
    }

    private Node requireNode(int id) {
        Node node = network.getNode(id);
        if (node == null) {
            throw new IllegalArgumentException(Messages.text("error.nodeMissing", id));
        }
        return node;
    }

    private int nextId(Set<Integer> used) {
        for (long candidate = 1; candidate <= Integer.MAX_VALUE; candidate++) {
            if (!used.contains((int) candidate)) { return (int) candidate; }
        }
        throw new IllegalArgumentException(Messages.text("error.DiagramEditor.no.unused.positive.identities.remain.in.this.diagram"));
    }
}
