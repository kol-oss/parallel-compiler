package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
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

    // true when the token holds a numeric literal (integer or float)
    private boolean isNumeric(Token token) {
        try {
            Double.parseDouble(token.value());
            return true;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    // processes the tree node, if value - return unchanged
    public TokenNode process(TokenNode node) {
        if (node instanceof ValueNode)
            return node;

        // functions stay opaque, only their arguments can be optimized
        if (node instanceof FunctionNode function)
            return processFunction(function);

        OperationNode operation = (OperationNode) node;
        OperatorValue operatorValue = TokenMapper.tokenToOperator(operation.getToken());

        // flattening the constants into the single value
        if (isAssociative(operatorValue)) {
            List<TokenNode> operands = new ArrayList<>();
            collectChain(operation, operatorValue, operands);

            List<TokenNode> processed = operands.stream().map(this::process).toList();
            return foldConstants(processed, operatorValue);
        }

        // recursive processing of children, then local optimizations
        operation.setLeft(process(operation.getLeft()));
        operation.setRight(process(operation.getRight()));

        return processOperation(operation, operatorValue);
    }

    private boolean isAssociative(OperatorValue operatorValue) {
        return operatorValue == OperatorValue.PLUS || operatorValue == OperatorValue.MULTIPLY;
    }

    private TokenNode processFunction(FunctionNode function) {
        List<TokenNode> arguments = function.getArguments().stream().map(this::process).toList();
        return arguments.equals(function.getArguments())
                ? function
                : new FunctionNode(function.getToken(), arguments);
    }

    // collects every operand of a same-operator associative chain into a flat list
    private void collectChain(TokenNode node, OperatorValue operatorValue, List<TokenNode> operands) {
        if (node instanceof OperationNode operation && TokenMapper.tokenToOperator(operation.getToken()) == operatorValue) {
            collectChain(operation.getLeft(), operatorValue, operands);
            collectChain(operation.getRight(), operatorValue, operands);
        } else {
            operands.add(node);
        }
    }

    private TokenNode foldConstants(List<TokenNode> operands, OperatorValue operatorValue) {
        // separate numeric constants from the rest (variables / nested sub-expressions)
        List<TokenNode> nonConstants = new ArrayList<>();
        List<Token> constants = new ArrayList<>();
        for (TokenNode operand : operands) {
            if (operand instanceof ValueNode && isNumeric(operand.getToken()))
                constants.add(operand.getToken());
            else
                nonConstants.add(operand);
        }

        // multiplication by zero annihilates the whole chain
        if (operatorValue == OperatorValue.MULTIPLY &&
                constants.stream().anyMatch(token -> token.value().equals(NumberConstant.ZERO))) {
            record(joinOperands(operands, operatorValue) + " => 0");
            return new ValueNode(new Token(NumberConstant.ZERO, TokenType.INTEGER, 0));
        }

        // fold every numeric constant into a single value
        Token folded = null;
        if (!constants.isEmpty()) {
            folded = constants.getFirst();

            if (constants.size() > 1) {
                StringBuilder expression = new StringBuilder(folded.value());
                for (int i = 1; i < constants.size(); i++) {
                    Optional<Token> result = OperatorUtils.applyOperation(folded, constants.get(i), operatorValue);
                    if (result.isEmpty())
                        return rebuildChain(operands, operatorValue);

                    expression.append(" ").append(OperatorValue.toString(operatorValue)).append(" ").append(constants.get(i).value());
                    folded = result.get();
                }

                record(expression + " => " + folded.value());
            }
        }

        // a lone neutral element (0 for "+", 1 for "*") next to other operands can be dropped
        if (folded != null && !nonConstants.isEmpty() && isNeutral(folded.value(), operatorValue)) {
            record(joinOperands(operands, operatorValue) + " => " + joinOperands(nonConstants, operatorValue));
            folded = null;
        }

        List<TokenNode> rebuilt = new ArrayList<>(nonConstants);
        if (folded != null)
            rebuilt.add(new ValueNode(folded));

        return rebuildChain(rebuilt, operatorValue);
    }

    private boolean isNeutral(String value, OperatorValue operatorValue) {
        if (operatorValue == OperatorValue.PLUS)
            return value.equals(NumberConstant.ZERO);

        if (operatorValue == OperatorValue.MULTIPLY)
            return value.equals(NumberConstant.ONE);

        return false;
    }

    private String joinOperands(List<TokenNode> operands, OperatorValue operatorValue) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < operands.size(); i++) {
            if (i > 0)
                builder.append(" ").append(OperatorValue.toString(operatorValue)).append(" ");

            builder.append(operands.get(i).getToken().value());
        }

        return builder.toString();
    }

    private TokenNode rebuildChain(List<TokenNode> operands, OperatorValue operatorValue) {
        if (operands.isEmpty())
            return new ValueNode(new Token(NumberConstant.ZERO, TokenType.INTEGER, 0));

        Token operatorToken = new Token(OperatorValue.toString(operatorValue), TokenType.OPERATOR, 0);
        TokenNode result = operands.getFirst();
        for (int i = 1; i < operands.size(); i++)
            result = new OperationNode(operatorToken, result, operands.get(i));

        return result;
    }

    private TokenNode processOperation(OperationNode node, OperatorValue operatorValue) {
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