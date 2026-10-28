package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.exception.ZeroDivisionException;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.pipeline.TreeTransformationService;
import com.github.kol.oss.compiler.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OptimizationService extends TreeTransformationService {
    @Override
    public String name() {
        return TransformationService.OPTIMIZATIONS;
    }

    @Override
    protected void rewriteChain(OperationNode operation, Context context) {
        Chain chain = ChainAssembler.assemble(operation, context::resolve);
        List<Term> terms = chain.terms();
        boolean sum = chain.family() == OperatorValue.PLUS;

        if (!sum && hasDivisorZero(terms)) {
            reportDivisorZero(terms, context);
            context.replace(operation, rebuild(chain.family(), terms));
            return;
        }

        if (!sum && hasZeroNumerator(terms)) {
            TokenNode zero = zeroValue(terms);
            context.replace(operation, zero);
            context.record(operation, zero);
            return;
        }

        boolean cancelled = false;

        List<Term> reduced = cancelOpposites(terms);
        if (reduced.size() < terms.size()) {
            cancelled = true;

            if (reduced.isEmpty()) {

                TokenNode collapsed = sum ? cancelValue(terms) : oneValue(terms);
                context.replace(operation, collapsed);
                context.record(operation, collapsed);
                return;
            }

            terms = reduced;
        }

        List<Term> constants = new ArrayList<>();
        List<Term> others = new ArrayList<>();
        for (Term term : terms) {
            if (isNumericValue(term.node()))
                constants.add(term);
            else
                others.add(term);
        }

        if (constants.isEmpty()) {
            TokenNode rebuilt = rebuild(chain.family(), terms);
            context.replace(operation, rebuilt);

            if (cancelled)
                context.record(operation, rebuilt);

            return;
        }

        Token folded = foldConstants(constants, sum);
        if (others.isEmpty()) {

            TokenNode value = NodeFactory.value(folded);
            context.replace(operation, value);
            context.record(operation, value);
            return;
        }

        boolean safeDrop = isNeutral(folded, sum) && others.getFirst().sign() == TermSign.POSITIVE;

        if (constants.size() == 1 && !safeDrop) {
            TokenNode rebuilt = rebuild(chain.family(), terms);
            context.replace(operation, rebuilt);

            if (cancelled)
                context.record(operation, rebuilt);

            return;
        }

        List<Term> optimized = safeDrop ? others : replaceFirstConstant(terms, folded, sum);
        TokenNode rebuilt = rebuild(chain.family(), optimized);

        context.replace(operation, rebuilt);
        context.record(operation, rebuilt);
    }

    @Override
    protected void rewriteOperation(OperationNode operation, Context context) {
        TokenNode left = context.resolve(operation.getLeft());
        TokenNode right = context.resolve(operation.getRight());
        OperatorValue operator = TokenMapper.tokenToOperator(operation.getToken());

        if (right instanceof ValueNode rightValue && isZero(rightValue) && ArithmeticUtils.isDivisionLike(operator)) {
            reportDivisorZero(rightValue, context);
        } else if (left instanceof ValueNode leftValue && right instanceof ValueNode rightValue) {

            Optional<Token> folded = ArithmeticUtils.applyOperation(
                    leftValue.getToken(),
                    rightValue.getToken(),
                    operator
            );

            if (folded.isPresent()) {
                TokenNode value = NodeFactory.value(folded.get());
                context.replace(operation, value);
                context.record(operation, value);
                return;
            }
        }

        if (left != operation.getLeft() || right != operation.getRight())
            context.replace(operation, NodeFactory.operation(operation.getToken(), left, right));
    }

    private List<Term> cancelOpposites(List<Term> terms) {
        List<Term> result = new ArrayList<>();
        boolean[] consumed = new boolean[terms.size()];

        for (int i = 0; i < terms.size(); i++) {
            if (consumed[i])
                continue;

            int pair = findOpposite(terms, consumed, i);

            if (pair >= 0) {
                consumed[i] = true;
                consumed[pair] = true;
                continue;
            }

            result.add(terms.get(i));
        }

        return result;
    }

    private int findOpposite(List<Term> terms, boolean[] consumed, int index) {
        Term term = terms.get(index);

        for (int i = index + 1; i < terms.size(); i++)
            if (!consumed[i] && terms.get(i).sign() != term.sign() && TreeWalker.same(term.node(), terms.get(i).node()))
                return i;

        return -1;
    }

    private TokenNode zeroValue(List<Term> terms) {
        Token token = terms.stream()
                .filter(term -> isZero(term.node()))
                .map(term -> term.node().getToken())
                .findFirst()
                .orElseGet(this::zeroToken);

        return NodeFactory.value(token);
    }

    private TokenNode cancelValue(List<Term> terms) {
        Token token = new Token(NumberConstant.ZERO, TokenType.INTEGER, terms.getFirst().node().getToken().position());
        return NodeFactory.value(token);
    }

    private TokenNode oneValue(List<Term> terms) {
        Token token = new Token(NumberConstant.ONE, TokenType.INTEGER, terms.getFirst().node().getToken().position());
        return NodeFactory.value(token);
    }

    private List<Term> replaceFirstConstant(List<Term> terms, Token folded, boolean sum) {
        List<Term> result = new ArrayList<>();
        boolean inserted = false;

        for (Term term : terms) {
            if (isNumericValue(term.node()) && !inserted) {
                inserted = true;
                result.add(foldedTerm(folded, sum));
                continue;
            }

            if (!isNumericValue(term.node()))
                result.add(term);
        }

        return result;
    }

    private Term foldedTerm(Token folded, boolean sum) {
        boolean negative = sum && folded.value().startsWith(OperatorValue.MINUS.toSymbol());

        if (!negative)
            return new Term(NodeFactory.value(folded), TermSign.POSITIVE);

        Token absolute = ArithmeticUtils.applyOperation(zeroToken(), folded, OperatorValue.MINUS).orElseThrow();
        return new Term(NodeFactory.value(absolute), TermSign.NEGATIVE);
    }

    private Token foldConstants(List<Term> constants, boolean sum) {
        OperatorValue positiveJoin = sum ? OperatorValue.PLUS : OperatorValue.MULTIPLY;
        OperatorValue negativeJoin = sum ? OperatorValue.MINUS : OperatorValue.DIVISION;

        Token folded = constants.getFirst().node().getToken();
        if (constants.getFirst().sign() == TermSign.NEGATIVE) {
            Token identity = sum ? zeroToken() : oneToken();
            folded = ArithmeticUtils.applyOperation(identity, folded, negativeJoin).orElseThrow();
        }

        for (int i = 1; i < constants.size(); i++) {
            Term term = constants.get(i);
            OperatorValue join = term.sign() == TermSign.POSITIVE ? positiveJoin : negativeJoin;
            folded = ArithmeticUtils.applyOperation(folded, term.node().getToken(), join).orElseThrow();
        }

        return folded;
    }

    private boolean hasDivisorZero(List<Term> terms) {
        return terms.stream()
                .filter(term -> term.sign() == TermSign.NEGATIVE)
                .anyMatch(term -> isZero(term.node()));
    }

    private boolean hasZeroNumerator(List<Term> terms) {
        return terms.stream()
                .filter(term -> term.sign() == TermSign.POSITIVE)
                .anyMatch(term -> isZero(term.node()));
    }

    private void reportDivisorZero(List<Term> terms, Context context) {
        for (Term term : terms) {
            if (term.sign() != TermSign.NEGATIVE || !isZero(term.node()))
                continue;

            reportDivisorZero(term.node(), context);
            return;
        }
    }

    private void reportDivisorZero(TokenNode node, Context context) {
        PositionedException exception = new ZeroDivisionException();
        exception.setIndex(node.getToken().position());
        context.report(exception);
    }

    private boolean isNumericValue(TokenNode node) {
        return node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken());
    }

    private boolean isZero(TokenNode node) {
        return node instanceof ValueNode value && ArithmeticUtils.isZero(value.getToken());
    }

    private boolean isZero(ValueNode node) {
        return ArithmeticUtils.isZero(node.getToken());
    }

    private boolean isNeutral(Token folded, boolean sum) {
        return sum ? ArithmeticUtils.isZero(folded) : ArithmeticUtils.isOne(folded);
    }

    private TokenNode rebuild(OperatorValue family, List<Term> terms) {
        List<Term> full = terms;

        if (terms.getFirst().sign() == TermSign.NEGATIVE) {
            Token anchor = family == OperatorValue.PLUS ? zeroToken() : oneToken();
            full = new ArrayList<>(terms);
            full.addFirst(new Term(NodeFactory.value(anchor), TermSign.POSITIVE));
        }

        TokenNode result = full.getFirst().node();
        for (int i = 1; i < full.size(); i++) {
            Term term = full.get(i);
            OperatorValue join = join(family, term.sign());
            result = NodeFactory.operation(join, result, term.node());
        }

        return result;
    }

    private OperatorValue join(OperatorValue family, TermSign sign) {
        boolean positive = sign == TermSign.POSITIVE;

        if (family == OperatorValue.PLUS)
            return positive ? OperatorValue.PLUS : OperatorValue.MINUS;

        return positive ? OperatorValue.MULTIPLY : OperatorValue.DIVISION;
    }

    private Token zeroToken() {
        return new Token(NumberConstant.ZERO, TokenType.INTEGER, 0);
    }

    private Token oneToken() {
        return new Token(NumberConstant.ONE, TokenType.INTEGER, 0);
    }
}
