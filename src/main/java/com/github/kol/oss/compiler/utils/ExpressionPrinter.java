package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorPriority;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.stream.Collectors;

public class ExpressionPrinter {
    // renders the tree as an infix expression with only the required parentheses
    public static String print(TokenNode node) {
        if (node instanceof FunctionNode function) {
            String arguments = function.getArguments().stream()
                    .map(ExpressionPrinter::print)
                    .collect(Collectors.joining(", "));

            return function.getToken().value() + "(" + arguments + ")";
        }

        if (!(node instanceof OperationNode operation))
            return node.getToken().value();

        String operator = operation.getToken().value();
        int parentPriority = OperatorPriority.getPriority(operator);

        String left = printChild(operation.getLeft(), parentPriority, false, operator);
        String right = printChild(operation.getRight(), parentPriority, true, operator);

        return left + " " + operator + " " + right;
    }

    private static String printChild(TokenNode child, int parentPriority, boolean isRight, String parentOperator) {
        String printed = print(child);

        if (!(child instanceof OperationNode))
            return printed;

        int childPriority = OperatorPriority.getPriority(child.getToken().value());

        if (childPriority != parentPriority)
            return childPriority < parentPriority ? "(" + printed + ")" : printed;

        // equal priority: right children always bind, "^" is right-associative so its left child wraps
        boolean needsParentheses = isRight || parentOperator.equals("^");
        return needsParentheses ? "(" + printed + ")" : printed;
    }
}