package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.Optional;

public class OperatorUtils {
    private static double calculate(double left, double right, OperatorValue operatorValue) {
        double result = 0;
        if (operatorValue == OperatorValue.PLUS)
            result = left + right;
        else if (operatorValue == OperatorValue.MINUS)
            result = left - right;
        else if (operatorValue == OperatorValue.MULTIPLY)
            result = left * right;
        else if (operatorValue == OperatorValue.DIVISION)
            result = left / right;
        else if (operatorValue == OperatorValue.EXPONENTIATION)
            result = Math.pow(left, right);
        else if (operatorValue == OperatorValue.MODULO)
            result = left % right;

        return result;
    }

    // returns true when the value has no fractional part, i.e. it is an integer
    private static boolean isInteger(double value) {
        return value == (long) value;
    }

    public static Optional<Token> applyOperation(Token leftToken, Token rightToken, OperatorValue operatorValue) {
        double left, right;

        try {
            left = Double.parseDouble(leftToken.value());
            right = Double.parseDouble(rightToken.value());
        } catch (NumberFormatException _) {
            return Optional.empty();
        }

        double result = calculate(left, right, operatorValue);
        boolean isInteger = isInteger(result);

        return Optional.of(new Token(
                isInteger ? String.valueOf((long) result) : String.valueOf(result),
                isInteger ? TokenType.INTEGER : TokenType.FLOAT,
                leftToken.position()
        ));
    }
}
