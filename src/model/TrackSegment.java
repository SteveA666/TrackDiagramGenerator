package model;

import java.util.Objects;

/**
 * A segment of the track connecting two nodes.
 */
public class TrackSegment {
    private final int id;
    private Node start;
    private Node end;
    private TrackType type;

    public TrackSegment(int id, Node start, Node end, TrackType type) {
        this.id = id;
        this.start = Objects.requireNonNull(start, "TrackSegments: start node cannot be null");
        this.end = Objects.requireNonNull(end, "TrackSegments: end node cannot be null");
        this.type = Objects.requireNonNull(type, "TrackSegments: track type cannot be null");
    }

    public int getId() {
        return id;
    }

    public Node getStart() {
        return start;
    }

    public void setStart(Node start) {
        this.start = start;
    }

    public Node getEnd() {
        return end;
    }

    public void setEnd(Node end) {
        this.end = end;
    }

    public TrackType getTrackType() {
        return type;
    }

    public void setTrackType(TrackType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TrackSegment [id=" + id + ", type=" + type + ", start=" + start.getId() + ", end=" + end.getId() + "]";
    }
}