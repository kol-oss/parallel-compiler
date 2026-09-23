package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.SyntaxProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApplicationTest {
    static Stream<String> providePositiveCases() {
        return Stream.of(
                "a + b * c",
                "x1 - 42 + value_2",
                "-temperature + 15.5",
                "sum(10, 20) * average(x, y)",
                "(a + b) * (c - d)",
                "value / 2.5 + offset",
                "foo((a + 3) * b)",
                "x * (y + (z - 10))",
                "count_1 + count_2 - count_3 * 100",
                "((a + b) / c) - (d * e)",
                "price * quantity + tax",
                "pow(x, 2) + sqrt(y)",
                "func(a, b + c, multiply(x, y))",
                "((x)) + (((y)))",
                "total / (items + 1) * discount",
                "-calculate(12.5, (x * 4) + 7, result)",
                "process(10, (1 + compute(var_1, 2))) / 10"
        );
    }

    static Stream<Arguments> provideNegativeCases() {
        return Stream.of(
                Arguments.of(
                        "*a + nb -",
                        List.of(0, 8)
                ),
                Arguments.of(
                        "a ++ nb /* k -+/ g",
                        List.of(3, 9, 14, 15)
                ),
                Arguments.of(
                        "a^b$c - d#h + q%t + !b&(z|t)",
                        List.of(3, 4, 9, 10, 20)
                ),
                Arguments.of(
                        "**f(*k, -p+1, ))2.1.1 + 1.8q((-5x ++ i)",
                        List.of(0, 1, 4, 8, 14, 15, 16, 19, 20, 30, 35)
                ),
                Arguments.of(
                        "a-+(t*5.81.8-))/",
                        List.of(2, 10, 11, 13, 14, 15)
                ),
                Arguments.of(
                        "-a++b-2v*func((t+2-,sin(x/*2.01.2),)/8(-)**",
                        List.of(3, 19, 19, 26, 31, 32, 34, 35, 38, 39, 40, 42, 42, 42)
                )
        );
    }

    @ParameterizedTest
    @DisplayName("Positive cases")
    @MethodSource("providePositiveCases")
    public void givenPositiveCases_whenProcessed_thenReturnNoExceptions(String expression) {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        LexicalProcessor lexicalProcessor = new LexicalProcessor(exceptionHandler);
        SyntaxProcessor syntaxProcessor = new SyntaxProcessor(exceptionHandler);

        List<Token> tokens = lexicalProcessor.process(expression);
        syntaxProcessor.process(tokens);

        List<PositionedException> exceptions = exceptionHandler.getAllAndClear();
        assertEquals(0, exceptions.size());
    }

    @ParameterizedTest
    @DisplayName("Negative cases")
    @MethodSource("provideNegativeCases")
    public void givenNegativeCases_whenProcessed_thenReturnExceptions(String expression, List<Integer> errorIndexes) {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        LexicalProcessor lexicalProcessor = new LexicalProcessor(exceptionHandler);
        SyntaxProcessor syntaxProcessor = new SyntaxProcessor(exceptionHandler);

        List<Token> tokens = lexicalProcessor.process(expression);
        syntaxProcessor.process(tokens);

        List<PositionedException> exceptions = exceptionHandler.getAllAndClear();
        List<Integer> indexes = exceptions.stream().map(PositionedException::getIndex).toList();

        assertEquals(errorIndexes, indexes);
    }
}
