package ui;

import javax.swing.*;
import java.awt.*;

import model.*;

/**
 * ProgramWindow<br>
 * Hosts the diagram panel and menus in the main application window.<br>
 * Handles menu actions, including changing debug node visibility.<br>
 */
public class ProgramWindow extends JFrame implements MenuBar.MenuActions {
    private final DiagramPanel diagramPanel;

    public ProgramWindow(Network network) {
        setTitle("Track Diagram Generator");
        setSize(800, 600);
        setMinimumSize(new Dimension(800, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        diagramPanel = new DiagramPanel(network);
        JPanel controls = new JPanel(new GridLayout(0, 1));
        controls.add(new EditorToolbar(diagramPanel));
        controls.add(new NavigationToolbar(diagramPanel));
        add(controls, BorderLayout.NORTH);
        add(diagramPanel, BorderLayout.CENTER);
        JLabel status = new JLabel(diagramPanel.getStatusMessage());
        status.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        diagramPanel.addPropertyChangeListener("status", event -> {
            status.setText(diagramPanel.getStatusMessage());
            status.setToolTipText(diagramPanel.getStatusMessage());
            status.setForeground(diagramPanel.isStatusError() ? new Color(170, 35, 35) : Color.DARK_GRAY);
        });
        add(status, BorderLayout.SOUTH);

        setJMenuBar(new MenuBar(this));
    }

    @Override public void onNew()      { JOptionPane.showMessageDialog(this, "New (TODO)"); }
    @Override public void onAbout()  { JOptionPane.showMessageDialog(this, "Track Diagram Generator\nVersion 0.0.1\nInternal"); }
    @Override public void onOpen()     { JOptionPane.showMessageDialog(this, "Open (TODO)"); }
    @Override public void onSave()     { JOptionPane.showMessageDialog(this, "Save (TODO)"); }
    @Override public void onSaveAs()   { JOptionPane.showMessageDialog(this, "Save As (TODO)"); }
    @Override public void onExport()   { JOptionPane.showMessageDialog(this, "Export (TODO)"); }
    @Override public void onSettings() { JOptionPane.showMessageDialog(this, "Settings (TODO)"); }

    @Override public void onExit()     { dispose(); System.exit(0); }
    @Override public void onSetDebugNodes(boolean visible) {
        diagramPanel.setShowDebugNodes(visible);
    }

}
