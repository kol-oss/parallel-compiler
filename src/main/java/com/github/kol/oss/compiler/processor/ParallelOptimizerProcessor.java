package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.dto.node.OperandNode;
import com.github.kol.oss.compiler.dto.node.OperatorNode;
import com.github.kol.oss.compiler.dto.node.Node;

public class ParallelOptimizerProcessor {

    public Node process(Node root) {
        Node optimizedTree = optimizeOperations(root);
        calculateLevels(optimizedTree);
        return optimizedTree;
    }

    private Node optimizeOperations(Node node) {
        if (node instanceof OperandNode) {
            return node;
        }

        OperatorNode op = (OperatorNode) node;
        op.setLeft(optimizeOperations(op.getLeft()));
        op.setRight(optimizeOperations(op.getRight()));

        if (op.getValue().equals("-") && op.getLeft() instanceof OperatorNode) {
            OperatorNode leftOp = (OperatorNode) op.getLeft();
            if (leftOp.getValue().equals("-")) {
                OperatorNode newRight = new OperatorNode("+", leftOp.getRight(), op.getRight());
                leftOp.setRight(newRight);
                return optimizeOperations(leftOp);
            }
        }

        if (op.getValue().equals("/") && op.getLeft() instanceof OperatorNode) {
            OperatorNode leftOp = (OperatorNode) op.getLeft();
            if (leftOp.getValue().equals("/")) {
                OperatorNode newRight = new OperatorNode("*", leftOp.getRight(), op.getRight());
                leftOp.setRight(newRight);
                return optimizeOperations(leftOp);
            }
        }

        return op;
    }

    private void calculateLevels(Node node) {
        if (node instanceof OperandNode) {
            node.setLevel(0);
            return;
        }

        OperatorNode op = (OperatorNode) node;
        calculateLevels(op.getLeft());
        calculateLevels(op.getRight());

        int nextLevel = Math.max(op.getLeft().getLevel(), op.getRight().getLevel()) + 1;
        op.setLevel(nextLevel);
    }
}