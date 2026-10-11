package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.exception.ZeroDivisionException;
import com.github.kol.oss.compiler.utils.OperatorUtils;
import com.github.kol.oss.compiler.utils.TokenMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OptimizationProcessor {
    private final ExceptionHandler exceptionHandler;

    // human-readable log of every applied optimization, surfaced to the user afterwards
    private final List<String> optimizations = new ArrayList<>();

    public OptimizationProcessor(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public List<String> getOptimizations() {
        return optimizations;
    }

    private void record(String message) {
        optimizations.add(message);
    }

    // processes the tree node, if value - return unchanged
    public TokenNode process(TokenNode node) {
        if (node instanceof ValueNode)
            return node;
        else
            return processOperation((OperationNode) node);
    }

    private TokenNode processOperation(OperationNode node) {
        OperatorValue operatorValue = TokenMapper.tokenToOperator(node.getToken());

        // recursive processing of children
        node.setLeft(process(node.getLeft()));
        node.setRight(process(node.getRight()));

        TokenNode leftNode = node.getLeft();
        Token leftToken = leftNode.getToken();
        boolean isLeftOperand = leftNode instanceof ValueNode;

        TokenNode rightNode = node.getRight();
        Token rightToken = rightNode.getToken();
        boolean isRightOperand = rightNode instanceof ValueNode;

        // optimization of left operand
        if (isLeftOperand) {
            String value = leftToken.value();
            if (value.equals(NumberConstant.ZERO)) {
                if (operatorValue == OperatorValue.PLUS) {
                    record("0 + " + rightToken.value() + " => " + rightToken.value());
                    return rightNode;
                }

                if (operatorValue == OperatorValue.MULTIPLY) {
                    record("0 * " + rightToken.value() + " => 0");
                    return leftNode;
                }

                if (operatorValue == OperatorValue.DIVISION) {
                    record("0 / " + rightToken.value() + " => 0");
                    return leftNode;
                }
            } else if (value.equals(NumberConstant.ONE)) {
                if (operatorValue == OperatorValue.MULTIPLY) {
                    record("1 * " + rightToken.value() + " => " + rightToken.value());
                    return rightNode;
                }
            }
        }

        // optimization of right operand
        if (isRightOperand) {
            String value = rightToken.value();
            if (value.equals(NumberConstant.ZERO)) {
                if (operatorValue == OperatorValue.PLUS) {
                    record(leftToken.value() + " + 0 => " + leftToken.value());
                    return leftNode;
                }

                if (operatorValue == OperatorValue.MULTIPLY) {
                    record(leftToken.value() + " * 0 => 0");
                    return rightNode;
                }

                if (operatorValue == OperatorValue.MINUS) {
                    record(leftToken.value() + " - 0 => " + leftToken.value());
                    return leftNode;
                }

                if (operatorValue == OperatorValue.DIVISION) {
                    PositionedException exception = new ZeroDivisionException();
                    exception.setIndex(rightToken.position());

                    exceptionHandler.add(exception);
                    return node;
                }
            } else if (value.equals(NumberConstant.ONE)) {
                if (operatorValue == OperatorValue.MULTIPLY) {
                    record(leftToken.value() + " * 1 => " + leftToken.value());
                    return leftNode;
                }

                if (operatorValue == OperatorValue.DIVISION) {
                    record(leftToken.value() + " / 1 => " + leftToken.value());
                    return leftNode;
                }
            }
        }

        // optimization of premature calculations
        if (isLeftOperand && isRightOperand) {
            Optional<Token> result = OperatorUtils.applyOperation(leftNode.getToken(), rightNode.getToken(), operatorValue);

            // if both values are numbers then the result can be precalculated
            if (result.isPresent()) {
                record(leftToken.value() + " " + OperatorValue.toString(operatorValue) + " " + rightToken.value() + " => " + result.get().value());
                return new ValueNode(result.get());
            }
        }

        return node;
    }
}