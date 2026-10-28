package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TreeMetrics {
    public static int height(TokenNode root) {
        Map<TokenNode, Integer> heights = new IdentityHashMap<>();

        for (TokenNode node : TreeWalker.postOrder(root))
            heights.put(node, heightOf(node, heights));

        return heights.get(root);
    }

    public static int width(TokenNode root) {
        int[] levels = new int[height(root) + 1];
        Deque<LevelFrame> pending = new ArrayDeque<>();
        pending.push(new LevelFrame(root, 1));

        while (!pending.isEmpty()) {
            LevelFrame frame = pending.pop();
            TokenNode node = frame.node();
            int level = frame.level();

            if (node instanceof OperationNode operation) {
                levels[level]++;

                pending.push(new LevelFrame(operation.getRight(), level + 1));
                pending.push(new LevelFrame(operation.getLeft(), level + 1));
                continue;
            }

            if (node instanceof FunctionNode function) {
                List<TokenNode> arguments = function.getArguments();

                for (int i = arguments.size() - 1; i >= 0; i--)
                    pending.push(new LevelFrame(arguments.get(i), level));
            }
        }

        int result = 0;
        for (int level : levels)
            result = Math.max(result, level);

        return result;
    }

    private static int heightOf(TokenNode node, Map<TokenNode, Integer> heights) {
        if (node instanceof OperationNode operation)
            return 1 + Math.max(heights.get(operation.getLeft()), heights.get(operation.getRight()));

        if (node instanceof FunctionNode function) {
            int result = 0;

            for (TokenNode argument : function.getArguments())
                result = Math.max(result, heights.get(argument));

            return result;
        }

        return 0;
    }

    private record LevelFrame(TokenNode node, int level) {
    }
}
