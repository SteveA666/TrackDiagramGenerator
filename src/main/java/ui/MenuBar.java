package ui;

import i18n.Messages;

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
    private final JMenuItem undo = new JMenuItem(Messages.text("menuBar.undo")), redo = new JMenuItem(Messages.text("menuBar.redo"));
    private final JCheckBoxMenuItem debugItem = new JCheckBoxMenuItem(Messages.text("menuBar.toggle.debug.nodes"));

    public MenuBar(MenuActions menuActions) {
        this.menuActions = menuActions;
        buildFileMenu();
        buildEditMenu();
        buildViewMenu();
        buildHelpMenu();
    }

    // Menu construction
    private void buildEditMenu() {
        JMenu edit = new JMenu(Messages.text("menuBar.edit"));
        edit.setMnemonic(Messages.text("menu.mnemonic.edit").codePointAt(0));
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
        edit.add(menuItem(Messages.text("menu.settings"), event -> menuActions.onSettings()));
        add(edit);
    }

    public void updateHistory(editor.EditHistory history) {
        undo.setEnabled(history.canUndo()); redo.setEnabled(history.canRedo());
        undo.setText(history.canUndo() ? Messages.text("history.undo", history.undoName()) : Messages.text("menuBar.undo"));
        redo.setText(history.canRedo() ? Messages.text("history.redo", history.redoName()) : Messages.text("menuBar.redo"));
    }

    public void setDebugNodesSelected(boolean selected) { debugItem.setSelected(selected); }

    private void buildFileMenu() {
        JMenu fileMenu = new JMenu(Messages.text("menuBar.file"));
        fileMenu.setMnemonic(Messages.text("menu.mnemonic.file").codePointAt(0));
        
        int shortcut = GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        fileMenu.add(fileItem(Messages.text("menuBar.new"), e -> menuActions.onNew(), KeyEvent.VK_N, shortcut));
        fileMenu.add(fileItem(Messages.text("menuBar.open"), e -> menuActions.onOpen(), KeyEvent.VK_O, shortcut));
        fileMenu.add(fileItem(Messages.text("menuBar.save"), e -> menuActions.onSave(), KeyEvent.VK_S, shortcut));
        fileMenu.add(fileItem(Messages.text("menuBar.save.as"), e -> menuActions.onSaveAs(), KeyEvent.VK_S, shortcut | InputEvent.SHIFT_DOWN_MASK));
        fileMenu.add(fileItem(Messages.text("menuBar.export.image"), e -> menuActions.onExport(), KeyEvent.VK_E, shortcut));
        fileMenu.addSeparator();
        fileMenu.add(menuItem(Messages.text("menuBar.exit"), e -> menuActions.onExit()));

        add(fileMenu);
    }

    private void buildViewMenu() {
        JMenu viewMenu = new JMenu(Messages.text("menuBar.view"));
        viewMenu.setMnemonic(Messages.text("menu.mnemonic.view").codePointAt(0));

        debugItem.addActionListener(e -> menuActions.onSetDebugNodes(debugItem.isSelected()));
        viewMenu.add(debugItem);
        viewMenu.addSeparator();
        viewMenu.add(menuItem(Messages.text("menuBar.settings"), e -> menuActions.onSettings()));

        add(viewMenu);
    }

    private void buildHelpMenu() {
        JMenu helpMenu = new JMenu(Messages.text("menuBar.help"));
        helpMenu.setMnemonic(Messages.text("menu.mnemonic.help").codePointAt(0));

        helpMenu.add(menuItem(Messages.text("menuBar.about"), e -> menuActions.onAbout()));

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
