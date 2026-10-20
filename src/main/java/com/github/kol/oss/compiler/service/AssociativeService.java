package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.pipeline.Pass;
import com.github.kol.oss.compiler.pipeline.TreeRewritePass;
import com.github.kol.oss.compiler.utils.ArithmeticUtils;
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.ChainAssembler;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class AssociativeService extends TreeRewritePass {
    private static final int SEARCH_BUDGET = 200_000;

    private record Factor(TokenNode node, TermSign sign, String norm, String flip) {
    }

    private record Grouping(List<Integer> indexes, Factor factor) {
    }

    private record Keys(String norm, String flip) {
    }

    private static final class SearchContext {
        private final Map<String, List<Term>> memo = new HashMap<>();
        private final Map<TokenNode, Keys> keys = new WeakHashMap<>();
        private final Map<TokenNode, List<Factor>> factors = new WeakHashMap<>();
        private int budget = SEARCH_BUDGET;
    }

    @Override
    public String name() {
        return Pass.ASSOCIATIVE;
    }

    @Override
    protected boolean handlesChain(OperatorValue family) {
        return family == OperatorValue.PLUS;
    }

    @Override
    protected void rewriteChain(OperationNode operation, Context context) {
        SearchContext search = new SearchContext();

        Chain chain = ChainAssembler.assemble(operation, context.replacements());
        List<Term> grouped = groupFactors(chain.terms(), search);
        TokenNode rebuilt = BalancedTreeBuilder.buildSigned(new Chain(chain.family(), grouped));

        context.replace(operation, rebuilt);

        if (grouped.size() < chain.terms().size())
            context.record(operation, rebuilt);
    }

    private List<Term> groupFactors(List<Term> terms, SearchContext search) {
        String state = signature(terms, search);
        List<Term> cached = search.memo.get(state);

        if (cached != null)
            return cached;

        if (--search.budget <= 0)
            return terms;

        List<Term> best = terms;
        int bestCost = cost(terms);

        for (Grouping grouping : options(terms, search)) {
            List<Term> candidate = groupFactors(applyGrouping(terms, grouping, search), search);
            int candidateCost = cost(candidate);

            if (candidateCost < bestCost) {
                best = candidate;
                bestCost = candidateCost;
            }
        }

        search.memo.put(state, best);
        return best;
    }

    private List<Grouping> options(List<Term> terms, SearchContext search) {
        List<List<Factor>> termFactors = new ArrayList<>();
        for (Term term : terms)
            termFactors.add(factorsOf(term.node(), search));

        List<Grouping> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (List<Factor> factors : termFactors) {
            for (Factor candidate : factors) {
                if (!seen.add(candidate.sign() + "|" + candidate.norm()))
                    continue;

                List<Integer> indexes = matchingIndexes(termFactors, candidate);

                if (indexes.size() >= 2 && !allNeutral(terms, termFactors, indexes, candidate, search))
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

    private boolean allNeutral(List<Term> terms, List<List<Factor>> termFactors, List<Integer> indexes, Factor candidate, SearchContext search) {
        return indexes.stream().allMatch(index -> {
            Term remainder = remainderTerm(terms.get(index), termFactors.get(index), candidate);
            return isUnit(remainder) || key(remainder.node(), search).equals(key(terms.get(index).node(), search));
        });
    }

    private List<Term> applyGrouping(List<Term> terms, Grouping grouping, SearchContext search) {
        List<Term> remainders = new ArrayList<>();

        for (int index : grouping.indexes())
            remainders.add(remainderTerm(terms.get(index), factorsOf(terms.get(index).node(), search), grouping.factor()));

        boolean allNegative = remainders.stream().allMatch(term -> term.sign() == TermSign.NEGATIVE);
        TermSign groupSign = allNegative ? TermSign.NEGATIVE : TermSign.POSITIVE;
        List<Term> inner = allNegative ? flipped(remainders) : remainders;

        inner = groupFactors(inner, search);

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
        boolean[] consumed = new boolean[terms.size()];
        for (int index : indexes)
            consumed[index] = true;

        List<Term> result = new ArrayList<>();
        boolean inserted = false;

        for (int i = 0; i < terms.size(); i++) {
            if (i == indexes.getFirst() && !inserted) {
                inserted = true;
                result.add(group);
            }

            if (!consumed[i])
                result.add(terms.get(i));
        }

        return result;
    }

    private List<Term> flipped(List<Term> terms) {
        return terms.stream().map(term -> new Term(term.node(), TermSign.POSITIVE)).toList();
    }

    private boolean isUnit(Term term) {
        return term.node() instanceof ValueNode value && ArithmeticUtils.isOne(value.getToken());
    }

    private TermSign flip(TermSign sign) {
        return sign == TermSign.POSITIVE ? TermSign.NEGATIVE : TermSign.POSITIVE;
    }

    private List<Factor> factorsOf(TokenNode node, SearchContext search) {
        List<Factor> cached = search.factors.get(node);
        if (cached != null)
            return cached;

        List<Factor> factors;
        if (TreeWalker.chainFamily(node) != OperatorValue.MULTIPLY) {
            factors = List.of(factorOf(node, TermSign.POSITIVE, search));
        } else {
            factors = ChainAssembler.assemble(node, Map.of()).terms().stream()
                    .map(term -> factorOf(term.node(), term.sign(), search))
                    .toList();
        }

        search.factors.put(node, factors);
        return factors;
    }

    private Factor factorOf(TokenNode node, TermSign sign, SearchContext search) {
        Keys keys = keysOf(node, search);
        return new Factor(node, sign, keys.norm(), keys.flip());
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

    private String signature(List<Term> terms, SearchContext search) {
        List<String> keys = new ArrayList<>();

        for (Term term : terms)
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node(), search));

        Collections.sort(keys);
        return String.join(";", keys);
    }

    private Keys keysOf(TokenNode node, SearchContext search) {
        Keys cached = search.keys.get(node);
        if (cached != null)
            return cached;

        Keys keys = new Keys(computeKey(node, search), flipKey(node, search));
        search.keys.put(node, keys);
        return keys;
    }

    private String key(TokenNode node, SearchContext search) {
        return keysOf(node, search).norm();
    }

    private String computeKey(TokenNode node, SearchContext search) {
        OperatorValue family = TreeWalker.chainFamily(node);

        if (family == OperatorValue.PLUS)
            return sumKey(node, false, search);

        if (family == OperatorValue.MULTIPLY)
            return productKey(node, search);

        if (node instanceof FunctionNode function) {
            StringBuilder builder = new StringBuilder(function.getToken().value()).append('(');

            for (int i = 0; i < function.getArguments().size(); i++) {
                if (i > 0)
                    builder.append(',');

                builder.append(key(function.getArguments().get(i), search));
            }

            return builder.append(')').toString();
        }

        if (node instanceof OperationNode operation)
            return "(" + key(operation.getLeft(), search) + operation.getToken().value() + key(operation.getRight(), search) + ")";

        if (node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return valueKey(value.getToken());

        return node.getToken().value();
    }

    private String flipKey(TokenNode node, SearchContext search) {
        if (TreeWalker.chainFamily(node) == OperatorValue.PLUS)
            return sumKey(node, true, search);

        if (node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return valueKey(negated(value.getToken()));

        return null;
    }

    private String sumKey(TokenNode node, boolean invert, SearchContext search) {
        List<String> keys = new ArrayList<>();

        for (Term term : ChainAssembler.assemble(node, Map.of()).terms()) {
            boolean negative = term.sign() == TermSign.POSITIVE;

            if (invert)
                negative = !negative;

            keys.add((negative ? "-" : "+") + key(term.node(), search));
        }

        Collections.sort(keys);
        return "{" + String.join(",", keys) + "}";
    }

    private String productKey(TokenNode node, SearchContext search) {
        List<String> keys = new ArrayList<>();

        for (Term term : ChainAssembler.assemble(node, Map.of()).terms())
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node(), search));

        Collections.sort(keys);
        return "(" + String.join(",", keys) + ")";
    }

    private String valueKey(Token token) {
        return "#" + new BigDecimal(token.value()).stripTrailingZeros().toPlainString();
    }

    private Token negated(Token token) {
        return new Token(new BigDecimal(token.value()).negate().stripTrailingZeros().toPlainString(),
                token.type(), token.position());
    }

    private Token oneToken() {
        return new Token(NumberConstant.ONE, TokenType.INTEGER, 0);
    }
}
