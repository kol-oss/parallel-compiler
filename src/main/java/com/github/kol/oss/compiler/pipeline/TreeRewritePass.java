package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.change.Transformation;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.*;

public abstract class TreeRewritePass implements Pass {
    @Override
    public final PassResult apply(TokenNode root) {
        Context context = new Context();

        // find chain roots
        Set<TokenNode> chainRoots = Collections.newSetFromMap(new IdentityHashMap<>());
        chainRoots.addAll(TreeWalker.collectChainRoots(root, strictChains()));

        // process nodes in postorder
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

                // rewrite whole chain
                rewriteChain(operation, context);
                continue;
            }

            // rewrite single operation
            rewriteOperation(operation, context);
        }

        return new PassResult(
                name(),
                TreeWalker.resolve(root, context.replacements()),
                context.applied(),
                context.diagnostics()
        );
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

    // propagate changes to function args
    protected final void propagateFunction(FunctionNode function, Context context) {
        List<TokenNode> arguments = function.getArguments().stream()
                .map(argument -> TreeWalker.resolve(argument, context.replacements()))
                .toList();

        if (!arguments.equals(function.getArguments()))
            context.replace(function, NodeFactory.function(function.getToken(), arguments));
    }

    // propagate changes to operation children
    protected final void propagateOperation(OperationNode operation, Context context) {
        TokenNode left = TreeWalker.resolve(operation.getLeft(), context.replacements());
        TokenNode right = TreeWalker.resolve(operation.getRight(), context.replacements());

        if (left != operation.getLeft() || right != operation.getRight())
            context.replace(operation, NodeFactory.operation(operation.getToken(), left, right));
    }

    // holds replacements, applied changes and errors for current pass
    protected static final class Context {
        private final Map<TokenNode, TokenNode> replacements = new IdentityHashMap<>();
        private final List<Transformation> applied = new ArrayList<>();
        private final List<PositionedException> diagnostics = new ArrayList<>();

        public Map<TokenNode, TokenNode> replacements() {
            return replacements;
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
