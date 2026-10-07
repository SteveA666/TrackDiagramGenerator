package ui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.Toolkit;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * MenuBar<br>
 * Builds the application menus and forwards menu actions to their<br>
 * handler.<br>
 */
public class MenuBar extends JMenuBar {
    /**
     * MenuActions<br>
     * Defines the menu callbacks handled by the application window.<br>
     */
    public interface MenuActions{
        void onNew();
        void onOpen();
        void onSave();
        void onSaveAs();
        void onExport();
        void onExit();
        void onAbout();
        void onSettings();
        void onSetDebugNodes(boolean visible);
        default void onUndo() {}
        default void onRedo() {}
    }

    private final MenuActions menuActions;
    private final JMenuItem undo = new JMenuItem("Undo"), redo = new JMenuItem("Redo");
    private final JCheckBoxMenuItem debugItem = new JCheckBoxMenuItem("Toggle Debug Nodes");

    public MenuBar(MenuActions menuActions) {
        this.menuActions = menuActions;
        buildFileMenu();
        buildEditMenu();
        buildViewMenu();
        buildHelpMenu();
    }

    // Menu construction
    private void buildEditMenu() {
        JMenu edit = new JMenu("Edit");
        edit.setMnemonic('E');
        int shortcut = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut));
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcut));
        undo.addActionListener(event -> menuActions.onUndo());
        redo.addActionListener(event -> menuActions.onRedo());
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                shortcut | InputEvent.SHIFT_DOWN_MASK), "redo");
        getActionMap().put("redo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (redo.isEnabled()) { menuActions.onRedo(); }
            }
        });
        undo.setEnabled(false); redo.setEnabled(false);
        edit.add(undo); edit.add(redo);
        edit.addSeparator();
        edit.add(menuItem("Settings...", event -> menuActions.onSettings()));
        add(edit);
    }

    public void updateHistory(editor.EditHistory history) {
        undo.setEnabled(history.canUndo()); redo.setEnabled(history.canRedo());
        undo.setText(history.canUndo() ? "Undo " + history.undoName() : "Undo");
        redo.setText(history.canRedo() ? "Redo " + history.redoName() : "Redo");
    }

    public void setDebugNodesSelected(boolean selected) { debugItem.setSelected(selected); }

    private void buildFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        
        int shortcut = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        fileMenu.add(fileItem("New", e -> menuActions.onNew(), KeyEvent.VK_N, shortcut));
        fileMenu.add(fileItem("Open...", e -> menuActions.onOpen(), KeyEvent.VK_O, shortcut));
        fileMenu.add(fileItem("Save", e -> menuActions.onSave(), KeyEvent.VK_S, shortcut));
        fileMenu.add(fileItem("Save As...", e -> menuActions.onSaveAs(), KeyEvent.VK_S, shortcut | InputEvent.SHIFT_DOWN_MASK));
        fileMenu.add(menuItem("Export", e -> menuActions.onExport()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", e -> menuActions.onExit()));

        add(fileMenu);
    }

    private void buildViewMenu() {
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');

        debugItem.addActionListener(e -> menuActions.onSetDebugNodes(debugItem.isSelected()));
        viewMenu.add(debugItem);
        viewMenu.addSeparator();
        viewMenu.add(menuItem("Settings", e -> menuActions.onSettings()));

        add(viewMenu);
    }

    private void buildHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        helpMenu.add(menuItem("About", e -> menuActions.onAbout()));

        add(helpMenu);
    }

    // Menu item helpers
    private JMenuItem fileItem(String name, ActionListener actionListener, int key, int modifiers) {
        JMenuItem item = menuItem(name, actionListener);
        item.setAccelerator(KeyStroke.getKeyStroke(key, modifiers));
        return item;
    }

    private JMenuItem menuItem(String name, ActionListener actionListener) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(actionListener);
        return item;
    }
}
