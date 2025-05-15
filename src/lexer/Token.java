package lexer;

public class Token {
    private final TokenType type;
    private final String value;
    private final int lineNumber;
    private String fileName;

    public Token(TokenType type, String value, int lineNumber, String fileName) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
        this.fileName = fileName;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return String.format("Line #: %d Token Text: %s Token Type: %s",
                lineNumber, value, type.getDescription());
    }
    public String getFileName() {
        return fileName;
    }
}