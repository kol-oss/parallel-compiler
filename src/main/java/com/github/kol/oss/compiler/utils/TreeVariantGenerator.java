package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// builds tree variants by combining chain rewrites in nested expressions
public final class TreeVariantGenerator {
    private final Set<TokenNode> chainRoots;
    private final Function<OperationNode, List<TokenNode>> rewriter;
    private final int limit;
    private final Map<TokenNode, List<TokenNode>> memo = new IdentityHashMap<>();

    private TreeVariantGenerator(
            Set<TokenNode> chainRoots,
            Function<OperationNode, List<TokenNode>> rewriter,
            int limit
    ) {
        this.chainRoots = chainRoots;
        this.rewriter = rewriter;
        this.limit = limit;
    }

    public static List<TokenNode> generate(
            TokenNode root,
            Set<TokenNode> chainRoots,
            Function<OperationNode, List<TokenNode>> rewriter,
            int limit
    ) {
        return new TreeVariantGenerator(chainRoots, rewriter, limit).visit(root);
    }

    private List<TokenNode> visit(TokenNode node) {
        List<TokenNode> cached = memo.get(node);
        if (cached != null)
            return cached;

        List<TokenNode> variants = distinct(switch (node) {
            case FunctionNode function -> functionVariants(function);
            case OperationNode operation -> operationVariants(operation);
            default -> List.of(node);
        });

        memo.put(node, variants);
        return variants;
    }

    private List<TokenNode> functionVariants(FunctionNode function) {
        List<List<TokenNode>> argumentVariants = new ArrayList<>();
        for (TokenNode argument : function.getArguments())
            argumentVariants.add(visit(argument));

        List<TokenNode> variants = new ArrayList<>();
        for (List<TokenNode> arguments : combinations(argumentVariants)) {
            variants.add(NodeFactory.function(function.getToken(), arguments));
            if (variants.size() >= limit)
                break;
        }

        return variants;
    }

    private List<TokenNode> operationVariants(OperationNode operation) {
        List<TokenNode> leftVariants = visit(operation.getLeft());
        List<TokenNode> rightVariants = visit(operation.getRight());
        List<TokenNode> combined = combine(operation.getToken(), leftVariants, rightVariants);

        if (!chainRoots.contains(operation))
            return combined;

        List<TokenNode> variants = new ArrayList<>();
        for (TokenNode candidate : combined) {
            List<TokenNode> rewritten = rewriter.apply((OperationNode) candidate);

            if (rewritten.isEmpty())
                variants.add(candidate);
            else
                variants.addAll(rewritten);

            if (variants.size() >= limit)
                break;
        }

        return variants;
    }

    private List<TokenNode> combine(Token operator, List<TokenNode> leftVariants, List<TokenNode> rightVariants) {
        List<TokenNode> combined = new ArrayList<>();

        for (TokenNode left : leftVariants) {
            for (TokenNode right : rightVariants) {
                combined.add(NodeFactory.operation(operator, left, right));

                if (combined.size() >= limit)
                    return combined;
            }
        }

        return combined;
    }

    private List<List<TokenNode>> combinations(List<List<TokenNode>> pools) {
        List<List<TokenNode>> combinations = new ArrayList<>();
        combinations.add(List.of());

        for (List<TokenNode> pool : pools) {
            List<List<TokenNode>> next = new ArrayList<>();

            for (List<TokenNode> combination : combinations) {
                for (TokenNode variant : pool) {
                    List<TokenNode> extended = new ArrayList<>(combination);
                    extended.add(variant);
                    next.add(extended);

                    if (next.size() >= limit)
                        break;
                }

                if (next.size() >= limit)
                    break;
            }

            combinations = next;
        }

        return combinations;
    }

    private List<TokenNode> distinct(List<TokenNode> variants) {
        Map<String, TokenNode> unique = new LinkedHashMap<>();

        for (TokenNode variant : variants) {
            unique.putIfAbsent(TreeWalker.structuralKey(variant), variant);

            if (unique.size() >= limit)
                break;
        }

        return List.copyOf(unique.values());
    }
}
