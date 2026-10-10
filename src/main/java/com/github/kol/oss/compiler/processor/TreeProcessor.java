package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.OperatorPriority;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TreeProcessor {
    public TokenNode buildTree(List<Token> tokens) {
        Stack<TokenNode> nodes = new Stack<>();
        Stack<Token> operators = new Stack<>();

        for (Token token : tokens) {
            String value = token.value();
            TokenType tokenType = token.type();

            // if not operator or parenthesis - just add as node
            if (tokenType.isOperand()) {
                nodes.push(new ValueNode(token));
                continue;
            }

            // if opening parenthesis - save for future structure detection
            if (tokenType == TokenType.LPAREN) {
                operators.push(token);
                continue;
            }

            // if closing parenthesis - if there are nested operators, then build node
            if (tokenType == TokenType.RPAREN) {
                while (!operators.isEmpty() && !(operators.peek().type() == TokenType.LPAREN)) {
                    OperationNode node = createParentNode(operators.pop(), nodes);
                    nodes.push(node);
                }

                operators.pop();
                continue;
            }

            // if operator - compare priorities and create nodes if supreme
            if (tokenType.isOperator()) {
                int priority = OperatorPriority.getPriority(value);
                while (!operators.isEmpty() && operators.peek().type() != TokenType.LPAREN &&
                        (OperatorPriority.getPriority(operators.peek().value()) > priority ||
                                (OperatorPriority.getPriority(operators.peek().value()) == priority && !value.equals("^")))) {
                    OperationNode node = createParentNode(operators.pop(), nodes);
                    nodes.push(node);
                }

                operators.push(token);
            }
        }

        // create nodes for left operators
        while (!operators.isEmpty()) {
            OperationNode node = createParentNode(operators.pop(), nodes);
            nodes.push(node);
        }

        return nodes.isEmpty() ? null : balance(nodes.pop());
    }

    public TokenNode balance(TokenNode node) {
        if (!(node instanceof OperationNode operation))
            return node;

        String operator = operation.getToken().value();
        if (isAssociative(operator)) {
            List<TokenNode> operands = new ArrayList<>();
            List<Token> operators = new ArrayList<>();
            collectChain(operation, operator, operands, operators);
            List<TokenNode> balancedOperands = operands.stream().map(this::balance).toList();
            return buildBalanced(balancedOperands, operators, 0, balancedOperands.size() - 1);
        }

        operation.setLeft(balance(operation.getLeft()));
        operation.setRight(balance(operation.getRight()));
        return operation;
    }

    private boolean isAssociative(String operator) {
        return operator.equals("+") || operator.equals("*");
    }

    private void collectChain(TokenNode node, String operator, List<TokenNode> operands, List<Token> operators) {
        if (node instanceof OperationNode operation && operation.getToken().value().equals(operator)) {
            collectChain(operation.getLeft(), operator, operands, operators);
            operators.add(operation.getToken());
            collectChain(operation.getRight(), operator, operands, operators);
        } else {
            operands.add(node);
        }
    }

    private TokenNode buildBalanced(List<TokenNode> operands, List<Token> operators, int start, int end) {
        if (start == end)
            return operands.get(start);

        int middleOperator = (start + end) / 2;
        TokenNode left = buildBalanced(operands, operators, start, middleOperator);
        TokenNode right = buildBalanced(operands, operators, middleOperator + 1, end);
        return new OperationNode(operators.get(middleOperator), left, right);
    }

    private OperationNode createParentNode(Token token, Stack<TokenNode> nodes) {
        TokenNode right = nodes.pop();
        TokenNode left = nodes.pop();

        return new OperationNode(token, left, right);
    }
}
