import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import model.*;
import ui.DiagramPanel;
import ui.EditorToolbar;

/**
 * CanvasTests<br>
 * Exercises real Swing mouse handlers and toolbar actions on the event<br>
 * dispatch thread.<br>
 * Checks preview, commit, cancellation, validation feedback,<br>
 * and rendering without a window.<br>
 */
public final class CanvasTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(CanvasTests::run);
        System.out.println("PASS: " + checks + " canvas checks");
    }

    private static void run() {
        Network network = new Network();
        DiagramPanel panel = new DiagramPanel(network);
        panel.setSize(800, 500);
        EditorToolbar toolbar = new EditorToolbar(panel);
        for (String key : new String[]{"N", "shift N", "T", "shift T"}) {
            Object action = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(key));
            panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, key));
            DiagramPanel.Tool expected = key.endsWith("N") ? DiagramPanel.Tool.ADD_NODE : DiagramPanel.Tool.ADD_TRACK;
            check(panel.getTool() == expected, "shortcut switches tool: " + key);
            for (Component component : toolbar.getComponents()) {
                if (component instanceof JToggleButton && !(component instanceof JCheckBox)) {
                    JToggleButton button = (JToggleButton) component;
                    String expectedLabel = key.endsWith("N") ? "Add Node" : "Add Track";
                    check(button.isSelected() == button.getText().equals(expectedLabel), "shortcut keeps toolbar synchronized");
                }
            }
        }
        clickTool(toolbar, "Add Node");
        click(panel, 103, 97);
        Node a = network.getNode(1);
        check(a.getX() == 100 && a.getY() == 100, "node creation snaps to grid");
        check(panel.getSelectedNode() == a, "created node is selected");
        click(panel, 104, 102);
        check(network.nodeCount() == 1, "clicking existing node does not create duplicates");
        nodeChoice(toolbar).setSelectedIndex(1);
        click(panel, 300, 100);
        Node b = network.getNode(2);
        check(b.getNodeType() == NodeType.STUB_END, "toolbar controls node type");
        clickTool(toolbar, "Add Track");
        trackChoice(toolbar).setSelectedIndex(2);
        click(panel, 100, 100);
        click(panel, 100, 100);
        check(panel.isStatusError() && network.segmentCount() == 0, "self connection shows error without mutation");
        click(panel, 300, 100);
        TrackSegment track = network.getTrackSegment(1);
        check(track != null && track.getStart() == a && track.getEnd() == b
                && track.getTrackType() == TrackType.SIDING, "track creation uses selected endpoints and type");
        check(!panel.isStatusError(), "valid edit clears error");
        click(panel, 100, 100);
        cancel(panel);
        click(panel, 300, 100);
        check(network.segmentCount() == 1, "Escape cancels pending track start");
        mouse(panel, MouseEvent.MOUSE_PRESSED, 400, 300, MouseEvent.BUTTON3, 0);
        click(panel, 400, 300);
        check(panel.isStatusError() && network.nodeCount() == 2, "track tool does not create dangling nodes");
        click(panel, 100, 100);
        clickTool(toolbar, "Select / Move");
        clickTool(toolbar, "Add Track");
        click(panel, 300, 100);
        check(network.segmentCount() == 1, "switching tools discards pending track start");
        cancel(panel);
        clickTool(toolbar, "Select / Move");
        press(panel, 103, 100);
        drag(panel, 146, 139);
        check(a.getX() == 100 && a.getY() == 100, "drag is a preview until release");
        check(panel.getSelectedNode() == a, "selection works when debug nodes are hidden");
        render(panel);
        release(panel, 146, 139);
        check(a.getX() == 140 && a.getY() == 140, "release commits snapped position and preserves grab offset");
        check(track.getStart() == a && network.segmentsAt(1).contains(track), "drag preserves connectivity");
        press(panel, 140, 140); drag(panel, 200, 200); cancel(panel); release(panel, 200, 200);
        check(a.getX() == 140 && a.getY() == 140, "Escape discards a drag preview");
        press(panel, 140, 140); drag(panel, 300, 100); release(panel, 300, 100);
        check(panel.isStatusError() && a.getX() == 140 && a.getY() == 140, "invalid move leaves original coordinates");
        press(panel, 140, 140); release(panel, 140, 140);
        check(a.getX() == 140 && a.getY() == 140, "selection alone does not move a node");
        snapChoice(toolbar).doClick();
        check(!panel.isSnapToGrid(), "toolbar disables snapping");
        press(panel, 140, 140); drag(panel, 153, 157); release(panel, 153, 157);
        check(a.getX() == 153 && a.getY() == 157, "free drag keeps exact coordinates");
        clickTool(toolbar, "Add Node"); click(panel, 477, 233);
        check(network.getNode(3).getX() == 477 && network.getNode(3).getY() == 233, "free node placement");
        clickTool(toolbar, "Select / Move"); click(panel, 700, 400);
        check(panel.getSelectedNode() == null, "clicking empty canvas clears selection");
        snapChoice(toolbar).doClick();
        click(panel, 153, 157);
        check(a.getX() == 153 && a.getY() == 157, "selecting an off-grid node does not snap it");
        press(panel, 153, 157); drag(panel, 210, 210); snapChoice(toolbar).doClick(); release(panel, 210, 210);
        check(a.getX() == 153 && a.getY() == 157, "changing snapping cancels the active preview");
        mouse(panel, MouseEvent.MOUSE_PRESSED, 477, 233, MouseEvent.BUTTON3, 0);
        check(network.nodeCount() == 3, "right click does not edit the model");
        press(panel, 153, 157); drag(panel, 210, 210); clickTool(toolbar, "Add Node"); release(panel, 210, 210);
        check(a.getX() == 153 && a.getY() == 157, "tool switch cancels drag preview");
        // A node can disappear through another editing surface between press and release.
        clickTool(toolbar, "Select / Move"); press(panel, 477, 233); drag(panel, 490, 240);
        network.removeNode(3); release(panel, 490, 240);
        check(panel.isStatusError() && network.getNode(3) == null, "stale drag target is rejected gracefully");
        panel.setShowDebugNodes(true); render(panel);
        panel.setShowDebugNodes(false); render(panel);
        check(!panel.isShowDebugNodes(), "debug visibility remains independent of editing");
        final boolean[] visible = {false};
        ui.MenuBar.MenuActions actions = (ui.MenuBar.MenuActions) java.lang.reflect.Proxy.newProxyInstance(
                ui.MenuBar.MenuActions.class.getClassLoader(), new Class<?>[]{ui.MenuBar.MenuActions.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("onSetDebugNodes")) { visible[0] = (Boolean) arguments[0]; }
                    return null;
                });
        ui.MenuBar menu = new ui.MenuBar(actions);
        javax.swing.JCheckBoxMenuItem debug = (javax.swing.JCheckBoxMenuItem) menu.getMenu(1).getItem(0);
        check(!debug.isSelected(), "debug menu starts off");
        debug.doClick();
        check(visible[0], "debug menu sends checked value");
        debug.doClick();
        check(!visible[0], "debug menu sends unchecked value");
    }

    private static void render(DiagramPanel panel) {
        BufferedImage image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.setColor(java.awt.Color.MAGENTA);
        graphics.fillRect(799, 499, 1, 1);
        check(image.getRGB(799, 499) == java.awt.Color.MAGENTA.getRGB(), "painting preserves caller graphics");
        graphics.dispose();
    }

    private static void clickTool(EditorToolbar toolbar, String text) {
        for (Component component : toolbar.getComponents()) {
            if (component instanceof JToggleButton && ((JToggleButton) component).getText().equals(text)) {
                ((JToggleButton) component).doClick(); return;
            }
        }
        throw new AssertionError("Missing toolbar button: " + text);
    }

    private static JComboBox<?> nodeChoice(EditorToolbar toolbar) { return choice(toolbar, 0); }
    private static JComboBox<?> trackChoice(EditorToolbar toolbar) { return choice(toolbar, 1); }
    private static JComboBox<?> choice(EditorToolbar toolbar, int index) {
        for (Component component : toolbar.getComponents()) {
            if (component instanceof JComboBox && index-- == 0) { return (JComboBox<?>) component; }
        }
        throw new AssertionError("Missing type choice");
    }

    private static JCheckBox snapChoice(EditorToolbar toolbar) {
        for (Component component : toolbar.getComponents()) {
            if (component instanceof JCheckBox) { return (JCheckBox) component; }
        }
        throw new AssertionError("Missing snapping control");
    }

    private static void cancel(DiagramPanel panel) {
        panel.getActionMap().get("cancel").actionPerformed(new ActionEvent(panel, 0, "cancel"));
    }

    private static void click(DiagramPanel panel, int x, int y) { press(panel, x, y); release(panel, x, y); }
    private static void press(DiagramPanel panel, int x, int y) {
        mouse(panel, MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1, MouseEvent.BUTTON1_DOWN_MASK);
    }
    private static void release(DiagramPanel panel, int x, int y) {
        mouse(panel, MouseEvent.MOUSE_RELEASED, x, y, MouseEvent.BUTTON1, 0);
    }
    private static void drag(DiagramPanel panel, int x, int y) {
        mouse(panel, MouseEvent.MOUSE_DRAGGED, x, y, MouseEvent.NOBUTTON, MouseEvent.BUTTON1_DOWN_MASK);
    }
    private static void mouse(DiagramPanel panel, int id, int x, int y, int button, int modifiers) {
        panel.dispatchEvent(new MouseEvent(panel, id, System.currentTimeMillis(), modifiers, x, y, 1, false, button));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) { throw new AssertionError(message); }
    }
}
