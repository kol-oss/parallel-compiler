package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.pipeline.Pass;
import com.github.kol.oss.compiler.pipeline.TreeRewritePass;
import com.github.kol.oss.compiler.utils.ArithmeticUtils;
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.ChainAssembler;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CommutativeService extends TreeRewritePass {
    @Override
    public String name() {
        return Pass.COMMUTATIVE;
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
