package parser;

import lexer.Token;
import lexer.TokenType;
import error.CompilerError;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

public class Parser {
    private List<Token> tokens;
    private ListIterator<Token> tokenIterator;
    private Token currentToken;
    private List<CompilerError> errors;
    private List<CompilerError> success;
    private ParseTree parseTree;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.tokenIterator = tokens.listIterator();  // Use ListIterator
        this.errors = new ArrayList<>();
        this.success = new ArrayList<>();
        this.parseTree = new ParseTree();

        if (tokenIterator.hasNext()) {
            currentToken = tokenIterator.next();
        }
    }
    public ParseTree parse() {
        parseProgram();
        return parseTree;
    }
    public List<CompilerError> getErrors() {
        return errors;
    }

    public List<CompilerError> getSuccess() {
        return success;
    }

    private void advance() {
        if (tokenIterator.hasNext()) {
            currentToken = tokenIterator.next();
        } else {
            currentToken = null;
        }
    }
    private void match(TokenType expectedType) {
        if (currentToken != null && currentToken.getType() == expectedType) {
            parseTree.addNode(currentToken);
            success.add(new CompilerError(
                    currentToken != null ? currentToken.getLineNumber() : -1,
                    "Matched Rule used: " + expectedType,
                    currentToken != null ? currentToken.getFileName() : null
            ));
            advance();
        } else {
            String found = currentToken != null ?
                    "'" + currentToken.getValue() + "' (" + currentToken.getType() + ")" :
                    "end of input";

            errors.add(new CompilerError(
                    currentToken != null ? currentToken.getLineNumber() : -1,
                    "Expected " + expectedType + " but found " + found,
                    currentToken != null ? currentToken.getFileName() : null

            ));
        }
    }
    private int getCurrentPosition() {
        // Returns the index of the token that would be returned by next()
        return tokenIterator.nextIndex() - 1;
    }
    private void resetToPosition(int position) {
        if (position < 0 || position >= tokens.size()) {
            currentToken = null;
            return;
        }

        int currentPos = tokenIterator.nextIndex() - 1;
        if (position == currentPos) return;

        if (position < currentPos) {
            // Move backward
            while (currentPos > position && tokenIterator.hasPrevious()) {
                tokenIterator.previous();
                currentPos--;
            }
        } else {
            // Move forward
            while (currentPos < position && tokenIterator.hasNext()) {
                tokenIterator.next();
                currentPos++;
            }
        }
        currentToken = tokens.get(position);
    }
    private void parseProgram() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Program",lineNumber,fileName);
        match(TokenType.START_STATEMENT);
        parseClassDeclarationList();
        match(TokenType.END_STATEMENT);
        parseTree.endRule();
    }
    private void parseClassDeclarationList() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ClassDeclarationList",lineNumber,fileName);
        while (currentToken != null && currentToken.getType() == TokenType.CLASS) {
            parseClassDeclaration();
        }
        parseTree.endRule();
    }
    private void parseClassDeclaration() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ClassDeclaration",lineNumber,fileName);
        match(TokenType.CLASS);
        match(TokenType.IDENTIFIER);

        if (currentToken != null && currentToken.getType() == TokenType.INHERITANCE) {
            match(TokenType.INHERITANCE);
            match(TokenType.IDENTIFIER);
        }

        match(TokenType.BRACES); // {
        parseClassImplementation();
        match(TokenType.BRACES); // }
        parseTree.endRule();
    }
    private void parseClassImplementation() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ClassImplementation",lineNumber,fileName);
        while (currentToken != null && currentToken.getType() != TokenType.BRACES) {
            parseClassItem();
        }
        parseTree.endRule();
    }
    private void parseClassItem() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ClassItem",lineNumber,fileName);
        if (currentToken != null) {
            /*if (isValidType(currentToken.getType())) {
                if (isLikelyMethodDeclaration()) {
                    parseMethodDeclaration();
                } else if(isAssignment()){
                    parseAssignment();
                } else {
                    parseVarDeclaration();
                }
            }*/
            switch (currentToken.getType()) {

                case INTEGER:
                case SINTEGER:
                case CHARACTER:
                case STRING:
                case FLOAT:
                case SFLOAT:
                case BOOLEAN:
                case VOID:
                    // Check if this is a method declaration (has parentheses after identifier)
                    if (isLikelyMethodDeclaration()) {
                        parseMethodDeclaration();
                    } else if(isAssignment()){
                        parseAssignment();
                    } else {
                        parseVarDeclaration();
                    }
                    break;

                // Other class items
                case INCLUSION:
                    parseUsingCommand();
                    break;
                case COMMENT:
                    parseComment();
                    break;
                case START_STATEMENT:
                    parseProgram();
                    break;
                case IDENTIFIER:
                    if (isFunctionCall()) {
                        parseFuncCall(); //  id id = 20;  id id () {}  id id; id id ();
                    } else if(isAssignment()){
                        parseAssignment();
                    } else if (isLikelyMethodDeclaration()) {
                        parseMethodDeclaration();
                    } else if (isVarDecl()){
                        parseVarDeclaration();
                    }
                    else {
                        errors.add(new CompilerError(
                                currentToken.getLineNumber(),
                                currentToken.getFileName(),
                                "Not Matched Error: '" + currentToken.getValue() + "' is not a valid Type"
                        ));
                        advance();
                    }
                    break;
                default:
                    errors.add(new CompilerError(
                            currentToken.getLineNumber(),
                            currentToken.getFileName(),
                            "Not Matched Error: '" + currentToken.getValue() + "' is an unexpected token in class implementation"
                    ));
                    advance();
            }
        }
        parseTree.endRule();
    }
    private boolean isVarDecl(){
        int currentPosition = getCurrentPosition();
        boolean isVar = false;
        try {
            parseType();  // Skip type
            if (currentToken == null || currentToken.getType() != TokenType.IDENTIFIER) {
                return false;
            }
            advance();  // Skip identifier

            // Check if next token is ';'
            isVar = currentToken != null && currentToken.getType() == TokenType.SEMICOLON && currentToken.getValue().equals(";");
            return isVar;
        } finally {
            // Restore position
            resetToPosition(currentPosition);
        }
    }
    private boolean isLikelyMethodDeclaration() {
        // Save current position
        int currentPosition = getCurrentPosition();
        boolean isMethod = false;
        try {
            parseType();  // Skip type
            if (currentToken == null || currentToken.getType() != TokenType.IDENTIFIER) {
                return false;
            }
            advance();  // Skip identifier
            advance();  // Skip (
            advance();  // Skip )

            // Check if next token is '('
            isMethod = currentToken != null && currentToken.getType() == TokenType.BRACES && currentToken.getValue().equals("{");
            return isMethod;
        } finally {
            // Restore position
            resetToPosition(currentPosition);
            /*if(!isValidType(currentToken.getType()) && isMethod){
                resetToPosition(currentPosition);
            }*/
        }
    }
    private void parseMethodDeclaration() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("MethodDeclaration",lineNumber,fileName);
        try {
            if (currentToken == null /*|| !isValidType(currentToken.getType())*/) {
                errors.add(new CompilerError(
                        currentToken != null ? currentToken.getLineNumber() : -1,
                        currentToken != null ? currentToken.getFileName() : null,
                        "Not Matched Error: '" + (currentToken != null ? currentToken.getValue() : "null") + "' is not a valid Type"
                ));
                return;
            }

            // Parse the function declaration part (Type ID ( ParameterList ))
            parseFuncDeclaration();

            // Check which form we have
            if (currentToken != null && currentToken.getType() == TokenType.SEMICOLON) {
                // Form 1: FuncDecl ;
                match(TokenType.SEMICOLON);
            } else {
                // Form 2: FuncDecl { VarDeclaration Statements }
                match(TokenType.BRACES); // {

                // Parse variable declarations
                while (currentToken != null && isValidType(currentToken.getType())) {
                    if(isAssignment()){
                        parseAssignment();
                    } else{
                        parseVarDeclaration();
                    }
                }

                // Parse statements
                parseStatements();

                match(TokenType.BRACES); // }
            }
        } finally {
            parseTree.endRule();
        }
    }
    private void parseFuncDeclaration() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("FuncDeclaration",lineNumber,fileName);

        // Rule 7: FuncDecl → Type ID ( ParameterList )
        parseType();
        match(TokenType.IDENTIFIER);
        match(TokenType.BRACES); // (
        parseParameterList();
        match(TokenType.BRACES); // )

        parseTree.endRule();
    }

    private void parseParameterList() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ParameterList",lineNumber,fileName);

        // Rule 9: ParameterList → ε | None | NonEmptyParameterList
        if (currentToken != null && currentToken.getType() == TokenType.VOID) {
            match(TokenType.VOID);  // Special case for "None" in parameters
        } else if (currentToken != null && currentToken.getType() != TokenType.BRACES) {
            parseNonEmptyParameterList();
        }
        // Else ε case (empty parameter list)

        parseTree.endRule();
    }
    private void parseNonEmptyParameterList() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("NonEmptyParameterList",lineNumber,fileName);

        // Rule 10: NonEmptyParameterList → Type ID | NonEmptyParameterList , Type ID
        parseType();
        match(TokenType.IDENTIFIER);

        while (currentToken != null && currentToken.getType() == TokenType.COMMA) {
            match(TokenType.COMMA);
            parseType();
            match(TokenType.IDENTIFIER);
        }

        parseTree.endRule();
    }
    private void parseVarDeclaration() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("VarDeclaration",lineNumber,fileName);
        parseType();
        parseIDList();
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }
    private void parseAssignmentVarDeclaration() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("VarDeclaration",lineNumber,fileName);
        if(isValidType(currentToken.getType())) // int x =5; x =5; w w = 5;
            parseType();
        parseIDList(); // int x =6; int x,z = 5; w , w = 5
        parseTree.endRule();
    }
    private void parseType() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Type",lineNumber,fileName);
        if (currentToken != null && (
                currentToken.getType() == TokenType.INTEGER ||
                        currentToken.getType() == TokenType.SINTEGER ||
                        currentToken.getType() == TokenType.CHARACTER ||
                        currentToken.getType() == TokenType.STRING ||
                        currentToken.getType() == TokenType.FLOAT ||
                        currentToken.getType() == TokenType.SFLOAT ||
                        currentToken.getType() == TokenType.BOOLEAN ||
                        currentToken.getType() == TokenType.VOID)) {
            match(currentToken.getType());

        } else {
            errors.add(new CompilerError(
                    currentToken.getLineNumber(),
                    "Not Matched Error: Expected valid type but found '" + currentToken.getValue() + "' (" + currentToken.getType() + ")",
                    currentToken.getFileName()
            ));
            advance();
        }
        parseTree.endRule();
    }

    private void parseIDList() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("IDList",lineNumber,fileName);
        match(TokenType.IDENTIFIER);// w w
        while (currentToken != null && currentToken.getType() == TokenType.COMMA) { // ,
            match(TokenType.COMMA);
            match(TokenType.IDENTIFIER);
        }
        parseTree.endRule();
    }
    private void parseUsingCommand() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("UsingCommand",lineNumber,fileName);
        match(TokenType.INCLUSION);
        match(TokenType.BRACES); // (
        match(TokenType.STRING); // Filename
        match(TokenType.BRACES); // )
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }

    private void parseFuncCall() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("FuncCall",lineNumber,fileName);
        match(TokenType.IDENTIFIER); // Function name
        match(TokenType.BRACES); // (
        parseArgumentList();
        match(TokenType.BRACES); // )
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }
    private boolean isAssignment() {
        int pos = getCurrentPosition();
        boolean isAssignment = false;
        try {
            parseType(); // x = 5;
            // Look ahead to see if this is an assignment
            match(TokenType.IDENTIFIER);
            isAssignment = currentToken != null && currentToken.getType() == TokenType.ASSIGN_OP;
            return isAssignment;
        } finally {
            resetToPosition(pos);
            if(!isValidType(currentToken.getType()) && isAssignment){
                resetToPosition(pos+1);
            }
        }
    }
    private void parseArgumentList() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ArgumentList",lineNumber,fileName);
        if (currentToken != null && currentToken.getType() != TokenType.BRACES) { // )
            parseNonEmptyArgumentList();
        }
        parseTree.endRule();
    }

    private void parseNonEmptyArgumentList() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("NonEmptyArgumentList",lineNumber,fileName);
        parseExpression();
        while (currentToken != null && currentToken.getType() == TokenType.COMMA) {
            match(TokenType.COMMA);
            parseExpression();
        }
        parseTree.endRule();
    }

    private void parseComment() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Comment",lineNumber,fileName);
        if (currentToken.getType() == TokenType.COMMENT) {
            // Single-line comment
            if (currentToken.getValue().startsWith("/-")) {
                match(TokenType.COMMENT);
            }
            // Multi-line comment
            else if (currentToken.getValue().startsWith("/##")) {
                match(TokenType.COMMENT);
                // Need to handle until ##// is found
                while (currentToken != null && !currentToken.getValue().endsWith("##//")) {
                    match(TokenType.COMMENT);
                }
            }
        }
        parseTree.endRule();
    }

    private void parseExpression() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Expression",lineNumber,fileName);
        parseTerm();
        // Only handle + and - at expression level
        while (currentToken != null &&
                (currentToken.getValue().equals("+") ||
                        currentToken.getValue().equals("-"))) {
            match(TokenType.ARITH_OP);  // Match the operator
            parseTerm();
        }
        parseTree.endRule();
    }

    private void parseTerm() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Term",lineNumber,fileName);
        parseFactor();
        // Only handle * and / at term level
        while (currentToken != null &&
                (currentToken.getValue().equals("*") ||
                        currentToken.getValue().equals("/"))) {
            match(TokenType.ARITH_OP);  // Match the operator
            parseFactor();
        }
        parseTree.endRule();
    }

    private void parseFactor() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Factor",lineNumber,fileName);
        if (currentToken != null) {
            switch (currentToken.getType()) {
                case IDENTIFIER:
                case CONSTANT:
                    match(currentToken.getType());
                    break;
                case BRACES:
                    if (currentToken.getValue().equals("(")) {
                        match(TokenType.BRACES);  // (
                        parseExpression();
                        match(TokenType.BRACES);  // )
                    }
                    break;
                default:
                    errors.add(new CompilerError(
                            currentToken.getLineNumber(),
                            currentToken.getFileName(),
                            "Not Matched Error: Unexpected token in factor expression: '" +
                                    currentToken.getValue() + "' (" + currentToken.getType() + ")"
                    ));
                    advance();
            }
        }
        parseTree.endRule();
    }

    private void parseStatements() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Statements",lineNumber,fileName);
        while (currentToken != null && !currentToken.getValue().equals("}")) {
            parseStatement();
        }
        parseTree.endRule();
    }

    private void parseStatement() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Statement",lineNumber,fileName);
        if (currentToken != null) {
            switch (currentToken.getType()) {
                case INTEGER:
                case SINTEGER:
                case CHARACTER:
                case STRING:
                case FLOAT:
                case SFLOAT:
                case BOOLEAN:
                case VOID:
                    if(isAssignment()){
                        parseAssignment();
                    } else{
                        parseVarDeclaration();
                    }
                    break;
                case CONDITION:
                    parseWhetherDoStatement();
                    break;
                case LOOP:
                    if (currentToken.getValue().equals("Rotatewhen")) {
                        parseRotateWhenStatement();
                    } else {
                        parseContinueWhenStatement();
                    }
                    break;
                case RETURN:
                    parseReplyWithStatement();
                    break;
                case BREAK:
                    parseTerminateThisStatement();
                    break;
                case READ:
                    parseReadStatement();
                    break;
                case WRITE:
                    parseWriteStatement();
                    break;
                case IDENTIFIER:
                    if (looksLikeFuncCall()) {
                        parseFuncCall();
                    } else {
                        parseAssignment();
                    }
                    break;
                default:
                    errors.add(new CompilerError(
                            currentToken.getLineNumber(),
                            currentToken.getFileName(),
                            "Not Matched Error: Unexpected statement '" + currentToken.getValue() + "' (" + currentToken.getType() + ")"
                    ));
                    advance();
            }
        }
        parseTree.endRule();
    }
    private void parseWhetherDoStatement() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("WhetherDoStatement",lineNumber,fileName);
        match(TokenType.CONDITION); // "WhetherDo"
        match(TokenType.BRACES);    // "("
        parseConditionExpression();
        match(TokenType.BRACES);    // ")"
        parseBlockStatements();

        // Handle optional Else clause
        if (currentToken != null && currentToken.getValue().equals("Else")) {
            match(TokenType.CONDITION); // "Else"
            parseBlockStatements();
        }
        parseTree.endRule();
    }

    private void parseConditionExpression() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ConditionExpression",lineNumber,fileName);
        parseCondition();
        while (currentToken != null &&
                (currentToken.getType() == TokenType.LOGIC_OP)) {
            match(currentToken.getType()); // AND/OR
            parseCondition();
        }
        parseTree.endRule();
    }

    private void parseCondition() {int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Condition",lineNumber,fileName);
        parseExpression();
        match(TokenType.REL_OP); // ==, !=, etc.
        parseExpression();
        parseTree.endRule();
    }

    private void parseRotateWhenStatement() {int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("RotateWhenStatement",lineNumber,fileName);
        match(TokenType.LOOP);      // "Rotatewhen"
        match(TokenType.BRACES);    // "("
        parseConditionExpression();
        match(TokenType.BRACES);    // ")"
        parseBlockStatements();
        parseTree.endRule();
    }

    private void parseContinueWhenStatement() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ContinueWhenStatement",lineNumber,fileName);
        match(TokenType.LOOP);      // "Continuewhen"
        match(TokenType.BRACES);    // "("
        parseExpression();          // Initialization
        match(TokenType.SEMICOLON);
        parseExpression(); // Condition parseConditionExpression();
        match(TokenType.SEMICOLON);
        parseExpression();          // Increment
        match(TokenType.BRACES);    // ")"
        parseBlockStatements();
        parseTree.endRule();
    }

    private void parseReplyWithStatement() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ReplyWithStatement",lineNumber,fileName);
        match(TokenType.RETURN);    // "Replywith"
        if (currentToken != null && currentToken.getType() == TokenType.IDENTIFIER) {
            match(TokenType.IDENTIFIER);
        } else {
            parseExpression();
        }
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }

    private void parseTerminateThisStatement() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("TerminateThisStatement",lineNumber,fileName);
        match(TokenType.BREAK);     // "terminatethis"
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }

    private void parseAssignment() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("Assignment",lineNumber,fileName);
        parseAssignmentVarDeclaration();
        match(TokenType.ASSIGN_OP); // "="
        parseExpression();
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }

    private void parseBlockStatements() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("BlockStatements",lineNumber,fileName);
        match(TokenType.BRACES);    // "{"
        parseStatements();
        match(TokenType.BRACES);    // "}"
        parseTree.endRule();
    }
    private boolean looksLikeFuncCall() {
        // Check if identifier is followed by '('
        int pos = getCurrentPosition();
        try {
            match(TokenType.IDENTIFIER);
            return currentToken != null &&
                    currentToken.getType() == TokenType.BRACES &&
                    currentToken.getValue().equals("(");
        } finally {
            resetToPosition(pos);
        }
    }
    private void parseReadStatement() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("ReadStatement",lineNumber,fileName);
        match(TokenType.READ);      // "read"
        match(TokenType.BRACES);    // "("
        match(TokenType.IDENTIFIER); // Variable to read into
        match(TokenType.BRACES);    // ")"
        match(TokenType.SEMICOLON); // ";"
        parseTree.endRule();
    }
    private void parseWriteStatement() {
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : -1;
        String fileName = currentToken != null ? currentToken.getFileName() : null;
        parseTree.startRule("WriteStatement", lineNumber,fileName);
        match(TokenType.WRITE);     // "write"
        match(TokenType.BRACES);    // "("
        parseExpression();          // Expression to output
        match(TokenType.BRACES);    // ")"
        match(TokenType.SEMICOLON); // ";"
        parseTree.endRule();
    }
    private boolean isValidType(TokenType type) {
        return type == TokenType.INTEGER ||
                type == TokenType.SINTEGER ||
                type == TokenType.CHARACTER ||
                type == TokenType.STRING ||
                type == TokenType.FLOAT ||
                type == TokenType.SFLOAT ||
                type == TokenType.BOOLEAN ||
                type == TokenType.VOID;
    }
    private boolean isFunctionCall() {
        int pos = getCurrentPosition();
        try {
            match(TokenType.IDENTIFIER);
            return currentToken != null &&
                    currentToken.getType() == TokenType.BRACES &&
                    currentToken.getValue().equals("(");
        } finally {
            resetToPosition(pos);
        }
    }
}