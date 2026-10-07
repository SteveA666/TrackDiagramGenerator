package ui;

import java.awt.*;
import javax.swing.*;
import model.NodeType;
import model.TrackType;

/**
 * EditorToolbar<br>
 * Provides canvas tools, object type choices,<br>
 * and optional grid snapping.<br>
 * Sends editing preferences to the diagram panel without modifying the<br>
 * model directly.<br>
 */
public final class EditorToolbar extends JToolBar {

    public EditorToolbar(DiagramPanel panel) {
        setFloatable(false);
        ButtonGroup tools = new ButtonGroup();
        addTool(tools, panel, "Select / Move", DiagramPanel.Tool.SELECT);
        addTool(tools, panel, "Add Node", DiagramPanel.Tool.ADD_NODE);
        addTool(tools, panel, "Add Track", DiagramPanel.Tool.ADD_TRACK);
        addTool(tools, panel, "Add Station", DiagramPanel.Tool.ADD_STATION);
        addTool(tools, panel, "Custom Text", DiagramPanel.Tool.ADD_TEXT);
        addTool(tools, panel, "Add Platform", DiagramPanel.Tool.ADD_PLATFORM);
        addTool(tools, panel, "Pan", DiagramPanel.Tool.PAN);
        addSeparator();
        add(new JLabel("Node: "));
        JComboBox<String> nodes = new JComboBox<>(new String[]{"Regular", "Stub end"});
        nodes.setToolTipText("Type of node to create");
        nodes.addActionListener(event -> panel.setNodeType(nodes.getSelectedIndex() == 0
                ? NodeType.REGULAR : NodeType.STUB_END));
        add(nodes);
        addSeparator();
        add(new JLabel("Track: "));
        JComboBox<String> tracks = new JComboBox<>(new String[]{"Mainline", "Station", "Siding"});
        tracks.setToolTipText("Type of track to create");
        TrackType[] types = {TrackType.MAINLINE, TrackType.STATION, TrackType.SIDING};
        tracks.addActionListener(event -> panel.setTrackType(types[tracks.getSelectedIndex()]));
        add(tracks);
        addSeparator();
        JCheckBox snap = new JCheckBox("Snap to grid", panel.isSnapToGrid());
        snap.setToolTipText("Place and move objects on a 20-unit grid");
        snap.addActionListener(event -> panel.setSnapToGrid(snap.isSelected()));
        add(snap);
        JCheckBox continuous = new JCheckBox("Continuous Draw", panel.isContinuousDraw());
        continuous.setToolTipText("Use each completed track's endpoint as the next starting point");
        continuous.addActionListener(event -> panel.setContinuousDraw(continuous.isSelected()));
        panel.addPropertyChangeListener("continuousDraw", event -> continuous.setSelected(panel.isContinuousDraw()));
        add(continuous);
        // Keep both rows accessible at the minimum window width.
        setLayout(new GridBagLayout());
        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            GridBagConstraints cell = new GridBagConstraints();
            cell.gridx = i < 7 ? i : i - 7;
            cell.gridy = i < 7 ? 0 : 1;
            if (components[i] == continuous) { cell.gridx = 7; cell.gridy = 0; }
            cell.anchor = GridBagConstraints.WEST;
            cell.insets = new Insets(2, 2, 2, 2);
            add(components[i], cell);
        }
    }

    // Tool buttons
    private void addTool(ButtonGroup group, DiagramPanel panel, String label, DiagramPanel.Tool tool) {
        JToggleButton button = new JToggleButton(label, panel.getTool() == tool);
        String shortcut = tool == DiagramPanel.Tool.ADD_NODE ? " (N)"
                : tool == DiagramPanel.Tool.ADD_TRACK ? " (T)"
                : tool == DiagramPanel.Tool.ADD_STATION ? " (S)"
                : tool == DiagramPanel.Tool.ADD_PLATFORM ? " (P)"
                : tool == DiagramPanel.Tool.ADD_TEXT ? " (X)" : "";
        button.setToolTipText(label + shortcut);
        button.setMargin(new Insets(4, 8, 4, 8));
        button.addActionListener(event -> panel.setTool(tool));
        panel.addPropertyChangeListener("tool", event -> button.setSelected(panel.getTool() == tool));
        group.add(button);
        add(button);
    }
}
