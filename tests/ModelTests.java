import model.*;
import java.util.*;

/**
 * ModelTests<br>
 * Checks model connectivity, ownership, platform placement,<br>
 * and validation.<br>
 * Uses no external test libraries.<br>
 * Runs through the PowerShell test script in the tests folder.<br>
 */
public final class ModelTests {
    private static int checks;

    public static void main(String[] args) {
        endpointEdits();
        stationLifecycle();
        platformEdits();
        validation();
        graphStress();
        System.out.println("PASS: " + checks + " model checks");
    }

    /**
     * Fixture<br>
     * Provides a small registered track network and reusable platform<br>
     * placements for model checks.<br>
     */
    private static class Fixture {
        final Network network = new Network();
        final Node a = new Node(1, 0, 0, NodeType.REGULAR);
        final Node b = new Node(2, 100, 0, NodeType.REGULAR);
        final Node c = new Node(3, 0, 30, NodeType.REGULAR);
        final Node d = new Node(4, 100, 30, NodeType.REGULAR);
        final TrackSegment upper = new TrackSegment(1, a, b, TrackType.STATION);
        final TrackSegment lower = new TrackSegment(2, c, d, TrackType.STATION);
        Fixture() {
            for (Node node : Arrays.asList(a, b, c, d)) { network.addNode(node); }
            network.addTrackSegment(upper);
            network.addTrackSegment(lower);
        }
        PlatformEdge upperEdge() { return new PlatformEdge(upper, TrackSide.RIGHT, .2, .8, 10); }
        PlatformEdge lowerEdge() { return new PlatformEdge(lower, TrackSide.LEFT, .1, .9, 10); }
    }

    private static void endpointEdits() {
        Fixture f = new Fixture();
        f.upper.setEnd(f.c);
        check(!f.network.segmentsAt(2).contains(f.upper), "old endpoint disconnected");
        check(f.network.segmentsAt(3).contains(f.upper), "new endpoint connected");
        check(f.network.neighborsOf(1).contains(f.c), "neighbors reflect edit");
        f.upper.setStart(f.d);
        check(!f.network.segmentsAt(1).contains(f.upper), "start edit disconnects old node");
        f.upper.setEndpoints(f.a, f.b);
        f.network.reconnectTrackSegment(1, f.b, f.a);
        check(f.network.segmentsAt(1).size() == 1 && f.network.segmentsAt(2).size() == 1,
                "endpoint swap introduces no duplicates");
        expect(IllegalArgumentException.class, () -> f.upper.setEnd(new Node(1, 999, 999, NodeType.REGULAR)));
        expect(IllegalArgumentException.class, () -> f.upper.setEnd(new Node(9, 0, 0, NodeType.REGULAR)));
        expect(IllegalArgumentException.class, () -> f.upper.setEnd(f.b));
        expect(NullPointerException.class, () -> f.upper.setStart(null));
        check(f.upper.getStart() == f.b && f.upper.getEnd() == f.a, "failed edits preserve endpoints");
        check(f.network.segmentsAt(1).contains(f.upper) && f.network.segmentsAt(2).contains(f.upper),
                "failed edits preserve adjacency");
        TrackSegment impostor = new TrackSegment(3, new Node(1, 999, 999, NodeType.REGULAR), f.c, TrackType.MAINLINE);
        expect(IllegalArgumentException.class, () -> f.network.addTrackSegment(impostor));
        check(f.network.segmentCount() == 2, "failed add leaves network unchanged");
        expect(IllegalArgumentException.class, () -> f.network.addTrackSegment(new TrackSegment(1, f.a, f.c, TrackType.MAINLINE)));
        expect(IllegalArgumentException.class, () -> f.network.addNode(new Node(1, 0, 0, NodeType.REGULAR)));
        Network other = new Network(); other.addNode(f.a); other.addNode(f.b);
        expect(IllegalArgumentException.class, () -> other.addTrackSegment(f.upper));
        check(f.network.removeTrackSegment(1), "segment removal succeeds");
        check(f.network.segmentsAt(1).isEmpty() && f.network.segmentsAt(2).isEmpty(), "no stale adjacency after removal");
        f.upper.setEndpoints(f.a, f.b);
        other.addTrackSegment(f.upper);
        f.upper.setEndpoints(f.b, f.a);
        check(other.segmentsAt(1).size() == 1, "removed segment can be transferred and edited");
        expect(IllegalArgumentException.class, () -> f.network.reconnectTrackSegment(99, f.a, f.b));
    }

    private static void stationLifecycle() {
        Fixture f = new Fixture();
        Station station = new Station(10, "Central");
        Platform island = new Platform(1, f.upperEdge(), f.lowerEdge());
        station.addPlatform(island);
        f.network.addStation(station);
        check(f.network.getStation(10) == station && f.network.stationCount() == 1, "station registered");
        check(station.getNetwork() == f.network && island.getStation() == station, "ownership established");
        check(island.isIslandPlatform() && !island.isSidePlatform(), "island has two edges");
        station.setName("Central Station");
        check(f.network.getStation(10).getName().equals("Central Station"), "station identity survives rename");
        expect(IllegalArgumentException.class, () -> f.network.addStation(new Station(10, "Duplicate")));
        expect(IllegalArgumentException.class, () -> new Network().addStation(station));
        expect(UnsupportedOperationException.class, () -> station.getPlatforms().clear());
        expect(UnsupportedOperationException.class, () -> f.network.getAllStations().clear());
        expect(UnsupportedOperationException.class, () -> f.network.getAllNodes().clear());
        expect(UnsupportedOperationException.class, () -> f.network.getAllTrackSegments().clear());
        expect(UnsupportedOperationException.class, () -> f.network.segmentsAt(1).clear());
        check(f.network.removeNode(1), "node removal succeeds");
        check(f.network.getTrackSegment(1) == null && f.network.getTrackSegment(2) == f.lower, "only connected track deleted");
        check(station.getPlatforms().isEmpty() && island.getStation() == null,
                "deleting either island track removes whole platform and releases ownership");
        check(f.network.getStation(10) == station, "empty station retained");
        check(!f.network.removeNode(99) && !f.network.removeTrackSegment(99), "missing removals report false");
        Platform side = new Platform(2, f.lower);
        station.addPlatform(side);
        check(f.network.removeStation(10), "station removal succeeds");
        check(station.getNetwork() == null && station.getPlatforms().contains(side), "detached station retains platforms");
        check(f.network.getTrackSegment(2) == f.lower, "station removal keeps tracks");
        f.network.addStation(station);
        check(station.removePlatform(2) && side.getStation() == null, "explicit platform removal releases ownership");
        check(!station.removePlatform(99) && !f.network.removeStation(99), "missing station/platform removals report false");

        TrackSegment foreign = new TrackSegment(2, f.c, f.d, TrackType.STATION);
        Station invalid = new Station(20, "Foreign");
        invalid.addPlatform(new Platform(1, foreign));
        expect(IllegalArgumentException.class, () -> f.network.addStation(invalid));
        check(invalid.getNetwork() == null && f.network.getStation(20) == null, "failed station add is atomic");
        expect(IllegalArgumentException.class, () -> station.addPlatform(new Platform(1, foreign)));
    }

    private static void platformEdits() {
        Fixture f = new Fixture();
        Station station = new Station(1, "Central"); f.network.addStation(station);
        Platform p = new Platform(1, f.upperEdge()); station.addPlatform(p);
        Platform second = new Platform(2, f.lower); station.addPlatform(second);
        expect(IllegalArgumentException.class, () -> second.setNumber(1));
        check(second.getNumber() == 2, "failed renumber preserves number");
        second.setNumber(3);
        check(station.getPlatform(2) == null && station.getPlatform(3) == second, "renumber updates lookup");
        expect(IllegalArgumentException.class, () -> station.addPlatform(new Platform(1, f.lower)));
        expect(IllegalArgumentException.class, () -> new Station(2, "Other").addPlatform(p));
        expect(IllegalArgumentException.class, () -> station.addPlatform(p));
        PlatformEdge[] supplied = {f.upperEdge(), f.lowerEdge()};
        p.setEdges(supplied); supplied[0] = f.lowerEdge();
        check(p.isIslandPlatform() && p.getEdges().get(0).getTrackSegment() == f.upper, "edge inputs defensively copied");
        expect(UnsupportedOperationException.class, () -> p.getEdges().clear());
        expect(IllegalStateException.class, () -> p.setTrackSegment(f.lower));
        TrackSegment foreign = new TrackSegment(2, f.c, f.d, TrackType.STATION);
        expect(IllegalArgumentException.class, () -> p.setEdges(new PlatformEdge(foreign, TrackSide.LEFT, 0, 1, 5)));
        check(p.isIslandPlatform(), "invalid edge edit preserves island");
        p.setEdges(f.upperEdge());
        check(p.isSidePlatform() && !p.isIslandPlatform(), "island can become side platform");
        p.setTrackSegment(f.lower);
        PlatformEdge edge = p.getEdges().get(0);
        check(edge.getTrackSegment() == f.lower && edge.getSide() == TrackSide.RIGHT
                && edge.getStartFraction() == .2 && edge.getEndFraction() == .8 && edge.getOffset() == 10,
                "side track edit preserves placement");
        expect(IllegalArgumentException.class, () -> p.setTrackSegment(foreign));
        check(p.getTrackSegment() == f.lower, "failed side track edit preserves track");
        f.lower.setEnd(f.b);
        check(p.getTrackSegment().getEnd() == f.b, "platform follows track edits");
    }

    private static void validation() {
        Fixture f = new Fixture();
        expect(NullPointerException.class, () -> new Node(1, 0, 0, null));
        expect(NullPointerException.class, () -> f.a.setNodeType(null));
        expect(NullPointerException.class, () -> f.upper.setTrackType(null));
        check(f.a.getNodeType() == NodeType.REGULAR && f.upper.getTrackType() == TrackType.STATION, "failed type edits preserve values");
        expect(IllegalArgumentException.class, () -> new TrackSegment(3, f.a, f.a, TrackType.MAINLINE));
        expect(NullPointerException.class, () -> new Station(1, null));
        expect(IllegalArgumentException.class, () -> new Station(1, " \t "));
        Station station = new Station(1, "Valid");
        expect(IllegalArgumentException.class, () -> station.setName(""));
        check(station.getName().equals("Valid"), "invalid rename preserves name");
        expect(NullPointerException.class, () -> station.addPlatform(null));
        expect(IllegalArgumentException.class, () -> new Platform(0, f.upper));
        expect(IllegalArgumentException.class, () -> new Platform(1, new PlatformEdge[0]));
        expect(NullPointerException.class, () -> new Platform(1, new PlatformEdge[]{null}));
        expect(IllegalArgumentException.class, () -> new Platform(1, f.upperEdge(), f.upperEdge()));
        expect(IllegalArgumentException.class, () -> new Platform(1, f.upperEdge(), f.lowerEdge(), f.upperEdge()));
        expect(NullPointerException.class, () -> new PlatformEdge(null, TrackSide.LEFT, 0, 1, 1));
        expect(NullPointerException.class, () -> new PlatformEdge(f.upper, null, 0, 1, 1));
        double[][] badExtents = {{-.1, .5}, {0, 1.1}, {.5, .5}, {.8, .2}, {Double.NaN, 1}, {0, Double.POSITIVE_INFINITY}};
        for (double[] extent : badExtents) {
            expect(IllegalArgumentException.class, () -> new PlatformEdge(f.upper, TrackSide.LEFT, extent[0], extent[1], 1));
        }
        for (double offset : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            expect(IllegalArgumentException.class, () -> new PlatformEdge(f.upper, TrackSide.LEFT, 0, 1, offset));
        }
        PlatformEdge valid = new PlatformEdge(f.upper, TrackSide.LEFT, 0, 1, 0);
        check(valid.getOffset() == 0, "boundary placement accepted");
    }

    private static void graphStress() {
        Fixture f = new Fixture();
        List<Node> nodes = Arrays.asList(f.a, f.b, f.c, f.d);
        Random random = new Random(42);
        for (int i = 0; i < 100; i++) {
            int start = random.nextInt(4);
            int end = (start + 1 + random.nextInt(3)) % 4;
            f.upper.setEndpoints(nodes.get(start), nodes.get(end));
            for (Node node : nodes) {
                int expected = (node == f.upper.getStart() || node == f.upper.getEnd()) ? 1 : 0;
                int actual = Collections.frequency(f.network.segmentsAt(node.getId()), f.upper);
                check(actual == expected, "connectivity invariant after repeated edits");
            }
        }
        f.network.removeTrackSegment(1);
        for (Node node : nodes) {
            check(!f.network.segmentsAt(node.getId()).contains(f.upper), "no stale references after repeated edits and removal");
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) { throw new AssertionError(message); }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) { return; }
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
