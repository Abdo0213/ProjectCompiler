package error;

public class CompilerError {
    private final int lineNumber;
    private final String message;
    private String fileName;

    public CompilerError(int lineNumber, String message, String fileName) {
        this.lineNumber = lineNumber;
        this.message = message;
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMessage() {
        return message;
    }
    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        String fileInfo = fileName != null ? " [File: " + fileName + "]" : "";
        return String.format("Line #: %d%s: %s",
                lineNumber, fileInfo, message);
    }
}