package ui;

import i18n.Messages;

import javax.swing.*;
// import java.awt.*;
import java.io.IOException;
import editor.DiagramDocument;
import io.DiagramStore;
import settings.SettingsStore;

/**
 * StartupDialogue<br>
 * Lets the user choose to create a diagram, open a diagram,<br>
 * or exit the application.<br>
 */
public class StartupDialogue extends JDialog {

    /**
     * Choice<br>
     * Lists the actions available from the startup dialogue.<br>
     */
    public enum Choice {
        NEW, OPEN, EXIT
    }

    private Choice result=Choice.EXIT;
    private DiagramDocument document;

    public StartupDialogue(JFrame parent) {
        super(parent, Messages.text("app.name"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(new StartupPanel(SettingsStore.defaultStore(), () -> {
            document = new DiagramDocument(DiagramStore.defaultStore());
            result = Choice.NEW;
            dispose();
        }, () -> {
            try {
                var store = DiagramStore.defaultStore();
                String name = DiagramFileDialogs.chooseOpen(this, store);
                if (name == null) { return; }
                var opened = new DiagramDocument(store);
                opened.open(name);
                document = opened;
                result = Choice.OPEN;
                dispose();
            } catch (IOException | IllegalArgumentException failure) {
                DiagramFileDialogs.showError(this, Messages.text("file.open"), failure);
            }
        }, () -> {
            result = Choice.EXIT;
            dispose();
        }, () -> {
            setTitle(Messages.text("app.name"));
            pack();
            setLocationRelativeTo(parent);
        }, failure -> DiagramFileDialogs.showError(this, Messages.text("programWindow.save.settings"), failure)));
        pack();
        setLocationRelativeTo(parent);
    }

    // Dialogue result
    public Choice getResult(){ return result; }
    public DiagramDocument getDocument(){ return document; }
}
