package model;

import java.util.*;

/**
 * Platform<br>
 * Represents a numbered physical platform with one track edge or two<br>
 * island platform edges.<br>
 * Validates placement edits and its ownership by a station.<br>
 */
public class Platform {
    private int number;
    private List<PlatformEdge> edges;
    private Station station;

    /**
     * Convenience placement spanning the entire track, on its left,<br>
     * offset by 8 units.<br>
     */
    public Platform(int number, TrackSegment trackSegment){ this(number, new PlatformEdge(trackSegment, TrackSide.LEFT, 0, 1, 8)); }

    public Platform(int number, PlatformEdge... edges) {
        setNumber(number);
        setEdges(edges);
    }

    // Numbering
    public int getNumber(){ return number; }

    public void setNumber(int number) {
        if (number <= 0) { throw new IllegalArgumentException("Platform number must be positive"); }
        if (station != null) { station.validateNumber(this, number); }
        this.number = number;
    }

    // Placement and platform form
    public List<PlatformEdge> getEdges(){ return edges; }

    /**
     * For compatibility, returns the first edge's track.<br>
     * Use getEdges() for islands.<br>
     */
    public TrackSegment getTrackSegment(){ return edges.get(0).getTrackSegment(); }

    public boolean isSidePlatform(){ return edges.size() == 1; }
    public boolean isIslandPlatform(){ return edges.size() == 2; }

    /**
     * Atomically replaces placement and changes between side and<br>
     * island forms.<br>
     */
    public void setEdges(PlatformEdge... edges) {
        Objects.requireNonNull(edges, "Edges cannot be null");
        if (edges.length < 1 || edges.length > 2) {
            throw new IllegalArgumentException("A platform requires one or two edges");
        }
        List<PlatformEdge> replacement = new ArrayList<>();
        for (PlatformEdge edge : edges) {
            replacement.add(Objects.requireNonNull(edge, "Platform edge cannot be null"));
        }
        if (replacement.size() == 2
                && replacement.get(0).getTrackSegment().getId() == replacement.get(1).getTrackSegment().getId()) {
            throw new IllegalArgumentException("Island edges must reference different tracks");
        }
        if (station != null) { station.validateEdges(replacement); }
        this.edges = Collections.unmodifiableList(replacement);
    }

    /**
     * Changes a side platform's track while preserving its placement<br>
     * values.<br>
     */
    public void setTrackSegment(TrackSegment trackSegment) {
        if (isIslandPlatform()) {
            throw new IllegalStateException("Use setEdges to edit an island platform");
        }
        PlatformEdge edge = edges.get(0);
        setEdges(new PlatformEdge(trackSegment, edge.getSide(), edge.getStartFraction(),
                edge.getEndFraction(), edge.getOffset()));
    }

    // Station ownership
    public Station getStation(){ return station; }
    void attach(Station station){ this.station = station; }
}
