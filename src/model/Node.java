package model;

/**
 * A node/endpoint on the track network.
 */
public class Node {
    private final int id;
    private int x;
    private int y;
    private NodeType type;

    public Node(int id, int x, int y, NodeType type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public NodeType getNodeType() {
        return type;
    }

    public void setNodeType(NodeType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Node [id=" + id + ", x=" + x + ", y=" + y + "]";
    }
}