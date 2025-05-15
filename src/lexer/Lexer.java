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
    private String currentDirectory = "bin/";
    private static final Pattern TOKEN_PATTERNS = Pattern.compile(
            "\\s*(?:" +
                    "/(?:-|##.*?##//)|" + // Comments
                    "\"(?:\\\\.|[^\"\\\\])*\"|" + // Strings
                    "'(?:\\\\.|[^'\\\\])'|" + // Characters
                    "\\d+|" + // Numbers
                    "[a-zA-Z_][a-zA-Z0-9_]*|" + // Identifiers and keywords
                    "&&|\\|\\||~|==|!=|<=|>=|[=<>+\\-*/.,;{}()\\[\\]]" + // Operators and delimiters
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
            while (matcher.find()) {
                String tokenValue = matcher.group().trim();
                if (tokenValue.isEmpty()) continue;

                // Skip comments
                if (tokenValue.startsWith("/-") || tokenValue.startsWith("/##")) {
                    continue;
                }

                TokenType type = determineTokenType(tokenValue);
                tokens.add(new Token(type, tokenValue, i + 1, sourceFileName));
            }
        }

        return tokens;
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
            System.out.println(includedFile);

            if (includedFile.exists()) {
                try {
                    String includedContent = Files.readString(includedFile.toPath());
                    List<Token> includedTokens = tokenize(includedContent, includedFile.getAbsolutePath());
                    tokens.addAll(includedTokens);
                } catch (IOException e) {
                    // Optionally add error token
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