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
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.ChainAssembler;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AssociativeService {
    private record Factor(TokenNode node, TermSign sign) {
    }

    private record Grouping(List<Integer> indexes, Factor factor) {
    }

    public TransformationResult applyAssociativeLaw(TokenNode root) {
        Map<TokenNode, TokenNode> replacements = new IdentityHashMap<>();
        List<Transformation> applied = new ArrayList<>();
        Set<TokenNode> chainRoots = Collections.newSetFromMap(new IdentityHashMap<>());
        chainRoots.addAll(TreeWalker.collectChainRoots(root, false));

        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (node instanceof ValueNode)
                continue;

            if (node instanceof FunctionNode function) {
                propagateFunction(function, replacements);
                continue;
            }

            OperationNode operation = (OperationNode) node;

            if (TreeWalker.chainFamily(node) == OperatorValue.PLUS && chainRoots.contains(node)) {
                groupChain(operation, replacements, applied);
                continue;
            }

            propagateOperation(operation, replacements);
        }

        return new TransformationResult(TreeWalker.resolve(root, replacements), applied);
    }

    private void propagateFunction(FunctionNode function, Map<TokenNode, TokenNode> replacements) {
        List<TokenNode> arguments = function.getArguments().stream()
                .map(argument -> TreeWalker.resolve(argument, replacements))
                .toList();

        if (!arguments.equals(function.getArguments()))
            replacements.put(function, NodeFactory.function(function.getToken(), arguments));
    }

    private void propagateOperation(OperationNode operation, Map<TokenNode, TokenNode> replacements) {
        TokenNode left = TreeWalker.resolve(operation.getLeft(), replacements);
        TokenNode right = TreeWalker.resolve(operation.getRight(), replacements);

        if (left != operation.getLeft() || right != operation.getRight())
            replacements.put(operation, NodeFactory.operation(operation.getToken(), left, right));
    }

    private void groupChain(OperationNode operation, Map<TokenNode, TokenNode> replacements, List<Transformation> applied) {
        Chain chain = ChainAssembler.assemble(operation, replacements);
        List<Term> grouped = groupFactors(chain.terms());
        TokenNode rebuilt = BalancedTreeBuilder.buildSigned(new Chain(chain.family(), grouped));

        replacements.put(operation, rebuilt);

        if (grouped.size() < chain.terms().size())
            applied.add(new Transformation(operation, rebuilt));
    }

    private List<Term> groupFactors(List<Term> terms) {
        List<Term> current = new ArrayList<>(terms);

        while (true) {
            Optional<Grouping> grouping = findGrouping(current);
            if (grouping.isEmpty())
                return current;

            current = applyGrouping(current, grouping.get());
        }
    }

    private Optional<Grouping> findGrouping(List<Term> terms) {
        for (int i = 0; i < terms.size(); i++) {
            for (Factor candidate : factorsOf(terms.get(i).node())) {
                List<Integer> indexes = matchingIndexes(terms, candidate);

                if (indexes.size() >= 2 && !allNeutral(terms, indexes, candidate))
                    return Optional.of(new Grouping(indexes, candidate));
            }
        }

        return Optional.empty();
    }

    private List<Integer> matchingIndexes(List<Term> terms, Factor candidate) {
        List<Integer> indexes = new ArrayList<>();

        for (int i = 0; i < terms.size(); i++) {
            if (factorsOf(terms.get(i).node()).stream().anyMatch(factor -> matches(factor, candidate)))
                indexes.add(i);
        }

        return indexes;
    }

    private boolean allNeutral(List<Term> terms, List<Integer> indexes, Factor candidate) {
        return indexes.stream().allMatch(index -> remainderOf(terms.get(index), candidate).isEmpty());
    }

    private List<Term> applyGrouping(List<Term> terms, Grouping grouping) {
        List<Term> remainders = grouping.indexes().stream()
                .map(index -> remainderTerm(terms.get(index), grouping.factor()))
                .toList();

        boolean allNegative = remainders.stream().allMatch(term -> term.sign() == TermSign.NEGATIVE);
        TermSign groupSign = allNegative ? TermSign.NEGATIVE : TermSign.POSITIVE;
        List<Term> inner = allNegative ? flipped(remainders) : remainders;

        TokenNode innerSum = BalancedTreeBuilder.buildSigned(new Chain(OperatorValue.PLUS, inner));
        TokenNode groupNode = grouping.factor().sign() == TermSign.POSITIVE
                ? NodeFactory.operation(OperatorValue.MULTIPLY, grouping.factor().node(), innerSum)
                : NodeFactory.operation(OperatorValue.DIVISION, innerSum, grouping.factor().node());

        return insertGroup(terms, grouping.indexes(), new Term(groupNode, groupSign));
    }

    private Term remainderTerm(Term term, Factor factor) {
        List<Factor> remaining = remainderOf(term, factor);
        return new Term(buildRemainder(remaining), term.sign());
    }

    private List<Factor> remainderOf(Term term, Factor factor) {
        List<Factor> remaining = new ArrayList<>();
        boolean removed = false;

        for (Factor candidate : factorsOf(term.node())) {
            if (!removed && matches(candidate, factor)) {
                removed = true;
                continue;
            }

            remaining.add(candidate);
        }

        return remaining;
    }

    private TokenNode buildRemainder(List<Factor> factors) {
        if (factors.isEmpty())
            return NodeFactory.value(oneToken());

        List<Term> terms = factors.stream()
                .map(factor -> new Term(factor.node(), factor.sign()))
                .toList();

        return BalancedTreeBuilder.buildSigned(new Chain(OperatorValue.MULTIPLY, terms));
    }

    private List<Term> insertGroup(List<Term> terms, List<Integer> indexes, Term group) {
        List<Term> result = new ArrayList<>();
        int first = indexes.getFirst();

        for (int i = 0; i < terms.size(); i++) {
            if (i == first)
                result.add(group);

            if (!indexes.contains(i))
                result.add(terms.get(i));
        }

        return result;
    }

    private List<Term> flipped(List<Term> terms) {
        return terms.stream().map(term -> new Term(term.node(), TermSign.POSITIVE)).toList();
    }

    private List<Factor> factorsOf(TokenNode node) {
        if (TreeWalker.chainFamily(node) != OperatorValue.MULTIPLY)
            return List.of(new Factor(node, TermSign.POSITIVE));

        return ChainAssembler.assemble(node, Map.of()).terms().stream()
                .map(term -> new Factor(term.node(), term.sign()))
                .toList();
    }

    private boolean matches(Factor left, Factor right) {
        return left.sign() == right.sign() && same(left.node(), right.node());
    }

    private boolean same(TokenNode left, TokenNode right) {
        if (left == right)
            return true;

        if (left == null || right == null || left.getClass() != right.getClass())
            return false;

        if (!left.getToken().value().equals(right.getToken().value()))
            return false;

        List<TokenNode> leftChildren = TreeWalker.children(left);
        List<TokenNode> rightChildren = TreeWalker.children(right);

        if (leftChildren.size() != rightChildren.size())
            return false;

        for (int i = 0; i < leftChildren.size(); i++)
            if (!same(leftChildren.get(i), rightChildren.get(i)))
                return false;

        return true;
    }

    private Token oneToken() {
        return new Token(NumberConstant.ONE, TokenType.INTEGER, 0);
    }
}
