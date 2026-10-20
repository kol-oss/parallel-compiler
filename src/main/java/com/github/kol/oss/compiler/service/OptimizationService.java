package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.change.Transformation;
import com.github.kol.oss.compiler.dto.change.TransformationResult;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.exception.ZeroDivisionException;
import com.github.kol.oss.compiler.utils.ArithmeticUtils;
import com.github.kol.oss.compiler.utils.ChainAssembler;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TokenMapper;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OptimizationService {
    private final ExceptionHandler exceptionHandler;

    public OptimizationService(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public TransformationResult optimize(TokenNode root) {
        Map<TokenNode, TokenNode> replacements = new IdentityHashMap<>();
        List<Transformation> applied = new ArrayList<>();
        Set<TokenNode> chainRoots = Collections.newSetFromMap(new IdentityHashMap<>());
        chainRoots.addAll(TreeWalker.collectChainRoots(root, false));

        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (node instanceof ValueNode)
                continue;

            if (node instanceof FunctionNode function) {
                optimizeFunction(function, replacements);
                continue;
            }

            OperationNode operation = (OperationNode) node;

            if (TreeWalker.chainFamily(node) != null) {
                if (!chainRoots.contains(node))
                    continue;

                optimizeChain(operation, replacements, applied);
                continue;
            }

            optimizeOperation(operation, replacements, applied);
        }

        return new TransformationResult(TreeWalker.resolve(root, replacements), applied);
    }

    private void optimizeFunction(FunctionNode function, Map<TokenNode, TokenNode> replacements) {
        List<TokenNode> arguments = function.getArguments().stream()
                .map(argument -> TreeWalker.resolve(argument, replacements))
                .toList();

        if (!arguments.equals(function.getArguments()))
            replacements.put(function, NodeFactory.function(function.getToken(), arguments));
    }

    private void optimizeChain(OperationNode operation, Map<TokenNode, TokenNode> replacements, List<Transformation> applied) {
        Chain chain = ChainAssembler.assemble(operation, replacements);
        List<Term> terms = chain.terms();
        boolean sum = chain.family() == OperatorValue.PLUS;

        if (!sum && hasDivisorZero(terms)) {
            reportDivisorZero(terms);
            replacements.put(operation, rebuild(chain.family(), terms));
            return;
        }

        if (!sum && hasZeroNumerator(terms)) {
            TokenNode zero = zeroValue(terms);
            replacements.put(operation, zero);
            applied.add(new Transformation(operation, zero));
            return;
        }

        boolean cancelled = false;
        List<Term> reduced = cancelOpposites(terms);

        if (reduced.size() < terms.size()) {
            cancelled = true;

            if (reduced.isEmpty()) {
                TokenNode collapsed = sum ? cancelValue(terms) : oneValue(terms);
                replacements.put(operation, collapsed);
                applied.add(new Transformation(operation, collapsed));
                return;
            }

            terms = reduced;
        }

        List<Term> constants = new ArrayList<>();
        List<Term> others = new ArrayList<>();
        for (Term term : terms) {
            if (term.node() instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
                constants.add(term);
            else
                others.add(term);
        }

        if (constants.isEmpty()) {
            TokenNode rebuilt = rebuild(chain.family(), terms);
            replacements.put(operation, rebuilt);

            if (cancelled)
                applied.add(new Transformation(operation, rebuilt));

            return;
        }

        Token folded = foldConstants(constants, sum);

        if (others.isEmpty()) {
            TokenNode value = NodeFactory.value(folded);
            replacements.put(operation, value);
            applied.add(new Transformation(operation, value));
            return;
        }

        boolean safeDrop = isNeutral(folded, sum) && others.getFirst().sign() == TermSign.POSITIVE;

        if (constants.size() == 1 && !safeDrop) {
            TokenNode rebuilt = rebuild(chain.family(), terms);
            replacements.put(operation, rebuilt);

            if (cancelled)
                applied.add(new Transformation(operation, rebuilt));

            return;
        }

        List<Term> optimized = safeDrop ? others : replaceFirstConstant(terms, folded, sum);
        TokenNode rebuilt = rebuild(chain.family(), optimized);

        replacements.put(operation, rebuilt);
        applied.add(new Transformation(operation, rebuilt));
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
                .filter(term -> isLiteral(term.node(), NumberConstant.ZERO))
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
            boolean constant = term.node() instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken());

            if (constant && !inserted) {
                inserted = true;
                result.add(foldedTerm(folded, sum));
                continue;
            }

            if (!constant)
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
                .anyMatch(term -> isLiteral(term.node(), NumberConstant.ZERO));
    }

    private boolean hasZeroNumerator(List<Term> terms) {
        return terms.stream()
                .filter(term -> term.sign() == TermSign.POSITIVE)
                .anyMatch(term -> isLiteral(term.node(), NumberConstant.ZERO));
    }

    private void reportDivisorZero(List<Term> terms) {
        for (Term term : terms) {
            if (term.sign() != TermSign.NEGATIVE || !isLiteral(term.node(), NumberConstant.ZERO))
                continue;

            PositionedException exception = new ZeroDivisionException();
            exception.setIndex(term.node().getToken().position());
            exceptionHandler.add(exception);
            return;
        }
    }

    private boolean isLiteral(TokenNode node, String literal) {
        return node instanceof ValueNode && node.getToken().value().equals(literal);
    }

    private boolean isNeutral(Token folded, boolean sum) {
        return folded.value().equals(sum ? NumberConstant.ZERO : NumberConstant.ONE);
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

    private void optimizeOperation(OperationNode operation, Map<TokenNode, TokenNode> replacements, List<Transformation> applied) {
        TokenNode left = TreeWalker.resolve(operation.getLeft(), replacements);
        TokenNode right = TreeWalker.resolve(operation.getRight(), replacements);

        if (left instanceof ValueNode leftValue && right instanceof ValueNode rightValue) {
            Optional<Token> folded = ArithmeticUtils.applyOperation(
                    leftValue.getToken(),
                    rightValue.getToken(),
                    TokenMapper.tokenToOperator(operation.getToken())
            );

            if (folded.isPresent()) {
                TokenNode value = NodeFactory.value(folded.get());
                replacements.put(operation, value);
                applied.add(new Transformation(operation, value));
                return;
            }
        }

        if (left != operation.getLeft() || right != operation.getRight())
            replacements.put(operation, NodeFactory.operation(operation.getToken(), left, right));
    }

    private Token zeroToken() {
        return new Token(NumberConstant.ZERO, TokenType.INTEGER, 0);
    }

    private Token oneToken() {
        return new Token(NumberConstant.ONE, TokenType.INTEGER, 0);
    }
}