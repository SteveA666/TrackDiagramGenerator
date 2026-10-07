package model;
import java.util.*;

/**
 * Network<br>
 * Stores the diagram nodes, track segments, and stations.<br>
 * Maintains track connectivity and validates edits to registered<br>
 * objects.<br>
 */
public class Network {
    private final Map<Integer, Node> nodes;
    private final Map<Integer, TrackSegment> trackSegments;
    private final Map<Integer, List<TrackSegment>> adjacencyList;
    private final Map<Integer, Station> stations = new LinkedHashMap<>();

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

        if (segment.getNetwork() != null) {
            throw new IllegalArgumentException("Track segment already belongs to a network");
        }

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
        segment.attach(this);
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

    /**
     * Validates before changing anything,<br>
     * so failed edits leave the graph intact.<br>
     */
    public void reconnectTrackSegment(int id, Node start, Node end) {
        TrackSegment segment = trackSegments.get(id);
        if (segment == null) {
            throw new IllegalArgumentException("Unknown track segment ID: " + id);
        }
        TrackSegment.validateEndpoints(start, end);
        requireNode(start);
        requireNode(end);
        adjacencyList.get(segment.getStart().getId()).remove(segment);
        adjacencyList.get(segment.getEnd().getId()).remove(segment);
        segment.assignEndpoints(start, end);
        adjacencyList.get(start.getId()).add(segment);
        adjacencyList.get(end.getId()).add(segment);
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

        // Remove the entire physical platform if either edge loses its track.
        for (Station station : stations.values()) {
            station.removePlatformsOn(segment);
        }
        segment.attach(null);

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

    public void addStation(Station station) {
        Objects.requireNonNull(station, "Station cannot be null");
        if (stations.containsKey(station.getId()) || station.getNetwork() != null) {
            throw new IllegalArgumentException("Station ID already exists or station already belongs to a network");
        }
        for (Platform platform : station.getPlatforms()) {
            validatePlatformEdges(platform.getEdges());
        }
        stations.put(station.getId(), station);
        station.attach(this);
    }

    public Station getStation(int id) { return stations.get(id); }

    public Collection<Station> getAllStations() {
        return Collections.unmodifiableCollection(stations.values());
    }

    public int stationCount() { return stations.size(); }

    /**
     * Detaches the station with its platforms intact,<br>
     * without deleting tracks.<br>
     */
    public boolean removeStation(int id) {
        Station station = stations.remove(id);
        if (station == null) { return false; }
        station.attach(null);
        return true;
    }

    void validatePlatformEdges(List<PlatformEdge> edges) {
        for (PlatformEdge edge : edges) {
            TrackSegment segment = edge.getTrackSegment();
            if (trackSegments.get(segment.getId()) != segment) {
                throw new IllegalArgumentException("Platform edge must reference the registered track segment");
            }
        }
    }

    private void requireNode(Node node) {
        Objects.requireNonNull(node, "Network: Node cannot be null.");
        Node registered = nodes.get(node.getId());
        if (registered == null) {
            throw new IllegalArgumentException(
                "Network: Segment references unknown node id: " + node.getId());
        }
        if (registered != node) {
            throw new IllegalArgumentException(
                "Network: Endpoint must be the registered node instance (ID: " + node.getId() + ")");
        }
    }
    
}
