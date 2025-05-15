package lexer;

public enum TokenType {
    // Keywords
    CLASS("Class"),
    INHERITANCE("Inheritance"),
    CONDITION("Condition"),
    INTEGER("Integer"),
    SINTEGER("SInteger"),
    CHARACTER("Character"),
    STRING("String"),
    FLOAT("Float"),
    SFLOAT("SFloat"),
    VOID("Void"),
    BOOLEAN("Boolean"),
    BREAK("Terminate_this/Break"),
    LOOP("Loop"),
    RETURN("Return"),
    STRUCT("Struct"),
    SWITCH("Switch"),
    START_STATEMENT("Start Statement"),
    END_STATEMENT("End Statement"),
    INCLUSION("Inclusion"),

    // Operators
    ARITH_OP("Arithmetic Operation"),
    LOGIC_OP("Logic operators"),
    REL_OP("relational operators"),
    ASSIGN_OP("Assignment operator"),
    ACCESS_OP("Access Operator"),

    // Other
    IDENTIFIER("Identifier"),
    CONSTANT("Constant"),
    QUOTE("Quotation Mark"),
    COMMENT("Comment"),
    BRACES("Braces"),
    SEMICOLON(";"),
    COMMA(","),
    READ("Read"),
    WRITE("Write"),
    ERROR("Error"),
    UNKNOWN("Unknown");



    private final String description;

    TokenType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}