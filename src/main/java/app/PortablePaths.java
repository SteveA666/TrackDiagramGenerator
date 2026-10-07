package app;

import java.net.URISyntaxException;
import java.nio.file.*;

/** Portable distributions locate their data by the marker beside the application, not the shell's directory. */
public final class PortablePaths {
    public static final String HOME_PROPERTY = "trackdiagram.home";
    public static final String INSTALLED_PROPERTY = "trackdiagram.installed";
    public static final String MARKER = ".portable";
    private PortablePaths() {}

    public static Path home() {
        String override = System.getProperty(HOME_PROPERTY);
        if (override != null && !override.isBlank()) { return Path.of(override).toAbsolutePath().normalize(); }
        if (Boolean.getBoolean(INSTALLED_PROPERTY)) {
            return installedHome(System.getenv("LOCALAPPDATA"), Path.of(System.getProperty("user.home")));
        }
        try {
            Path location = Path.of(PortablePaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return resolve(location, Path.of("."), null);
        } catch (URISyntaxException failure) {
            throw new IllegalStateException("Cannot locate the application directory.", failure);
        }
    }

    static Path installedHome(String localAppData, Path userHome) {
        Path base = localAppData == null || localAppData.isBlank()
                ? userHome.resolve("AppData/Local") : Path.of(localAppData);
        return base.resolve("TrackDiagramGenerator").toAbsolutePath().normalize();
    }

    static Path resolve(Path application, Path workingDirectory, String override) {
        if (override != null && !override.isBlank()) { return Path.of(override).toAbsolutePath().normalize(); }
        Path directory = application.toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) { directory = directory.getParent(); }
        for (; directory != null; directory = directory.getParent()) {
            if (Files.isRegularFile(directory.resolve(MARKER))) { return directory; }
        }
        // Keep the existing project-local behavior for development and standalone JAR launches.
        return workingDirectory.toAbsolutePath().normalize();
    }
}
