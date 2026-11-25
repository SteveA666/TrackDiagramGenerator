package model;

import i18n.Messages;

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
        this.start = Objects.requireNonNull(start, Messages.text("error.TrackSegment.tracksegments.start.node.cannot.be.null"));
        this.end = Objects.requireNonNull(end, Messages.text("error.TrackSegment.tracksegments.end.node.cannot.be.null"));
        this.type = Objects.requireNonNull(type, Messages.text("error.TrackSegment.tracksegments.track.type.cannot.be.null"));
        validateEndpoints(start, end);
    }

    // Identity
    public int getId(){ return id; }

    // Endpoints and connectivity
    public Node getStart(){ return start; }
    public Node getEnd(){ return end; }
    public void setStart(Node start){ setEndpoints(start, end); }
    public void setEnd(Node end){ setEndpoints(start, end); }

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

    void assignEndpoints(Node start, Node end) {
        this.start = start;
        this.end = end;
    }

    // Track type
    public TrackType getTrackType(){ return type; }
    public void setTrackType(TrackType type){ this.type = Objects.requireNonNull(type, Messages.text("error.TrackSegment.track.type.cannot.be.null")); }

    // Network ownership
    Network getNetwork(){ return network; }
    void attach(Network network){ this.network = network; }

    // Validation
    static void validateEndpoints(Node start, Node end) {
        Objects.requireNonNull(start, Messages.text("error.TrackSegment.start.node.cannot.be.null"));
        Objects.requireNonNull(end, Messages.text("error.TrackSegment.end.node.cannot.be.null"));
        if (start.getId() == end.getId()) {
            throw new IllegalArgumentException(Messages.text("error.TrackSegment.a.track.segment.cannot.connect.a.node.to.itself"));
        }
    }

    // Text representation
    @Override
    public String toString(){ return "TrackSegment [id=" + id + ", type=" + type + ", start=" + start.getId() + ", end=" + end.getId() + "]"; }
}
