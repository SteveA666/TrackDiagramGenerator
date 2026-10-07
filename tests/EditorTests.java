import editor.DiagramEditor;
import model.*;

/**
 * EditorTests<br>
 * Checks editing validation, identity allocation,<br>
 * and unchanged state after rejected edits.<br>
 */
public final class EditorTests {
    private static int checks;

    public static void main(String[] args) {
        Network network = new Network();
        network.addNode(new Node(Integer.MAX_VALUE, 500, 500, NodeType.REGULAR));
        DiagramEditor editor = new DiagramEditor(network);
        Node a = editor.createNode(-20, 30, NodeType.REGULAR);
        check(a.getId() == 1 && a.getX() == -20, "IDs do not overflow and negative coordinates are valid");
        network.addNode(new Node(2, 100, 100, NodeType.REGULAR));
        Node b = editor.createNode(40, 30, NodeType.STUB_END);
        check(b.getId() == 3 && b.getNodeType() == NodeType.STUB_END, "allocation sees external additions");
        network.addTrackSegment(new TrackSegment(Integer.MAX_VALUE, a, b, TrackType.MAINLINE));
        TrackSegment track = editor.createTrack(a.getId(), b.getId(), TrackType.SIDING);
        check(track.getId() == 1 && track.getTrackType() == TrackType.SIDING, "track ID allocation and type");
        check(network.segmentsAt(a.getId()).size() == 2, "parallel tracks remain supported");
        editor.moveNode(a.getId(), -40, 50);
        check(a.getX() == -40 && a.getY() == 50 && track.getStart() == a, "moving preserves endpoint references");
        rejected(() -> editor.moveNode(a.getId(), b.getX(), b.getY()));
        check(a.getX() == -40 && a.getY() == 50, "failed move preserves both coordinates");
        rejected(() -> editor.moveNode(999, 10, 10));
        rejected(() -> editor.createTrack(a.getId(), a.getId(), TrackType.MAINLINE));
        rejected(() -> editor.createTrack(a.getId(), 999, TrackType.MAINLINE));
        rejected(() -> editor.createTrack(a.getId(), b.getId(), null));
        rejected(() -> editor.createNode(0, 0, null));
        check(network.nodeCount() == 4 && network.segmentCount() == 2, "invalid edits add no partial objects");
        Node coincident = editor.createNode(b.getX(), b.getY(), NodeType.REGULAR);
        rejected(() -> editor.createTrack(b.getId(), coincident.getId(), TrackType.MAINLINE));
        check(network.segmentCount() == 2, "zero length track rejected without mutation");
        editor.moveNode(coincident.getId(), -40, 50);
        check(coincident.getX() == a.getX(), "unconnected overlapping nodes are not a structural error");
        network.removeNode(a.getId());
        rejected(() -> editor.moveNode(a.getId(), 0, 0));
        rejected(() -> editor.createTrack(a.getId(), b.getId(), TrackType.MAINLINE));
        check(network.segmentCount() == 0, "editing retains model deletion behavior");
        check(editor.createNode(0, 0, NodeType.REGULAR).getId() == 1, "freed identities are safely reused");
        System.out.println("PASS: " + checks + " editor checks");
    }

    private static void rejected(Runnable action) {
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            if (expected.getMessage() == null || expected.getMessage().isEmpty()) {
                throw new AssertionError("Missing validation message");
            }
            return;
        }
        throw new AssertionError("Expected a rejected edit");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) { throw new AssertionError(message); }
    }
}
