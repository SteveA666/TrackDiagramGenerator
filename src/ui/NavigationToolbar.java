package ui;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.*;

/**
 * NavigationToolbar<br>
 * Provides zoom buttons, the current scale, and diagram fit controls.<br>
 * Changes the view without modifying diagram coordinates.<br>
 */
public final class NavigationToolbar extends JToolBar {

    public NavigationToolbar(DiagramPanel panel) {
        setFloatable(false);
        addButton("-", "Zoom out at the centre of the canvas", panel::zoomOut);
        JLabel zoom = new JLabel();
        zoom.setHorizontalAlignment(SwingConstants.CENTER);
        zoom.setPreferredSize(new Dimension(62, 24));
        zoom.setMinimumSize(new Dimension(62, 24));
        zoom.setMaximumSize(new Dimension(62, 24));
        Runnable update = () -> zoom.setText(Math.round(panel.getZoom() * 100) + "%");
        panel.addPropertyChangeListener("view", event -> update.run());
        update.run();
        add(zoom);
        addButton("+", "Zoom in at the centre of the canvas", panel::zoomIn);
        addSeparator();
        addButton("Fit", "Fit the diagram in the canvas", panel::fitToDiagram);
        addButton("100%", "Reset the zoom and pan", panel::resetView);
        addSeparator();
        add(new JLabel("Mouse wheel: zoom | Middle drag or Pan tool: move the view"));
    }

    // Navigation buttons
    private void addButton(String label, String tooltip, Runnable action) {
        JButton button = new JButton(label);
        button.setMargin(new Insets(4, 10, 4, 10));
        button.setToolTipText(tooltip);
        button.addActionListener(event -> action.run());
        add(button);
    }
}
