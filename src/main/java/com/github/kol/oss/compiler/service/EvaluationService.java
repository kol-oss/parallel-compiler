package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.utils.ArithmeticUtils;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TokenMapper;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.*;

public class EvaluationService {
    // get numeric value if root folds to number
    public Optional<Token> evaluate(TokenNode root) {
        TokenNode foldedTree = fold(root);
        if (foldedTree instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return Optional.of(value.getToken());
        return Optional.empty();
    }

    // fold constants in whole tree
    public TokenNode fold(TokenNode root) {
        Map<TokenNode, TokenNode> folded = new IdentityHashMap<>();
        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (node instanceof ValueNode) {
                continue;
            }
            if (node instanceof FunctionNode function) {
                // fold function args
                List<TokenNode> args = new ArrayList<>();
                boolean changed = false;

                for (TokenNode arg : function.getArguments()) {
                    TokenNode resolved = TreeWalker.resolve(arg, folded);
                    args.add(resolved);

                    if (resolved != arg)
                        changed = true;
                }

                if (changed)
                    folded.put(function, NodeFactory.function(function.getToken(), args));

                continue;
            }
            if (node instanceof OperationNode operation) {
                // fold children
                TokenNode left = TreeWalker.resolve(operation.getLeft(), folded);
                TokenNode right = TreeWalker.resolve(operation.getRight(), folded);

                // try to fold both values
                if (left instanceof ValueNode leftValue && right instanceof ValueNode rightValue) {
                    Optional<Token> result = ArithmeticUtils.applyOperation(
                            leftValue.getToken(),
                            rightValue.getToken(),
                            TokenMapper.tokenToOperator(operation.getToken())
                    );
                    if (result.isPresent()) {
                        folded.put(operation, NodeFactory.value(result.get()));
                        continue;
                    }
                }

                // propagate if changed
                if (left != operation.getLeft() || right != operation.getRight()) {
                    folded.put(operation, NodeFactory.operation(operation.getToken(), left, right));
                }
            }
        }
        return TreeWalker.resolve(root, folded);
    }
}