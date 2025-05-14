package parser;

import lexer.Token;

import java.util.ArrayList;
import java.util.List;

public class ParseTree {
    private ParseTreeNode root;
    private ParseTreeNode currentNode;
    private List<ParseTreeNode> nodes;

    public ParseTree() {
        this.root = new ParseTreeNode("ROOT");
        this.currentNode = root;
        this.nodes = new ArrayList<>();
    }

    public void startRule(String ruleName) {
        ParseTreeNode newNode = new ParseTreeNode(ruleName);
        currentNode.addChild(newNode);
        currentNode = newNode;
        nodes.add(newNode);
    }

    public void endRule() {
        if (currentNode != root) {
            currentNode = currentNode.getParent();
        }
    }

    public void addNode(Token token) {
        ParseTreeNode newNode = new ParseTreeNode(token.getType().getDescription(), token.getValue());
        currentNode.addChild(newNode);
        nodes.add(newNode);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        printTree(root, 0, sb);
        return sb.toString();
    }

    private void printTree(ParseTreeNode node, int depth, StringBuilder sb) {
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        sb.append(node.getName());
        if (node.getValue() != null) {
            sb.append(": ").append(node.getValue());
        }
        sb.append("\n");

        for (ParseTreeNode child : node.getChildren()) {
            printTree(child, depth + 1, sb);
        }
    }

    public List<ParseTreeNode> getNodes() {
        return nodes;
    }
}