package com.github.kol.oss.compiler.dto.node;

import com.github.kol.oss.compiler.dto.token.Token;

public class ValueNode extends TokenNode {
    public ValueNode(Token token) {
        super(token, 0);
    }
}