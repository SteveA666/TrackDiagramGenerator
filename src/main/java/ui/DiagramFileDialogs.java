package ui;

import i18n.Messages;

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
            JOptionPane.showMessageDialog(parent, Messages.text("file.none", store.getDirectory().toString()),
                    Messages.text("diagramFileDialogs.open.diagram"), JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return (String) JOptionPane.showInputDialog(parent, Messages.text("diagramFileDialogs.choose.a.diagram.from.saved.diagrams"),
                Messages.text("diagramFileDialogs.open.diagram"), JOptionPane.PLAIN_MESSAGE, null, files.toArray(), files.get(0));
    }

    static void showError(Component parent, String action, Exception failure) {
        JOptionPane.showMessageDialog(parent, failure.getMessage(), Messages.text("file.failed", action), JOptionPane.ERROR_MESSAGE);
    }
}
