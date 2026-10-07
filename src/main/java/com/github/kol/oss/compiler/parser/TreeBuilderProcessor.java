package com.github.kol.oss.compiler.parser;

import com.github.kol.oss.compiler.dto.Token;

import java.util.List;
import java.util.Stack;

public class TreeBuilderProcessor {

    public TreeNode buildAst(List<Token> tokens) {
        Stack<TreeNode> nodes = new Stack<>();
        Stack<String> operators = new Stack<>();

        for (Token token : tokens) {
            String val = token.value();

            if (isOperand(val)) {
                nodes.push(new OperandNode(val));
            } else if (val.equals("(")) {
                operators.push(val);
            } else if (val.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    nodes.push(buildNode(operators.pop(), nodes));
                }
                operators.pop();
            } else if (isOperator(val)) {
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(val)) {
                    nodes.push(buildNode(operators.pop(), nodes));
                }
                operators.push(val);
            }
        }

        while (!operators.isEmpty()) {
            nodes.push(buildNode(operators.pop(), nodes));
        }

        return nodes.isEmpty() ? null : nodes.pop();
    }

    private OperatorNode buildNode(String operator, Stack<TreeNode> nodes) {
        TreeNode right = nodes.pop();
        TreeNode left = nodes.pop();
        return new OperatorNode(operator, left, right);
    }

    private boolean isOperand(String val) {
        return val.matches("[a-zA-Z0-9.]+");
    }

    private boolean isOperator(String val) {
        return val.matches("[+\\-*/]");
    }

    private int precedence(String operator) {
        if (operator.equals("+") || operator.equals("-")) return 1;
        if (operator.equals("*") || operator.equals("/")) return 2;
        return 0;
    }
}