package com.github.kol.oss.compiler.lexica;

import com.github.kol.oss.compiler.exception.SyntaxException;
import com.github.kol.oss.compiler.syntax.SyntaxAnalyzer;
import com.github.kol.oss.compiler.token.Token;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SyntaxAnalyzerTest {
    @DisplayName("Positive syntax cases")
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
    public void givenValidSyntax_whenAnalyzing_thenReturnNothing(String expression) {
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        List<Token> tokens = lexicalAnalyzer.analyze(expression);

        SyntaxAnalyzer analyzer = new SyntaxAnalyzer();
        assertDoesNotThrow(() -> analyzer.analyze(tokens));
    }

    @DisplayName("Negative syntax cases")
    @ParameterizedTest
    @ValueSource(strings = {
            "x y",
            "a * / b",
            "(1 + (2.9)",
            "1++2",
            "1*/2",
            "1 2",
            "1(2)",
            "(1)2",
            "()",
            "(1",
            "1)",
            "((1)",
            "(1))",
            "(1 + (2)",
            "sinx(1)"
    })
    public void givenInvalidSyntax_whenAnalyzing_thenThrowsSyntax(String expression) {
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        List<Token> tokens = lexicalAnalyzer.analyze(expression);

        SyntaxAnalyzer analyzer = new SyntaxAnalyzer();
        assertThrows(SyntaxException.class, () -> analyzer.analyze(tokens));
    }
}
