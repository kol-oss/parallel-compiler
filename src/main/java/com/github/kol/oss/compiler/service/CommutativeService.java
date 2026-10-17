package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.change.Transformation;
import com.github.kol.oss.compiler.dto.change.TransformationResult;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.ChainAssembler;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommutativeService {
    public TransformationResult applyCommutativeLaw(TokenNode root) {
        Map<TokenNode, TokenNode> replacements = new IdentityHashMap<>();
        List<Transformation> applied = new ArrayList<>();
        Set<TokenNode> chainRoots = Collections.newSetFromMap(new IdentityHashMap<>());
        chainRoots.addAll(TreeWalker.collectChainRoots(root, false));

        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (node instanceof ValueNode)
                continue;

            if (node instanceof FunctionNode function) {
                commuteFunction(function, replacements);
                continue;
            }

            OperationNode operation = (OperationNode) node;

            if (TreeWalker.chainFamily(node) != null) {
                if (!chainRoots.contains(node))
                    continue;

                commuteChain(operation, replacements, applied);
                continue;
            }

            TokenNode left = TreeWalker.resolve(operation.getLeft(), replacements);
            TokenNode right = TreeWalker.resolve(operation.getRight(), replacements);

            if (left != operation.getLeft() || right != operation.getRight())
                replacements.put(operation, NodeFactory.operation(operation.getToken(), left, right));
        }

        return new TransformationResult(TreeWalker.resolve(root, replacements), applied);
    }

    private void commuteFunction(FunctionNode function, Map<TokenNode, TokenNode> replacements) {
        List<TokenNode> arguments = function.getArguments().stream()
                .map(argument -> TreeWalker.resolve(argument, replacements))
                .toList();

        if (!arguments.equals(function.getArguments()))
            replacements.put(function, NodeFactory.function(function.getToken(), arguments));
    }

    private void commuteChain(OperationNode operation, Map<TokenNode, TokenNode> replacements, List<Transformation> applied) {
        Chain chain = ChainAssembler.assemble(operation, replacements);
        List<Term> terms = chain.family() == OperatorValue.PLUS ? dropZeros(chain.terms()) : chain.terms();

        List<Term> positives = filter(terms, TermSign.POSITIVE);
        List<Term> negatives = filter(terms, TermSign.NEGATIVE);
        List<Term> sortedPositives = sortedByWeight(positives);
        List<Term> sortedNegatives = sortedByWeight(negatives);

        boolean orderChanged = !sameOrder(positives, sortedPositives) || !sameOrder(negatives, sortedNegatives);

        List<Term> reordered = new ArrayList<>(sortedPositives);
        reordered.addAll(sortedNegatives);

        TokenNode rebuilt = BalancedTreeBuilder.buildSigned(new Chain(chain.family(), reordered));
        replacements.put(operation, rebuilt);

        if (orderChanged)
            applied.add(new Transformation(operation, rebuilt));
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
        return node instanceof ValueNode && node.getToken().value().equals(NumberConstant.ZERO);
    }
}