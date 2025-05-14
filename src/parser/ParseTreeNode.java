package parser;

import java.util.ArrayList;
import java.util.List;

public class ParseTreeNode {
    private String name;
    private String value;
    private ParseTreeNode parent;
    private List<ParseTreeNode> children;

    public ParseTreeNode(String name) {
        this(name, null);
    }

    public ParseTreeNode(String name, String value) {
        this.name = name;
        this.value = value;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public ParseTreeNode getParent() {
        return parent;
    }

    public List<ParseTreeNode> getChildren() {
        return children;
    }

    public void addChild(ParseTreeNode child) {
        child.parent = this;
        children.add(child);
    }
}