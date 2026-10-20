package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

public class ArithmeticUtils {
    private static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL64;
    private static final int MAX_EXACT_EXPONENT = 1_000;

    public static boolean isNumeric(Token token) {
        return parse(token).isPresent();
    }

    public static boolean isZero(Token token) {
        return parse(token).map(value -> value.signum() == 0).orElse(false);
    }

    public static boolean isOne(Token token) {
        return parse(token).map(value -> value.compareTo(BigDecimal.ONE) == 0).orElse(false);
    }

    public static boolean isDivisionLike(OperatorValue operatorValue) {
        return operatorValue == OperatorValue.DIVISION || operatorValue == OperatorValue.MODULO;
    }

    public static Optional<Token> applyOperation(Token leftToken, Token rightToken, OperatorValue operatorValue) {
        Optional<BigDecimal> left = parse(leftToken);
        Optional<BigDecimal> right = parse(rightToken);

        if (left.isEmpty() || right.isEmpty())
            return Optional.empty();

        Optional<BigDecimal> result = calculate(left.get(), right.get(), operatorValue);
        if (result.isEmpty())
            return Optional.empty();

        BigDecimal normalized = result.get().stripTrailingZeros();
        TokenType type = normalized.scale() <= 0 ? TokenType.INTEGER : TokenType.FLOAT;

        return Optional.of(new Token(normalized.toPlainString(), type, leftToken.position()));
    }

    private static Optional<BigDecimal> parse(Token token) {
        try {
            return Optional.of(new BigDecimal(token.value()));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    private static Optional<BigDecimal> calculate(BigDecimal left, BigDecimal right, OperatorValue operatorValue) {
        return switch (operatorValue) {
            case PLUS -> Optional.of(left.add(right));
            case MINUS -> Optional.of(left.subtract(right));
            case MULTIPLY -> Optional.of(left.multiply(right));
            case DIVISION -> divide(left, right);
            case MODULO -> right.signum() == 0 ? Optional.empty() : Optional.of(left.remainder(right));
            case EXPONENTIATION -> power(left, right);
            case AND -> Optional.of(truth(left) && truth(right) ? BigDecimal.ONE : BigDecimal.ZERO);
            case OP -> Optional.of(truth(left) || truth(right) ? BigDecimal.ONE : BigDecimal.ZERO);
        };
    }

    private static Optional<BigDecimal> divide(BigDecimal left, BigDecimal right) {
        if (right.signum() == 0)
            return Optional.empty();

        try {
            return Optional.of(left.divide(right));
        } catch (ArithmeticException _) {
            return Optional.of(left.divide(right, DIVISION_CONTEXT));
        }
    }

    private static Optional<BigDecimal> power(BigDecimal left, BigDecimal right) {
        BigDecimal exponent = right.stripTrailingZeros();

        if (exponent.scale() > 0 || exponent.abs().compareTo(BigDecimal.valueOf(MAX_EXACT_EXPONENT)) > 0)
            return approximatePower(left, right);

        int value = exponent.intValueExact();
        if (value >= 0)
            return Optional.of(left.pow(value));

        return divide(BigDecimal.ONE, left.pow(-value));
    }

    private static Optional<BigDecimal> approximatePower(BigDecimal left, BigDecimal right) {
        double result = Math.pow(left.doubleValue(), right.doubleValue());
        if (!Double.isFinite(result))
            return Optional.empty();

        return Optional.of(BigDecimal.valueOf(result));
    }

    private static boolean truth(BigDecimal value) {
        return value.signum() != 0;
    }
}
