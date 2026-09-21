package com.github.kol.oss.compiler.lexica;

import com.github.kol.oss.compiler.constant.LexicalRegex;
import com.github.kol.oss.compiler.exception.GroupedException;
import com.github.kol.oss.compiler.exception.LexicalException;
import com.github.kol.oss.compiler.token.Token;
import com.github.kol.oss.compiler.token.TokenBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LexicalAnalyzer {
    private LexicalState state = LexicalState.UNKNOWN;
    private TokenBuilder tokenBuilder;

    private List<LexicalException> exceptions = new ArrayList<>();
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

    // empty symbols
    private void processEmpty(char symbol) {
        formToken();
    }

    // integer numbers
    private void processNumber(char symbol) {
        if (state == LexicalState.STRING) {
            processError("Can not create word literals that contains numbers");
            return;
        }

        if (state == LexicalState.OPERATOR)
            formToken();

        tokenBuilder.append(symbol);

        if (state == LexicalState.UNKNOWN && Character.isDigit(symbol))
            state = LexicalState.NUMBER;
    }

    // decimal numbers
    private void processFraction(char symbol) {
        if (state != LexicalState.NUMBER) {
            processError("Fraction symbol (.) can not be placed when analyzer in " + state + " state");
            return;
        }

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
            processError("Can not create word literals that contains numbers");
            return;
        }

        if (state != LexicalState.UNKNOWN && state != LexicalState.STRING)
            formToken();

        tokenBuilder.append(symbol);
        state = LexicalState.STRING;
    }

    // exception
    private void processError(String message) {
        LexicalException exception = new LexicalException(index, message);
        exceptions.add(exception);

        tokenBuilder.clearToken();
        state = LexicalState.UNKNOWN;
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

    private void clear() {
        tokenBuilder = new TokenBuilder(isDebug);
        state = LexicalState.UNKNOWN;

        index = 0;
        exceptions = new ArrayList<>();
    }

    public List<Token> analyze(String expression) {
        clear();

        for (char symbol : expression.toCharArray()) {
            boolean isProcessed = processSymbol(symbol);
            if (!isProcessed)
                processError("Symbol " + symbol + " can not be processed because it's signature is unknown");

            index++;
        }

        formToken();
        if (!exceptions.isEmpty()) {
            throw new GroupedException(exceptions);
        }

        return tokenBuilder.getTokens();
    }
}
