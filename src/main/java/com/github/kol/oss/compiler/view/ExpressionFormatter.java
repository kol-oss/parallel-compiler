package com.github.kol.oss.compiler.view;

import com.github.kol.oss.compiler.constant.OperatorPriority;
import com.github.kol.oss.compiler.constant.OperatorSymbol;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpressionFormatter {
    public static String format(TokenNode root) {
        Map<TokenNode, String> rendered = new IdentityHashMap<>();

        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (node instanceof FunctionNode function) {
                String arguments = function.getArguments().stream()
                        .map(rendered::get)
                        .collect(Collectors.joining(OperatorSymbol.COMMA + " "));

                rendered.put(node, function.getToken().value()
                        + OperatorSymbol.LEFT_PARENTHESIS + arguments + OperatorSymbol.RIGHT_PARENTHESIS);
                continue;
            }

            if (!(node instanceof OperationNode operation)) {
                rendered.put(node, node.getToken().value());
                continue;
            }

            String operator = operation.getToken().value();
            int priority = OperatorPriority.getPriority(OperatorValue.fromSymbol(operator));

            String left = wrap(operation.getLeft(), priority, false, operator, rendered);
            String right = wrap(operation.getRight(), priority, true, operator, rendered);
            rendered.put(node, left + " " + operator + " " + right);
        }

        return rendered.get(root);
    }

    private static String wrap(TokenNode child, int parentPriority, boolean isRight, String parentOperator, Map<TokenNode, String> rendered) {
        String printed = rendered.get(child);

        if (!(child instanceof OperationNode))
            return printed;

        int childPriority = OperatorPriority.getPriority(OperatorValue.fromSymbol(child.getToken().value()));

        if (childPriority != parentPriority)
            return childPriority < parentPriority ? parenthesize(printed) : printed;

        boolean needsParentheses = isRight || parentOperator.equals(OperatorValue.EXPONENTIATION.toSymbol());
        return needsParentheses ? parenthesize(printed) : printed;
    }

    private static String parenthesize(String printed) {
        return OperatorSymbol.LEFT_PARENTHESIS + printed + OperatorSymbol.RIGHT_PARENTHESIS;
    }
}