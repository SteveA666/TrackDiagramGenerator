package app;

import io.DiagramStore;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import settings.SettingsStore;
import static org.junit.jupiter.api.Assertions.*;

class PortablePathsTests {
    @TempDir Path temporary;

    @Test void findsPortableRootThroughAllNativeLayouts() throws Exception {
        Path root = Files.createDirectories(temporary.resolve("USB folder with spaces"));
        Files.createFile(root.resolve(PortablePaths.MARKER));
        for (String jar : new String[]{"app/application.jar", "lib/app/application.jar",
                "TrackDiagramGenerator.app/Contents/app/application.jar", "application.jar"}) {
            Path location = root.resolve(jar);
            Files.createDirectories(location.getParent());
            Files.createFile(location);
            assertEquals(root, PortablePaths.resolve(location, temporary, null));
        }
    }

    @Test void unmarkedJarUsesWorkingDirectoryAndOverrideWins() throws Exception {
        Path root = Files.createDirectories(temporary.resolve("application"));
        Files.createFile(root.resolve(PortablePaths.MARKER));
        Path elsewhere = temporary.resolve("chosen data");
        assertEquals(elsewhere, PortablePaths.resolve(root.resolve("app.jar"), temporary, elsewhere.toString()));
        assertEquals(temporary, PortablePaths.resolve(temporary.resolve("plain/app.jar"), temporary, ""));
    }

    @Test void overrideRoutesBothSettingsAndDiagramsToTheSameWritableHome() throws Exception {
        String previous = System.getProperty(PortablePaths.HOME_PROPERTY);
        try {
            System.setProperty(PortablePaths.HOME_PROPERTY, temporary.toString());
            assertEquals(temporary.resolve("settings.json"), SettingsStore.defaultStore().getPath());
            assertEquals(temporary.resolve("saved_diagrams"), DiagramStore.defaultStore().getDirectory());
            var settings = SettingsStore.defaultStore();
            var preferences = settings.load();
            preferences.gridSpacing = 30;
            settings.save(preferences);
            assertEquals(30, settings.load().gridSpacing);
            assertTrue(DiagramStore.defaultStore().list().isEmpty());
            assertTrue(Files.isDirectory(temporary.resolve("saved_diagrams")));
        } finally {
            if (previous == null) { System.clearProperty(PortablePaths.HOME_PROPERTY); }
            else { System.setProperty(PortablePaths.HOME_PROPERTY, previous); }
        }
    }

    @Test void installedDataLivesInTheUsersLocalDataDirectory() {
        assertEquals(temporary.resolve("local/TrackDiagramGenerator"),
                PortablePaths.installedHome(temporary.resolve("local").toString(), temporary));
        assertEquals(temporary.resolve("AppData/Local/TrackDiagramGenerator"),
                PortablePaths.installedHome(null, temporary));
    }

    @Test void explicitHomeWinsOverInstalledMode() {
        String previousMode = System.getProperty(PortablePaths.INSTALLED_PROPERTY);
        String previousHome = System.getProperty(PortablePaths.HOME_PROPERTY);
        try {
            System.setProperty(PortablePaths.INSTALLED_PROPERTY, "true");
            System.setProperty(PortablePaths.HOME_PROPERTY, temporary.toString());
            assertEquals(temporary, PortablePaths.home());
            System.clearProperty(PortablePaths.HOME_PROPERTY);
            assertEquals(PortablePaths.installedHome(System.getenv("LOCALAPPDATA"),
                    Path.of(System.getProperty("user.home"))), PortablePaths.home());
        } finally {
            if (previousMode == null) { System.clearProperty(PortablePaths.INSTALLED_PROPERTY); }
            else { System.setProperty(PortablePaths.INSTALLED_PROPERTY, previousMode); }
            if (previousHome == null) { System.clearProperty(PortablePaths.HOME_PROPERTY); }
            else { System.setProperty(PortablePaths.HOME_PROPERTY, previousHome); }
        }
    }
}
