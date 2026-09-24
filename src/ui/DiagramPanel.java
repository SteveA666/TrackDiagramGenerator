package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

import model.*;

/**
 * 
 * DiagramPanel<br>
 * The panel to display and draw the track diagram. <br>
 * It is a subclass of JPanel 
 * and overrides the paintComponent method to draw the track segments on the panel.
 */
public class DiagramPanel extends JPanel {

    // TODO: Move to a settings class
    private boolean showDebugNodes = true; // Flag to control the display of debug nodes
    private final Network network; 

    public DiagramPanel(Network network) {
        super();
        this.network = network;
        this.setBackground(Color.WHITE);
    }

    @Override 
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        try{
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if(network == null){g2.drawString("Network is null", 10, 20); return;}
            drawSegments(g2);
            drawNodes(g2);
        } catch(Exception e){
            g2.setColor(Color.RED);
            g2.drawString("Error drawing network: " + e.getMessage(), 10, 20);
        } finally{
            g2.dispose();
        }
    }

    private void drawSegments(Graphics2D g2) {
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.BLACK);
        for (TrackSegment segment : network.getAllTrackSegments()) {
            Node start = segment.getStart();
            Node end = segment.getEnd();
            g2.draw(new Line2D.Double(start.getX(), start.getY(), end.getX(), end.getY()));
        }
    }

    private void drawNodes(Graphics2D g2) {
        
        for (Node node : network.getAllNodes()) {
            if (showDebugNodes) {
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(node.getX() - 3, node.getY() - 3, 6, 6));
                g2.setColor(Color.BLACK);
                g2.draw(new Ellipse2D.Double(node.getX() - 3, node.getY() - 3, 6, 6));
            }
            switch(node.getNodeType()){
                case STUB_END:
                    drawStubEnd(g2, node);
                    break;
                default: break;
            }

        }
    }

    private void drawStubEnd(Graphics2D g2, Node node) {
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(Color.BLACK);
        g2.draw(new Line2D.Double(node.getX() - 5, node.getY() - 5,
                              node.getX() + 5, node.getY() + 5));
    }
}
