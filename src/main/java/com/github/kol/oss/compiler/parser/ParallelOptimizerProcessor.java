package com.github.kol.oss.compiler.parser;

public class ParallelOptimizerProcessor {

    public TreeNode process(TreeNode root) {
        TreeNode optimizedTree = optimizeOperations(root);
        calculateLevels(optimizedTree);
        return optimizedTree;
    }

    private TreeNode optimizeOperations(TreeNode node) {
        if (node instanceof OperandNode) {
            return node;
        }

        OperatorNode op = (OperatorNode) node;
        op.setLeft(optimizeOperations(op.getLeft()));
        op.setRight(optimizeOperations(op.getRight()));

        if (op.getOperator().equals("-") && op.getLeft() instanceof OperatorNode) {
            OperatorNode leftOp = (OperatorNode) op.getLeft();
            if (leftOp.getOperator().equals("-")) {
                OperatorNode newRight = new OperatorNode("+", leftOp.getRight(), op.getRight());
                leftOp.setRight(newRight);
                return optimizeOperations(leftOp);
            }
        }

        if (op.getOperator().equals("/") && op.getLeft() instanceof OperatorNode) {
            OperatorNode leftOp = (OperatorNode) op.getLeft();
            if (leftOp.getOperator().equals("/")) {
                OperatorNode newRight = new OperatorNode("*", leftOp.getRight(), op.getRight());
                leftOp.setRight(newRight);
                return optimizeOperations(leftOp);
            }
        }

        return op;
    }

    private void calculateLevels(TreeNode node) {
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