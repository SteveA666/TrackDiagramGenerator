package ui;

import java.awt.Insets;
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
        snap.setToolTipText("Place and move nodes on a 20-unit grid");
        snap.addActionListener(event -> panel.setSnapToGrid(snap.isSelected()));
        add(snap);
    }

    private void addTool(ButtonGroup group, DiagramPanel panel, String label, DiagramPanel.Tool tool) {
        JToggleButton button = new JToggleButton(label, panel.getTool() == tool);
        String shortcut = tool == DiagramPanel.Tool.ADD_NODE ? " (N)"
                : tool == DiagramPanel.Tool.ADD_TRACK ? " (T)" : "";
        button.setToolTipText(label + shortcut);
        button.setMargin(new Insets(4, 8, 4, 8));
        button.addActionListener(event -> panel.setTool(tool));
        panel.addPropertyChangeListener("tool", event -> button.setSelected(panel.getTool() == tool));
        group.add(button);
        add(button);
    }
}
