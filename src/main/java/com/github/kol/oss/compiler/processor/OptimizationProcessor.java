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

import java.util.Map;
import java.util.Optional;

public class OptimizationProcessor {
    private static final Map<OperatorValue, OperatorValue> SEQUENCE_REPLACEMENTS = Map.of(
            OperatorValue.MINUS, OperatorValue.PLUS,
            OperatorValue.DIVISION, OperatorValue.MULTIPLY
    );

    private final ExceptionHandler exceptionHandler;

    public OptimizationProcessor(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
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
                if (operatorValue == OperatorValue.PLUS)
                    return rightNode;

                if (operatorValue == OperatorValue.MULTIPLY)
                    return leftNode;

                if (operatorValue == OperatorValue.DIVISION)
                    return leftNode;
            } else if (value.equals(NumberConstant.ONE)) {
                if (operatorValue == OperatorValue.MULTIPLY)
                    return rightNode;
            }
        }

        // optimization of right operand
        if (isRightOperand) {
            String value = rightToken.value();
            if (value.equals(NumberConstant.ZERO)) {
                if (operatorValue == OperatorValue.PLUS)
                    return leftNode;

                if (operatorValue == OperatorValue.MULTIPLY)
                    return rightNode;

                if (operatorValue == OperatorValue.MINUS)
                    return leftNode;

                if (operatorValue == OperatorValue.DIVISION) {
                    PositionedException exception = new ZeroDivisionException();
                    exception.setIndex(rightToken.position());

                    exceptionHandler.add(exception);
                    return node;
                }
            } else if (value.equals(NumberConstant.ONE)) {
                return leftNode;
            }
        }

        // optimization of premature calculations
        if (isLeftOperand && isRightOperand) {
            Optional<Token> result = OperatorUtils.applyOperation(leftNode.getToken(), rightNode.getToken(), operatorValue);

            // if both values are numbers then the result can be precalculated
            if (result.isPresent())
                return new ValueNode(result.get());
        }

        // optimization of sequential patterns
        for (Map.Entry<OperatorValue, OperatorValue> optimization : SEQUENCE_REPLACEMENTS.entrySet()) {
            boolean isOptimized = optimizeSequence(node, optimization.getKey(), optimization.getValue());
            if (isOptimized)
                return process(leftNode);
        }

        return node;
    }

    // replaces sequence of same operators with more optimized one
    private boolean optimizeSequence(OperationNode operationNode, OperatorValue base, OperatorValue replacement) {
        OperatorValue operator = TokenMapper.tokenToOperator(operationNode.getToken());
        TokenNode left = operationNode.getLeft();
        TokenNode right = operationNode.getRight();

        if (operator == base && left instanceof OperationNode leftNode) {
            OperatorValue leftOperator = TokenMapper.tokenToOperator(leftNode.getToken());
            if (leftOperator == base) {
                Token rightToken = right.getToken();
                Token newRightToken = new Token(OperatorValue.toString(replacement), rightToken.type(), rightToken.position());

                OperationNode newRight = new OperationNode(newRightToken, leftNode.getRight(), right);
                leftNode.setRight(newRight);

                return true;
            }
        }

        return false;
    }
}