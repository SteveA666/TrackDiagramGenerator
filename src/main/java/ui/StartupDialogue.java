package ui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import editor.DiagramDocument;
import io.DiagramStore;

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
    private final DiagramDocument document = new DiagramDocument(DiagramStore.defaultStore());

    public StartupDialogue(JFrame parent) {
        super(parent, "Track Diagram Generator", true);
        setSize(300, 150);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Welcome to Track Diagram Generator!", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20,40,20,40));

        JButton newButton = new JButton("New Network");
        JButton openButton = new JButton("Open Network...");
        JButton exitButton = new JButton("Exit");

        newButton.addActionListener(e -> {
            result = Choice.NEW;
            dispose();
        });

        openButton.addActionListener(e -> {
            try {
                String name = DiagramFileDialogs.chooseOpen(this, document.getStore());
                if (name == null) { return; }
                document.open(name);
                result = Choice.OPEN;
                dispose();
            } catch (IOException | IllegalArgumentException failure) {
                DiagramFileDialogs.showError(this, "Open", failure);
            }
        });

        exitButton.addActionListener(e -> {
            result = Choice.EXIT;
            dispose();
        });

        buttonPanel.add(newButton);
        buttonPanel.add(openButton);
        buttonPanel.add(exitButton);

        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    // Dialogue result
    public Choice getResult(){ return result; }
    public DiagramDocument getDocument(){ return document; }
}
