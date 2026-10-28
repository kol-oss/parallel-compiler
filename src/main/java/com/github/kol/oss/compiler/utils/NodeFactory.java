package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.List;

public class NodeFactory {
    public static ValueNode value(Token token) {
        return new ValueNode(token);
    }

    public static OperationNode operation(Token token, TokenNode left, TokenNode right) {
        return new OperationNode(token, left, right);
    }

    public static OperationNode operation(OperatorValue operator, TokenNode left, TokenNode right) {
        return operation(new Token(operator.toSymbol(), TokenType.OPERATOR, 0), left, right);
    }

    public static FunctionNode function(Token token, List<TokenNode> arguments) {
        return new FunctionNode(token, arguments);
    }
}