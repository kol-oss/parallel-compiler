package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.OperatorType;
import com.github.kol.oss.compiler.dto.node.Node;
import com.github.kol.oss.compiler.dto.node.OperandNode;
import com.github.kol.oss.compiler.dto.node.OperatorNode;
import com.github.kol.oss.compiler.exception.ZeroDivisionException;

import java.util.Map;

public class OptimizationProcessor {
    private static final Map<OperatorType, OperatorType> OPTIMIZATIONS = Map.of(
            OperatorType.MINUS, OperatorType.PLUS,
            OperatorType.DIVISION, OperatorType.MULTIPLY
    );

    public Node process(Node root) {
        return optimizeOperations(root);
    }

    private Node optimizeOperations(Node node) {
        if (node instanceof OperandNode)
            return node;

        OperatorNode operatorNode = (OperatorNode) node;
        Node left = operatorNode.getLeft();
        Node right = operatorNode.getRight();

        OperatorType operatorType = OperatorType.fromString(operatorNode.getValue());

        // optimization of branches
        operatorNode.setLeft(optimizeOperations(left));
        operatorNode.setRight(optimizeOperations(right));

        left = operatorNode.getLeft();
        right = operatorNode.getRight();

        if (left instanceof OperandNode leftOperand) {
            if (leftOperand.getValue().equals("0")) {
                if (operatorType == OperatorType.PLUS)
                    return right;

                if (operatorType == OperatorType.MULTIPLY)
                    return left;

                if (operatorType == OperatorType.DIVISION)
                    return left;
            }
            else if (leftOperand.getValue().equals("1")) {
                if (operatorType == OperatorType.MULTIPLY)
                    return right;
            }
        }

        if (right instanceof OperandNode rightOperand) {
            if (rightOperand.getValue().equals("0")) {
                if (operatorType == OperatorType.PLUS)
                    return left;

                if (operatorType == OperatorType.MULTIPLY)
                    return right;

                if (operatorType == OperatorType.MINUS)
                    return left;

                if (operatorType == OperatorType.DIVISION)
                    throw new ZeroDivisionException();
            }
            else if (rightOperand.getValue().equals("1")) {
                return left;
            }
        }

        // TODO: ADD CALCULATIONS
        if (left instanceof OperandNode leftOperand && right instanceof OperandNode rightOperand) {
            System.out.println("CAN BE CALCULATED");
        }

        // optimization of sequential patterns
        for (Map.Entry<OperatorType, OperatorType> optimization : OPTIMIZATIONS.entrySet()) {
            boolean isOptimized = optimizeSequence(operatorNode, optimization.getKey(), optimization.getValue());
            if (isOptimized)
                return optimizeOperations(left);
        }

        return operatorNode;
    }

    private boolean optimizeSequence(OperatorNode operatorNode, OperatorType base, OperatorType replacement) {
        OperatorType operator = OperatorType.fromString(operatorNode.getValue());
        Node left = operatorNode.getLeft();
        Node right = operatorNode.getRight();

        if (operator == base && left instanceof OperatorNode leftNode) {
            OperatorType leftOperator = OperatorType.fromString(leftNode.getValue());
            if (leftOperator == base) {
                OperatorNode newRight = new OperatorNode(OperatorType.toString(replacement), leftNode.getRight(), right);
                leftNode.setRight(newRight);

                return true;
            }
        }

        return false;
    }
}