package ui;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import model.*;
import settings.AppSettings;

/** Editable draft. Reading validates every field before any preference is applied. */
public final class SettingsPanel extends JPanel {
    private final JSpinner windowWidth = spinner(800, 800, 7680), windowHeight = spinner(600, 400, 4320);
    private final JCheckBox maximize = new JCheckBox("Open maximized");
    private final JCheckBox snap = new JCheckBox("Snap to grid");
    private final JCheckBox continuous = new JCheckBox("Continuous drawing by default");
    private final JSpinner spacing = spinner(20, 1, 200), undo = spinner(100, 1, 1000);
    private final JComboBox<NodeType> node = new JComboBox<>(NodeType.values());
    private final JComboBox<TrackType> track = new JComboBox<>(TrackType.values());
    private final JCheckBox grid = new JCheckBox("Show grid (independent of snapping)");
    private final JCheckBox debug = new JCheckBox("Show debug node circles");
    private final JCheckBox antialias = new JCheckBox("Smooth lines and text");
    private final JTextField background = new JTextField(12), gridColor = new JTextField(12), textColor = new JTextField(12);
    private final JSpinner wheel = spinner(15, 1, 100), textSize = spinner(16, 1, 512);
    private final JCheckBox invert = new JCheckBox("Invert mouse-wheel zoom");
    private final JCheckBox fit = new JCheckBox("Fit diagram when opening a file");
    private final JComboBox<String> font = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    private final JSpinner start = spinner(20, 0, 99), end = spinner(80, 1, 100);
    private final JSpinner offset = new JSpinner(new SpinnerNumberModel(8.0, 0.0, 1000.0, 1.0));
    private final JCheckBox backup = new JCheckBox("Keep previous save as filename.json.bak");
    private final JCheckBox overwrite = new JCheckBox("Confirm replacement in Save As");

    public SettingsPanel(AppSettings settings) {
        super(new BorderLayout(8, 8));
        font.setEditable(true);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Window", form("Window width (pixels):", windowWidth, "Window height (pixels):", windowHeight,
                "", maximize, "", new JLabel("Window dimensions are limited to the available screen area.")));
        tabs.addTab("Editing", form("", snap, "Grid spacing (units):", spacing, "", continuous,
                "Default node type:", node, "Default track type:", track, "Undo history (edits):", undo));
        tabs.addTab("Appearance", form("", grid, "", debug, "", antialias,
                "Background (#RRGGBB):", background, "Grid (#RRGGBB):", gridColor));
        tabs.addTab("Navigation", form("Wheel zoom step (%):", wheel, "", invert, "", fit));
        tabs.addTab("New objects", form("Text font:", font, "Text size (units):", textSize,
                "Text colour (#RRGGBB):", textColor, "Platform start (%):", start,
                "Platform end (%):", end, "Platform offset (units):", offset));
        tabs.addTab("Saving", form("", backup, "", overwrite,
                "Diagram folder:", new JLabel("saved_diagrams/"),
                "", new JLabel("Changes apply immediately; existing object styles are preserved.")));
        add(tabs, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(620, 350));
        load(settings);
    }

    public void load(AppSettings settings) {
        windowWidth.setValue(settings.windowWidth); windowHeight.setValue(settings.windowHeight);
        maximize.setSelected(settings.startMaximized);
        snap.setSelected(settings.snapToGrid); spacing.setValue(settings.gridSpacing);
        continuous.setSelected(settings.continuousDraw); node.setSelectedItem(settings.defaultNodeType);
        track.setSelectedItem(settings.defaultTrackType); undo.setValue(settings.undoLimit);
        grid.setSelected(settings.showGrid); debug.setSelected(settings.showDebugNodes);
        antialias.setSelected(settings.antialiasing); background.setText(settings.backgroundColor);
        gridColor.setText(settings.gridColor); wheel.setValue(settings.wheelZoomPercent);
        invert.setSelected(settings.invertWheelZoom); fit.setSelected(settings.fitOnOpen);
        font.setSelectedItem(settings.textFont); textSize.setValue(settings.textSize); textColor.setText(settings.textColor);
        start.setValue(settings.platformStartPercent); end.setValue(settings.platformEndPercent); offset.setValue(settings.platformOffset);
        backup.setSelected(settings.backupOnSave); overwrite.setSelected(settings.confirmOverwrite);
    }

    public AppSettings readSettings() {
        for (JSpinner field : new JSpinner[]{windowWidth, windowHeight, spacing, undo, wheel, textSize, start, end, offset}) {
            try { field.commitEdit(); }
            catch (ParseException failure) { throw new IllegalArgumentException("Enter valid numbers within the displayed ranges."); }
        }
        AppSettings settings = new AppSettings();
        settings.windowWidth = (int) windowWidth.getValue(); settings.windowHeight = (int) windowHeight.getValue();
        settings.startMaximized = maximize.isSelected();
        settings.snapToGrid = snap.isSelected(); settings.gridSpacing = (int) spacing.getValue();
        settings.continuousDraw = continuous.isSelected(); settings.defaultNodeType = (NodeType) node.getSelectedItem();
        settings.defaultTrackType = (TrackType) track.getSelectedItem(); settings.undoLimit = (int) undo.getValue();
        settings.showGrid = grid.isSelected(); settings.showDebugNodes = debug.isSelected();
        settings.antialiasing = antialias.isSelected(); settings.backgroundColor = background.getText().trim();
        settings.gridColor = gridColor.getText().trim(); settings.wheelZoomPercent = (int) wheel.getValue();
        settings.invertWheelZoom = invert.isSelected(); settings.fitOnOpen = fit.isSelected();
        settings.textFont = font.getSelectedItem() == null ? "" : font.getSelectedItem().toString().trim();
        settings.textSize = (int) textSize.getValue();
        settings.textColor = textColor.getText().trim(); settings.platformStartPercent = (int) start.getValue();
        settings.platformEndPercent = (int) end.getValue(); settings.platformOffset = ((Number) offset.getValue()).doubleValue();
        settings.backupOnSave = backup.isSelected(); settings.confirmOverwrite = overwrite.isSelected();
        settings.validate();
        return settings;
    }

    private static JSpinner spinner(int value, int min, int max) { return new JSpinner(new SpinnerNumberModel(value, min, max, 1)); }

    private static JPanel form(Object... fields) {
        JPanel form = new JPanel(new GridBagLayout());
        for (int i = 0; i < fields.length; i += 2) {
            GridBagConstraints label = new GridBagConstraints();
            label.gridx = 0; label.gridy = i / 2; label.anchor = GridBagConstraints.WEST;
            label.insets = new Insets(7, 7, 7, 7);
            JLabel description = new JLabel((String) fields[i]);
            description.setLabelFor((Component) fields[i + 1]);
            form.add(description, label);
            GridBagConstraints input = (GridBagConstraints) label.clone();
            input.gridx = 1; input.weightx = 1; input.fill = GridBagConstraints.HORIZONTAL;
            form.add((Component) fields[i + 1], input);
        }
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }
}
