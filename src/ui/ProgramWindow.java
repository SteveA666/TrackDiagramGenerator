package ui;

import javax.swing.*;
//import java.awt.*;

import model.*;

public class ProgramWindow extends JFrame implements MenuBar.MenuActions {
    public ProgramWindow(Network network) {
        setTitle("Track Diagram Generator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new DiagramPanel(network)); 

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
    @Override public void onToggleDebugNodes() {
        DiagramPanel diagramPanel = (DiagramPanel) getContentPane().getComponent(0);
        diagramPanel.setShowDebugNodes(!diagramPanel.isShowDebugNodes());
    }

}
