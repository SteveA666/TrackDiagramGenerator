package ui;

import javax.swing.*;
import java.awt.event.ActionListener;

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
    }

    private final MenuActions menuActions;

    public MenuBar(MenuActions menuActions) {
        this.menuActions = menuActions;
        buildFileMenu();
        buildViewMenu();
        buildHelpMenu();
    }

    // Menu construction
    private void buildFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        
        fileMenu.add(menuItem("New", e -> menuActions.onNew()));
        fileMenu.add(menuItem("Open...", e -> menuActions.onOpen()));
        fileMenu.add(menuItem("Save", e -> menuActions.onSave()));
        fileMenu.add(menuItem("Save As...", e -> menuActions.onSaveAs()));
        fileMenu.add(menuItem("Export", e -> menuActions.onExport()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", e -> menuActions.onExit()));

        add(fileMenu);
    }

    private void buildViewMenu() {
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');

        JCheckBoxMenuItem debugItem = new JCheckBoxMenuItem("Toggle Debug Nodes");
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
    private JMenuItem menuItem(String name, ActionListener actionListener) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(actionListener);
        return item;
    }
}
