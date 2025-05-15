
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
        List<Token> tokens = lexer.tokenize(code,"");

        // Display tokens
        outputArea.append("=== Scanner Output ===\n");
        for (Token token : tokens) {
            outputArea.append(token.toString() + "\n");
        }

        // Syntax analysis
        Parser parser = new Parser(tokens);
        ParseTree parseTree = parser.parse();
        List<CompilerError> errors = parser.getErrors();
        List<CompilerError> successes = parser.getSuccess();

        // Display parse tree
        parseTreeArea.append(parseTree.toString());

        // Display errors
        outputArea.append("\n=== Parser Output ===\n");
        outputArea.append("Parser Match Success: \n");
        for (CompilerError success : successes) {
            outputArea.append(success.toString() + "\n");
        }
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
/*
*
package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lexer.*;
import parser.*;
import error.CompilerError;
import java.util.List;

public class CompilerGUI extends JFrame {
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JTextArea parseTreeArea;
    private JButton compileButton;
    private JButton openFileButton;
    private JButton saveButton;
    private File currentFile;

    public CompilerGUI() {
        setTitle("Project #1 Compiler");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize components
        initializeComponents();
    }

    private void initializeComponents() {
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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        openFileButton = new JButton("Open File");
        openFileButton.addActionListener(this::openFileAction);

        compileButton = new JButton("Compile");
        compileButton.addActionListener(this::compileAction);

        saveButton = new JButton("Save");
        saveButton.addActionListener(this::saveAction);

        buttonPanel.add(openFileButton);
        buttonPanel.add(compileButton);
        buttonPanel.add(saveButton);

        // Split panes for layout
        JSplitPane topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        topBottomSplit.setTopComponent(inputPanel);
        topBottomSplit.setBottomComponent(outputPanel);
        topBottomSplit.setDividerLocation(350);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setLeftComponent(topBottomSplit);
        mainSplit.setRightComponent(treePanel);
        mainSplit.setDividerLocation(600);

        add(mainSplit, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void openFileAction(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Source File");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try {
                String content = Files.readString(currentFile.toPath());
                inputArea.setText(content);
                outputArea.setText("");
                parseTreeArea.setText("");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveAction(ActionEvent e) {
        if (currentFile == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save File");
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
            } else {
                return;
            }
        }

        try {
            Files.writeString(currentFile.toPath(), inputArea.getText());
            JOptionPane.showMessageDialog(this, "File saved successfully!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void compileAction(ActionEvent e) {
        if (inputArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No code to compile!",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String code = inputArea.getText();
        File tempFile = null;
        outputArea.setText("");
        parseTreeArea.setText("");

        try {
            // Create temp file to maintain directory context
            tempFile = File.createTempFile("compiler_temp", ".pr1");
            Files.writeString(tempFile.toPath(), code);

            // Lexical analysis
            Lexer lexer = new Lexer();
            //String sourcePath = currentFile != null ? currentFile.getAbsolutePath() : "input.pr1";
            //List<Token> tokens = lexer.tokenize(inputArea.getText(), sourcePath);
            List<Token> tokens = lexer.tokenize(code, tempFile.getAbsolutePath());

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
            parseTreeArea.append(parseTree.toStringWithFileInfo());

            // Display errors
            outputArea.append("\n=== Parser Output ===\n");
            if (errors.isEmpty()) {
                outputArea.append("No syntax errors found.\n");

                // Display matched rules
                for (ParseTreeNode node : parseTree.getMatchedRules()) {
                    String fileInfo = node.getFileName() != null ?
                            " [File: " + node.getFileName() + "]" : "";
                    outputArea.append(String.format("Line #: %d%s Matched Rule Used: %s%n",
                            node.getLineNumber(),
                            fileInfo,
                            node.getName()));
                }
            } else {
                outputArea.append("Syntax errors:\n");
                for (CompilerError error : errors) {
                    outputArea.append(error.toString() + "\n");
                }
            }

            outputArea.append(String.format("\nTotal NO of errors: %d%n", errors.size()));

        } catch (Exception ex) {
            outputArea.append("\n=== Compilation Error ===\n");
            outputArea.append("System error during compilation: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CompilerGUI gui = new CompilerGUI();
            gui.setVisible(true);
        });
    }
}
* */