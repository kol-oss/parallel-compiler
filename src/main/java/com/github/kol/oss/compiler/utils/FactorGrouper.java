package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Factor;
import com.github.kol.oss.compiler.dto.chain.GroupPlan;
import com.github.kol.oss.compiler.dto.chain.Grouping;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FactorGrouper {
    private final ExpressionKeyResolver keys;
    private final int maxGroupingsPerFactor;

    public FactorGrouper(ExpressionKeyResolver keys, int maxGroupingsPerFactor) {
        this.keys = keys;
        this.maxGroupingsPerFactor = maxGroupingsPerFactor;
    }

    public List<Grouping> options(List<Term> terms, boolean includeSubsets) {
        List<List<Factor>> termFactors = new ArrayList<>();
        for (Term term : terms)
            termFactors.add(keys.factorsOf(term.node()));

        List<Grouping> result = new ArrayList<>();
        List<Grouping> partial = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (List<Factor> factors : termFactors) {
            for (Factor candidate : factors) {
                if (!seen.add(candidate.sign() + "|" + candidate.norm()))
                    continue;

                List<Integer> indexes = matchingIndexes(termFactors, candidate);
                if (indexes.size() < 2)
                    continue;

                if (!allNeutral(terms, termFactors, indexes, candidate))
                    result.add(new Grouping(indexes, candidate));

                if (!includeSubsets)
                    continue;

                for (List<Integer> group : indexGroups(indexes)) {
                    if (group.size() == indexes.size())
                        continue;

                    if (!allNeutral(terms, termFactors, group, candidate))
                        partial.add(new Grouping(group, candidate));
                }
            }
        }

        result.addAll(partial);
        return result;
    }

    public GroupPlan plan(List<Term> terms, Grouping grouping) {
        List<Term> remainders = new ArrayList<>();

        for (int index : grouping.indexes())
            remainders.add(remainderTerm(
                    terms.get(index),
                    keys.factorsOf(terms.get(index).node()),
                    grouping.factor()
            ));

        boolean allNegative = remainders.stream().allMatch(term -> term.sign() == TermSign.NEGATIVE);
        TermSign groupSign = allNegative ? TermSign.NEGATIVE : TermSign.POSITIVE;
        List<Term> inner = allNegative ? flipped(remainders) : remainders;

        return new GroupPlan(terms, grouping, groupSign, inner);
    }

    public List<Term> apply(GroupPlan plan, List<Term> innerGrouped) {
        Term group = new Term(groupNode(plan.grouping().factor(), innerGrouped), plan.groupSign());
        return insertGroup(plan.terms(), plan.grouping().indexes(), group);
    }

    public List<List<Term>> applyVariants(GroupPlan plan, List<List<Term>> innerVariants, int limit) {
        List<List<Term>> variants = new ArrayList<>();

        for (List<Term> innerVariant : innerVariants) {
            Term group = new Term(groupNode(plan.grouping().factor(), innerVariant), plan.groupSign());
            variants.add(insertGroup(plan.terms(), plan.grouping().indexes(), group));

            if (variants.size() >= limit)
                break;
        }

        return variants;
    }

    private TokenNode groupNode(Factor factor, List<Term> inner) {
        TokenNode innerSum = BalancedTreeBuilder.buildBalanced(new Chain(OperatorValue.PLUS, inner));

        return factor.sign().isPositive()
                ? NodeFactory.operation(OperatorValue.MULTIPLY, factor.node(), innerSum)
                : NodeFactory.operation(OperatorValue.DIVISION, innerSum, factor.node());
    }

    private List<Integer> matchingIndexes(List<List<Factor>> termFactors, Factor candidate) {
        List<Integer> indexes = new ArrayList<>();

        for (int i = 0; i < termFactors.size(); i++) {
            if (termFactors.get(i).stream().anyMatch(factor -> provides(factor, candidate)))
                indexes.add(i);
        }

        return indexes;
    }

    private boolean allNeutral(
            List<Term> terms,
            List<List<Factor>> termFactors,
            List<Integer> indexes,
            Factor candidate
    ) {
        return indexes.stream().allMatch(index -> {
            Term remainder = remainderTerm(terms.get(index), termFactors.get(index), candidate);
            return isUnit(remainder) || keys.key(remainder.node()).equals(keys.key(terms.get(index).node()));
        });
    }

    private Term remainderTerm(Term term, List<Factor> factors, Factor target) {
        List<Factor> remaining = new ArrayList<>();
        TermSign sign = term.sign();
        boolean removed = false;

        for (Factor factor : factors) {
            if (!removed && provides(factor, target)) {
                if (opposite(factor, target))
                    sign = sign.opposite();

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

        return BalancedTreeBuilder.buildBalanced(new Chain(OperatorValue.MULTIPLY, terms));
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

    private List<List<Integer>> indexGroups(List<Integer> indexes) {
        List<List<Integer>> groups = new ArrayList<>();
        if (indexes.size() < 2)
            return groups;

        for (int size = indexes.size(); size >= 2 && groups.size() < maxGroupingsPerFactor; size--)
            collectIndexGroups(indexes, size, groups);

        return groups;
    }

    private void collectIndexGroups(List<Integer> indexes, int size, List<List<Integer>> groups) {
        int[] positions = new int[size];
        for (int i = 0; i < size; i++)
            positions[i] = i;

        while (groups.size() < maxGroupingsPerFactor) {
            List<Integer> group = new ArrayList<>(size);
            for (int position : positions)
                group.add(indexes.get(position));

            groups.add(group);

            int position = size - 1;
            while (position >= 0 && positions[position] == indexes.size() - size + position)
                position--;

            if (position < 0)
                return;

            positions[position]++;
            for (int i = position + 1; i < size; i++)
                positions[i] = positions[i - 1] + 1;
        }
    }

    private List<Term> flipped(List<Term> terms) {
        return terms.stream().map(term -> new Term(term.node(), TermSign.POSITIVE)).toList();
    }

    private boolean isUnit(Term term) {
        return term.node() instanceof ValueNode value && ArithmeticUtils.isOne(value.getToken());
    }

    private Token oneToken() {
        return new Token(NumberConstant.ONE, TokenType.INTEGER, 0);
    }
}
