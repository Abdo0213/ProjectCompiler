package error;

public class CompilerError {
    private final int lineNumber;
    private final String message;

    public CompilerError(int lineNumber, String message) {
        this.lineNumber = lineNumber;
        this.message = message;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("Line #: %d Error: %s", lineNumber, message);
    }
}