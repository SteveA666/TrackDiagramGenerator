package ui;

import javax.swing.*;
//import java.awt.*;

import model.*;

public class ProgramWindow extends JFrame {
    public ProgramWindow(Network network) {
        setTitle("Track Diagram Generator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new DiagramPanel(network)); 
    }
    
}
