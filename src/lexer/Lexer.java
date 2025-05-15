package lexer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private Stack<LexerState> stateStack = new Stack<>();
    private String currentDirectory = "src/bin/";
    private boolean inMultiLineComment = false;
    private static final Pattern TOKEN_PATTERNS = Pattern.compile(
            "\\s*(?:" +
                    "/(?:-|##)|" +  // Comment starters
                    "\"(?:\\\\.|[^\"\\\\])*\"|" + // Strings
                    "'(?:\\\\.|[^'\\\\])'|" + // Characters
                    "\\d+|" + // Numbers
                    "[a-zA-Z_][a-zA-Z0-9_]*|" + // Identifiers and keywords
                    "&&|\\|\\||~|==|!=|<=|>=|[=<>+\\-*/.,;{}()\\[\\]]" + // Operators
                    ")"
    );

    private static final String[] KEYWORDS = {
            "Division", "InferedFrom", "WhetherDoElse", "Ire", "Sire", "Clo",
            "SetOfClo", "FBU", "SFBU", "None", "Logical", "terminatethis",
            "Rotatewhen", "Continuewhen", "Replywith", "Seop", "Check",
            "Program", "End", "Using"
    };

    public List<Token> tokenize(String input, String sourceFileName) {
        List<Token> tokens = new ArrayList<>();
        String[] lines = input.split("\n");
        currentDirectory = sourceFileName != null ?
                new File(sourceFileName).getParent() : currentDirectory;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Skip empty lines unless we're in a multi-line comment
            if (line.isEmpty() && !inMultiLineComment) continue;

            // Handle multi-line comments
            if (inMultiLineComment) {
                if (line.contains("##/")) {
                    // End of multi-line comment
                    int endIndex = line.indexOf("##/");
                    tokens.add(new Token(TokenType.COMMENT, line.substring(0, endIndex + 3), i + 1, sourceFileName));
                    inMultiLineComment = false;

                    // Process remaining content after comment
                    String remaining = line.substring(endIndex + 3).trim();
                    if (!remaining.isEmpty()) {
                        processLine(remaining, i + 1, sourceFileName, tokens);
                    }
                } else {
                    // Entire line is part of multi-line comment
                    tokens.add(new Token(TokenType.COMMENT, line, i + 1, sourceFileName));
                }
                continue;
            }

            // Check for start of multi-line comment
            if (line.contains("/##")) {
                int startIndex = line.indexOf("/##");

                // Process any content before the comment
                String beforeComment = line.substring(0, startIndex).trim();
                if (!beforeComment.isEmpty()) {
                    processLine(beforeComment, i + 1, sourceFileName, tokens);
                }

                // Handle the comment
                if (line.contains("##/")) {
                    // Single-line comment block
                    int endIndex = line.indexOf("##/", startIndex);
                    tokens.add(new Token(TokenType.COMMENT,
                            line.substring(startIndex, endIndex + 3),
                            i + 1, sourceFileName));

                    // Process remaining content after comment
                    String remaining = line.substring(endIndex + 3).trim();
                    if (!remaining.isEmpty()) {
                        processLine(remaining, i + 1, sourceFileName, tokens);
                    }
                } else {
                    // Start of multi-line comment
                    tokens.add(new Token(TokenType.COMMENT,
                            line.substring(startIndex),
                            i + 1, sourceFileName));
                    inMultiLineComment = true;
                }
                continue;
            }

            // Check for single-line comments
            if (line.startsWith("/-")) {
                tokens.add(new Token(TokenType.COMMENT, line, i + 1, sourceFileName));
                continue;
            }

            // Check for Using command (must be at start of line)
            if (line.startsWith("Using")) {
                processUsingCommand(line, i + 1, currentDirectory, tokens);
                continue;
            }

            // Normal line processing
            processLine(line, i + 1, sourceFileName, tokens);
        }
        return tokens;
    }

    private void processLine(String line, int lineNumber, String sourceFileName, List<Token> tokens) {
        Matcher matcher = TOKEN_PATTERNS.matcher(line);
        while (matcher.find()) {
            String tokenValue = matcher.group().trim();
            if (tokenValue.isEmpty()) continue;

            // Skip any comment tokens (shouldn't happen here if patterns are correct)
            if (tokenValue.startsWith("/-") || tokenValue.startsWith("/##")) {
                tokens.add(new Token(TokenType.COMMENT, tokenValue, lineNumber, sourceFileName));
                break;
            }

            TokenType type = determineTokenType(tokenValue);
            tokens.add(new Token(type, tokenValue, lineNumber, sourceFileName));
        }
    }

    private TokenType determineTokenType(String token) {
        // Check for keywords
        for (String keyword : KEYWORDS) {
            if (token.equals(keyword)) {
                return getKeywordTokenType(keyword);
            }
        }

        // Check for operators
        if (token.matches("[+\\-*/]")) return TokenType.ARITH_OP;
        if (token.matches("&&|\\|\\||~")) return TokenType.LOGIC_OP;
        if (token.matches("==|!=|<=|>=|<>|>|<")) return TokenType.REL_OP;
        if (token.matches("=")) return TokenType.ASSIGN_OP;
        if (token.equals(".")) return TokenType.ACCESS_OP;

        // Check for other tokens
        if (token.matches("[{}()\\[\\]]")) return TokenType.BRACES;
        if (token.equals(";")) return TokenType.SEMICOLON;
        if (token.matches("\".*\"")) return TokenType.STRING;
        if (token.matches("'.*'")) return TokenType.CHARACTER;
        if (token.matches("\\d+")) return TokenType.CONSTANT;
        if (token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) return TokenType.IDENTIFIER;

        return TokenType.UNKNOWN;
    }

    private void processUsingCommand(String line, int lineNumber, String currentDirectory, List<Token> tokens) {
        Pattern usingPattern = Pattern.compile("Using\\s*\\(\\s*\"([^\"]+)\"\\s*\\)\\s*;");
        Matcher matcher = usingPattern.matcher(line);

        if (matcher.find()) {
            String includedPath = matcher.group(1);
            File includedFile;

            // Handle both absolute and relative paths
            if (new File(includedPath).isAbsolute()) {
                includedFile = new File(includedPath);
            } else {
                includedFile = new File(currentDirectory, includedPath);
            }

            if (includedFile.exists()) {
                try {
                    String includedContent = Files.readString(includedFile.toPath());
                    List<Token> includedTokens = tokenize(includedContent, includedFile.getAbsolutePath());
                    tokens.addAll(includedTokens);
                } catch (IOException e) {
                    tokens.add(new Token(TokenType.ERROR, "File not found: " + includedPath, lineNumber, currentDirectory));
                }
            } else {
                tokens.add(new Token(TokenType.ERROR, "File not found: " + includedPath, lineNumber, currentDirectory));
            }
        }
    }

    private TokenType getKeywordTokenType(String keyword) {
        switch (keyword) {
            case "Division": return TokenType.CLASS;
            case "InferedFrom": return TokenType.INHERITANCE;
            case "WhetherDoElse": return TokenType.CONDITION;
            case "Ire": return TokenType.INTEGER;
            case "Sire": return TokenType.SINTEGER;
            case "Clo": return TokenType.CHARACTER;
            case "SetOfClo": return TokenType.STRING;
            case "FBU": return TokenType.FLOAT;
            case "SFBU": return TokenType.SFLOAT;
            case "None": return TokenType.VOID;
            case "Logical": return TokenType.BOOLEAN;
            case "terminatethis": return TokenType.BREAK;
            case "Rotatewhen":
            case "Continuewhen": return TokenType.LOOP;
            case "Replywith": return TokenType.RETURN;
            case "Seop": return TokenType.STRUCT;
            case "Check": return TokenType.SWITCH;
            case "Program": return TokenType.START_STATEMENT;
            case "End": return TokenType.END_STATEMENT;
            case "Using": return TokenType.INCLUSION;
            default: return TokenType.UNKNOWN;
        }
    }

    private static class LexerState {
        String directory;
        int returnLineNumber;

        LexerState(String directory, int returnLineNumber) {
            this.directory = directory;
            this.returnLineNumber = returnLineNumber;
        }
    }
}
/*
public List<Token> tokenize(String input, String sourceFileName) {
        isComment = false;
        List<Token> tokens = new ArrayList<>();
        String[] lines = input.split("\n");
        String currentDirectory = sourceFileName != null ?
                new File(sourceFileName).getParent() : "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Check for using command at start of line
            if (line.startsWith("Using")) {
                processUsingCommand(line, i + 1, currentDirectory, tokens);
                continue;
            }

            // Normal token processing
            Matcher matcher = TOKEN_PATTERNS.matcher(line);
            Matcher matcher1 = COMMENT_PATTERN.matcher((line));
            while (matcher.find()) {
                String tokenValue = matcher.group().trim();
                if (tokenValue.isEmpty()) continue;

                // Skip comments
                if (tokenValue.startsWith("/-") || tokenValue.startsWith("/##")) {
                    isComment = true;
                    tokens.add(new Token(TokenType.COMMENT, tokenValue, i + 1, sourceFileName));
                    break;
                } else{
                    isComment = false;
                }


                if(!isComment){
                    TokenType type = determineTokenType(tokenValue);
                    tokens.add(new Token(type, tokenValue, i + 1, sourceFileName));
                }
            }
        }

        return tokens;
    }
* */