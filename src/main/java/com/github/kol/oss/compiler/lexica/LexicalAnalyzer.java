package com.github.kol.oss.compiler.lexica;

import com.github.kol.oss.compiler.constant.LexicalRegex;
import com.github.kol.oss.compiler.exception.LexicalException;
import com.github.kol.oss.compiler.token.Token;
import com.github.kol.oss.compiler.token.TokenBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LexicalAnalyzer {
    private LexicalState state = LexicalState.UNKNOWN;
    private TokenBuilder tokenBuilder;
    private int index;

    private final Map<String, Consumer<Character>> processors = Map.of(
            LexicalRegex.SPACE, this::processEmpty,
            LexicalRegex.NUMBER, this::processNumber,
            LexicalRegex.FRACTION, this::processFraction,
            LexicalRegex.OPERATOR, this::processOperator,
            LexicalRegex.STRING, this::processString
    );

    private boolean isDebug = false;

    public boolean toggleDebug() {
        isDebug = !isDebug;
        return isDebug;
    }

    private void formToken() {
        tokenBuilder.createToken(state);
        state = LexicalState.UNKNOWN;
    }

    // empty symbols (like space)
    private void processEmpty(char symbol) {
        formToken();
    }

    // integer numbers
    private void processNumber(char symbol) {
        if (state == LexicalState.STRING)
            throw new LexicalException(index, "Can not create word literals that contains numbers");

        if (state == LexicalState.OPERATOR)
            formToken();

        tokenBuilder.append(symbol);

        if (state == LexicalState.UNKNOWN && Character.isDigit(symbol))
            state = LexicalState.NUMBER;
    }

    // decimal numbers
    private void processFraction(char symbol) {
        if (state != LexicalState.NUMBER)
            throw new LexicalException(index, "Fraction symbol (.) can not be placed when already in " + state + " state");

        tokenBuilder.append(symbol);
        state = LexicalState.FRACTION;
    }

    // operators
    private void processOperator(char symbol) {
        formToken();

        tokenBuilder.append(symbol);
        state = LexicalState.OPERATOR;
    }

    // strings
    private void processString(char symbol) {
        if (state == LexicalState.NUMBER || state == LexicalState.FRACTION) {
            throw new LexicalException(index, "Can not create word literals that contains numbers");
        }

        if (state != LexicalState.UNKNOWN && state != LexicalState.STRING)
            formToken();

        tokenBuilder.append(symbol);
        state = LexicalState.STRING;
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
        tokenBuilder = new TokenBuilder(isDebug);

        state = LexicalState.UNKNOWN;
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
