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
import com.github.kol.oss.compiler.utils.ArithmeticUtils;
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.ChainAssembler;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssociativeService {
    private static final int SEARCH_BUDGET = 200_000;

    private record Factor(TokenNode node, TermSign sign, String norm, String flip) {
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
        return groupFactors(terms, new HashMap<>(), new int[]{SEARCH_BUDGET});
    }

    private List<Term> groupFactors(List<Term> terms, Map<String, List<Term>> memo, int[] budget) {
        String state = signature(terms);
        List<Term> cached = memo.get(state);

        if (cached != null)
            return cached;

        if (--budget[0] <= 0)
            return terms;

        List<Term> best = terms;
        int bestCost = cost(terms);

        for (Grouping grouping : options(terms)) {
            List<Term> candidate = groupFactors(applyGrouping(terms, grouping, budget), memo, budget);
            int candidateCost = cost(candidate);

            if (candidateCost < bestCost) {
                best = candidate;
                bestCost = candidateCost;
            }
        }

        memo.put(state, best);
        return best;
    }

    private List<Grouping> options(List<Term> terms) {
        List<List<Factor>> termFactors = new ArrayList<>();
        for (Term term : terms)
            termFactors.add(factorsOf(term.node()));

        List<Grouping> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (List<Factor> factors : termFactors) {
            for (Factor candidate : factors) {
                if (!seen.add(candidate.sign() + "|" + candidate.norm()))
                    continue;

                List<Integer> indexes = matchingIndexes(termFactors, candidate);

                if (indexes.size() >= 2 && !allNeutral(terms, termFactors, indexes, candidate))
                    result.add(new Grouping(indexes, candidate));
            }
        }

        return result;
    }

    private List<Integer> matchingIndexes(List<List<Factor>> termFactors, Factor candidate) {
        List<Integer> indexes = new ArrayList<>();

        for (int i = 0; i < termFactors.size(); i++) {
            if (termFactors.get(i).stream().anyMatch(factor -> provides(factor, candidate)))
                indexes.add(i);
        }

        return indexes;
    }

    private boolean allNeutral(List<Term> terms, List<List<Factor>> termFactors, List<Integer> indexes, Factor candidate) {
        return indexes.stream().allMatch(index -> {
            Term remainder = remainderTerm(terms.get(index), termFactors.get(index), candidate);
            return isUnit(remainder) || key(remainder.node()).equals(key(terms.get(index).node()));
        });
    }

    private List<Term> applyGrouping(List<Term> terms, Grouping grouping, int[] budget) {
        List<Term> remainders = new ArrayList<>();

        for (int index : grouping.indexes())
            remainders.add(remainderTerm(terms.get(index), factorsOf(terms.get(index).node()), grouping.factor()));

        boolean allNegative = remainders.stream().allMatch(term -> term.sign() == TermSign.NEGATIVE);
        TermSign groupSign = allNegative ? TermSign.NEGATIVE : TermSign.POSITIVE;
        List<Term> inner = allNegative ? flipped(remainders) : remainders;

        inner = groupFactors(inner, new HashMap<>(), budget);

        TokenNode innerSum = BalancedTreeBuilder.buildSigned(new Chain(OperatorValue.PLUS, inner));
        TokenNode groupNode = grouping.factor().sign() == TermSign.POSITIVE
                ? NodeFactory.operation(OperatorValue.MULTIPLY, grouping.factor().node(), innerSum)
                : NodeFactory.operation(OperatorValue.DIVISION, innerSum, grouping.factor().node());

        return insertGroup(terms, grouping.indexes(), new Term(groupNode, groupSign));
    }

    private Term remainderTerm(Term term, List<Factor> factors, Factor target) {
        List<Factor> remaining = new ArrayList<>();
        TermSign sign = term.sign();
        boolean removed = false;

        for (Factor factor : factors) {
            if (!removed && provides(factor, target)) {
                if (opposite(factor, target))
                    sign = flip(sign);

                removed = true;
                continue;
            }

            remaining.add(factor);
        }

        return new Term(buildRemainder(remaining), sign);
    }

    private boolean provides(Factor factor, Factor candidate) {
        return equivalent(factor, candidate) || opposite(factor, candidate);
    }

    private boolean equivalent(Factor factor, Factor candidate) {
        return factor.sign() == candidate.sign() && factor.norm().equals(candidate.norm());
    }

    private boolean opposite(Factor factor, Factor candidate) {
        if (factor.sign() != candidate.sign())
            return false;

        return candidate.flip() != null && factor.norm().equals(candidate.flip())
                || factor.flip() != null && candidate.norm().equals(factor.flip());
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

    private boolean isUnit(Term term) {
        return term.node() instanceof ValueNode value && NumberConstant.ONE.equals(value.getToken().value());
    }

    private TermSign flip(TermSign sign) {
        return sign == TermSign.POSITIVE ? TermSign.NEGATIVE : TermSign.POSITIVE;
    }

    private List<Factor> factorsOf(TokenNode node) {
        if (TreeWalker.chainFamily(node) != OperatorValue.MULTIPLY)
            return List.of(factorOf(node, TermSign.POSITIVE));

        return ChainAssembler.assemble(node, Map.of()).terms().stream()
                .map(term -> factorOf(term.node(), term.sign()))
                .toList();
    }

    private Factor factorOf(TokenNode node, TermSign sign) {
        return new Factor(node, sign, key(node), flipKey(node));
    }

    private int cost(List<Term> terms) {
        int cost = 0;
        int positives = 0;
        int negatives = 0;

        for (Term term : terms) {
            cost += term.node().getWeight();

            if (term.sign() == TermSign.POSITIVE)
                positives++;
            else
                negatives++;
        }

        if (positives > 1)
            cost += positives - 1;

        if (negatives > 1)
            cost += negatives - 1;

        if (negatives > 0)
            cost += 1;

        return cost;
    }

    private String signature(List<Term> terms) {
        List<String> keys = new ArrayList<>();

        for (Term term : terms)
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node()));

        Collections.sort(keys);
        return String.join(";", keys);
    }

    private String key(TokenNode node) {
        OperatorValue family = TreeWalker.chainFamily(node);

        if (family == OperatorValue.PLUS)
            return sumKey(node, false);

        if (family == OperatorValue.MULTIPLY)
            return productKey(node);

        if (node instanceof FunctionNode function) {
            StringBuilder builder = new StringBuilder(function.getToken().value()).append('(');

            for (int i = 0; i < function.getArguments().size(); i++) {
                if (i > 0)
                    builder.append(',');

                builder.append(key(function.getArguments().get(i)));
            }

            return builder.append(')').toString();
        }

        if (node instanceof OperationNode operation)
            return "(" + key(operation.getLeft()) + operation.getToken().value() + key(operation.getRight()) + ")";

        if (node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return valueKey(Double.parseDouble(value.getToken().value()));

        return node.getToken().value();
    }

    private String flipKey(TokenNode node) {
        if (TreeWalker.chainFamily(node) == OperatorValue.PLUS)
            return sumKey(node, true);

        if (node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return valueKey(-Double.parseDouble(value.getToken().value()));

        return null;
    }

    private String sumKey(TokenNode node, boolean invert) {
        List<String> keys = new ArrayList<>();

        for (Term term : ChainAssembler.assemble(node, Map.of()).terms()) {
            boolean negative = term.sign() == TermSign.POSITIVE;

            if (invert)
                negative = !negative;

            keys.add((negative ? "-" : "+") + key(term.node()));
        }

        Collections.sort(keys);
        return "{" + String.join(",", keys) + "}";
    }

    private String productKey(TokenNode node) {
        List<String> keys = new ArrayList<>();

        for (Term term : ChainAssembler.assemble(node, Map.of()).terms())
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node()));

        Collections.sort(keys);
        return "(" + String.join(",", keys) + ")";
    }

    private String valueKey(double value) {
        return "#" + value;
    }

    private Token oneToken() {
        return new Token(NumberConstant.ONE, TokenType.INTEGER, 0);
    }
}
