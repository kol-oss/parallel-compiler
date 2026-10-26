package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.pipeline.TreeTransformationService;
import com.github.kol.oss.compiler.utils.ArithmeticUtils;
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.ChainAssembler;
import com.github.kol.oss.compiler.utils.TreeVariantGenerator;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommutativeService extends TreeTransformationService {
    private static final int VARIANT_LIMIT = 64;
    private static final int ORDER_LIMIT = 12;

    @Override
    public String name() {
        return TransformationService.COMMUTATIVE;
    }

    @Override
    protected List<TokenNode> equivalentForms(TokenNode original, TokenNode transformed) {
        Set<TokenNode> roots = collectChainRoots(original, _ -> true);
        List<TokenNode> candidates = TreeVariantGenerator.generate(
                original,
                roots,
                this::commutativeCandidates,
                VARIANT_LIMIT
        );

        return mergeForms(transformed, candidates);
    }

    @Override
    protected void rewriteChain(OperationNode operation, Context context) {
        Chain chain = ChainAssembler.assemble(operation, context.replacements());
        // drop zeros for sum
        List<Term> terms = chain.family() == OperatorValue.PLUS ? dropZeros(chain.terms()) : chain.terms();

        // split by sign
        List<Term> positives = filter(terms, TermSign.POSITIVE);
        List<Term> negatives = filter(terms, TermSign.NEGATIVE);

        // sort by weight
        List<Term> sortedPositives = sortedByWeight(positives);
        List<Term> sortedNegatives = sortedByWeight(negatives);

        // check order change
        boolean orderChanged = !sameOrder(positives, sortedPositives) || !sameOrder(negatives, sortedNegatives);

        List<Term> reordered = new ArrayList<>(sortedPositives);
        reordered.addAll(sortedNegatives);

        // rebuild
        TokenNode rebuilt = BalancedTreeBuilder.buildSigned(new Chain(chain.family(), reordered));
        context.replace(operation, rebuilt);

        if (orderChanged || !TreeWalker.same(operation, rebuilt))
            context.record(operation, rebuilt);
    }

    private List<TokenNode> commutativeCandidates(OperationNode operation) {
        Chain chain = ChainAssembler.assemble(operation, Map.of());
        List<Term> terms = chain.family() == OperatorValue.PLUS ? dropZeros(chain.terms()) : chain.terms();

        if (terms.size() < 2)
            return List.of(BalancedTreeBuilder.buildSigned(new Chain(chain.family(), terms)));

        Map<String, TokenNode> forms = new LinkedHashMap<>();
        addCandidate(forms, chain, canonicalOrder(terms));

        for (List<Term> ordered : alternativeOrders(terms)) {
            addCandidate(forms, chain, ordered);

            if (forms.size() >= VARIANT_LIMIT)
                break;
        }

        return List.copyOf(forms.values());
    }

    // same order as the primary commutative transformation
    private List<Term> canonicalOrder(List<Term> terms) {
        List<Term> reordered = new ArrayList<>(sortedByWeight(filter(terms, TermSign.POSITIVE)));
        reordered.addAll(sortedByWeight(filter(terms, TermSign.NEGATIVE)));
        return reordered;
    }

    private List<List<Term>> alternativeOrders(List<Term> terms) {
        List<List<Term>> positiveOrders = orderings(filter(terms, TermSign.POSITIVE));
        List<List<Term>> negativeOrders = orderings(filter(terms, TermSign.NEGATIVE));
        List<List<Term>> result = new ArrayList<>();

        for (List<Term> positives : positiveOrders) {
            for (List<Term> negatives : negativeOrders) {
                List<Term> ordered = new ArrayList<>(positives);
                ordered.addAll(negatives);
                result.add(ordered);

                if (result.size() >= ORDER_LIMIT)
                    return result;
            }
        }

        return result;
    }

    // different orders of terms with the same sign
    private List<List<Term>> orderings(List<Term> terms) {
        if (terms.size() <= 1)
            return List.of(terms);

        Map<String, List<Term>> orders = new LinkedHashMap<>();
        addOrder(orders, terms);
        addOrder(orders, sortedByWeight(terms));
        addOrder(orders, sortedByWeightAscending(terms));
        addOrder(orders, alternating(terms));
        addOrder(orders, reversed(terms));

        List<Term> descending = sortedByWeight(terms);
        for (int shift = 1; shift < descending.size(); shift++) {
            addOrder(orders, rotate(descending, shift));

            if (orders.size() >= ORDER_LIMIT)
                return List.copyOf(orders.values());
        }

        if (terms.size() <= 4) {
            for (List<Term> permutation : permutations(terms)) {
                addOrder(orders, permutation);

                if (orders.size() >= ORDER_LIMIT)
                    break;
            }
        }

        return List.copyOf(orders.values());
    }

    private void addOrder(Map<String, List<Term>> orders, List<Term> terms) {
        StringBuilder key = new StringBuilder();

        for (Term term : terms) {
            key.append(term.sign())
                    .append(TreeWalker.structuralKey(term.node()))
                    .append(';');
        }

        orders.putIfAbsent(key.toString(), List.copyOf(terms));
    }

    private void addCandidate(Map<String, TokenNode> forms, Chain chain, List<Term> ordered) {
        TokenNode rebuilt = BalancedTreeBuilder.buildSigned(new Chain(chain.family(), ordered));
        forms.putIfAbsent(TreeWalker.structuralKey(rebuilt), rebuilt);
    }

    private List<Term> sortedByWeightAscending(List<Term> terms) {
        return terms.stream()
                .sorted(Comparator.comparingInt(term -> term.node().getWeight()))
                .toList();
    }

    private List<Term> alternating(List<Term> terms) {
        List<Term> sorted = sortedByWeight(terms);
        List<Term> result = new ArrayList<>();
        int left = 0;
        int right = sorted.size() - 1;

        while (left <= right) {
            result.add(sorted.get(left++));

            if (left <= right)
                result.add(sorted.get(right--));
        }

        return result;
    }

    private List<Term> reversed(List<Term> terms) {
        List<Term> result = new ArrayList<>(terms);
        Collections.reverse(result);
        return result;
    }

    private List<Term> rotate(List<Term> terms, int shift) {
        List<Term> result = new ArrayList<>(terms.size());

        for (int i = 0; i < terms.size(); i++)
            result.add(terms.get((i + shift) % terms.size()));

        return result;
    }

    private List<List<Term>> permutations(List<Term> terms) {
        List<Term> mutable = new ArrayList<>(terms);
        List<List<Term>> result = new ArrayList<>();
        permute(mutable, 0, result);
        return result;
    }

    private void permute(List<Term> terms, int index, List<List<Term>> result) {
        if (index == terms.size()) {
            result.add(List.copyOf(terms));
            return;
        }

        for (int i = index; i < terms.size(); i++) {
            Collections.swap(terms, index, i);
            permute(terms, index + 1, result);
            Collections.swap(terms, index, i);
        }
    }

    // filter by sign
    private List<Term> filter(List<Term> terms, TermSign sign) {
        return terms.stream().filter(term -> term.sign() == sign).toList();
    }

    // sort by weight desc
    private List<Term> sortedByWeight(List<Term> terms) {
        return terms.stream()
                .sorted(Comparator.comparingInt((Term term) -> term.node().getWeight()).reversed())
                .toList();
    }

    private boolean sameOrder(List<Term> original, List<Term> sorted) {
        if (original.size() != sorted.size())
            return false;

        for (int i = 0; i < original.size(); i++)
            if (original.get(i).node() != sorted.get(i).node())
                return false;

        return true;
    }

    private List<Term> dropZeros(List<Term> terms) {
        boolean hasNonZero = terms.stream().anyMatch(term -> !isZero(term.node()));
        if (!hasNonZero)
            return terms;

        return terms.stream().filter(term -> !isZero(term.node())).toList();
    }

    private boolean isZero(TokenNode node) {
        return node instanceof ValueNode value && ArithmeticUtils.isZero(value.getToken());
    }
}
