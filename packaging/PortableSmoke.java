import app.PortablePaths;
import editor.DiagramEditor;
import io.*;
import java.awt.Color;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import model.*;
import settings.SettingsStore;
import ui.DiagramPanel;

/** Runs against the packaged runtime and JAR, before any user data enters the archive. */
public class PortableSmoke {
    public static void main(String[] args) throws Exception {
        Path expected = Path.of(args[0]).toAbsolutePath().normalize();
        if (!PortablePaths.home().equals(expected)) { throw new AssertionError("Portable home: " + PortablePaths.home()); }
        if (!DiagramStore.defaultStore().getDirectory().equals(expected.resolve("saved_diagrams"))) {
            throw new AssertionError("Diagram directory is not portable.");
        }
        SettingsStore.defaultStore().loadDefaults().validate();
        Path scratch = Files.createTempDirectory(expected, "smoke-");
        try {
            Network network = new Network();
            DiagramEditor editor = new DiagramEditor(network);
            Node start = editor.createNode(-100, 0, NodeType.REGULAR);
            Node end = editor.createNode(100, 0, NodeType.STUB_END);
            editor.createTrack(start.getId(), end.getId(), TrackType.MAINLINE);
            editor.createStation("中央 Station", -100, -50);
            DiagramIO.save(network, scratch.resolve("diagram.json"));
            Network restored = DiagramIO.load(scratch.resolve("diagram.json"));
            if (!DiagramIO.toJson(network).equals(DiagramIO.toJson(restored))) { throw new AssertionError("JSON round trip failed."); }
            SettingsStore store = new SettingsStore(scratch.resolve("settings.json"));
            store.save(store.loadDefaults());
            store.load().validate();
            SwingUtilities.invokeAndWait(() -> {
                try {
                    for (DiagramImageIO.Format format : DiagramImageIO.Format.values()) {
                        var image = DiagramPanel.renderImage(network, 2, 20, format == DiagramImageIO.Format.PNG ? null : Color.WHITE);
                        Path path = scratch.resolve("export." + format.name().toLowerCase());
                        DiagramImageIO.save(image, path, format, 90);
                        if (ImageIO.read(path.toFile()) == null) { throw new AssertionError("Image decoder missing: " + format); }
                    }
                } catch (Exception failure) { throw new RuntimeException(failure); }
            });
            System.out.println("PASS: portable paths, JSON, settings, fonts, PNG and JPG using bundled Java");
        } finally {
            try (var children = Files.list(scratch)) {
                for (Path child : children.toList()) { Files.delete(child); }
            }
            Files.delete(scratch);
        }
    }
}
