package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LexicalAnalysisServiceTest {
    private LexicalAnalysisService lexicalAnalysisService;

    private static Stream<Arguments> provideNumbersCases() {
        return Stream.of(
                Arguments.of("5", List.of(
                        new Token("5", TokenType.INTEGER, 0)
                )),
                Arguments.of("123", List.of(
                        new Token("123", TokenType.INTEGER, 0)
                )),
                Arguments.of("5.2", List.of(
                        new Token("5.2", TokenType.FLOAT, 0)
                )),
                Arguments.of("0.01", List.of(
                        new Token("0.01", TokenType.FLOAT, 0)
                ))
        );
    }

    private static Stream<Arguments> provideVariablesCases() {
        return Stream.of(
                Arguments.of("a", List.of(
                        new Token("a", TokenType.VARIABLE, 0)
                )),
                Arguments.of("abc", List.of(
                        new Token("abc", TokenType.VARIABLE, 0)
                )),
                Arguments.of("a1", List.of(
                        new Token("a1", TokenType.VARIABLE, 0)
                )),
                Arguments.of("a_b", List.of(
                        new Token("a_b", TokenType.VARIABLE, 0)
                )),
                Arguments.of("A", List.of(
                        new Token("A", TokenType.VARIABLE, 0)
                ))
        );
    }

    private static Stream<Arguments> provideFunctionsCases() {
        return Stream.of(
                Arguments.of("sin()", List.of(
                        new Token("sin", TokenType.FUNCTION, 0),
                        new Token("(", TokenType.LPAREN, 3),
                        new Token(")", TokenType.RPAREN, 4)
                )),
                Arguments.of("func(a)", List.of(
                        new Token("func", TokenType.FUNCTION, 0),
                        new Token("(", TokenType.LPAREN, 4),
                        new Token("a", TokenType.VARIABLE, 5),
                        new Token(")", TokenType.RPAREN, 6)
                )),
                Arguments.of("sin(x)", List.of(
                        new Token("sin", TokenType.FUNCTION, 0),
                        new Token("(", TokenType.LPAREN, 3),
                        new Token("x", TokenType.VARIABLE, 4),
                        new Token(")", TokenType.RPAREN, 5)
                )),
                Arguments.of("f(a,b)", List.of(
                        new Token("f", TokenType.FUNCTION, 0),
                        new Token("(", TokenType.LPAREN, 1),
                        new Token("a", TokenType.VARIABLE, 2),
                        new Token(",", TokenType.COMMA, 3),
                        new Token("b", TokenType.VARIABLE, 4),
                        new Token(")", TokenType.RPAREN, 5)
                ))
        );
    }

    private static Stream<Arguments> provideOperatorsCases() {
        return Stream.of(
                Arguments.of("a+b", List.of(
                        new Token("a", TokenType.VARIABLE, 0),
                        new Token("+", TokenType.OPERATOR, 1),
                        new Token("b", TokenType.VARIABLE, 2)
                )),
                Arguments.of("a-b", List.of(
                        new Token("a", TokenType.VARIABLE, 0),
                        new Token("-", TokenType.OPERATOR, 1),
                        new Token("b", TokenType.VARIABLE, 2)
                )),
                Arguments.of("a*b", List.of(
                        new Token("a", TokenType.VARIABLE, 0),
                        new Token("*", TokenType.OPERATOR, 1),
                        new Token("b", TokenType.VARIABLE, 2)
                )),
                Arguments.of("a/b", List.of(
                        new Token("a", TokenType.VARIABLE, 0),
                        new Token("/", TokenType.OPERATOR, 1),
                        new Token("b", TokenType.VARIABLE, 2)
                )),
                Arguments.of("a^b", List.of(
                        new Token("a", TokenType.VARIABLE, 0),
                        new Token("^", TokenType.OPERATOR, 1),
                        new Token("b", TokenType.VARIABLE, 2)
                )),
                Arguments.of("1+2*3", List.of(
                        new Token("1", TokenType.INTEGER, 0),
                        new Token("+", TokenType.OPERATOR, 1),
                        new Token("2", TokenType.INTEGER, 2),
                        new Token("*", TokenType.OPERATOR, 3),
                        new Token("3", TokenType.INTEGER, 4)
                )),
                Arguments.of("a + b - c", List.of(
                        new Token("a", TokenType.VARIABLE, 0),
                        new Token("+", TokenType.OPERATOR, 2),
                        new Token("b", TokenType.VARIABLE, 4),
                        new Token("-", TokenType.OPERATOR, 6),
                        new Token("c", TokenType.VARIABLE, 8)
                ))
        );
    }

    private static Stream<Arguments> provideParenthesesCases() {
        return Stream.of(
                Arguments.of("(a+b)", List.of(
                        new Token("(", TokenType.LPAREN, 0),
                        new Token("a", TokenType.VARIABLE, 1),
                        new Token("+", TokenType.OPERATOR, 2),
                        new Token("b", TokenType.VARIABLE, 3),
                        new Token(")", TokenType.RPAREN, 4)
                ))
        );
    }

    private static Stream<Arguments> provideUnaryMinusCases() {
        return Stream.of(
                Arguments.of("-a", List.of(
                        new Token("0", TokenType.INTEGER, 0),
                        new Token("-", TokenType.OPERATOR, 0),
                        new Token("a", TokenType.VARIABLE, 1)
                )),
                Arguments.of("a*-b", List.of(
                        new Token("a", TokenType.VARIABLE, 0),
                        new Token("*", TokenType.OPERATOR, 1),
                        new Token("(", TokenType.LPAREN, 2),
                        new Token("0", TokenType.INTEGER, 2),
                        new Token("-", TokenType.OPERATOR, 2),
                        new Token("b", TokenType.VARIABLE, 3),
                        new Token(")", TokenType.RPAREN, 2)
                ))
        );
    }

    @BeforeEach
    public void setUp() {
        lexicalAnalysisService = new LexicalAnalysisService();
    }

    @ParameterizedTest
    @MethodSource("provideNumbersCases")
    public void givenNumber_whenTokenize_thenIntegerOrFloatToken(String expression, List<Token> expectedTokens) {
        assertTokens(expression, expectedTokens);
    }

    @ParameterizedTest
    @MethodSource("provideVariablesCases")
    public void givenName_whenTokenize_thenVariableToken(String expression, List<Token> expectedTokens) {
        assertTokens(expression, expectedTokens);
    }

    @ParameterizedTest
    @MethodSource("provideFunctionsCases")
    public void givenFunctionCall_whenTokenize_thenFunctionToken(String expression, List<Token> expectedTokens) {
        assertTokens(expression, expectedTokens);
    }

    @ParameterizedTest
    @MethodSource("provideOperatorsCases")
    public void givenOperators_whenTokenize_thenOperatorTokens(String expression, List<Token> expectedTokens) {
        assertTokens(expression, expectedTokens);
    }

    @ParameterizedTest
    @MethodSource("provideParenthesesCases")
    public void givenParentheses_whenTokenize_thenParenthesisTokens(String expression, List<Token> expectedTokens) {
        assertTokens(expression, expectedTokens);
    }

    @ParameterizedTest
    @MethodSource("provideUnaryMinusCases")
    public void givenUnaryMinus_whenTokenize_thenZeroMinusOperandTokens(String expression, List<Token> expectedTokens) {
        assertTokens(expression, expectedTokens);
    }

    private void assertTokens(String expression, List<Token> expectedTokens) {
        List<Token> actualTokens = lexicalAnalysisService.tokenize(expression).tokens();

        assertEquals(expectedTokens.size(), actualTokens.size());

        for (int i = 0; i < expectedTokens.size(); i++) {
            assertEquals(expectedTokens.get(i).value(), actualTokens.get(i).value());
            assertEquals(expectedTokens.get(i).type(), actualTokens.get(i).type());
            assertEquals(expectedTokens.get(i).position(), actualTokens.get(i).position());
        }
    }
}
