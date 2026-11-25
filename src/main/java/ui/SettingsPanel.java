package ui;

import i18n.Messages;

import java.awt.*;
import java.text.ParseException;
import javax.swing.*;
import model.*;
import settings.AppSettings;

/** Editable draft. Reading validates every field before any preference is applied. */
public final class SettingsPanel extends JPanel {
    private final LanguagePicker language = new LanguagePicker();
    private final JSpinner windowWidth = spinner(800, 800, 7680), windowHeight = spinner(600, 400, 4320);
    private final JCheckBox maximize = new JCheckBox(Messages.text("settingsPanel.open.maximized"));
    private final JCheckBox snap = new JCheckBox(Messages.text("settingsPanel.snap.to.grid"));
    private final JCheckBox continuous = new JCheckBox(Messages.text("settingsPanel.continuous.drawing.by.default"));
    private final JSpinner spacing = spinner(20, 1, 200), undo = spinner(100, 1, 1000);
    private final JComboBox<NodeType> node = new JComboBox<>(NodeType.values());
    private final JComboBox<TrackType> track = new JComboBox<>(TrackType.values());
    private final JCheckBox grid = new JCheckBox(Messages.text("settingsPanel.show.grid.independent.of.snapping"));
    private final JCheckBox debug = new JCheckBox(Messages.text("settingsPanel.show.debug.node.circles"));
    private final JCheckBox antialias = new JCheckBox(Messages.text("settingsPanel.smooth.lines.and.text"));
    private final JTextField background = new JTextField(12), gridColor = new JTextField(12), textColor = new JTextField(12);
    private final JSpinner wheel = spinner(15, 1, 100), textSize = spinner(16, 1, 512);
    private final JCheckBox invert = new JCheckBox(Messages.text("settingsPanel.invert.mouse.wheel.zoom"));
    private final JCheckBox fit = new JCheckBox(Messages.text("settingsPanel.fit.diagram.when.opening.a.file"));
    private final JComboBox<String> font = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    private final JSpinner start = spinner(20, 0, 99), end = spinner(80, 1, 100);
    private final JSpinner offset = new JSpinner(new SpinnerNumberModel(8.0, 0.0, 1000.0, 1.0));
    private final JCheckBox backup = new JCheckBox(Messages.text("settingsPanel.keep.previous.save.as.filename.json.bak"));
    private final JCheckBox overwrite = new JCheckBox(Messages.text("settingsPanel.confirm.replacement.in.save.as"));

    public SettingsPanel(AppSettings settings) {
        super(new BorderLayout(8, 8));
        font.setEditable(true);
        DefaultListCellRenderer enumRenderer = new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                return super.getListCellRendererComponent(list,
                        value instanceof Enum<?> item ? Messages.enumLabel(item) : value, index, selected, focused);
            }
        };
        node.setRenderer(enumRenderer); track.setRenderer(enumRenderer);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(Messages.text("settings.language"), form(Messages.text("settings.language"), language,
                "", new JLabel(Messages.text("settings.languageHelp"))));
        tabs.addTab(Messages.text("settingsPanel.window"), form(Messages.text("settingsPanel.window.width.pixels"), windowWidth, Messages.text("settingsPanel.window.height.pixels"), windowHeight,
                "", maximize, "", new JLabel(Messages.text("settingsPanel.window.dimensions.are.limited.to.the.available.screen.area"))));
        tabs.addTab(Messages.text("settingsPanel.editing"), form("", snap, Messages.text("settingsPanel.grid.spacing.units"), spacing, "", continuous,
                Messages.text("settingsPanel.default.node.type"), node, Messages.text("settingsPanel.default.track.type"), track, Messages.text("settingsPanel.undo.history.edits"), undo));
        tabs.addTab(Messages.text("settingsPanel.appearance"), form("", grid, "", debug, "", antialias,
                Messages.text("settingsPanel.background.rrggbb"), background, Messages.text("settingsPanel.grid.rrggbb"), gridColor));
        tabs.addTab(Messages.text("settingsPanel.navigation"), form(Messages.text("settingsPanel.wheel.zoom.step"), wheel, "", invert, "", fit));
        tabs.addTab(Messages.text("settingsPanel.new.objects"), form(Messages.text("settingsPanel.text.font"), font, Messages.text("settingsPanel.text.size.units"), textSize,
                Messages.text("settingsPanel.text.colour.rrggbb"), textColor, Messages.text("settingsPanel.platform.start"), start,
                Messages.text("settingsPanel.platform.end"), end, Messages.text("settingsPanel.platform.offset.units"), offset));
        tabs.addTab(Messages.text("settingsPanel.saving"), form("", backup, "", overwrite,
                Messages.text("settingsPanel.diagram.folder"), new JLabel("saved_diagrams/"),
                "", new JLabel(Messages.text("settingsPanel.changes.apply.immediately.existing.object.styles.are.preserved"))));
        add(tabs, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(Math.max(620, getPreferredSize().width), Math.max(350, getPreferredSize().height)));
        load(settings);
    }

    public void load(AppSettings settings) {
        language.selectLanguage(settings.languageTag);
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
            catch (ParseException failure) { throw new IllegalArgumentException(Messages.text("settingsPanel.enter.valid.numbers.within.the.displayed.ranges")); }
        }
        AppSettings settings = new AppSettings();
        Object selectedLanguage = language.getSelectedItem();
        settings.languageTag = selectedLanguage == null ? "" : selectedLanguage.toString().trim();
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
