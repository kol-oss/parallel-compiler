package com.github.kol.oss.compiler.constant;

public class SymbolRegex {
    public static final String SKIP = "[ ]";
    public static final String NUMBER = "[0-9]";
    public static final String FLOAT = "[.]";
    public static final String OPERATOR = "[+\\-/*^%]";
    public static final String LOGICAL_OPERATOR = "[&|]";
    public static final String UNARY_OPERATOR = "[-]";
    public static final String COMMA = "[,]";
    public static final String LPAREN = "[(]";
    public static final String RPAREN = "[)]";
    public static final String TEXT = "[A-Za-z_]";
}
