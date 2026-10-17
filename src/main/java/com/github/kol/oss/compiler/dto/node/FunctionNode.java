package com.github.kol.oss.compiler.dto.node;

import com.github.kol.oss.compiler.dto.token.Token;

import java.util.List;

public class FunctionNode extends TokenNode {
    private final List<TokenNode> arguments;

    public FunctionNode(Token token, List<TokenNode> arguments) {
        super(token);

        this.arguments = List.copyOf(arguments);
    }

    public List<TokenNode> getArguments() {
        return arguments;
    }
}