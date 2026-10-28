package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.SearchLimits;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.chain.GroupPlan;
import com.github.kol.oss.compiler.dto.chain.Grouping;
import com.github.kol.oss.compiler.dto.chain.Term;

import java.util.*;

public class AssociativeSearch {
    private final ExpressionKeyResolver keys = new ExpressionKeyResolver();
    private final FactorGrouper grouper = new FactorGrouper(keys, SearchLimits.MAX_GROUPINGS_PER_FACTOR);
    private final Map<String, List<Term>> memo = new HashMap<>();
    private final Map<String, List<List<Term>>> variantMemo = new HashMap<>();
    private final Map<String, List<List<Term>>> subsetVariantMemo = new HashMap<>();
    private int budget = SearchLimits.ASSOCIATIVE_SEARCH_BUDGET;
    private int variantBudget = SearchLimits.ASSOCIATIVE_VARIANT_SEARCH_BUDGET;

    public List<Term> bestGrouping(List<Term> terms) {
        Deque<GroupStep> pending = new ArrayDeque<>();
        pending.push(new SolveGroup(terms));
        List<Term> result = null;

        while (!pending.isEmpty()) {
            GroupStep step = pending.pop();

            switch (step) {
                case SolveGroup solve -> {
                    String signature = keys.signature(solve.terms());
                    List<Term> cached = memo.get(signature);

                    if (cached != null) {
                        result = cached;
                        continue;
                    }

                    if (--budget <= 0) {
                        result = solve.terms();
                        continue;
                    }

                    GroupState state = new GroupState(solve.terms(), signature);
                    state.best = state.terms;
                    state.bestCost = cost(state.terms);
                    state.options = grouper.options(state.terms, false);
                    pending.push(new IterateGroup(state));
                }
                case IterateGroup iterate -> {
                    GroupState state = iterate.state();

                    if (state.nextOption >= state.options.size()) {
                        memo.put(state.signature, state.best);
                        result = state.best;
                        continue;
                    }

                    Grouping grouping = state.options.get(state.nextOption);
                    GroupPlan plan = grouper.plan(state.terms, grouping);
                    pending.push(new InnerGroupSolved(state, plan));
                    pending.push(new SolveGroup(plan.inner()));
                }
                case InnerGroupSolved inner -> {
                    List<Term> candidate = grouper.apply(inner.plan(), result);
                    pending.push(new CandidateSolved(inner.state()));
                    pending.push(new SolveGroup(candidate));
                }
                case CandidateSolved candidate -> {
                    GroupState state = candidate.state();
                    int candidateCost = cost(result);

                    if (candidateCost < state.bestCost) {
                        state.best = result;
                        state.bestCost = candidateCost;
                    }

                    state.nextOption++;
                    pending.push(new IterateGroup(state));
                }
                default -> {
                }
            }
        }

        return result;
    }

    public List<List<Term>> variants(List<Term> terms, boolean includeSubsets) {
        Deque<VariantStep> pending = new ArrayDeque<>();
        pending.push(new SolveVariants(terms, includeSubsets));
        List<List<Term>> result = null;

        while (!pending.isEmpty()) {
            VariantStep step = pending.pop();

            switch (step) {
                case SolveVariants solve -> {
                    String signature = keys.orderedSignature(solve.terms());
                    List<List<Term>> cached = variantMemo(solve.includeSubsets()).get(signature);

                    if (cached != null) {
                        result = cached;
                        continue;
                    }

                    if (--variantBudget <= 0) {
                        result = List.of(solve.terms());
                        continue;
                    }

                    VariantState state = new VariantState(solve.terms(), solve.includeSubsets(), signature);
                    state.options = grouper.options(state.terms, state.includeSubsets);
                    pending.push(new IterateVariants(state));
                }
                case IterateVariants iterate -> {
                    VariantState state = iterate.state();

                    if (state.nextOption >= state.options.size() || state.results.size() >= SearchLimits.ASSOCIATIVE_VARIANT_LIMIT) {
                        List<List<Term>> variants = List.copyOf(state.results.values());
                        variantMemo(state.includeSubsets).put(state.signature, variants);
                        result = variants;
                        continue;
                    }

                    Grouping grouping = state.options.get(state.nextOption);
                    GroupPlan plan = grouper.plan(state.terms, grouping);
                    pending.push(new InnerVariantsSolved(state, plan));
                    pending.push(new SolveVariants(plan.inner(), state.includeSubsets));
                }
                case InnerVariantsSolved inner -> {
                    List<List<Term>> grouped = grouper.applyVariants(
                            inner.plan(),
                            result,
                            SearchLimits.ASSOCIATIVE_VARIANT_LIMIT
                    );
                    pending.push(new ProcessGrouped(inner.state(), grouped, 0));
                }
                case ProcessGrouped process -> {
                    VariantState state = process.state();

                    if (process.index() >= process.grouped().size()
                            || state.results.size() >= SearchLimits.ASSOCIATIVE_VARIANT_LIMIT) {
                        state.nextOption++;
                        pending.push(new IterateVariants(state));
                        continue;
                    }

                    List<Term> grouped = process.grouped().get(process.index());
                    state.results.putIfAbsent(keys.orderedSignature(grouped), grouped);
                    pending.push(new FurtherVariantsSolved(state, process.grouped(), process.index()));
                    pending.push(new SolveVariants(grouped, state.includeSubsets));
                }
                case FurtherVariantsSolved further -> {
                    VariantState state = further.state();

                    for (List<Term> variant : result) {
                        state.results.putIfAbsent(keys.orderedSignature(variant), variant);

                        if (state.results.size() >= SearchLimits.ASSOCIATIVE_VARIANT_LIMIT)
                            break;
                    }

                    if (state.results.size() >= SearchLimits.ASSOCIATIVE_VARIANT_LIMIT)
                        pending.push(new IterateVariants(state));
                    else
                        pending.push(new ProcessGrouped(state, further.grouped(), further.index() + 1));
                }
                default -> {
                }
            }
        }

        return result;
    }

    private Map<String, List<List<Term>>> variantMemo(boolean includeSubsets) {
        return includeSubsets ? subsetVariantMemo : variantMemo;
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

    private sealed interface GroupStep permits SolveGroup, IterateGroup, InnerGroupSolved, CandidateSolved {
    }

    private sealed interface VariantStep permits
            SolveVariants,
            IterateVariants,
            InnerVariantsSolved,
            ProcessGrouped,
            FurtherVariantsSolved {
    }

    private static final class GroupState {
        private final List<Term> terms;
        private final String signature;
        private List<Term> best;
        private int bestCost;
        private List<Grouping> options;
        private int nextOption;

        private GroupState(List<Term> terms, String signature) {
            this.terms = terms;
            this.signature = signature;
        }
    }

    private static final class VariantState {
        private final List<Term> terms;
        private final boolean includeSubsets;
        private final String signature;
        private final Map<String, List<Term>> results = new LinkedHashMap<>();
        private List<Grouping> options;
        private int nextOption;

        private VariantState(List<Term> terms, boolean includeSubsets, String signature) {
            this.terms = terms;
            this.includeSubsets = includeSubsets;
            this.signature = signature;
            results.put(signature, terms);
        }
    }

    private record SolveGroup(List<Term> terms) implements GroupStep {
    }

    private record IterateGroup(GroupState state) implements GroupStep {
    }

    private record InnerGroupSolved(GroupState state, GroupPlan plan) implements GroupStep {
    }

    private record CandidateSolved(GroupState state) implements GroupStep {
    }

    private record SolveVariants(List<Term> terms, boolean includeSubsets) implements VariantStep {
    }

    private record IterateVariants(VariantState state) implements VariantStep {
    }

    private record InnerVariantsSolved(VariantState state, GroupPlan plan) implements VariantStep {
    }

    private record ProcessGrouped(VariantState state, List<List<Term>> grouped, int index) implements VariantStep {
    }

    private record FurtherVariantsSolved(VariantState state, List<List<Term>> grouped,
                                         int index) implements VariantStep {
    }
}
