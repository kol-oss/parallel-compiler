package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.SearchLimits;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.pipeline.TreeTransformationService;
import com.github.kol.oss.compiler.utils.*;

import java.util.*;

public class CommutativeService extends TreeTransformationService {
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
                SearchLimits.MAX_TREE_VARIANTS
        );

        return optimalForms(original, transformed, candidates);
    }

    @Override
    protected void rewriteChain(OperationNode operation, Context context) {
        Chain chain = ChainAssembler.assemble(operation, context::resolve);
        List<Term> terms = chain.family() == OperatorValue.PLUS ? dropZeros(chain.terms()) : chain.terms();

        List<Term> positives = filter(terms, TermSign.POSITIVE);
        List<Term> negatives = filter(terms, TermSign.NEGATIVE);

        List<Term> sortedPositives = sortedByWeight(positives);
        List<Term> sortedNegatives = sortedByWeight(negatives);

        boolean orderChanged = !sameOrder(positives, sortedPositives) || !sameOrder(negatives, sortedNegatives);

        List<Term> reordered = new ArrayList<>(sortedPositives);
        reordered.addAll(sortedNegatives);

        TokenNode rebuilt = BalancedTreeBuilder.buildBalanced(new Chain(chain.family(), reordered));
        context.replace(operation, rebuilt);

        if (orderChanged || !TreeWalker.same(operation, rebuilt))
            context.record(operation, rebuilt);
    }

    private List<TokenNode> commutativeCandidates(OperationNode operation) {
        Chain chain = ChainAssembler.assemble(operation);
        List<Term> terms = chain.family() == OperatorValue.PLUS ? dropZeros(chain.terms()) : chain.terms();

        if (terms.size() < 2)
            return List.of(BalancedTreeBuilder.buildBalanced(new Chain(chain.family(), terms)));

        TreeFormCollector forms = new TreeFormCollector();
        addCandidate(forms, chain, canonicalOrder(terms));

        List<List<Term>> positiveOrders = TermPermutations.generate(
                distinctTerms(filter(terms, TermSign.POSITIVE)),
                SearchLimits.MAX_ORDER_VARIANTS
        );
        List<List<Term>> negativeOrders = TermPermutations.generate(
                distinctTerms(filter(terms, TermSign.NEGATIVE)),
                SearchLimits.MAX_ORDER_VARIANTS
        );
        int examined = 0;

        for (List<Term> positives : positiveOrders) {
            for (List<Term> negatives : negativeOrders) {
                List<Term> ordered = new ArrayList<>(positives);
                ordered.addAll(negatives);
                addCandidate(forms, chain, ordered);

                if (++examined >= SearchLimits.MAX_CHAIN_FORMS || forms.size() >= SearchLimits.MAX_CHAIN_FORMS)
                    return forms.forms();
            }
        }

        return forms.forms();
    }

    private List<Term> canonicalOrder(List<Term> terms) {
        List<Term> reordered = new ArrayList<>(sortedByWeight(filter(terms, TermSign.POSITIVE)));
        reordered.addAll(sortedByWeight(filter(terms, TermSign.NEGATIVE)));
        return reordered;
    }

    private List<Term> distinctTerms(List<Term> terms) {
        Map<String, Term> unique = new LinkedHashMap<>();

        for (Term term : terms)
            unique.putIfAbsent(term.sign() + ":" + TreeWalker.structuralKey(term.node()), term);

        return List.copyOf(unique.values());
    }

    private void addCandidate(TreeFormCollector forms, Chain chain, List<Term> ordered) {
        Chain reordered = new Chain(chain.family(), ordered);

        forms.add(BalancedTreeBuilder.buildBalanced(reordered));
        forms.add(BalancedTreeBuilder.buildSigned(reordered));
    }

    private List<Term> filter(List<Term> terms, TermSign sign) {
        return terms.stream().filter(term -> term.sign() == sign).toList();
    }

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
