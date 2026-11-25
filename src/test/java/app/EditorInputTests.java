package app;

import editor.DiagramEditor;
import io.DiagramIO;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import model.*;
import org.junit.jupiter.api.Test;
import ui.DiagramPanel;
import static org.junit.jupiter.api.Assertions.*;

class EditorInputTests {
    private static void key(DiagramPanel panel, String stroke) {
        Object action = panel.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(stroke));
        assertNotNull(action);
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, ""));
        assertNull(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(stroke)));
    }

    private static void mouse(DiagramPanel panel, int id, int x, int y) {
        panel.dispatchEvent(new MouseEvent(panel, id, 0,
                id == MouseEvent.MOUSE_RELEASED ? 0 : MouseEvent.BUTTON1_DOWN_MASK,
                x, y, 1, false, id == MouseEvent.MOUSE_DRAGGED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
    }

    private static void click(DiagramPanel panel, int x, int y) {
        mouse(panel, MouseEvent.MOUSE_PRESSED, x, y);
        mouse(panel, MouseEvent.MOUSE_RELEASED, x, y);
    }

    @Test void deletionRestoresDependenciesWithOneUndo() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (boolean node : new boolean[]{true, false}) {
                Network network = new Network();
                DiagramEditor editor = new DiagramEditor(network);
                Node a = editor.createNode(100, 100, NodeType.REGULAR);
                Node b = editor.createNode(300, 100, NodeType.REGULAR);
                TrackSegment track = editor.createTrack(a.getId(), b.getId(), TrackType.MAINLINE);
                Station station = editor.createStation("Station", 400, 400);
                editor.createPlatform(station.getId(), 1, new PlatformEdge(track, TrackSide.LEFT, .2, .8, 8));
                DiagramPanel panel = new DiagramPanel(editor);
                panel.setSize(800, 600);
                String before = DiagramIO.toJson(network);
                click(panel, node ? 100 : 200, 100);
                if (!node) { assertSame(track, panel.getSelectedTrack()); }
                key(panel, node ? "DELETE" : "BACK_SPACE");
                assertEquals(node ? 1 : 2, network.nodeCount());
                assertEquals(0, network.segmentCount());
                assertTrue(network.getStation(station.getId()).getPlatforms().isEmpty());
                String after = DiagramIO.toJson(network);
                panel.undo();
                assertEquals(before, DiagramIO.toJson(network));
                panel.redo();
                assertEquals(after, DiagramIO.toJson(network));
                key(panel, "DELETE");
                assertEquals(after, DiagramIO.toJson(network));
            }
        });
    }

    @Test void deletesLabelsAndPlatforms() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Network network = new Network();
            DiagramEditor editor = new DiagramEditor(network);
            TrackSegment track = editor.createTrackBetween(null, 100, 100, null, 300, 100,
                    NodeType.REGULAR, TrackType.MAINLINE);
            Station station = editor.createStation("Station", 400, 400);
            editor.createCustomText("Text", 500, 200, 20, Font.SANS_SERIF, "#000000");
            editor.createPlatform(station.getId(), 1, new PlatformEdge(track, TrackSide.LEFT, .2, .8, 8));
            DiagramPanel panel = new DiagramPanel(editor);
            panel.setSize(800, 600);
            click(panel, 180, 85);
            assertNotNull(panel.getSelectedPlatform());
            key(panel, "DELETE");
            assertEquals(1, network.segmentCount());
            assertTrue(station.getPlatforms().isEmpty());
            click(panel, 505, 205);
            assertNotNull(panel.getSelectedCustomText());
            key(panel, "BACK_SPACE");
            assertEquals(0, network.customTextCount());
            click(panel, 400, 400);
            assertNotNull(panel.getSelectedStation());
            key(panel, "DELETE");
            assertEquals(0, network.stationCount());
            assertEquals(1, network.segmentCount());
        });
    }

    @Test void spacePansFromEveryToolAndResetsOnFocusLoss() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (DiagramPanel.Tool tool : DiagramPanel.Tool.values()) {
                Network network = new Network();
                DiagramEditor editor = new DiagramEditor(network);
                editor.createNode(100, 100, NodeType.REGULAR);
                DiagramPanel panel = new DiagramPanel(editor);
                panel.setSize(800, 600);
                panel.setTool(tool);
                String before = DiagramIO.toJson(network);
                key(panel, "pressed SPACE");
                assertEquals(Cursor.HAND_CURSOR, panel.getCursor().getType());
                mouse(panel, MouseEvent.MOUSE_PRESSED, 100, 100);
                mouse(panel, MouseEvent.MOUSE_DRAGGED, 140, 130);
                key(panel, "released SPACE");
                mouse(panel, MouseEvent.MOUSE_RELEASED, 150, 140);
                assertEquals(new java.awt.geom.Point2D.Double(50, 40), panel.worldToScreen(new Point()));
                assertEquals(before, DiagramIO.toJson(network));
                assertEquals(tool, panel.getTool());
                assertEquals("Add node", editor.getHistory().undoName());
                key(panel, "pressed SPACE");
                for (FocusListener listener : panel.getFocusListeners()) {
                    listener.focusLost(new FocusEvent(panel, FocusEvent.FOCUS_LOST));
                }
                assertEquals(tool == DiagramPanel.Tool.PAN ? Cursor.HAND_CURSOR
                        : tool == DiagramPanel.Tool.SELECT ? Cursor.DEFAULT_CURSOR : Cursor.CROSSHAIR_CURSOR,
                        panel.getCursor().getType());
            }
        });
    }
}
