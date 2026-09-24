import ui.ProgramWindow;
import model.*;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        Network network = new Network();
        Node a = new Node(1, 100, 100, NodeType.REGULAR);
        Node b = new Node(2, 300, 100, NodeType.REGULAR);
        Node c = new Node(3, 300, 250, NodeType.REGULAR);

        network.addNode(a);
        network.addNode(b);
        network.addNode(c);

        network.addTrackSegment(new TrackSegment(1, a, b, TrackType.MAINLINE));
        network.addTrackSegment(new TrackSegment(2, b, c, TrackType.SIDING));

        SwingUtilities.invokeLater(()-> new ProgramWindow(network).setVisible(true));
    }
}
