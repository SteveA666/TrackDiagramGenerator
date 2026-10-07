package settings;

import com.google.gson.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Project-local preferences, separate from diagram data. Never overwrites malformed settings on load. */
public final class SettingsStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().setStrictness(Strictness.STRICT).create();
    private final Path path;

    public SettingsStore(Path path) { this.path = path.toAbsolutePath().normalize(); }
    public static SettingsStore defaultStore() { return new SettingsStore(Path.of("settings.json")); }
    public Path getPath() { return path; }
    public Path getDefaultsPath() { return path.resolveSibling("default.json"); }

    public AppSettings loadDefaults() throws IOException {
        String data;
        if (Files.exists(getDefaultsPath())) {
            data = Files.readString(getDefaultsPath(), StandardCharsets.UTF_8);
        } else {
            try (InputStream input = SettingsStore.class.getResourceAsStream("/default.json")) {
                if (input == null) { throw new IOException("Bundled default.json is missing."); }
                data = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return decode(readObject(data, "default.json"));
    }

    public AppSettings load() throws IOException {
        AppSettings defaults = loadDefaults();
        if (!Files.exists(path)) { return defaults; }
        JsonObject merged = JSON.toJsonTree(defaults).getAsJsonObject();
        JsonObject overrides = readObject(Files.readString(path, StandardCharsets.UTF_8), "settings.json");
        for (var entry : overrides.entrySet()) { merged.add(entry.getKey(), entry.getValue()); }
        return decode(merged);
    }

    private static JsonObject readObject(String data, String description) throws IOException {
        try {
            JsonElement value = JSON.fromJson(data, JsonElement.class);
            if (value == null || !value.isJsonObject()) { throw new IllegalArgumentException("Expected a settings object."); }
            JsonObject object = value.getAsJsonObject();
            for (var entry : object.entrySet()) {
                if (entry.getValue().isJsonNull()) { throw new IllegalArgumentException("Missing value for " + entry.getKey()); }
            }
            return object;
        } catch (JsonParseException | IllegalArgumentException failure) {
            throw new IOException("Invalid " + description + ": " + failure.getMessage(), failure);
        }
    }

    private static AppSettings decode(JsonObject values) throws IOException {
        try {
            AppSettings settings = JSON.fromJson(values, AppSettings.class);
            settings.validate();
            return settings;
        } catch (JsonParseException | IllegalArgumentException failure) {
            throw new IOException("Invalid settings: " + failure.getMessage(), failure);
        }
    }

    public void save(AppSettings settings) throws IOException {
        settings.validate();
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".settings-", ".tmp");
        try {
            Files.writeString(temporary, JSON.toJson(settings) + "\n", StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
