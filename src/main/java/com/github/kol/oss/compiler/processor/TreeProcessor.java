package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.OperatorPriority;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;

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
                while (!operators.isEmpty() && OperatorPriority.getPriority(operators.peek().value()) >= priority) {
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

        return nodes.isEmpty() ? null : nodes.pop();
    }

    private OperationNode createParentNode(Token token, Stack<TokenNode> nodes) {
        TokenNode right = nodes.pop();
        TokenNode left = nodes.pop();

        return new OperationNode(token, left, right);
    }
}