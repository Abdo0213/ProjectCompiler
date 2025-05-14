package parser;

import lexer.Token;
import lexer.TokenType;
import error.CompilerError;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class Parser {
    private List<Token> tokens;
    private Iterator<Token> iterator;
    private Token currentToken;
    private List<CompilerError> errors;
    private ParseTree parseTree;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.iterator = tokens.iterator();
        this.errors = new ArrayList<>();
        this.parseTree = new ParseTree();

        if (iterator.hasNext()) {
            currentToken = iterator.next();
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
        if (iterator.hasNext()) {
            currentToken = iterator.next();
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
        // Return current position in token list
        return tokens.indexOf(currentToken);
    }
    private void resetToPosition(int position) {
        // Reset to a specific position in token list
        if (position >= 0 && position < tokens.size()) {
            currentToken = tokens.get(position);
        } else {
            currentToken = null;
        }
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
                    parseFuncCall();
                    break;
                default:
                    errors.add(new CompilerError(currentToken.getLineNumber(),
                            "Unexpected token in class implementation: " + currentToken.getType()));
                    advance();
            }
        }
        parseTree.endRule();
    }
    private boolean isLikelyMethodDeclaration() {
        // Save current position
        int currentPosition = getCurrentPosition();

        try {
            parseType();  // Skip type
            if (currentToken == null || currentToken.getType() != TokenType.IDENTIFIER) {
                return false;
            }
            advance();  // Skip identifier

            // Check if next token is '('
            return currentToken != null && currentToken.getType() == TokenType.BRACES &&
                    currentToken.getValue().equals("(");
        } finally {
            // Restore position
            resetToPosition(currentPosition);
        }
    }



    private void parseMethodDeclaration() {
        parseTree.startRule("MethodDeclaration");

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
            while (currentToken != null && (
                    currentToken.getType() == TokenType.INTEGER ||
                            currentToken.getType() == TokenType.SINTEGER ||
                            currentToken.getType() == TokenType.CHARACTER ||
                            currentToken.getType() == TokenType.STRING ||
                            currentToken.getType() == TokenType.FLOAT ||
                            currentToken.getType() == TokenType.SFLOAT ||
                            currentToken.getType() == TokenType.BOOLEAN ||
                            currentToken.getType() == TokenType.VOID)) {
                parseVarDeclaration();
            }

            // Parse statements
            parseStatements();

            match(TokenType.BRACES); // }
        }

        parseTree.endRule();
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
        match(TokenType.IDENTIFIER);
        while (currentToken != null && currentToken.getType() == TokenType.BRACES) { // ,
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
        while (currentToken != null &&
                (currentToken.getType() == TokenType.ARITH_OP ||
                        currentToken.getType() == TokenType.LOGIC_OP)) {
            match(currentToken.getType());
            parseTerm();
        }
        parseTree.endRule();
    }

    private void parseTerm() {
        parseTree.startRule("Term");
        parseFactor();
        while (currentToken != null &&
                (currentToken.getType() == TokenType.ARITH_OP ||
                        currentToken.getType() == TokenType.LOGIC_OP)) {
            match(currentToken.getType());
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
                case STRING:
                    match(currentToken.getType());
                    break;
                case BRACES:
                    if (currentToken.getValue().equals("(")) {
                        match(TokenType.BRACES);
                        parseExpression();
                        match(TokenType.BRACES); // )
                    }
                    break;
                default:
                    errors.add(new CompilerError(currentToken.getLineNumber(),
                            "Unexpected token in expression: " + currentToken.getType()));
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
                    parseVarDeclaration();
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
        parseConditionExpression(); // Condition
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
        parseVarDeclaration();
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
}