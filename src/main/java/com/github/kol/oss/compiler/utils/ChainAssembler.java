package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.UnaryOperator;

public class ChainAssembler {
    private record Frame(TokenNode node, TermSign sign) {
    }

    public static Chain assemble(TokenNode chainRoot) {
        return assemble(chainRoot, node -> node);
    }

    public static Chain assemble(TokenNode chainRoot, UnaryOperator<TokenNode> resolver) {
        OperatorValue family = TreeWalker.chainFamily(chainRoot);
        if (family == null)
            throw new IllegalArgumentException("Node does not start a chain");

        List<Term> terms = new ArrayList<>();
        Deque<Frame> pending = new ArrayDeque<>();
        pending.push(new Frame(chainRoot, TermSign.POSITIVE));

        while (!pending.isEmpty()) {
            Frame frame = pending.pop();
            TokenNode node = resolver.apply(frame.node());

            if (TreeWalker.chainFamily(node) == family) {
                OperationNode operation = (OperationNode) node;
                boolean negatesRight = negates(operation);

                pending.push(new Frame(operation.getRight(), negatesRight ? frame.sign().opposite() : frame.sign()));
                pending.push(new Frame(operation.getLeft(), frame.sign()));
                continue;
            }

            terms.add(new Term(node, frame.sign()));
        }

        return new Chain(family, terms);
    }

    private static boolean negates(OperationNode operation) {
        OperatorValue operator = OperatorValue.fromSymbol(operation.getToken().value());
        return operator == OperatorValue.MINUS || operator == OperatorValue.DIVISION;
    }
}
