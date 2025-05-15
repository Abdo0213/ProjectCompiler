package parser;

import lexer.Token;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ParseTree {
    private ParseTreeNode root;
    private ParseTreeNode currentNode;
    private List<ParseTreeNode> nodes;
    private Stack<ParseTreeNode> nodeStack;  // For tracking parent nodes

    public ParseTree() {
        this.root = new ParseTreeNode("ROOT", -1);  // -1 for root line number
        this.currentNode = root;
        this.nodes = new ArrayList<>();
        this.nodeStack = new Stack<>();
    }

    public void startRule(String ruleName, int lineNumber) {
        startRule(ruleName, lineNumber, null);
    }

    public void startRule(String ruleName, int lineNumber, String fileName) {
        ParseTreeNode newNode = new ParseTreeNode(ruleName, null, lineNumber, fileName);
        currentNode.addChild(newNode);
        nodeStack.push(currentNode);  // Save current parent
        currentNode = newNode;        // Set new node as current
        nodes.add(newNode);
    }

    public void endRule() {
        if (!nodeStack.isEmpty()) {
            currentNode = nodeStack.pop();  // Restore parent node
        }
    }

    public void addNode(Token token) {
        ParseTreeNode newNode = new ParseTreeNode(
                token.getType().getDescription(),
                token.getValue(),
                token.getLineNumber(),
                token.getFileName()
        );
        currentNode.addChild(newNode);
        nodes.add(newNode);
    }

    public String toString() {
        return toString(false);
    }

    public String toStringWithFileInfo() {
        return toString(true);
    }

    private String toString(boolean showFileInfo) {
        StringBuilder sb = new StringBuilder();
        printTree(root, 0, sb, showFileInfo);
        return sb.toString();
    }

    private void printTree(ParseTreeNode node, int depth, StringBuilder sb, boolean showFileInfo) {
        // Indentation
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }

        // Node name and value
        sb.append(node.getName());
        if (node.getValue() != null) {
            sb.append(": ").append(node.getValue());
        }

        // Line number and file info
        if (node.getLineNumber() > 0) {
            sb.append(" (Line ").append(node.getLineNumber());
            if (showFileInfo && node.getFileName() != null) {
                sb.append(", File: ").append(node.getFileName());
            }
            sb.append(")");
        }

        sb.append("\n");

        // Children
        for (ParseTreeNode child : node.getChildren()) {
            printTree(child, depth + 1, sb, showFileInfo);
        }
    }

    public List<ParseTreeNode> getNodes() {
        return nodes;
    }

    public List<ParseTreeNode> getMatchedRules() {
        List<ParseTreeNode> rules = new ArrayList<>();
        for (ParseTreeNode node : nodes) {
            if (node.isRuleNode() && node.getLineNumber() > 0) {
                rules.add(node);
            }
        }
        return rules;
    }

    public ParseTreeNode getRoot() {
        return root;
    }
}