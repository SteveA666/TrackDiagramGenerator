package editor;

import io.DiagramIO;
import io.DiagramStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import model.Network;

/** File identity and the last saved contents, independent of window/dialog code. */
public final class DiagramDocument {
    private final DiagramStore store;
    private Network network;
    private DiagramEditor editor;
    private Path path;
    private boolean backupOnSave;
    private String savedContents = DiagramIO.toJson(new Network());

    public DiagramDocument(DiagramStore store) { this(store, new Network()); }

    public DiagramDocument(DiagramStore store, Network network) {
        this.store = Objects.requireNonNull(store);
        this.network = Objects.requireNonNull(network);
        this.editor = new DiagramEditor(network);
    }

    public DiagramStore getStore() { return store; }
    public Network getNetwork() { return network; }
    public DiagramEditor getEditor() { return editor; }
    public void setBackupOnSave(boolean enabled) { backupOnSave = enabled; }
    public Path getPath() { return path; }
    public boolean isDirty() { return !savedContents.equals(DiagramIO.toJson(network)); }

    public void newDiagram() {
        network = new Network();
        editor = new DiagramEditor(network);
        path = null;
        savedContents = DiagramIO.toJson(network);
    }

    public void open(String filename) throws IOException {
        Path candidate = store.resolve(filename);
        Network loaded = DiagramIO.load(candidate);
        String contents = DiagramIO.toJson(loaded);
        // Neither the active network nor file identity changes until the complete load succeeds.
        network = loaded;
        editor = new DiagramEditor(network);
        path = candidate;
        savedContents = contents;
    }

    public void save() throws IOException {
        if (path == null) { throw new IllegalStateException("Choose a filename using Save As first."); }
        saveAs(path.getFileName().toString());
    }

    public void saveAs(String filename) throws IOException {
        Path candidate = store.resolve(filename);
        DiagramIO.save(network, candidate, backupOnSave);
        path = candidate;
        savedContents = DiagramIO.toJson(network);
    }
}
