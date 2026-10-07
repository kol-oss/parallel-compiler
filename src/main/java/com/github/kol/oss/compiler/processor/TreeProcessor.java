package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.OperatorPriority;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.dto.node.OperandNode;
import com.github.kol.oss.compiler.dto.node.OperatorNode;
import com.github.kol.oss.compiler.dto.node.Node;

import java.util.List;
import java.util.Stack;

public class TreeProcessor {
    public Node buildTree(List<Token> tokens) {
        Stack<Node> nodes = new Stack<>();
        Stack<Token> operators = new Stack<>();

        for (Token token : tokens) {
            String value = token.value();
            TokenType tokenType = token.type();

            // if not operator or parenthesis - just add as node
            if (tokenType.isOperand()) {
                nodes.push(new OperandNode(value));
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
                    OperatorNode node = createOperatorNode(operators.pop(), nodes);
                    nodes.push(node);
                }

                operators.pop();
                continue;
            }

            // if operator - compare priorities and create nodes if supreme
            if (tokenType.isOperator()) {
                int priority = OperatorPriority.getPriority(value);
                while (!operators.isEmpty() && OperatorPriority.getPriority(operators.peek().value()) >= priority) {
                    OperatorNode node = createOperatorNode(operators.pop(), nodes);
                    nodes.push(node);
                }

                operators.push(token);
            }
        }

        // create nodes for left operators
        while (!operators.isEmpty()) {
            OperatorNode node = createOperatorNode(operators.pop(), nodes);
            nodes.push(node);
        }

        return nodes.isEmpty() ? null : nodes.pop();
    }

    private OperatorNode createOperatorNode(Token token, Stack<Node> nodes) {
        Node right = nodes.pop();
        Node left = nodes.pop();

        return new OperatorNode(token.value(), left, right);
    }
}