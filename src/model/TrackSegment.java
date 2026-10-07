package model;

import java.util.Objects;

/**
 * TrackSegment<br>
 * Connects two nodes with a typed track segment.<br>
 * Keeps network connectivity consistent when its endpoints change.<br>
 */
public class TrackSegment {
    private final int id;
    private Node start;
    private Node end;
    private TrackType type;
    private Network network;

    public TrackSegment(int id, Node start, Node end, TrackType type) {
        this.id = id;
        this.start = Objects.requireNonNull(start, "TrackSegments: start node cannot be null");
        this.end = Objects.requireNonNull(end, "TrackSegments: end node cannot be null");
        this.type = Objects.requireNonNull(type, "TrackSegments: track type cannot be null");
        validateEndpoints(start, end);
    }

    public int getId() {
        return id;
    }

    public Node getStart() {
        return start;
    }

    public void setStart(Node start) {
        setEndpoints(start, end);
    }

    public Node getEnd() {
        return end;
    }

    public void setEnd(Node end) {
        setEndpoints(start, end);
    }

    /**
     * Updates both endpoints atomically, including connectivity when<br>
     * registered.<br>
     */
    public void setEndpoints(Node start, Node end) {
        validateEndpoints(start, end);
        if (network != null) {
            network.reconnectTrackSegment(id, start, end);
        } else {
            assignEndpoints(start, end);
        }
    }

    static void validateEndpoints(Node start, Node end) {
        Objects.requireNonNull(start, "Start node cannot be null");
        Objects.requireNonNull(end, "End node cannot be null");
        if (start.getId() == end.getId()) {
            throw new IllegalArgumentException("A track segment cannot connect a node to itself");
        }
    }

    void assignEndpoints(Node start, Node end) {
        this.start = start;
        this.end = end;
    }

    Network getNetwork() { return network; }
    void attach(Network network) { this.network = network; }

    public TrackType getTrackType() {
        return type;
    }

    public void setTrackType(TrackType type) {
        this.type = Objects.requireNonNull(type, "Track type cannot be null");
    }

    @Override
    public String toString() {
        return "TrackSegment [id=" + id + ", type=" + type + ", start=" + start.getId() + ", end=" + end.getId() + "]";
    }
}
