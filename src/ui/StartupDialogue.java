package ui;

import javax.swing.*;
import java.awt.*;

public class StartupDialogue extends JDialog {

    public enum Choice {
        NEW, OPEN, EXIT
    }

    private Choice result=Choice.EXIT;

    public Choice getResult() {
        return result;
    }

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
            result = Choice.OPEN;
            dispose();
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
    
}
