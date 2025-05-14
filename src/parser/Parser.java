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
    private ParseTree parseTree;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.tokenIterator = tokens.listIterator();  // Use ListIterator
        this.errors = new ArrayList<>();
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
            advance();
        } else {
            String errorMsg = String.format("Expected %s but found %s",
                    expectedType, currentToken != null ? currentToken.getType() : "EOF");
            errors.add(new CompilerError(currentToken != null ? currentToken.getLineNumber() : -1, errorMsg));
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
        parseTree.startRule("Program");
        match(TokenType.START_STATEMENT);
        parseClassDeclarationList();
        match(TokenType.END_STATEMENT);
        parseTree.endRule();
    }
    private void parseClassDeclarationList() {
        parseTree.startRule("ClassDeclarationList");
        while (currentToken != null && currentToken.getType() == TokenType.CLASS) {
            parseClassDeclaration();
        }
        parseTree.endRule();
    }
    private void parseClassDeclaration() {
        parseTree.startRule("ClassDeclaration");
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
        parseTree.startRule("ClassImplementation");
        while (currentToken != null && currentToken.getType() != TokenType.BRACES) {
            parseClassItem();
        }
        parseTree.endRule();
    }
    private void parseClassItem() {
        parseTree.startRule("ClassItem");
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
                case IDENTIFIER:
                    if (isFunctionCall()) {
                        parseFuncCall(); //  id id = 20;Done  id id () {}  id id; id id ();
                    } else if(isAssignment()){
                        parseAssignment();
                    } else if (isLikelyMethodDeclaration()) {
                        parseMethodDeclaration();
                    } else if (isVarDecl()){
                        parseVarDeclaration();
                    }
                    else {
                        errors.add(new CompilerError(currentToken.getLineNumber(),
                                "Unexpected identifier: " + currentToken.getValue()));
                        advance();
                    }
                    break;
                default:
                    errors.add(new CompilerError(currentToken.getLineNumber(),
                            "Unexpected token in class implementation: " + currentToken.getType()));
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
        parseTree.startRule("MethodDeclaration");
        try {
            if (currentToken == null /*|| !isValidType(currentToken.getType())*/) {
                errors.add(new CompilerError(
                        currentToken != null ? currentToken.getLineNumber() : -1,
                        "'" + (currentToken != null ? currentToken.getValue() : "null") + "' is not a valid Type"));
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
        parseTree.startRule("FuncDeclaration");

        // Rule 7: FuncDecl → Type ID ( ParameterList )
        parseType();
        match(TokenType.IDENTIFIER);
        match(TokenType.BRACES); // (
        parseParameterList();
        match(TokenType.BRACES); // )

        parseTree.endRule();
    }

    private void parseParameterList() {
        parseTree.startRule("ParameterList");

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
        parseTree.startRule("NonEmptyParameterList");

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
        parseTree.startRule("VarDeclaration");
        parseType();
        parseIDList();
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }
    private void parseAssignmentVarDeclaration() {
        parseTree.startRule("VarDeclaration");
        if(isValidType(currentToken.getType())) // int x =5; x =5; w w = 5;
            parseType();
        parseIDList(); // int x =6; int x,z = 5; w , w = 5
        parseTree.endRule();
    }
    private void parseType() {
        parseTree.startRule("Type");
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
            errors.add(new CompilerError(currentToken.getLineNumber(),
                    "Expected type but found: " + currentToken.getType()));
            advance();
        }
        parseTree.endRule();
    }

    private void parseIDList() {
        parseTree.startRule("IDList");
        match(TokenType.IDENTIFIER);// w w
        while (currentToken != null && currentToken.getType() == TokenType.COMMA) { // ,
            match(TokenType.COMMA);
            match(TokenType.IDENTIFIER);
        }
        parseTree.endRule();
    }
    private void parseUsingCommand() {
        parseTree.startRule("UsingCommand");
        match(TokenType.INCLUSION);
        match(TokenType.BRACES); // (
        match(TokenType.STRING); // Filename
        match(TokenType.BRACES); // )
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }
    private void parseFuncCall() {
        parseTree.startRule("FuncCall");
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
        parseTree.startRule("ArgumentList");
        if (currentToken != null && currentToken.getType() != TokenType.BRACES) { // )
            parseNonEmptyArgumentList();
        }
        parseTree.endRule();
    }

    private void parseNonEmptyArgumentList() {
        parseTree.startRule("NonEmptyArgumentList");
        parseExpression();
        while (currentToken != null && currentToken.getType() == TokenType.COMMA) {
            match(TokenType.COMMA);
            parseExpression();
        }
        parseTree.endRule();
    }

    private void parseComment() {
        parseTree.startRule("Comment");
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
        parseTree.startRule("Expression");
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
        parseTree.startRule("Term");
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
        parseTree.startRule("Factor");
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
                    errors.add(new CompilerError(currentToken.getLineNumber(),
                            "Unexpected token in factor: " + currentToken.getType()));
                    advance();
            }
        }
        parseTree.endRule();
    }

    private void parseStatements() {
        parseTree.startRule("Statements");
        while (currentToken != null && !currentToken.getValue().equals("}")) {
            parseStatement();
        }
        parseTree.endRule();
    }

    private void parseStatement() {
        parseTree.startRule("Statement");
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
                    errors.add(new CompilerError(currentToken.getLineNumber(),
                            "Unexpected statement: " + currentToken.getType()));
                    advance();
            }
        }
        parseTree.endRule();
    }
    private void parseWhetherDoStatement() {
        parseTree.startRule("WhetherDoStatement");
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
        parseTree.startRule("ConditionExpression");
        parseCondition();
        while (currentToken != null &&
                (currentToken.getType() == TokenType.LOGIC_OP)) {
            match(currentToken.getType()); // AND/OR
            parseCondition();
        }
        parseTree.endRule();
    }

    private void parseCondition() {
        parseTree.startRule("Condition");
        parseExpression();
        match(TokenType.REL_OP); // ==, !=, etc.
        parseExpression();
        parseTree.endRule();
    }

    private void parseRotateWhenStatement() {
        parseTree.startRule("RotateWhenStatement");
        match(TokenType.LOOP);      // "Rotatewhen"
        match(TokenType.BRACES);    // "("
        parseConditionExpression();
        match(TokenType.BRACES);    // ")"
        parseBlockStatements();
        parseTree.endRule();
    }

    private void parseContinueWhenStatement() {
        parseTree.startRule("ContinueWhenStatement");
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
        parseTree.startRule("ReplyWithStatement");
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
        parseTree.startRule("TerminateThisStatement");
        match(TokenType.BREAK);     // "terminatethis"
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }

    private void parseAssignment() {
        parseTree.startRule("Assignment");
        parseAssignmentVarDeclaration();
        match(TokenType.ASSIGN_OP); // "="
        parseExpression();
        match(TokenType.SEMICOLON);
        parseTree.endRule();
    }

    private void parseBlockStatements() {
        parseTree.startRule("BlockStatements");
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
        parseTree.startRule("ReadStatement");
        match(TokenType.READ);      // "read"
        match(TokenType.BRACES);    // "("
        match(TokenType.IDENTIFIER); // Variable to read into
        match(TokenType.BRACES);    // ")"
        match(TokenType.SEMICOLON); // ";"
        parseTree.endRule();
    }
    private void parseWriteStatement() {
        parseTree.startRule("WriteStatement");
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