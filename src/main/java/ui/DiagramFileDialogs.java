package ui;

import io.DiagramStore;
import java.awt.Component;
import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;

/** Shared startup and File-menu file selection. Only saved diagram names are offered. */
final class DiagramFileDialogs {
    private DiagramFileDialogs() {}

    static String chooseOpen(Component parent, DiagramStore store) throws IOException {
        List<String> files = store.list();
        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No saved JSON diagrams in:\n" + store.getDirectory(),
                    "Open diagram", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return (String) JOptionPane.showInputDialog(parent, "Choose a diagram from saved_diagrams:",
                "Open diagram", JOptionPane.PLAIN_MESSAGE, null, files.toArray(), files.get(0));
    }

    static void showError(Component parent, String action, Exception failure) {
        JOptionPane.showMessageDialog(parent, failure.getMessage(), action + " failed", JOptionPane.ERROR_MESSAGE);
    }
}
