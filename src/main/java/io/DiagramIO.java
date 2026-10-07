package io;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import editor.PlatformGeometry;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import model.*;

/** Versioned JSON persistence. References are IDs; adjacency is rebuilt by Network. */
public final class DiagramIO {
    public static final int FORMAT_VERSION = 1;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().setStrictness(Strictness.STRICT).create();

    private DiagramIO() {}

    public static Network load(Path path) throws IOException {
        return fromJson(Files.readString(path, StandardCharsets.UTF_8));
    }

    /** Validate before touching the destination, then replace it from a sibling temp file. */
    public static void save(Network network, Path path) throws IOException {
        String data = toJson(network);
        fromJson(data);
        Path target = path.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".diagram-", ".tmp");
        try {
            Files.writeString(temporary, data + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Stable document snapshot, also used to compare committed edits with the last save. */
    public static String toJson(Network network) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", FORMAT_VERSION);
        JsonArray nodes = new JsonArray(), tracks = new JsonArray();
        JsonArray stations = new JsonArray(), texts = new JsonArray();
        root.add("nodes", nodes);
        root.add("tracks", tracks);
        root.add("stations", stations);
        root.add("customTexts", texts);
        for (Node node : network.getAllNodes()) {
            JsonObject value = positioned(node.getId(), node.getX(), node.getY());
            value.addProperty("type", node.getNodeType().name());
            nodes.add(value);
        }
        for (TrackSegment track : network.getAllTrackSegments()) {
            JsonObject value = new JsonObject();
            value.addProperty("id", track.getId());
            value.addProperty("startNodeId", track.getStart().getId());
            value.addProperty("endNodeId", track.getEnd().getId());
            value.addProperty("type", track.getTrackType().name());
            tracks.add(value);
        }
        for (Station station : network.getAllStations()) {
            JsonObject value = positioned(station.getId(), station.getX(), station.getY());
            value.addProperty("name", station.getName());
            JsonArray platforms = new JsonArray();
            value.add("platforms", platforms);
            for (Platform platform : station.getPlatforms()) {
                JsonObject placement = new JsonObject();
                placement.addProperty("number", platform.getNumber());
                JsonArray edges = new JsonArray();
                placement.add("edges", edges);
                for (PlatformEdge edge : platform.getEdges()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("trackId", edge.getTrackSegment().getId());
                    item.addProperty("side", edge.getSide().name());
                    item.addProperty("startFraction", edge.getStartFraction());
                    item.addProperty("endFraction", edge.getEndFraction());
                    item.addProperty("offset", edge.getOffset());
                    edges.add(item);
                }
                platforms.add(placement);
            }
            stations.add(value);
        }
        for (CustomText text : network.getAllCustomTexts()) {
            JsonObject value = positioned(text.getId(), text.getX(), text.getY());
            value.addProperty("text", text.getText());
            value.addProperty("size", text.getSize());
            value.addProperty("font", text.getFont());
            value.addProperty("color", text.getColor());
            texts.add(value);
        }
        return JSON.toJson(root);
    }

    private static JsonObject positioned(int id, int x, int y) {
        JsonObject value = new JsonObject();
        value.addProperty("id", id);
        value.addProperty("x", x);
        value.addProperty("y", y);
        return value;
    }

    public static Network fromJson(String data) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(data))) {
            reader.setStrictness(Strictness.STRICT);
            JsonObject root = object(readValue(reader, 0), "diagram");
            if (reader.peek() != JsonToken.END_DOCUMENT) { throw invalid("Unexpected content after diagram."); }
            int version = integer(root, "formatVersion");
            if (version != FORMAT_VERSION) { throw invalid("Unsupported formatVersion: " + version); }
            Network network = new Network();
            for (JsonElement item : array(root, "nodes")) {
                JsonObject value = object(item, "node");
                int id = integer(value, "id");
                try {
                    network.addNode(new Node(id, integer(value, "x"), integer(value, "y"),
                            enumeration(value, "type", NodeType.class)));
                } catch (IllegalArgumentException failure) { throw invalid("Node " + id + ": " + failure.getMessage()); }
            }
            for (JsonElement item : array(root, "tracks")) {
                JsonObject value = object(item, "track");
                int id = integer(value, "id");
                try {
                    int startId = integer(value, "startNodeId"), endId = integer(value, "endNodeId");
                    Node start = network.getNode(startId), end = network.getNode(endId);
                    if (start == null || end == null) {
                        throw invalid("Track " + id + " references missing node " + (start == null ? startId : endId));
                    }
                    if (start.getX() == end.getX() && start.getY() == end.getY()) {
                        throw invalid("Track " + id + " must have visible length.");
                    }
                    network.addTrackSegment(new TrackSegment(id, start, end, enumeration(value, "type", TrackType.class)));
                } catch (IllegalArgumentException failure) { throw invalid("Track " + id + ": " + failure.getMessage()); }
            }
            for (JsonElement item : array(root, "stations")) {
                JsonObject value = object(item, "station");
                int id = integer(value, "id");
                try {
                    Station station = new Station(id, string(value, "name"), integer(value, "x"), integer(value, "y"));
                    network.addStation(station);
                    for (JsonElement platformItem : array(value, "platforms")) {
                        JsonObject placement = object(platformItem, "platform");
                        int number = integer(placement, "number");
                        try {
                            JsonArray edgeValues = array(placement, "edges");
                            PlatformEdge[] edges = new PlatformEdge[edgeValues.size()];
                            for (int i = 0; i < edges.length; i++) {
                                JsonObject edge = object(edgeValues.get(i), "platform edge");
                                int trackId = integer(edge, "trackId");
                                TrackSegment track = network.getTrackSegment(trackId);
                                if (track == null) { throw invalid("references missing track " + trackId); }
                                edges[i] = new PlatformEdge(track, enumeration(edge, "side", TrackSide.class),
                                        number(edge, "startFraction"), number(edge, "endFraction"), number(edge, "offset"));
                            }
                            Platform platform = new Platform(number, edges);
                            PlatformGeometry.outline(platform);
                            station.addPlatform(platform);
                        } catch (IllegalArgumentException | IOException failure) {
                            throw invalid("Platform " + number + ": " + failure.getMessage());
                        }
                    }
                } catch (IllegalArgumentException | IOException failure) {
                    throw invalid("Station " + id + ": " + failure.getMessage());
                }
            }
            for (JsonElement item : array(root, "customTexts")) {
                JsonObject value = object(item, "custom text");
                int id = integer(value, "id");
                try {
                    network.addCustomText(new CustomText(string(value, "text"), integer(value, "x"), integer(value, "y"),
                            integer(value, "size"), string(value, "font"), string(value, "color"), id));
                } catch (IllegalArgumentException failure) { throw invalid("Custom text " + id + ": " + failure.getMessage()); }
            }
            return network;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new IOException("Invalid diagram JSON: " + failure.getMessage(), failure);
        }
    }

    // Read strictly, including duplicate property names that a normal JSON tree parser would overwrite.
    private static JsonElement readValue(JsonReader reader, int depth) throws IOException {
        if (depth > 32) { throw invalid("JSON is nested too deeply."); }
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                reader.beginObject();
                JsonObject object = new JsonObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (object.has(key)) { throw invalid("Duplicate JSON property: " + key); }
                    object.add(key, readValue(reader, depth + 1));
                }
                reader.endObject();
                return object;
            case BEGIN_ARRAY:
                reader.beginArray();
                JsonArray array = new JsonArray();
                while (reader.hasNext()) { array.add(readValue(reader, depth + 1)); }
                reader.endArray();
                return array;
            case STRING: return new JsonPrimitive(reader.nextString());
            case NUMBER: return new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            case NULL: reader.nextNull(); return JsonNull.INSTANCE;
            default: throw invalid("Expected a JSON value at " + reader.getPath());
        }
    }

    private static JsonObject object(JsonElement value, String description) throws IOException {
        if (value == null || !value.isJsonObject()) { throw invalid("Expected an object for " + description); }
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonObject parent, String key) throws IOException {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) { throw invalid("Expected an array for " + key); }
        return value.getAsJsonArray();
    }

    private static JsonPrimitive primitive(JsonObject parent, String key, boolean numeric) throws IOException {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive()
                || (numeric ? !value.getAsJsonPrimitive().isNumber() : !value.getAsJsonPrimitive().isString())) {
            throw invalid("Expected " + (numeric ? "a number" : "a string") + " for " + key);
        }
        return value.getAsJsonPrimitive();
    }

    private static int integer(JsonObject parent, String key) throws IOException {
        try { return primitive(parent, key, true).getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException failure) { throw invalid("Expected a 32-bit integer for " + key); }
    }

    private static double number(JsonObject parent, String key) throws IOException {
        double value = primitive(parent, key, true).getAsDouble();
        if (!Double.isFinite(value)) { throw invalid("Expected a finite number for " + key); }
        return value;
    }

    private static String string(JsonObject parent, String key) throws IOException {
        return primitive(parent, key, false).getAsString();
    }

    private static <T extends Enum<T>> T enumeration(JsonObject parent, String key, Class<T> type) throws IOException {
        String value = string(parent, key);
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException failure) { throw invalid("Unknown " + type.getSimpleName() + ": " + value); }
    }

    private static IOException invalid(String message) { return new IOException(message); }
}
