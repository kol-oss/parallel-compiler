package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorPriority;
import com.github.kol.oss.compiler.constant.OperatorSymbol;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.utils.NodeFactory;

import java.util.*;

public class TreeBuildService {
    public TokenNode buildTree(List<Token> tokens) {
        Deque<TokenNode> nodes = new ArrayDeque<>();
        Deque<Token> operators = new ArrayDeque<>();
        Deque<Integer> argumentMarks = new ArrayDeque<>();

        for (Token token : tokens) {
            String value = token.value();
            TokenType tokenType = token.type();

            if (tokenType == TokenType.FUNCTION) {
                operators.push(token);
                argumentMarks.push(nodes.size());
                continue;
            }

            if (tokenType.isOperand()) {
                nodes.push(NodeFactory.value(token));
                continue;
            }

            if (tokenType == TokenType.LPAREN) {
                operators.push(token);
                continue;
            }

            if (tokenType == TokenType.RPAREN) {

                while (!operators.isEmpty() && !(operators.peek().type() == TokenType.LPAREN)) {
                    nodes.push(createOperation(operators.pop(), nodes));
                }

                operators.pop();

                if (!operators.isEmpty() && operators.peek().type() == TokenType.FUNCTION) {
                    Token function = operators.pop();
                    int mark = argumentMarks.pop();

                    List<TokenNode> arguments = new ArrayList<>();
                    while (nodes.size() > mark)
                        arguments.add(nodes.pop());

                    Collections.reverse(arguments);
                    nodes.push(NodeFactory.function(function, arguments));
                }
                continue;
            }

            if (tokenType.isOperator()) {

                int priority = OperatorPriority.getPriority(OperatorValue.fromSymbol(value));
                while (!operators.isEmpty() && outranks(operators.peek(), priority, value)) {
                    nodes.push(createOperation(operators.pop(), nodes));
                }

                operators.push(token);
            }
        }

        while (!operators.isEmpty())
            nodes.push(createOperation(operators.pop(), nodes));

        if (nodes.size() != 1)
            throw new IllegalStateException("Expression must produce exactly one operand but produced " + nodes.size());

        return nodes.pop();
    }

    private boolean outranks(Token pending, int priority, String value) {
        if (!pending.type().isOperator())
            return false;

        int pendingPriority = OperatorPriority.getPriority(OperatorValue.fromSymbol(pending.value()));
        return pendingPriority > priority ||
                (pendingPriority == priority && !value.equals(OperatorSymbol.EXPONENTIATION));
    }

    private OperationNode createOperation(Token token, Deque<TokenNode> nodes) {
        TokenNode right = nodes.pop();
        TokenNode left = nodes.pop();

        return NodeFactory.operation(token, left, right);
    }
}
