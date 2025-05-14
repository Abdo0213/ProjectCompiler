package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import lexer.*;
import parser.*;
import error.CompilerError;
import java.util.List;  // Add this import for List interface

public class CompilerGUI extends JFrame {
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JTextArea parseTreeArea;
    private JButton compileButton;

    public CompilerGUI() {
        setTitle("Project #1 Compiler");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Code"));
        inputArea = new JTextArea();
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // Output panel
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Compiler Output"));
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        // Parse tree panel
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBorder(BorderFactory.createTitledBorder("Parse Tree"));
        parseTreeArea = new JTextArea();
        parseTreeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        parseTreeArea.setEditable(false);
        JScrollPane treeScroll = new JScrollPane(parseTreeArea);
        treePanel.add(treeScroll, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel();
        compileButton = new JButton("Compile");
        compileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                compileCode();
            }
        });
        buttonPanel.add(compileButton);

        // Split panes for layout
        JSplitPane topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        topBottomSplit.setTopComponent(inputPanel);
        topBottomSplit.setBottomComponent(outputPanel);
        topBottomSplit.setDividerLocation(300);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setLeftComponent(topBottomSplit);
        mainSplit.setRightComponent(treePanel);
        mainSplit.setDividerLocation(500);

        add(mainSplit, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void compileCode() {
        String code = inputArea.getText();
        outputArea.setText("");
        parseTreeArea.setText("");

        // Lexical analysis
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(code);

        // Display tokens
        outputArea.append("=== Scanner Output ===\n");
        for (Token token : tokens) {
            outputArea.append(token.toString() + "\n");
        }

        // Syntax analysis
        Parser parser = new Parser(tokens);
        ParseTree parseTree = parser.parse();
        List<CompilerError> errors = parser.getErrors();

        // Display parse tree
        parseTreeArea.append(parseTree.toString());

        // Display errors
        outputArea.append("\n=== Parser Output ===\n");
        if (errors.isEmpty()) {
            outputArea.append("No syntax errors found.\n");
        } else {
            outputArea.append("Syntax errors:\n");
            for (CompilerError error : errors) {
                outputArea.append(error.toString() + "\n");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CompilerGUI().setVisible(true);
            }
        });
    }
}