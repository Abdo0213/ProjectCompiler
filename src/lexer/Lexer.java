package lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
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

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        String[] lines = input.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            Matcher matcher = TOKEN_PATTERNS.matcher(line);
            while (matcher.find()) {
                String token = matcher.group().trim();
                if (token.isEmpty()) continue;

                // Skip comments
                if (token.startsWith("/-") || token.startsWith("/##")) {
                    continue;
                }

                TokenType type = determineTokenType(token);
                tokens.add(new Token(type, token, i + 1));
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
}