package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;

public final class TreeMetrics {
    private TreeMetrics() {
    }

    public static int height(TokenNode node) {
        if (node instanceof OperationNode operation)
            return 1 + Math.max(height(operation.getLeft()), height(operation.getRight()));

        if (node instanceof FunctionNode function) {
            int result = 0;
            for (TokenNode argument : function.getArguments())
                result = Math.max(result, height(argument));
            return result;
        }

        return 0;
    }

    public static int width(TokenNode node) {
        int[] levels = new int[height(node) + 1];
        fillWidth(node, 1, levels);

        int result = 0;
        for (int level : levels)
            result = Math.max(result, level);
        return result;
    }

    private static void fillWidth(TokenNode node, int level, int[] levels) {
        if (node instanceof OperationNode operation) {
            levels[level]++;
            fillWidth(operation.getLeft(), level + 1, levels);
            fillWidth(operation.getRight(), level + 1, levels);
            return;
        }

        if (node instanceof FunctionNode function) {
            for (TokenNode argument : function.getArguments())
                fillWidth(argument, level, levels);
        }
    }
}
