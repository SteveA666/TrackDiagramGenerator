package editor;

import io.DiagramIO;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import model.*;

/** Bounded before/after snapshots for complete, validated editor transactions. */
public final class EditHistory {
    private record Change(String name, String before, String after) {}
    private final Network network;
    private final Deque<Change> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int limit = 100;

    EditHistory(Network network) { this.network = network; }
    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }
    public String undoName() { return canUndo() ? undo.peekLast().name() : ""; }
    public String redoName() { return canRedo() ? redo.peekLast().name() : ""; }
    public void addListener(Runnable listener) { listeners.add(Objects.requireNonNull(listener)); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }

    public void setLimit(int limit) {
        if (limit < 1 || limit > 1000) { throw new IllegalArgumentException("Undo limit must be 1–1000."); }
        this.limit = limit;
        trim();
        changed();
    }

    <T> T perform(String name, Supplier<T> operation) {
        String before = DiagramIO.toJson(network);
        T result = operation.get();
        String after = DiagramIO.toJson(network);
        if (!before.equals(after)) {
            undo.addLast(new Change(name, before, after));
            redo.clear();
            trim();
            changed();
        }
        return result;
    }

    public void undo() {
        if (!canUndo()) { return; }
        Change change = undo.peekLast();
        restore(change.before());
        undo.removeLast();
        redo.addLast(change);
        changed();
    }

    public void redo() {
        if (!canRedo()) { return; }
        Change change = redo.peekLast();
        restore(change.after());
        redo.removeLast();
        undo.addLast(change);
        trim();
        changed();
    }

    private void trim() {
        while (undo.size() + redo.size() > limit) {
            if (!undo.isEmpty()) { undo.removeFirst(); }
            else { redo.removeFirst(); }
        }
    }

    private void changed() { for (Runnable listener : List.copyOf(listeners)) { listener.run(); } }

    private void restore(String snapshot) {
        Network restored;
        try { restored = DiagramIO.fromJson(snapshot); }
        catch (IOException failure) { throw new IllegalStateException("Cannot restore edit history.", failure); }
        // Parse and validate first. Transfer registered objects in dependency order, preserving array order.
        List<Station> stations = new ArrayList<>(restored.getAllStations());
        List<TrackSegment> tracks = new ArrayList<>(restored.getAllTrackSegments());
        List<CustomText> texts = new ArrayList<>(restored.getAllCustomTexts());
        for (Station station : stations) { restored.removeStation(station.getId()); }
        for (TrackSegment track : tracks) { restored.removeTrackSegment(track.getId()); }
        for (CustomText text : texts) { restored.removeCustomText(text.getId()); }
        for (Station station : new ArrayList<>(network.getAllStations())) { network.removeStation(station.getId()); }
        for (Node node : new ArrayList<>(network.getAllNodes())) { network.removeNode(node.getId()); }
        for (CustomText text : new ArrayList<>(network.getAllCustomTexts())) { network.removeCustomText(text.getId()); }
        for (Node node : restored.getAllNodes()) { network.addNode(node); }
        for (TrackSegment track : tracks) { network.addTrackSegment(track); }
        for (Station station : stations) { network.addStation(station); }
        for (CustomText text : texts) { network.addCustomText(text); }
    }
}
