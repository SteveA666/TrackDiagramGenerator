package ui;

import i18n.Messages;

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
        addTool(tools, panel, Messages.text("editorToolbar.select.move"), DiagramPanel.Tool.SELECT);
        addTool(tools, panel, Messages.text("editorToolbar.add.node"), DiagramPanel.Tool.ADD_NODE);
        addTool(tools, panel, Messages.text("editorToolbar.add.track"), DiagramPanel.Tool.ADD_TRACK);
        addTool(tools, panel, Messages.text("editorToolbar.add.station"), DiagramPanel.Tool.ADD_STATION);
        addTool(tools, panel, Messages.text("editorToolbar.custom.text"), DiagramPanel.Tool.ADD_TEXT);
        addTool(tools, panel, Messages.text("editorToolbar.add.platform"), DiagramPanel.Tool.ADD_PLATFORM);
        addTool(tools, panel, Messages.text("editorToolbar.pan"), DiagramPanel.Tool.PAN);
        addSeparator();
        add(new JLabel(Messages.text("editorToolbar.node")));
        JComboBox<String> nodes = new JComboBox<>(new String[]{Messages.text("editorToolbar.regular"), Messages.text("editorToolbar.stub.end")});
        nodes.setSelectedIndex(panel.getNodeType() == NodeType.REGULAR ? 0 : 1);
        nodes.setToolTipText(Messages.text("editorToolbar.type.of.node.to.create"));
        nodes.addActionListener(event -> panel.setNodeType(nodes.getSelectedIndex() == 0
                ? NodeType.REGULAR : NodeType.STUB_END));
        add(nodes);
        addSeparator();
        add(new JLabel(Messages.text("editorToolbar.track")));
        JComboBox<String> tracks = new JComboBox<>(new String[]{Messages.text("editorToolbar.mainline"), Messages.text("editorToolbar.station"), Messages.text("editorToolbar.siding")});
        tracks.setToolTipText(Messages.text("editorToolbar.type.of.track.to.create"));
        TrackType[] types = {TrackType.MAINLINE, TrackType.STATION, TrackType.SIDING};
        tracks.setSelectedIndex(panel.getTrackType().ordinal());
        tracks.addActionListener(event -> panel.setTrackType(types[tracks.getSelectedIndex()]));
        add(tracks);
        addSeparator();
        JCheckBox snap = new JCheckBox(Messages.text("editorToolbar.snap.to.grid"), panel.isSnapToGrid());
        snap.setToolTipText(Messages.text("toolbar.snapHint", panel.getGridSpacing()));
        snap.addActionListener(event -> panel.setSnapToGrid(snap.isSelected()));
        add(snap);
        JCheckBox continuous = new JCheckBox(Messages.text("editorToolbar.continuous.draw"), panel.isContinuousDraw());
        continuous.setToolTipText(Messages.text("editorToolbar.use.each.completed.track.s.endpoint.as.the.next"));
        continuous.addActionListener(event -> panel.setContinuousDraw(continuous.isSelected()));
        panel.addPropertyChangeListener("continuousDraw", event -> continuous.setSelected(panel.isContinuousDraw()));
        add(continuous);
        panel.addPropertyChangeListener("preferences", event -> {
            nodes.setSelectedIndex(panel.getNodeType() == NodeType.REGULAR ? 0 : 1);
            tracks.setSelectedIndex(panel.getTrackType().ordinal());
            snap.setSelected(panel.isSnapToGrid());
            snap.setToolTipText(Messages.text("toolbar.snapHint", panel.getGridSpacing()));
            continuous.setSelected(panel.isContinuousDraw());
        });
        // Keep tools together and wrap translated labels without shrinking controls.
        setLayout(new WrappingToolbarLayout());
    }

    private static final class WrappingToolbarLayout implements LayoutManager {
        private int lastWidth = -1;
        @Override public void addLayoutComponent(String name, Component component) {}
        @Override public void removeLayoutComponent(Component component) {}
        @Override public Dimension preferredLayoutSize(Container parent) { return arrange(parent, false); }
        @Override public Dimension minimumLayoutSize(Container parent) { return preferredLayoutSize(parent); }

        @Override public void layoutContainer(Container parent) {
            arrange(parent, true);
            // A width change may add or remove a row in the enclosing BorderLayout.
            if (parent.getWidth() != lastWidth) {
                lastWidth = parent.getWidth();
                if (parent instanceof JComponent component) { component.revalidate(); }
            }
        }

        private Dimension arrange(Container parent, boolean place) {
            Insets insets = parent.getInsets();
            int width = parent.getWidth() > 0 ? parent.getWidth() : 800;
            int left = insets.left + 2, right = width - insets.right - 2;
            int rowHeight = 0;
            for (Component child : parent.getComponents()) {
                if (child.isVisible()) { rowHeight = Math.max(rowHeight, child.getPreferredSize().height); }
            }
            int x = left, y = insets.top + 2, usedWidth = left;
            Component[] children = parent.getComponents();
            for (int i = 0; i < children.length; i++) {
                // Place Continuous Draw with the tools while preserving component/focus order.
                int index = i == 7 ? children.length - 1 : i > 7 ? i - 1 : i;
                Component child = children[index];
                if (!child.isVisible()) { continue; }
                Dimension size = child.getPreferredSize();
                // Begin the object choices on their own row, after the eight tools/toggles.
                if (x > left && (i == 8 || x + size.width > right)) {
                    x = left;
                    y += rowHeight + 4;
                }
                if (place) { child.setBounds(x, y + (rowHeight - size.height) / 2, size.width, size.height); }
                x += size.width + 4;
                usedWidth = Math.max(usedWidth, x);
            }
            return new Dimension(usedWidth + insets.right, y + rowHeight + insets.bottom + 2);
        }
    }

    // Tool buttons
    private void addTool(ButtonGroup group, DiagramPanel panel, String label, DiagramPanel.Tool tool) {
        JToggleButton button = new JToggleButton(label, panel.getTool() == tool);
        String shortcut = tool == DiagramPanel.Tool.SELECT ? " (A)"
                : tool == DiagramPanel.Tool.ADD_NODE ? " (N)"
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
