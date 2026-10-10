package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorType;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.Optional;

public class OperatorUtils {
    private static double calculate(double left, double right, OperatorType operatorType) {
        double result = 0;
        if (operatorType == OperatorType.PLUS)
            result = left + right;
        else if (operatorType == OperatorType.MINUS)
            result = left - right;
        else if (operatorType == OperatorType.MULTIPLY)
            result = left * right;
        else if (operatorType == OperatorType.DIVISION)
            result = left / right;
        else if (operatorType == OperatorType.EXPONENTIATION)
            result = Math.pow(left, right);

        return result;
    }

    private static boolean isFloat(double value) {
        return value == (long) value;
    }

    public static Optional<Token> applyOperation(Token leftToken, Token rightToken, OperatorType operatorType) {
        double left, right;

        try {
            left = Double.parseDouble(leftToken.value());
            right = Double.parseDouble(rightToken.value());
        } catch (NumberFormatException _) {
            return Optional.empty();
        }

        double result = calculate(left, right, operatorType);
        boolean isFloat = isFloat(result);

        return Optional.of(new Token(
                isFloat ? String.valueOf((long) result) : String.valueOf(result),
                isFloat ? TokenType.FLOAT : TokenType.INTEGER,
                leftToken.position()
        ));
    }
}
