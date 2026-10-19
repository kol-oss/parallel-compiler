package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.exception.PositionedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SyntaxValidationServiceTest {
    private ExceptionHandler exceptionHandler;

    private LexicalAnalysisService lexicalAnalysisService;
    private SyntaxValidationService syntaxValidationService;

    private static Stream<Arguments> provideCorrectExpressionCases() {
        return Stream.of(
                Arguments.of("a+b"),
                Arguments.of("a-b"),
                Arguments.of("a*b"),
                Arguments.of("a/b"),
                Arguments.of("a^b"),
                Arguments.of("-a"),
                Arguments.of("(a+b)*c"),
                Arguments.of("sin(x)"),
                Arguments.of("f(a,b,c)"),
                Arguments.of("1+2*3/(4-5)^6"),
                Arguments.of("a+-b"),
                Arguments.of("-(-a)"),
                Arguments.of("2.3*4.5")
        );
    }

    private static Stream<Arguments> provideIncorrectExpressionCases() {
        return Stream.of(
                Arguments.of("*a", 1, 0),
                Arguments.of("a+", 1, 1),
                Arguments.of("(a+b", 1, 3),
                Arguments.of("a+b)", 1, 3),
                Arguments.of("a++b", 1, 2),
                Arguments.of("a*+b", 1, 2),
                Arguments.of("f(a,)", 1, 4),
                Arguments.of("f(,a)", 1, 2),
                Arguments.of("8(a)", 1, 1),
                Arguments.of("a**b", 1, 2)
        );
    }

    @BeforeEach
    public void setUp() {
        exceptionHandler = new ExceptionHandler();

        lexicalAnalysisService = new LexicalAnalysisService(exceptionHandler);
        syntaxValidationService = new SyntaxValidationService(exceptionHandler);
    }

    @ParameterizedTest
    @MethodSource("provideCorrectExpressionCases")
    public void givenCorrectExpression_whenValidate_thenNoErrors(String expression) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        List<PositionedException> errors = exceptionHandler.getAllAndClear();

        assertTrue(errors.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideIncorrectExpressionCases")
    public void givenIncorrectExpression_whenValidate_thenErrorsDetected(String expression, int expectedErrorCount, int expectedFirstErrorPosition) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        List<PositionedException> errors = exceptionHandler.getAllAndClear();

        assertFalse(errors.isEmpty());
        assertEquals(expectedErrorCount, errors.size());
        assertEquals(expectedFirstErrorPosition, errors.getFirst().getIndex());
    }
}
