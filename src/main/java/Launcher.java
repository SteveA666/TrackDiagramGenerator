import javax.swing.SwingUtilities;
import i18n.Messages;
import settings.SettingsStore;
import java.io.IOException;

import ui.ProgramWindow;
import ui.StartupDialogue;

/**
 * Launcher<br>
 * Shows the startup dialogue and opens the main window for a new<br>
 * diagram.<br>
 */
public class Launcher {
    public static void start() {
        SwingUtilities.invokeLater(() -> {
            var store = SettingsStore.defaultStore();
            String language = "";
            try { language = store.load().languageTag; }
            catch (IOException failure) {
                try { language = store.loadDefaults().languageTag; }
                catch (IOException ignored) { /* The main window reports settings failures. */ }
            }
            Messages.initialize(language);
            StartupDialogue dialog = new StartupDialogue(null);
            dialog.setVisible(true);
            switch (dialog.getResult()) {
                case NEW:
                case OPEN:
                    new ProgramWindow(dialog.getDocument()).setVisible(true);
                    break;
                case EXIT:
                    System.exit(0);
                    break;
            }
        });
    }
}
