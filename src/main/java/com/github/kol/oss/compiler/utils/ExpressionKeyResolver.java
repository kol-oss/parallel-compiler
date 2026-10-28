package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.chain.Factor;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ExpressionKeyResolver {
    private final Map<TokenNode, Keys> keys = new WeakHashMap<>();
    private final Map<TokenNode, List<Factor>> factors = new WeakHashMap<>();

    public String key(TokenNode node) {
        return keysOf(node).norm();
    }

    private Factor factorOf(TokenNode node, TermSign sign) {
        Keys nodeKeys = keysOf(node);
        return new Factor(node, sign, nodeKeys.norm(), nodeKeys.flip());
    }

    public List<Factor> factorsOf(TokenNode node) {
        List<Factor> cached = factors.get(node);
        if (cached != null)
            return cached;

        List<Factor> result;

        if (TreeWalker.chainFamily(node) != OperatorValue.MULTIPLY) {
            result = List.of(factorOf(node, TermSign.POSITIVE));
        } else {
            result = ChainAssembler.assemble(node).terms().stream()
                    .map(term -> factorOf(term.node(), term.sign()))
                    .toList();
        }

        factors.put(node, result);
        return result;
    }

    public String signature(List<Term> terms) {
        List<String> keys = new ArrayList<>(terms.size());

        for (Term term : terms)
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node()));

        Collections.sort(keys);
        return String.join(";", keys);
    }

    public String orderedSignature(List<Term> terms) {
        List<String> keys = new ArrayList<>(terms.size());

        for (Term term : terms)
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node()));

        return String.join(";", keys);
    }

    private Keys keysOf(TokenNode node) {
        Keys cached = keys.get(node);
        if (cached != null)
            return cached;

        for (TokenNode current : TreeWalker.postOrder(node)) {
            if (!keys.containsKey(current))
                keys.put(current, computeKeys(current));
        }

        return keys.get(node);
    }

    private Keys computeKeys(TokenNode node) {
        return new Keys(computeKey(node), flipKey(node));
    }

    private String computeKey(TokenNode node) {
        OperatorValue family = TreeWalker.chainFamily(node);

        if (family == OperatorValue.PLUS)
            return sumKey(node, false);

        if (family == OperatorValue.MULTIPLY)
            return productKey(node);

        if (node instanceof FunctionNode function) {
            StringBuilder builder = new StringBuilder(function.getToken().value()).append('(');

            for (int i = 0; i < function.getArguments().size(); i++) {
                if (i > 0)
                    builder.append(',');

                builder.append(key(function.getArguments().get(i)));
            }

            return builder.append(')').toString();
        }

        if (node instanceof OperationNode operation)
            return "(" + key(operation.getLeft()) + operation.getToken().value() + key(operation.getRight()) + ")";

        if (node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return valueKey(value.getToken());

        return node.getToken().value();
    }

    private String flipKey(TokenNode node) {
        if (TreeWalker.chainFamily(node) == OperatorValue.PLUS)
            return sumKey(node, true);

        if (node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return valueKey(negated(value.getToken()));

        return null;
    }

    private String sumKey(TokenNode node, boolean invert) {
        List<String> keys = new ArrayList<>();

        for (Term term : ChainAssembler.assemble(node).terms()) {
            boolean negative = term.sign() == TermSign.POSITIVE;

            if (invert)
                negative = !negative;

            keys.add((negative ? "-" : "+") + key(term.node()));
        }

        Collections.sort(keys);
        return "{" + String.join(",", keys) + "}";
    }

    private String productKey(TokenNode node) {
        List<String> keys = new ArrayList<>();

        for (Term term : ChainAssembler.assemble(node).terms())
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node()));

        Collections.sort(keys);
        return "(" + String.join(",", keys) + ")";
    }

    private String valueKey(Token token) {
        return "#" + new BigDecimal(token.value()).stripTrailingZeros().toPlainString();
    }

    private Token negated(Token token) {
        return new Token(new BigDecimal(token.value()).negate().stripTrailingZeros().toPlainString(),
                token.type(), token.position());
    }

    private record Keys(String norm, String flip) {
    }
}
