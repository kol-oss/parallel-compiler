package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.TreeProcessor;
import com.github.kol.oss.compiler.utils.ExpressionPrinter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpressionPrinterTest {
    private final TreeProcessor treeProcessor = new TreeProcessor();

    private String print(String expression) {
        List<Token> tokens = new LexicalProcessor(new ExceptionHandler()).process(expression);

        return ExpressionPrinter.print(treeProcessor.buildTree(tokens));
    }

    static Stream<Arguments> provideCases() {
        return Stream.of(
                Arguments.of("a-(b+c)", "a - (b + c)"),
                Arguments.of("a-b-c", "a - b - c"),
                Arguments.of("a/(b/c)", "a / (b / c)"),
                Arguments.of("a*(b+c)", "a * (b + c)"),
                Arguments.of("(a+b)*(c+d)", "(a + b) * (c + d)"),
                Arguments.of("2*(3+4)-5", "2 * (3 + 4) - 5"),
                Arguments.of("sin(a+b)", "sin(a + b)"),
                Arguments.of("sum(10, 20)", "sum(10, 20)"),
                Arguments.of("(a^b)^c", "(a ^ b) ^ c"),
                Arguments.of("a^b^c", "a ^ (b ^ c)")
        );
    }

    @ParameterizedTest
    @DisplayName("Infix rendering keeps only required parentheses")
    @MethodSource("provideCases")
    public void givenTree_whenPrinted_thenExpectedInfix(String expression, String expected) {
        assertEquals(expected, print(expression));
    }
}