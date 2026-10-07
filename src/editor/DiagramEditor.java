package editor;

import java.util.*;
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

    public DiagramEditor(Network network){ this.network = Objects.requireNonNull(network, "A diagram network is required."); }

    // Network access
    public Network getNetwork(){ return network; }

    // Node editing
    public Node createNode(int x, int y, NodeType type) {
        if (type == null) { throw new IllegalArgumentException("Choose a node type."); }
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
        Node node = requireNode(nodeId);
        for (TrackSegment segment : network.segmentsAt(nodeId)) {
            Node other = segment.getStart() == node ? segment.getEnd() : segment.getStart();
            if (other.getX() == x && other.getY() == y) {
                throw new IllegalArgumentException("Move the node away from its connected endpoint;"
                        + " a track must have a visible length.");
            }
        }
        node.setPosition(x, y);
    }

    // Track editing
    public TrackSegment createTrack(int startId, int endId, TrackType type) {
        if (type == null) { throw new IllegalArgumentException("Choose a track type."); }
        Node start = requireNode(startId);
        Node end = requireNode(endId);
        if (start == end) {
            throw new IllegalArgumentException("Choose a different node for the other end of the track.");
        }
        if (start.getX() == end.getX() && start.getY() == end.getY()) {
            throw new IllegalArgumentException("Move the endpoints apart before connecting them.");
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
        if (nodeType == null || trackType == null) {
            throw new IllegalArgumentException("Choose node and track types.");
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
            throw new IllegalArgumentException("Choose distinct endpoints with visible space between them.");
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
            throw new IllegalArgumentException("Node " + id + " is no longer in this diagram.");
        }
        return node;
    }

    private int nextId(Set<Integer> used) {
        for (long candidate = 1; candidate <= Integer.MAX_VALUE; candidate++) {
            if (!used.contains((int) candidate)) { return (int) candidate; }
        }
        throw new IllegalArgumentException("No unused positive identities remain in this diagram.");
    }
}
