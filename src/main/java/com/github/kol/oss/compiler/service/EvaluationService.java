package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.utils.ArithmeticUtils;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TokenMapper;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public class EvaluationService {
    public Optional<Token> evaluate(TokenNode root) {
        Map<TokenNode, TokenNode> folded = new IdentityHashMap<>();

        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (!(node instanceof OperationNode operation))
                continue;

            TokenNode left = TreeWalker.resolve(operation.getLeft(), folded);
            TokenNode right = TreeWalker.resolve(operation.getRight(), folded);

            if (!(left instanceof ValueNode leftValue) || !(right instanceof ValueNode rightValue))
                continue;

            ArithmeticUtils.applyOperation(
                    leftValue.getToken(),
                    rightValue.getToken(),
                    TokenMapper.tokenToOperator(operation.getToken())
            ).ifPresent(token -> folded.put(node, NodeFactory.value(token)));
        }

        TokenNode result = TreeWalker.resolve(root, folded);
        if (result instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return Optional.of(value.getToken());

        return Optional.empty();
    }
}