package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.Optional;
import java.util.OptionalDouble;

public class ArithmeticUtils {
    public static boolean isNumeric(Token token) {
        return parse(token).isPresent();
    }

    public static Optional<Token> applyOperation(Token leftToken, Token rightToken, OperatorValue operatorValue) {
        OptionalDouble left = parse(leftToken);
        OptionalDouble right = parse(rightToken);

        if (left.isEmpty() || right.isEmpty())
            return Optional.empty();

        if (operatorValue == OperatorValue.AND || operatorValue == OperatorValue.OP)
            return Optional.empty();

        double result = calculate(left.getAsDouble(), right.getAsDouble(), operatorValue);
        boolean isInteger = result == (long) result;

        return Optional.of(new Token(
                isInteger ? String.valueOf((long) result) : String.valueOf(result),
                isInteger ? TokenType.INTEGER : TokenType.FLOAT,
                leftToken.position()
        ));
    }

    private static OptionalDouble parse(Token token) {
        try {
            return OptionalDouble.of(Double.parseDouble(token.value()));
        } catch (NumberFormatException _) {
            return OptionalDouble.empty();
        }
    }

    private static double calculate(double left, double right, OperatorValue operatorValue) {
        return switch (operatorValue) {
            case PLUS -> left + right;
            case MINUS -> left - right;
            case MULTIPLY -> left * right;
            case DIVISION -> left / right;
            case EXPONENTIATION -> Math.pow(left, right);
            case MODULO -> left % right;
            default -> 0;
        };
    }
}