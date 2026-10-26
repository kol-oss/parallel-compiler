package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;

import java.util.Map;

public final class TreeEvaluator {
    private TreeEvaluator() {
    }

    public static double evaluate(TokenNode node, Map<String, Double> values) {
        if (node instanceof ValueNode value) {
            String text = value.getToken().value();

            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException _) {
                return values.getOrDefault(text, 1.0);
            }
        }

        if (node instanceof FunctionNode function) {
            double argument = evaluate(function.getArguments().getFirst(), values);

            return switch (function.getToken().value()) {
                case "sin" -> Math.sin(argument);
                case "cos" -> Math.cos(argument);
                case "exp" -> Math.exp(argument);
                case "log" -> Math.log(argument);
                default -> throw new IllegalArgumentException("Unsupported function: " + function.getToken().value());
            };
        }

        OperationNode operation = (OperationNode) node;
        double left = evaluate(operation.getLeft(), values);
        double right = evaluate(operation.getRight(), values);

        return switch (operation.getToken().value()) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> left / right;
            default -> throw new IllegalArgumentException("Unsupported operator: " + operation.getToken().value());
        };
    }
}
