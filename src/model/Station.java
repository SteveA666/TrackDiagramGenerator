package model;

import java.util.*;

/**
 * Station<br>
 * Groups the numbered platforms of a diagram station<br>
 * under a stable identity and editable name.<br>
 * Validates platform ownership, numbering,<br>
 * and registered track references.<br>
 */
public class Station {
    private final int id;
    private final List<Platform> platforms = new ArrayList<>();
    private String name;
    private Network network;

    public Station(int id, String name) {
        this.id = id;
        setName(name);
    }

    // Identity and name
    public int getId(){ return id; }
    public String getName(){ return name; }

    public void setName(String name) {
        Objects.requireNonNull(name, "Station name cannot be null");
        if (name.trim().isEmpty()) { throw new IllegalArgumentException("Station name cannot be blank"); }
        this.name = name;
    }

    // Platforms
    public List<Platform> getPlatforms(){ return Collections.unmodifiableList(platforms); }

    public Platform getPlatform(int number) {
        for (Platform platform : platforms) {
            if (platform.getNumber() == number) { return platform; }
        }
        return null;
    }

    public void addPlatform(Platform platform) {
        Objects.requireNonNull(platform, "Platform cannot be null");
        if (platform.getStation() != null) {
            throw new IllegalArgumentException("Platform already belongs to a station");
        }
        validateNumber(platform, platform.getNumber());
        validateEdges(platform.getEdges());
        platforms.add(platform);
        platform.attach(this);
    }

    public boolean removePlatform(int number) {
        Platform platform = getPlatform(number);
        if (platform == null) { return false; }
        platforms.remove(platform);
        platform.attach(null);
        return true;
    }

    void removePlatformsOn(TrackSegment segment) {
        for (Platform platform : new ArrayList<>(platforms)) {
            for (PlatformEdge edge : platform.getEdges()) {
                if (edge.getTrackSegment() == segment) {
                    removePlatform(platform.getNumber());
                    break;
                }
            }
        }
    }

    // Network ownership
    public Network getNetwork(){ return network; }
    void attach(Network network){ this.network = network; }

    // Platform validation
    void validateNumber(Platform edited, int number) {
        Platform existing = getPlatform(number);
        if (existing != null && existing != edited) {
            throw new IllegalArgumentException("Platform number already exists in this station: " + number);
        }
    }

    void validateEdges(List<PlatformEdge> edges) {
        if (network != null) { network.validatePlatformEdges(edges); }
    }
}
