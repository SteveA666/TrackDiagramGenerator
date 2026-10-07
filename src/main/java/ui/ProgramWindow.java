package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import editor.DiagramDocument;
import io.DiagramStore;

import model.*;

/**
 * ProgramWindow<br>
 * Hosts the diagram panel and menus in the main application window.<br>
 * Handles menu actions, including changing debug node visibility.<br>
 */
public class ProgramWindow extends JFrame implements MenuBar.MenuActions {
    private DiagramPanel diagramPanel;
    private final DiagramDocument document;

    public ProgramWindow(Network network) {
        this(new DiagramDocument(DiagramStore.defaultStore(), network));
    }

    public ProgramWindow(DiagramDocument document) {
        this.document = document;
        setSize(800, 600);
        setMinimumSize(new Dimension(800, 400));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { onExit(); }
        });
        setLocationRelativeTo(null);
        setJMenuBar(new MenuBar(this));
        installDiagram(document.getPath() != null);
    }

    private void installDiagram(boolean fit) {
        boolean debug = diagramPanel != null && diagramPanel.isShowDebugNodes();
        if (diagramPanel != null) { diagramPanel.cancelInteraction(); }
        setContentPane(new JPanel(new BorderLayout()));
        diagramPanel = new DiagramPanel(document.getNetwork());
        diagramPanel.setShowDebugNodes(debug);
        JPanel controls = new JPanel(new BorderLayout());
        controls.add(new EditorToolbar(diagramPanel), BorderLayout.NORTH);
        controls.add(new NavigationToolbar(diagramPanel), BorderLayout.SOUTH);
        add(controls, BorderLayout.NORTH);
        add(diagramPanel, BorderLayout.CENTER);
        JLabel status = new JLabel(diagramPanel.getStatusMessage());
        status.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        diagramPanel.addPropertyChangeListener("status", event -> {
            status.setText(diagramPanel.getStatusMessage());
            status.setToolTipText(diagramPanel.getStatusMessage());
            status.setForeground(diagramPanel.isStatusError() ? new Color(170, 35, 35) : Color.DARK_GRAY);
            updateTitle();
        });
        add(status, BorderLayout.SOUTH);

        updateTitle();
        revalidate();
        repaint();
        DiagramPanel installed = diagramPanel;
        if (fit) { SwingUtilities.invokeLater(installed::fitToDiagram); }
    }

    private void updateTitle() {
        String name = document.getPath() == null ? "Untitled" : document.getPath().getFileName().toString();
        setTitle((document.isDirty() ? "* " : "") + name + " — Track Diagram Generator");
    }

    // File actions
    @Override public void onNew() {
        diagramPanel.cancelInteraction();
        if (!confirmDiscard()) { return; }
        document.newDiagram();
        installDiagram(false);
    }

    @Override public void onOpen() {
        diagramPanel.cancelInteraction();
        if (!confirmDiscard()) { return; }
        try {
            String name = DiagramFileDialogs.chooseOpen(this, document.getStore());
            if (name == null) { return; }
            document.open(name);
            installDiagram(true);
        } catch (IOException | IllegalArgumentException failure) {
            DiagramFileDialogs.showError(this, "Open", failure);
        }
    }

    @Override public void onSave() { save(false); }
    @Override public void onSaveAs() { save(true); }

    private boolean save(boolean saveAs) {
        diagramPanel.cancelInteraction();
        try {
            if (saveAs || document.getPath() == null) {
                String suggested = document.getPath() == null ? "Untitled.json" : document.getPath().getFileName().toString();
                while (true) {
                    String name = (String) JOptionPane.showInputDialog(this,
                            "Filename in saved_diagrams (.json is added automatically):", "Save diagram",
                            JOptionPane.PLAIN_MESSAGE, null, null, suggested);
                    if (name == null) { return false; }
                    suggested = name;
                    Path target;
                    try { target = document.getStore().resolve(name); }
                    catch (IllegalArgumentException failure) {
                        DiagramFileDialogs.showError(this, "Save", failure);
                        continue;
                    }
                    if (Files.exists(target) && JOptionPane.showConfirmDialog(this,
                            "Replace " + target.getFileName() + "?", "Confirm overwrite",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                        continue;
                    }
                    document.saveAs(name);
                    break;
                }
            } else { document.save(); }
            updateTitle();
            return true;
        } catch (IOException | IllegalArgumentException failure) {
            DiagramFileDialogs.showError(this, "Save", failure);
            return false;
        }
    }

    private boolean confirmDiscard() {
        if (!document.isDirty()) { return true; }
        int choice = JOptionPane.showOptionDialog(this, "Save your changes before continuing?",
                "Unsaved changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, new String[]{"Save", "Discard", "Cancel"}, "Save");
        return choice == 1 || (choice == 0 && save(false));
    }
    @Override public void onExport(){ JOptionPane.showMessageDialog(this, "Export (TODO)"); }
    @Override public void onExit() {
        diagramPanel.cancelInteraction();
        if (confirmDiscard()) { dispose(); }
    }

    // View actions
    @Override public void onSetDebugNodes(boolean visible){ diagramPanel.setShowDebugNodes(visible); }
    @Override public void onSettings(){ JOptionPane.showMessageDialog(this, "Settings (TODO)"); }

    // Help actions
    @Override public void onAbout(){ JOptionPane.showMessageDialog(this, "Track Diagram Generator\nVersion 0.0.1\nInternal"); }
}
