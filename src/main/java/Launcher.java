import javax.swing.SwingUtilities;

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
