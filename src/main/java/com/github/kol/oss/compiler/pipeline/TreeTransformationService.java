package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.change.Transformation;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TreeFormCollector;
import com.github.kol.oss.compiler.utils.TreeFormSelector;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public abstract class TreeTransformationService implements TransformationService {
    @Override
    public final TransformationResult apply(TokenNode root) {
        Context context = new Context();

        Set<TokenNode> chainRoots = Collections.newSetFromMap(new IdentityHashMap<>());
        chainRoots.addAll(TreeWalker.collectChainRoots(root, strictChains()));

        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (node instanceof ValueNode)
                continue;

            if (node instanceof FunctionNode function) {
                propagateFunction(function, context);
                continue;
            }

            OperationNode operation = (OperationNode) node;

            if (handlesChain(TreeWalker.chainFamily(node))) {
                if (!chainRoots.contains(node))
                    continue;

                rewriteChain(operation, context);
                continue;
            }

            rewriteOperation(operation, context);
        }

        TokenNode transformed = context.resolve(root);

        return new TransformationResult(
                name(),
                transformed,
                equivalentForms(root, transformed),
                context.applied(),
                context.diagnostics()
        );
    }

    protected List<TokenNode> equivalentForms(TokenNode original, TokenNode transformed) {
        return List.of(transformed);
    }

    protected final List<TokenNode> optimalForms(TokenNode original, TokenNode primary, List<TokenNode> candidates) {
        TreeFormCollector collector = new TreeFormCollector();
        collector.add(primary);
        collector.addAll(candidates);
        collector.add(original);

        return TreeFormSelector.selectOptimal(collector.forms());
    }

    protected final Set<TokenNode> collectChainRoots(TokenNode original, Predicate<OperatorValue> families) {
        Set<TokenNode> roots = Collections.newSetFromMap(new IdentityHashMap<>());

        for (TokenNode node : TreeWalker.collectChainRoots(original, strictChains())) {
            OperatorValue family = TreeWalker.chainFamily(node);

            if (family != null && families.test(family))
                roots.add(node);
        }

        return roots;
    }

    protected boolean handlesChain(OperatorValue family) {
        return family != null;
    }

    protected boolean strictChains() {
        return false;
    }

    protected abstract void rewriteChain(OperationNode operation, Context context);

    protected void rewriteOperation(OperationNode operation, Context context) {
        propagateOperation(operation, context);
    }

    protected final void propagateFunction(FunctionNode function, Context context) {
        List<TokenNode> arguments = function.getArguments().stream()
                .map(context::resolve)
                .toList();

        if (!arguments.equals(function.getArguments()))
            context.replace(function, NodeFactory.function(function.getToken(), arguments));
    }

    protected final void propagateOperation(OperationNode operation, Context context) {
        TokenNode left = context.resolve(operation.getLeft());
        TokenNode right = context.resolve(operation.getRight());

        if (left != operation.getLeft() || right != operation.getRight())
            context.replace(operation, NodeFactory.operation(operation.getToken(), left, right));
    }

    protected static final class Context {
        private final Map<TokenNode, TokenNode> replacements = new IdentityHashMap<>();
        private final List<Transformation> applied = new ArrayList<>();
        private final List<PositionedException> diagnostics = new ArrayList<>();

        public TokenNode resolve(TokenNode node) {
            return TreeWalker.resolve(node, replacements);
        }

        public List<Transformation> applied() {
            return applied;
        }

        public List<PositionedException> diagnostics() {
            return diagnostics;
        }

        public void replace(TokenNode before, TokenNode after) {
            replacements.put(before, after);
        }

        public void record(TokenNode before, TokenNode after) {
            applied.add(new Transformation(before, after));
        }

        public void report(PositionedException exception) {
            diagnostics.add(exception);
        }
    }
}
