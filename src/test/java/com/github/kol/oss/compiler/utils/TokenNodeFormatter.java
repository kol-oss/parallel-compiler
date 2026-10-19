package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;

public class TokenNodeFormatter {
    public static String toKey(TokenNode node) {
        switch (node) {
            case null -> {
                return "";
            }
            case ValueNode _ -> {
                return node.getToken().value();
            }
            case OperationNode op -> {
                return "(" + toKey(op.getLeft()) + node.getToken().value() + toKey(op.getRight()) + ")";
            }
            case FunctionNode fn -> {
                StringBuilder sb = new StringBuilder();
                sb.append(fn.getToken().value()).append("(");
                for (int i = 0; i < fn.getArguments().size(); i++) {
                    sb.append(toKey(fn.getArguments().get(i)));
                    if (i < fn.getArguments().size() - 1) sb.append(",");
                }

                sb.append(")");
                return sb.toString();
            }
            default -> {
            }
        }

        return node.getToken().value();
    }
}
