import javax.swing.SwingUtilities;

import ui.ProgramWindow;
import ui.StartupDialogue;

public class Launcher {
    public static void start() {
        SwingUtilities.invokeLater(() -> {
            StartupDialogue dialog = new StartupDialogue(null);
            dialog.setVisible(true);
            switch (dialog.getResult()) {
                case NEW:
                    new ProgramWindow(SampleNetwork.createSampleNetwork()).setVisible(true);;
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
