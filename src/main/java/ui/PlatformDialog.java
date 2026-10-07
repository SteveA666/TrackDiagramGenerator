package ui;

import editor.DiagramEditor;
import java.awt.*;
import javax.swing.*;
import model.*;

/**
 * PlatformDialog<br>
 * Edits station ownership for new platforms, numbering, and placement.<br>
 * Supports one side edge or two island edges with validated input.<br>
 * Leaves the model unchanged until the caller commits the result.<br>
 */
public final class PlatformDialog extends JPanel {
    private final DiagramEditor editor;
    private final Platform existing;
    private final JComboBox<Station> stations;
    private final JTextField number = new JTextField(8);
    private final JCheckBox island = new JCheckBox("Island platform (two track edges)");
    private final EdgeFields first;
    private final EdgeFields second;

    /**
     * Result<br>
     * Carries validated replacement data<br>
     * or an explicit delete request.<br>
     */
    public static final class Result {
        public final Station station;
        public final Platform platform;
        public final boolean delete;

        public Result(Station station, Platform platform, boolean delete) {
            this.station = station;
            this.platform = platform;
            this.delete = delete;
        }
    }

    public PlatformDialog(DiagramEditor editor, Platform existing, TrackSegment track) {
        this.editor = editor;
        this.existing = existing;
        Network network = editor.getNetwork();
        setLayout(new BorderLayout(6, 6));
        stations = new JComboBox<>(network.getAllStations().toArray(new Station[0]));
        stations.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                Station station = (Station) value;
                String label = station == null ? "" : station.getName() + " (" + station.getId() + ")";
                return super.getListCellRendererComponent(list, label, index, selected, focused);
            }
        });
        JPanel identity = new JPanel(new GridLayout(0, 2, 6, 6));
        identity.add(new JLabel("Station:")); identity.add(stations);
        identity.add(new JLabel("Platform number:")); identity.add(number);
        identity.add(island);
        add(identity, BorderLayout.NORTH);
        first = new EdgeFields(network, "First edge");
        second = new EdgeFields(network, "Second edge (islands only)");
        JPanel edges = new JPanel(new GridLayout(1, 2, 10, 0));
        edges.add(first); edges.add(second);
        add(edges, BorderLayout.CENTER);
        add(new JLabel("Sides use screen directions. Extents follow track start to end as percentages."),
                BorderLayout.SOUTH);
        if (track != null) { first.tracks.setSelectedItem(track); }
        if (network.segmentCount() > 1) {
            for (TrackSegment candidate : network.getAllTrackSegments()) {
                if (candidate != first.tracks.getSelectedItem()) { second.tracks.setSelectedItem(candidate); break; }
            }
        }
        second.side.setSelectedIndex(1);
        if (existing != null) {
            stations.setSelectedItem(existing.getStation());
            stations.setEnabled(false);
            number.setText("" + existing.getNumber());
            island.setSelected(existing.isIslandPlatform());
            first.load(existing.getEdges().get(0));
            if (existing.isIslandPlatform()) { second.load(existing.getEdges().get(1)); }
        } else {
            setNextNumber();
            stations.addActionListener(event -> setNextNumber());
        }
        island.addActionListener(event -> second.setFieldsEnabled(island.isSelected()));
        second.setFieldsEnabled(island.isSelected());
    }

    // Dialog result and validation
    public Result showDialog(Component parent) {
        String[] actions = existing == null ? new String[]{"Create", "Cancel"}
                : new String[]{"Apply", "Cancel", "Delete platform"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(parent, this,
                    existing == null ? "Add platform" : "Edit platform", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, actions, actions[0]);
            if (choice == 2 && existing != null) { return new Result(existing.getStation(), existing, true); }
            if (choice != 0) { return null; }
            try {
                return readResult();
            } catch (IllegalArgumentException failure) {
                JOptionPane.showMessageDialog(parent, failure.getMessage(), "Invalid platform", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public Result readResult() {
        Station station = (Station) stations.getSelectedItem();
        if (station == null) { throw new IllegalArgumentException("Add a station first."); }
        Platform replacement = island.isSelected()
                ? new Platform(Integer.parseInt(number.getText().trim()), first.read(), second.read())
                : new Platform(Integer.parseInt(number.getText().trim()), first.read());
        editor.validatePlatform(station.getId(), existing, replacement);
        return new Result(station, replacement, false);
    }

    private void setNextNumber() {
        Station station = (Station) stations.getSelectedItem();
        int candidate = 1;
        if (station != null) {
            while (station.getPlatform(candidate) != null && candidate < Integer.MAX_VALUE) { candidate++; }
        }
        number.setText("" + candidate);
    }

    /**
     * EdgeFields<br>
     * Collects one track edge's side, fractional extent, and offset.<br>
     */
    private static final class EdgeFields extends JPanel {
        private final JComboBox<TrackSegment> tracks;
        private final JComboBox<String> side = new JComboBox<>();
        private final JTextField start = new JTextField("20");
        private final JTextField end = new JTextField("80");
        private final JTextField offset = new JTextField("8");

        EdgeFields(Network network, String title) {
            setLayout(new GridLayout(0, 2, 6, 6));
            setBorder(BorderFactory.createTitledBorder(title));
            tracks = new JComboBox<>(network.getAllTrackSegments().toArray(new TrackSegment[0]));
            tracks.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean selected, boolean focused) {
                    TrackSegment track = (TrackSegment) value;
                    String label = track == null ? "" : "Track " + track.getId() + " ("
                            + track.getStart().getX() + "," + track.getStart().getY() + " to "
                            + track.getEnd().getX() + "," + track.getEnd().getY() + ")";
                    return super.getListCellRendererComponent(list, label, index, selected, focused);
                }
            });
            tracks.addActionListener(event -> updateSideChoices());
            updateSideChoices();
            add(new JLabel("Track:")); add(tracks);
            add(new JLabel("Side:")); add(side);
            add(new JLabel("Start (%):")); add(start);
            add(new JLabel("End (%):")); add(end);
            add(new JLabel("Offset (units):")); add(offset);
        }

        // Screen choices translate to the model's directed track sides.
        private void updateSideChoices() {
            int selected = Math.max(0, side.getSelectedIndex());
            TrackSegment track = (TrackSegment) tracks.getSelectedItem();
            boolean vertical = track != null && isMostlyVertical(track);
            side.removeAllItems();
            side.addItem(vertical ? "Left of screen" : "Above track");
            side.addItem(vertical ? "Right of screen" : "Below track");
            side.setSelectedIndex(selected);
        }

        private boolean isMostlyVertical(TrackSegment track) {
            double dx = (double) track.getEnd().getX() - track.getStart().getX();
            double dy = (double) track.getEnd().getY() - track.getStart().getY();
            return Math.abs(dy) >= Math.abs(dx);
        }

        private TrackSide firstScreenSide(TrackSegment track) {
            boolean leftNormalFacesFirst = isMostlyVertical(track)
                    ? track.getEnd().getY() < track.getStart().getY()
                    : track.getEnd().getX() > track.getStart().getX();
            return leftNormalFacesFirst ? TrackSide.LEFT : TrackSide.RIGHT;
        }

        void setFieldsEnabled(boolean enabled) {
            for (Component component : getComponents()) { component.setEnabled(enabled); }
        }

        void load(PlatformEdge edge) {
            tracks.setSelectedItem(edge.getTrackSegment());
            side.setSelectedIndex(edge.getSide() == firstScreenSide(edge.getTrackSegment()) ? 0 : 1);
            start.setText("" + edge.getStartFraction() * 100);
            end.setText("" + edge.getEndFraction() * 100);
            offset.setText("" + edge.getOffset());
        }

        PlatformEdge read() {
            TrackSegment track = (TrackSegment) tracks.getSelectedItem();
            if (track == null) { throw new IllegalArgumentException("Choose a track."); }
            TrackSide chosen = firstScreenSide(track);
            if (side.getSelectedIndex() == 1) { chosen = chosen == TrackSide.LEFT ? TrackSide.RIGHT : TrackSide.LEFT; }
            return new PlatformEdge(track, chosen,
                    Double.parseDouble(start.getText().trim()) / 100,
                    Double.parseDouble(end.getText().trim()) / 100, Double.parseDouble(offset.getText().trim()));
        }
    }
}
