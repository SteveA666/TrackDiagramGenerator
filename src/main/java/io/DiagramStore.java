package io;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Files offered by the application's Open and Save dialogs live in this directory. */
public final class DiagramStore {
    private final Path directory;

    public DiagramStore(Path directory) {
        this.directory = Objects.requireNonNull(directory).toAbsolutePath().normalize();
    }

    public static DiagramStore defaultStore() { return new DiagramStore(Path.of("saved_diagrams")); }
    public Path getDirectory() { return directory; }

    public Path resolve(String filename) {
        if (filename == null || filename.isBlank()) { throw new IllegalArgumentException("Enter a diagram filename."); }
        String name = filename.trim();
        if (name.equals(".") || name.equals("..") || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*")) {
            throw new IllegalArgumentException("Enter a filename without folders or special characters.");
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".json")) { name += ".json"; }
        if (name.equalsIgnoreCase(".json")) { throw new IllegalArgumentException("Enter a name before .json."); }
        Path target = directory.resolve(name).normalize();
        if (!directory.equals(target.getParent())) { throw new IllegalArgumentException("Choose a file in saved_diagrams."); }
        return target;
    }

    public List<String> list() throws IOException {
        Files.createDirectories(directory);
        try (var entries = Files.list(directory)) {
            return entries.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }
}
