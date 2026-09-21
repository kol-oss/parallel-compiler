package com.github.kol.oss.compiler.syntax;

import com.github.kol.oss.compiler.constant.SyntaxTransitions;
import com.github.kol.oss.compiler.exception.GroupedException;
import com.github.kol.oss.compiler.exception.SyntaxException;
import com.github.kol.oss.compiler.token.Token;
import com.github.kol.oss.compiler.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class SyntaxAnalyzer {
    private TokenType currentType = TokenType.START;
    private int parenthesisCount = 0;

    private List<SyntaxException> exceptions;

    private void checkTransition(Token token, TokenType nextType) {
        if (!SyntaxTransitions.isAllowedTransition(currentType, nextType)) {
            SyntaxException exception = new SyntaxException(token, "Not allowed transition from " + currentType + " into " + nextType);
            exceptions.add(exception);
        }
    }

    private void processToken(Token token) {
        TokenType nextType = token.type();

        if (nextType == TokenType.LPAREN) {
            parenthesisCount++;
        } else if (nextType == TokenType.RPAREN) {
            parenthesisCount--;

            if (parenthesisCount < 0) {
                SyntaxException exception = new SyntaxException(token, "Wrongly positioned or unnecessary closing parenthesis");
                exceptions.add(exception);
            }
        }

        checkTransition(token, nextType);
    }

    public void analyze(List<Token> tokens) {
        currentType = TokenType.START;
        parenthesisCount = 0;

        exceptions = new ArrayList<>();

        for (Token token : tokens) {
            processToken(token);
            currentType = token.type();
        }

        // checking the last token
        checkTransition(tokens.getLast(), TokenType.END);

        if (parenthesisCount > 0) {
            SyntaxException exception = new SyntaxException(null, "Expected more closing parenthesis (" + parenthesisCount + ")");
            exceptions.add(exception);
        }

        if (!exceptions.isEmpty())
            throw new GroupedException(exceptions);
    }
}
