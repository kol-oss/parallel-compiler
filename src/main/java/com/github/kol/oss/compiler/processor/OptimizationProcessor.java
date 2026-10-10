package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.OperatorType;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.exception.ZeroDivisionException;
import com.github.kol.oss.compiler.utils.TokenMapper;

import java.util.Map;

public class OptimizationProcessor {
    private static final Map<OperatorType, OperatorType> OPTIMIZATIONS = Map.of(
            OperatorType.MINUS, OperatorType.PLUS,
            OperatorType.DIVISION, OperatorType.MULTIPLY
    );

    private final ExceptionHandler exceptionHandler;

    public OptimizationProcessor(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public TokenNode process(TokenNode root) {
        return optimizeOperations(root);
    }

    private TokenNode optimizeOperations(TokenNode node) {
        if (node instanceof ValueNode)
            return node;

        OperationNode operationNode = (OperationNode) node;
        TokenNode left = operationNode.getLeft();
        TokenNode right = operationNode.getRight();

        OperatorType operatorType = TokenMapper.tokenToOperator(node.getToken());

        // optimization of branches
        operationNode.setLeft(optimizeOperations(left));
        operationNode.setRight(optimizeOperations(right));

        left = operationNode.getLeft();
        right = operationNode.getRight();

        if (left instanceof ValueNode leftOperand) {
            String value = leftOperand.getToken().value();
            if (value.equals("0")) {
                if (operatorType == OperatorType.PLUS)
                    return right;

                if (operatorType == OperatorType.MULTIPLY)
                    return left;

                if (operatorType == OperatorType.DIVISION)
                    return left;
            } else if (value.equals("1")) {
                if (operatorType == OperatorType.MULTIPLY)
                    return right;
            }
        }

        if (right instanceof ValueNode rightOperand) {
            String value = rightOperand.getToken().value();
            if (value.equals("0")) {
                if (operatorType == OperatorType.PLUS)
                    return left;

                if (operatorType == OperatorType.MULTIPLY)
                    return right;

                if (operatorType == OperatorType.MINUS)
                    return left;

                if (operatorType == OperatorType.DIVISION) {
                    PositionedException exception = new ZeroDivisionException();
                    exception.setIndex(rightOperand.getToken().position());

                    exceptionHandler.add(exception);
                    return operationNode;
                }
            } else if (value.equals("1")) {
                return left;
            }
        }

        // TODO: ADD CALCULATIONS
        if (left instanceof ValueNode leftOperand && right instanceof ValueNode rightOperand) {
            try {
                double leftValue = Double.parseDouble(leftOperand.getToken().value());
                double rightValue = Double.parseDouble(rightOperand.getToken().value());

                boolean isFloat = leftOperand.getToken().type() == TokenType.FLOAT
                        || rightOperand.getToken().type() == TokenType.FLOAT;

                double value = 0;

                if (operatorType == OperatorType.PLUS)
                    value = leftValue + rightValue;
                else if (operatorType == OperatorType.MINUS)
                    value = leftValue - rightValue;
                else if (operatorType == OperatorType.MULTIPLY)
                    value = leftValue * rightValue;
                else if (operatorType == OperatorType.DIVISION)
                    value = leftValue / rightValue;
                else if (operatorType == OperatorType.EXPONENTIATION)
                    value = Math.pow(leftValue, rightValue);

                String stringValue;

                if (value == (long) value) {
                    stringValue = String.valueOf((long) value);
                } else {
                    stringValue = String.valueOf(value);
                }

                return new ValueNode(
                        new Token(
                                stringValue,
                                isFloat ? TokenType.FLOAT : TokenType.INTEGER,
                                left.getToken().position()
                        )
                );

            } catch (NumberFormatException _) {
            }
        }

        // optimization of sequential patterns
        for (Map.Entry<OperatorType, OperatorType> optimization : OPTIMIZATIONS.entrySet()) {
            boolean isOptimized = optimizeSequence(operationNode, optimization.getKey(), optimization.getValue());
            if (isOptimized)
                return optimizeOperations(left);
        }

        return operationNode;
    }

    private boolean optimizeSequence(OperationNode operationNode, OperatorType base, OperatorType replacement) {
        OperatorType operator = TokenMapper.tokenToOperator(operationNode.getToken());
        TokenNode left = operationNode.getLeft();
        TokenNode right = operationNode.getRight();

        if (operator == base && left instanceof OperationNode leftNode) {
            OperatorType leftOperator = TokenMapper.tokenToOperator(leftNode.getToken());
            if (leftOperator == base) {
                Token rightToken = right.getToken();
                Token newRightToken = new Token(OperatorType.toString(replacement), rightToken.type(), rightToken.position());

                OperationNode newRight = new OperationNode(newRightToken, leftNode.getRight(), right);
                leftNode.setRight(newRight);

                return true;
            }
        }

        return false;
    }
}