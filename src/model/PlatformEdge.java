package model;

import java.util.Objects;

/**
 * PlatformEdge<br>
 * Stores the immutable placement of one platform edge<br>
 * beside a track segment.<br>
 * Measures its extent as fractions from the track start,<br>
 * with a perpendicular drawing offset.<br>
 * Uses the screen normal (dy, -dx) for the left side,<br>
 * where screen y increases downwards.<br>
 * Reversing the track endpoints reverses the meaning of its side<br>
 * and fractions.<br>
 */
public final class PlatformEdge {
    private final TrackSegment trackSegment;
    private final TrackSide side;
    private final double startFraction;
    private final double endFraction;
    private final double offset;

    public PlatformEdge(TrackSegment trackSegment, TrackSide side,
                        double startFraction, double endFraction, double offset) {
        this.trackSegment = Objects.requireNonNull(trackSegment, "Track segment cannot be null");
        this.side = Objects.requireNonNull(side, "Track side cannot be null");
        if (!Double.isFinite(startFraction) || !Double.isFinite(endFraction)
                || startFraction < 0 || endFraction > 1 || startFraction >= endFraction) {
            throw new IllegalArgumentException("Platform extent must satisfy 0 <= start < end <= 1");
        }
        if (!Double.isFinite(offset) || offset < 0) {
            throw new IllegalArgumentException("Platform offset must be finite and non-negative");
        }
        this.startFraction = startFraction;
        this.endFraction = endFraction;
        this.offset = offset;
    }

    // Track reference and side
    public TrackSegment getTrackSegment(){ return trackSegment; }
    public TrackSide getSide(){ return side; }

    // Extent and offset
    public double getStartFraction(){ return startFraction; }
    public double getEndFraction(){ return endFraction; }
    public double getOffset(){ return offset; }
}
