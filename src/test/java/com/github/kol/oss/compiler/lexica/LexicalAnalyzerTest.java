package com.github.kol.oss.compiler.lexica;

import com.github.kol.oss.compiler.exception.GroupedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LexicalAnalyzerTest {
    @DisplayName("Positive lexical cases")
    @ParameterizedTest
    @ValueSource(strings = {
            "1",
            "1.5",
            "x + 10",
            "Pi * 2",
            "sin(x)",
            "(1 / 2) * 3",
            "sqrt(x^2 + y^2)"
    })
    public void givenValidLexica_whenTokenizing_thenReturnTokens(String expression) {
        LexicalAnalyzer analyzer = new LexicalAnalyzer();
        assertDoesNotThrow(() -> analyzer.analyze(expression));
    }

    @DisplayName("Positive lexical, but negative syntax cases")
    @ParameterizedTest
    @ValueSource(strings = {
            "x y",
            "()",
            "a * / b",
            "(1 + (2.9)"
    })
    public void givenInvalidSyntax_whenTokenizing_thenReturnTokens(String expression) {
        LexicalAnalyzer analyzer = new LexicalAnalyzer();
        assertDoesNotThrow(() -> analyzer.analyze(expression));
    }

    @DisplayName("Negative lexical cases")
    @ParameterizedTest
    @ValueSource(strings = {
            "1abc",
            "123abc",
            "hello_world14",
            "10.1.100 + 45"
    })
    public void givenInvalidLexica_whenTokenizing_thenThrowsLexical(String expression) {
        LexicalAnalyzer analyzer = new LexicalAnalyzer();
        assertThrows(GroupedException.class, () -> analyzer.analyze(expression));
    }
}
