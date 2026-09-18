package com.github.kol.oss.compiler.lexer;

import com.github.kol.oss.compiler.constant.LexicalRegex;
import com.github.kol.oss.compiler.exception.LexicalException;
import com.github.kol.oss.compiler.token.Token;
import com.github.kol.oss.compiler.token.TokenBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LexicalAnalyzer {
    private LexerState state = LexerState.UNKNOWN;
    private TokenBuilder tokenBuilder;

    private int index;

    private final Map<String, Consumer<Character>> processors = Map.of(
            LexicalRegex.SPACE, this::processEmpty,
            LexicalRegex.NUMBER, this::processNumber,
            LexicalRegex.FRACTION, this::processFraction,
            LexicalRegex.OPERATOR, this::processOperator,
            LexicalRegex.STRING, this::processString
    );

    private void formToken() {
        tokenBuilder.createToken(state);
        state = LexerState.UNKNOWN;
    }

    private void processEmpty(char symbol) {
        formToken();
    }

    private void processNumber(char symbol) {
        if (state == LexerState.WORD || state == LexerState.OPERATOR)
            formToken();

        tokenBuilder.append(symbol);

        if (state == LexerState.UNKNOWN && Character.isDigit(symbol))
            state = LexerState.INT;
    }

    private void processFraction(char symbol) {
        if (state != LexerState.INT)
            throw new LexicalException(index, "Fraction symbol (.) can not be placed when already in " + state + " state");

        tokenBuilder.append(symbol);
        state = LexerState.FRACTION;
    }

    private void processOperator(char symbol) {
        formToken();

        tokenBuilder.append(symbol);
        state = LexerState.OPERATOR;
    }

    private void processString(char symbol) {
        if (state == LexerState.INT || state == LexerState.FRACTION) {
            throw new LexicalException(index, "Can not create word literals that contains numbers");
        }

        if (state != LexerState.UNKNOWN && state != LexerState.WORD)
            formToken();

        tokenBuilder.append(symbol);
        state = LexerState.WORD;
    }

    private boolean processSymbol(char symbol) {
        for (Map.Entry<String, Consumer<Character>> entry : processors.entrySet()) {
            String regex = entry.getKey();

            if (String.valueOf(symbol).matches(regex)) {
                entry.getValue().accept(symbol);
                return true;
            }
        }

        return false;
    }

    public List<Token> analyze(String expression) {
        state = LexerState.UNKNOWN;
        tokenBuilder = new TokenBuilder();
        index = 0;

        for (char symbol : expression.toCharArray()) {
            boolean isProcessed = processSymbol(symbol);
            if (!isProcessed)
                throw new LexicalException(index, "Symbol " + symbol + " can not be processed because it's signature is unknown");

            index++;
        }

        formToken();
        return tokenBuilder.getTokens();
    }
}
