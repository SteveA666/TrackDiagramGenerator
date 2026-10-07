package model;

import java.util.Objects;

/**
 * Node<br>
 * Represents a track endpoint with an identity, diagram position,<br>
 * and node type.<br>
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
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
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

    /**
     * Sets both diagram coordinates as one editing operation.<br>
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public NodeType getNodeType() {
        return type;
    }

    public void setNodeType(NodeType type) {
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
    }

    @Override
    public String toString() {
        return "Node [id=" + id + ", x=" + x + ", y=" + y + "]";
    }
}
