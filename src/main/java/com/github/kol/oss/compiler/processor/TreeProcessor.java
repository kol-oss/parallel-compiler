package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.OperatorPriority;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.utils.OperatorUtils;
import com.github.kol.oss.compiler.utils.TokenMapper;
import com.github.kol.oss.compiler.utils.TreeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class TreeProcessor {
    public TokenNode buildTree(List<Token> tokens) {
        Stack<TokenNode> nodes = new Stack<>();
        Stack<Token> operators = new Stack<>();
        Stack<Integer> argumentMarks = new Stack<>();

        for (Token token : tokens) {
            String value = token.value();
            TokenType tokenType = token.type();

            // functions are unwrapped once their own parenthesis pair closes
            if (tokenType == TokenType.FUNCTION) {
                operators.push(token);
                argumentMarks.push(nodes.size());
                continue;
            }

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

                // the function's own pair sits right above it on the operator stack
                if (!operators.isEmpty() && operators.peek().type() == TokenType.FUNCTION) {
                    Token function = operators.pop();
                    int mark = argumentMarks.pop();

                    List<TokenNode> arguments = new ArrayList<>(nodes.subList(mark, nodes.size()));
                    while (nodes.size() > mark)
                        nodes.pop();

                    nodes.push(new FunctionNode(function, arguments));
                }
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

        return nodes.isEmpty() ? null : nodes.pop();
    }

    // Rebuilds the expression tree into a parallel form of minimal height and maximal width.
    public TokenNode balance(TokenNode node) {
        if (node instanceof FunctionNode function)
            return new FunctionNode(function.getToken(),
                    function.getArguments().stream().map(this::balance).toList());

        if (!(node instanceof OperationNode operation))
            return node;

        String operator = operation.getToken().value();
        return switch (operator) {
            case "+", "*", "-", "/" -> balanceChain(operation, operator);
            default -> {
                operation.setLeft(balance(operation.getLeft()));
                operation.setRight(balance(operation.getRight()));
                yield operation;
            }
        };
    }

    private TokenNode balanceChain(OperationNode operation, String operator) {
        List<TokenNode> operands = new ArrayList<>();

        boolean flattenBoth = operator.equals("+") || operator.equals("*");
        collectChain(operation, operator, operands, flattenBoth);

        List<TokenNode> balancedOperands = operands.stream().map(this::balance).toList();
        int last = balancedOperands.size() - 1;

        return switch (operator) {
            case "+" -> buildSum(balancedOperands, 0, last);
            case "*" -> buildProduct(balancedOperands, 0, last);
            case "-" -> buildSubtraction(balancedOperands, 0, last);
            case "/" -> buildDivision(balancedOperands, 0, last);
            default -> throw new IllegalStateException("Unexpected operator: " + operator);
        };
    }

    private void collectChain(TokenNode node, String operator, List<TokenNode> operands, boolean flattenBoth) {
        if (node instanceof OperationNode operation && operation.getToken().value().equals(operator)) {
            collectChain(operation.getLeft(), operator, operands, flattenBoth);

            if (flattenBoth)
                collectChain(operation.getRight(), operator, operands, flattenBoth);
            else
                operands.add(operation.getRight());
        } else {
            operands.add(node);
        }
    }

    private TokenNode buildSum(List<TokenNode> operands, int start, int end) {
        return TreeUtils.buildBalanced(operands.subList(start, end + 1), "+");
    }

    private TokenNode buildProduct(List<TokenNode> operands, int start, int end) {
        return TreeUtils.buildBalanced(operands.subList(start, end + 1), "*");
    }

    // (a - b) - (c + d): the subtracted tail is grouped as a balanced sum and negated as a whole.
    private TokenNode buildSubtraction(List<TokenNode> operands, int start, int end) {
        if (start == end)
            return operands.get(start);

        int middle = (start + end) / 2;
        TokenNode left = buildSubtraction(operands, start, middle);
        TokenNode right = buildSum(operands, middle + 1, end);
        return new OperationNode(operatorToken("-"), left, right);
    }

    // (a / b) / (c * d): the divisor tail is grouped as a balanced product.
    private TokenNode buildDivision(List<TokenNode> operands, int start, int end) {
        if (start == end)
            return operands.get(start);

        int middle = (start + end) / 2;
        TokenNode left = buildDivision(operands, start, middle);
        TokenNode right = buildProduct(operands, middle + 1, end);
        return new OperationNode(operatorToken("/"), left, right);
    }

    private Token operatorToken(String value) {
        return new Token(value, TokenType.OPERATOR, 0);
    }

    // Evaluates a fully-constant tree, returning its value
    public Optional<Token> evaluate(TokenNode node) {
        if (node instanceof ValueNode value) {
            try {
                Double.parseDouble(value.getToken().value());
                return Optional.of(value.getToken());
            } catch (NumberFormatException _) {
                return Optional.empty();
            }
        }

        if (node instanceof OperationNode operation) {
            Optional<Token> left = evaluate(operation.getLeft());
            Optional<Token> right = evaluate(operation.getRight());

            if (left.isPresent() && right.isPresent())
                return OperatorUtils.applyOperation(left.get(), right.get(), TokenMapper.tokenToOperator(operation.getToken()));
        }

        return Optional.empty();
    }

    private OperationNode createParentNode(Token token, Stack<TokenNode> nodes) {
        TokenNode right = nodes.pop();
        TokenNode left = nodes.pop();

        return new OperationNode(token, left, right);
    }
}
