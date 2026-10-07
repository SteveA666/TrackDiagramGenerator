import javax.swing.SwingUtilities;

import model.Network;
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
                    new ProgramWindow(new Network()).setVisible(true);;
                    break;
                case OPEN:
                    // TODO: Handle opening an existing network
                    break;
                case EXIT:
                    System.exit(0);
                    break;
            }
        });
    }
}
