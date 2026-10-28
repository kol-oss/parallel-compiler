package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.SearchLimits;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.pipeline.TreeTransformationService;
import com.github.kol.oss.compiler.utils.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AssociativeService extends TreeTransformationService {
    @Override
    public String name() {
        return TransformationService.ASSOCIATIVE;
    }

    @Override
    protected boolean handlesChain(OperatorValue family) {
        return family == OperatorValue.PLUS;
    }

    @Override
    protected List<TokenNode> equivalentForms(TokenNode original, TokenNode transformed) {
        Set<TokenNode> roots = collectChainRoots(original, family -> family == OperatorValue.PLUS);

        if (roots.isEmpty())
            return rankedForms(original, transformed, List.of());

        List<TokenNode> candidates = TreeVariantGenerator.generate(
                original,
                roots,
                this::associativeCandidates,
                SearchLimits.MAX_TREE_VARIANTS
        );

        return rankedForms(original, transformed, candidates);
    }

    private List<TokenNode> rankedForms(TokenNode original, TokenNode transformed, List<TokenNode> candidates) {
        TreeFormCollector forms = new TreeFormCollector();
        forms.add(transformed);
        forms.addAll(candidates);
        forms.add(original);

        Set<String> mandatory = new LinkedHashSet<>();
        mandatory.add(TreeWalker.structuralKey(original));
        mandatory.add(TreeWalker.structuralKey(transformed));

        return TreeFormSelector.selectRanked(forms.forms(), SearchLimits.MAX_EQUIVALENT_FORMS, mandatory);
    }

    @Override
    protected void rewriteChain(OperationNode operation, Context context) {
        AssociativeSearch search = new AssociativeSearch();
        Chain chain = ChainAssembler.assemble(operation, context::resolve);

        List<Term> grouped = search.bestGrouping(chain.terms());

        if (grouped.size() >= chain.terms().size())
            return;

        TokenNode rebuilt = BalancedTreeBuilder.buildBalanced(new Chain(chain.family(), grouped));
        context.replace(operation, rebuilt);
        context.record(operation, rebuilt);
    }

    private List<TokenNode> associativeCandidates(OperationNode operation) {
        Chain chain = ChainAssembler.assemble(operation);

        if (chain.family() != OperatorValue.PLUS)
            return List.of(operation);

        List<List<Term>> variants = new ArrayList<>(
                new AssociativeSearch().variants(chain.terms(), false)
        );
        variants.addAll(new AssociativeSearch().variants(chain.terms(), true));

        TreeFormCollector forms = new TreeFormCollector();
        int originalSize = chain.terms().size();

        for (List<Term> terms : variants) {
            if (terms.size() >= originalSize)
                continue;

            Chain rebuiltChain = new Chain(chain.family(), terms);

            forms.add(BalancedTreeBuilder.buildBalanced(rebuiltChain));
            forms.add(BalancedTreeBuilder.buildSigned(rebuiltChain));

            if (forms.size() >= SearchLimits.ASSOCIATIVE_VARIANT_LIMIT)
                break;
        }

        return forms.forms();
    }
}
