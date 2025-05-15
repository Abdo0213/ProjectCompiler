package parser;

import java.util.ArrayList;
import java.util.List;

public class ParseTreeNode {
    private String name;
    private String value;
    private int lineNumber;
    private String fileName;
    private ParseTreeNode parent;
    private List<ParseTreeNode> children;

    public ParseTreeNode(String name, int lineNumber) {
        this(name, null, lineNumber, null);
    }

    public ParseTreeNode(String name, String value, int lineNumber) {
        this(name, value, lineNumber, null);
    }

    public ParseTreeNode(String name, String value, int lineNumber, String fileName) {
        this.name = name;
        this.value = value;
        this.lineNumber = lineNumber;
        this.fileName = fileName;
        this.children = new ArrayList<>();
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public ParseTreeNode getParent() {
        return parent;
    }

    public List<ParseTreeNode> getChildren() {
        return children;
    }

    // Setters
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    // Tree operations
    public void addChild(ParseTreeNode child) {
        child.parent = this;
        children.add(child);
    }

    public boolean isRuleNode() {
        return value == null;
    }

    public String toString() {
        return toStringWithFileInfo(false);
    }

    public String toStringWithFileInfo() {
        return toStringWithFileInfo(true);
    }

    private String toStringWithFileInfo(boolean showFileInfo) {
        StringBuilder sb = new StringBuilder();
        buildTreeString(this, 0, sb, showFileInfo);
        return sb.toString();
    }

    private void buildTreeString(ParseTreeNode node, int depth, StringBuilder sb, boolean showFileInfo) {
        // Indentation
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }

        // Node name and value
        sb.append(node.name);
        if (node.value != null) {
            sb.append(": ").append(node.value);
        }

        // Line number and file info
        if (node.lineNumber > 0) {
            sb.append(" (Line ").append(node.lineNumber);
            if (showFileInfo && node.fileName != null) {
                sb.append(", File: ").append(node.fileName);
            }
            sb.append(")");
        }

        sb.append("\n");

        // Children
        for (ParseTreeNode child : node.children) {
            buildTreeString(child, depth + 1, sb, showFileInfo);
        }
    }

    // Helper methods for tree analysis
    public List<ParseTreeNode> getMatchedRules() {
        List<ParseTreeNode> rules = new ArrayList<>();
        collectRuleNodes(this, rules);
        return rules;
    }

    private void collectRuleNodes(ParseTreeNode node, List<ParseTreeNode> rules) {
        if (node.isRuleNode() && node.lineNumber > 0) {
            rules.add(node);
        }
        for (ParseTreeNode child : node.children) {
            collectRuleNodes(child, rules);
        }
    }
}