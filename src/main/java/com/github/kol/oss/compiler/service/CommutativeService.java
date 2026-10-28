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
    // every order of a chain is a distinct equivalent form, so the complete
    // search is factorial; the caps keep the enumeration deterministic and bounded
    private static final int MAX_ORDER_VARIANTS = 50_000;
    private static final int MAX_CHAIN_FORMS = 50_000;
    private static final int MAX_TREE_VARIANTS = 50_000;

    @Override
    public String name() {
        return TransformationService.COMMUTATIVE;
    }

    @Override
    protected List<TokenNode> equivalentForms(TokenNode original, TokenNode transformed) {
        Set<TokenNode> roots = collectChainRoots(original, _ -> true);

        if (roots.isEmpty())
            return optimalForms(original, transformed, List.of());

        List<TokenNode> candidates = TreeVariantGenerator.generate(
                original,
                roots,
                this::commutativeCandidates,
                MAX_TREE_VARIANTS
        );

        return optimalForms(original, transformed, candidates);
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

    // returns every order of the chain, not only a heuristic subset
    private List<TokenNode> commutativeCandidates(OperationNode operation) {
        Chain chain = ChainAssembler.assemble(operation, Map.of());
        List<Term> terms = chain.family() == OperatorValue.PLUS ? dropZeros(chain.terms()) : chain.terms();

        if (terms.size() < 2)
            return List.of(BalancedTreeBuilder.buildSigned(new Chain(chain.family(), terms)));

        Map<String, TokenNode> forms = new LinkedHashMap<>();
        addCandidate(forms, chain, canonicalOrder(terms));

        List<List<Term>> positiveOrders = orderingsUpTo(distinctTerms(filter(terms, TermSign.POSITIVE)));
        List<List<Term>> negativeOrders = orderingsUpTo(distinctTerms(filter(terms, TermSign.NEGATIVE)));
        int examined = 0;

        for (List<Term> positives : positiveOrders) {
            for (List<Term> negatives : negativeOrders) {
                List<Term> ordered = new ArrayList<>(positives);
                ordered.addAll(negatives);
                addCandidate(forms, chain, ordered);

                if (++examined >= MAX_CHAIN_FORMS || forms.size() >= MAX_CHAIN_FORMS)
                    return List.copyOf(forms.values());
            }
        }

        return List.copyOf(forms.values());
    }

    // same order as the primary commutative transformation
    private List<Term> canonicalOrder(List<Term> terms) {
        List<Term> reordered = new ArrayList<>(sortedByWeight(filter(terms, TermSign.POSITIVE)));
        reordered.addAll(sortedByWeight(filter(terms, TermSign.NEGATIVE)));
        return reordered;
    }

    // identical terms produce identical forms, so they are enumerated once
    private List<Term> distinctTerms(List<Term> terms) {
        Map<String, Term> unique = new LinkedHashMap<>();

        for (Term term : terms)
            unique.putIfAbsent(term.sign() + ":" + TreeWalker.structuralKey(term.node()), term);

        return List.copyOf(unique.values());
    }

    private List<List<Term>> orderingsUpTo(List<Term> terms) {
        List<List<Term>> result = new ArrayList<>();

        if (terms.size() <= 1) {
            result.add(List.copyOf(terms));
            return result;
        }

        permute(new ArrayList<>(terms), 0, result);
        return result;
    }

    private void permute(List<Term> terms, int index, List<List<Term>> result) {
        if (result.size() >= MAX_ORDER_VARIANTS)
            return;

        if (index == terms.size()) {
            result.add(List.copyOf(terms));
            return;
        }

        for (int i = index; i < terms.size() && result.size() < MAX_ORDER_VARIANTS; i++) {
            Collections.swap(terms, index, i);
            permute(terms, index + 1, result);
            Collections.swap(terms, index, i);
        }
    }

    private void addCandidate(Map<String, TokenNode> forms, Chain chain, List<Term> ordered) {
        TokenNode rebuilt = BalancedTreeBuilder.buildSigned(new Chain(chain.family(), ordered));
        forms.putIfAbsent(TreeWalker.structuralKey(rebuilt), rebuilt);
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
