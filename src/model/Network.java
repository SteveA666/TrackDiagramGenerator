package model;
import java.util.*;

/**
 * 
 * Network <br>
 * A network of nodes and track segments.
 */
public class Network {
    private final Map<Integer, Node> nodes;
    private final Map<Integer, TrackSegment> trackSegments;
    private final Map<Integer, List<TrackSegment>> adjacencyList;

    public Network() {
        this.nodes = new LinkedHashMap<>();
        this.trackSegments = new LinkedHashMap<>();
        this.adjacencyList = new HashMap<>();
    }

    public void addNode(Node node) {
        
        Objects.requireNonNull(node, "Network: Cannot add a null node.");

        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Network: Node with ID " + node.getId() + " already exists.");
        }
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public Node getNode(int id) {
        return nodes.get(id);
    }

    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public boolean removeNode(int id) { 
        Node node = nodes.get(id);
        if (node == null) {return false;} // Node not found

        for (TrackSegment segment : new ArrayList<>(segmentsAt(id))) {
            // Remove all track segments connected to this node
            removeTrackSegment(segment.getId());
        }
        
        nodes.remove(id);
        adjacencyList.remove(id);
        return true;
    }

    // Track segment management
    public void addTrackSegment(TrackSegment segment) {

        Objects.requireNonNull(segment, "Network: Cannot add a null track segment.");

        if (trackSegments.containsKey(segment.getId())) {
            throw new IllegalArgumentException(
                "Network: Track segment with ID " 
                + segment.getId() + " already exists.");
        }

            
        requireNode(segment.getStart());
        requireNode(segment.getEnd());

        if (segment.getStart().getId() == segment.getEnd().getId()) {
                throw new IllegalArgumentException(
                    "Network: Track segment cannot connect a node to itself (ID: "
                    + segment.getStart().getId() + ")");
        }

        trackSegments.put(segment.getId(), segment);
        adjacencyList
                .computeIfAbsent(segment.getStart().getId(), k -> new ArrayList<>())
                .add(segment);
        adjacencyList
                .computeIfAbsent(segment.getEnd().getId(), k -> new ArrayList<>())
                .add(segment);
    }

    public TrackSegment getTrackSegment(int id) {
        return trackSegments.get(id);
    }

    public Collection<TrackSegment> getAllTrackSegments() {
        return Collections.unmodifiableCollection(trackSegments.values());
    }


    public boolean removeTrackSegment(int id) {
        TrackSegment segment = trackSegments.remove(id);
        if (segment == null) {return false;} // Segment not found

        List<TrackSegment> atStart = adjacencyList.get(segment.getStart().getId());
        if (atStart != null) {atStart.remove(segment);}

        List<TrackSegment> atEnd = adjacencyList.get(segment.getEnd().getId());
        if (atEnd != null) {atEnd.remove(segment);}

        return true;
    }




    // Helpers
    public List<TrackSegment> segmentsAt(int nodeId) {
        List<TrackSegment> list = adjacencyList.get(nodeId);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<Node> neighborsOf(int nodeId) {
        List<Node> neighbors = new ArrayList<>();
        for (TrackSegment segment : segmentsAt(nodeId)) {
            if (segment.getStart().getId() == nodeId) {
                neighbors.add(segment.getEnd());
            } else {
                neighbors.add(segment.getStart());
            }
        }
        return neighbors;
    }

    public int nodeCount()      { return nodes.size(); }
    public int segmentCount()   { return trackSegments.size(); }

    private void requireNode(Node node) {
        Objects.requireNonNull(node, "Network: Node cannot be null.");
        Node registered = nodes.get(node.getId());
        if (registered == null) {
            throw new IllegalArgumentException(
                "Network: Segment references unknown node id: " + node.getId());
        }
        // TODO move to io section
        // if (registered != node) {
        //     throw new IllegalArgumentException(
        //         "Network: Segment endpoint instance does not match the node registered in the network (ID: " + node.getId() + ")");
        // }
    }
    
}
