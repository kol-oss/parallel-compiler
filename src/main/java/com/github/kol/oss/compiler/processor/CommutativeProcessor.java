package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.utils.ExpressionPrinter;
import com.github.kol.oss.compiler.utils.TreeUtils;

import java.util.ArrayList;
import java.util.List;

public class CommutativeProcessor {
    private record SignedTerm(TokenNode node, boolean negative) {
    }

    private final List<String> transformations = new ArrayList<>();

    public List<String> getTransformations() {
        return transformations;
    }

    public TokenNode process(TokenNode node) {
        if (node instanceof ValueNode)
            return node;

        if (node instanceof FunctionNode function)
            return processFunction(function);

        OperationNode operation = (OperationNode) node;
        return switch (operation.getToken().value()) {
            case "+", "-" -> commuteTerms(operation, true);
            case "*", "/" -> commuteTerms(operation, false);
            default -> processChildren(operation);
        };
    }

    private TokenNode processFunction(FunctionNode function) {
        List<TokenNode> arguments = function.getArguments().stream().map(this::process).toList();
        return arguments.equals(function.getArguments())
                ? function
                : new FunctionNode(function.getToken(), arguments);
    }

    private TokenNode processChildren(OperationNode operation) {
        operation.setLeft(process(operation.getLeft()));
        operation.setRight(process(operation.getRight()));
        return operation;
    }

    private TokenNode commuteTerms(OperationNode operation, boolean isSum) {
        List<SignedTerm> collected = new ArrayList<>();
        collectTerms(operation, isSum, false, collected);

        List<SignedTerm> processed = collected.stream()
                .map(term -> new SignedTerm(process(term.node()), term.negative()))
                .toList();

        String before = ExpressionPrinter.print(foldChain(processed, isSum));
        List<SignedTerm> cleaned = isSum ? dropNeutralZeros(processed) : processed;

        List<TokenNode> positives = sortedTerms(cleaned, false);
        List<TokenNode> negatives = sortedTerms(cleaned, true);

        TokenNode positiveTree = positives.isEmpty()
                ? new ValueNode(new Token(NumberConstant.ZERO, TokenType.INTEGER, 0))
                : TreeUtils.buildBalanced(positives, isSum ? "+" : "*");

        TokenNode result = negatives.isEmpty()
                ? positiveTree
                : new OperationNode(
                TreeUtils.operatorToken(isSum ? "-" : "/"),
                positiveTree,
                TreeUtils.buildBalanced(negatives, isSum ? "+" : "*")
        );

        String after = ExpressionPrinter.print(result);
        if (!before.equals(after))
            transformations.add(before + " => " + after);

        return result;
    }

    // flattens a same-priority chain into signed terms, flipping the sign under "-" and "/"
    private void collectTerms(TokenNode node, boolean isSum, boolean negated, List<SignedTerm> terms) {
        String value = node instanceof OperationNode operation ? operation.getToken().value() : "";

        boolean chainOperator = isSum ? value.equals("+") || value.equals("-")
                : value.equals("*") || value.equals("/");

        if (!(node instanceof OperationNode operation) || !chainOperator) {
            terms.add(new SignedTerm(node, negated));
            return;
        }

        boolean flipsSign = value.equals("-") || value.equals("/");

        collectTerms(operation.getLeft(), isSum, negated, terms);
        collectTerms(operation.getRight(), isSum, negated ^ flipsSign, terms);
    }

    // "0 + ..." left by unary minus normalization would only clutter the sorted form
    private List<SignedTerm> dropNeutralZeros(List<SignedTerm> terms) {
        boolean hasNonZero = terms.stream().anyMatch(term -> !isZero(term.node()));
        if (!hasNonZero)
            return terms;

        return terms.stream().filter(term -> !isZero(term.node())).toList();
    }

    private boolean isZero(TokenNode node) {
        return node instanceof ValueNode && node.getToken().value().equals(NumberConstant.ZERO);
    }

    // stable sort keeps the original order of equally weighted terms
    private List<TokenNode> sortedTerms(List<SignedTerm> terms, boolean negative) {
        return terms.stream()
                .filter(term -> term.negative() == negative)
                .map(SignedTerm::node)
                .sorted((left, right) -> Integer.compare(TreeUtils.weight(right), TreeUtils.weight(left)))
                .toList();
    }

    // the original left-associative chain of already processed terms, used as the "before" snapshot
    private TokenNode foldChain(List<SignedTerm> terms, boolean isSum) {
        TokenNode result = terms.getFirst().node();

        for (int i = 1; i < terms.size(); i++) {
            SignedTerm term = terms.get(i);
            String operator = term.negative() ? (isSum ? "-" : "/") : (isSum ? "+" : "*");
            result = new OperationNode(TreeUtils.operatorToken(operator), result, term.node());
        }

        return result;
    }
}