package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.pipeline.TreeTransformationService;
import com.github.kol.oss.compiler.utils.*;

import java.math.BigDecimal;
import java.util.*;

public class AssociativeService extends TreeTransformationService {
    private static final int SEARCH_BUDGET = 200_000;
    private static final int VARIANT_LIMIT = 20_000;
    private static final int VARIANT_SEARCH_BUDGET = 1_000_000;
    private static final int MAX_TREE_VARIANTS = 50_000;

    @Override
    public String name() {
        return TransformationService.ASSOCIATIVE;
    }

    @Override
    protected boolean handlesChain(OperatorValue family) {
        return family == OperatorValue.PLUS;
    }

    @Override
    protected List<TokenNode> equivalentForms(TokenNode original, TokenNode transformed) {
        Set<TokenNode> roots = collectChainRoots(original, family -> family == OperatorValue.PLUS);

        if (roots.isEmpty())
            return optimalForms(original, transformed, List.of());

        List<TokenNode> candidates = TreeVariantGenerator.generate(
                original,
                roots,
                this::associativeCandidates,
                MAX_TREE_VARIANTS
        );

        return optimalForms(original, transformed, candidates);
    }

    @Override
    protected void rewriteChain(OperationNode operation, Context context) {
        SearchContext search = new SearchContext();
        Chain chain = ChainAssembler.assemble(operation, context.replacements());

        List<Term> grouped = groupFactors(chain.terms(), search);

        // nothing was factored out: keep the incoming (already balanced) tree
        if (grouped.size() >= chain.terms().size())
            return;

        TokenNode rebuilt = BalancedTreeBuilder.buildBalanced(new Chain(chain.family(), grouped));
        context.replace(operation, rebuilt);
        context.record(operation, rebuilt);
    }

    private List<TokenNode> associativeCandidates(OperationNode operation) {
        Chain chain = ChainAssembler.assemble(operation, Map.of());

        if (chain.family() != OperatorValue.PLUS)
            return List.of(operation);

        SearchContext search = new SearchContext();
        List<List<Term>> variants = groupVariants(chain.terms(), search);
        Map<String, TokenNode> forms = new LinkedHashMap<>();
        int originalSize = chain.terms().size();

        for (List<Term> terms : variants) {
            // only variants where at least one common factor was actually taken out
            if (terms.size() >= originalSize)
                continue;

            Chain rebuiltChain = new Chain(chain.family(), terms);

            // keep both sign-aware and sign-grouped builds so no tied form is lost
            addForm(forms, BalancedTreeBuilder.buildBalanced(rebuiltChain));
            addForm(forms, BalancedTreeBuilder.buildSigned(rebuiltChain));

            if (forms.size() >= VARIANT_LIMIT)
                break;
        }

        return List.copyOf(forms.values());
    }

    private void addForm(Map<String, TokenNode> forms, TokenNode form) {
        forms.putIfAbsent(TreeWalker.structuralKey(form), form);
    }

    // find best grouping
    private List<Term> groupFactors(List<Term> terms, SearchContext search) {
        String state = signature(terms, search);
        List<Term> cached = search.memo.get(state);

        if (cached != null)
            return cached;

        if (--search.budget <= 0)
            return terms;

        List<Term> best = terms;
        int bestCost = cost(terms);

        // try groupings
        for (Grouping grouping : options(terms, search)) {
            List<Term> candidate = groupFactors(applyGrouping(terms, grouping, search), search);
            int candidateCost = cost(candidate);

            if (candidateCost < bestCost) {
                best = candidate;
                bestCost = candidateCost;
            }
        }

        search.memo.put(state, best);
        return best;
    }

    // find all distinct groupings within the search budget
    private List<List<Term>> groupVariants(List<Term> terms, SearchContext search) {
        String state = orderedSignature(terms, search);
        List<List<Term>> cached = search.variantMemo.get(state);
        if (cached != null)
            return cached;

        if (--search.variantBudget <= 0)
            return List.of(terms);

        Map<String, List<Term>> results = new LinkedHashMap<>();
        results.put(state, terms);

        for (Grouping grouping : options(terms, search)) {
            for (List<Term> grouped : applyGroupingVariants(terms, grouping, search)) {
                results.putIfAbsent(orderedSignature(grouped, search), grouped);

                for (List<Term> further : groupVariants(grouped, search)) {
                    results.putIfAbsent(orderedSignature(further, search), further);

                    if (results.size() >= VARIANT_LIMIT)
                        break;
                }

                if (results.size() >= VARIANT_LIMIT)
                    break;
            }

            if (results.size() >= VARIANT_LIMIT)
                break;
        }

        List<List<Term>> variants = List.copyOf(results.values());
        search.variantMemo.put(state, variants);
        return variants;
    }

    private List<List<Term>> applyGroupingVariants(List<Term> terms, Grouping grouping, SearchContext search) {
        List<Term> remainders = new ArrayList<>();
        for (int index : grouping.indexes())
            remainders.add(remainderTerm(terms.get(index), factorsOf(terms.get(index).node(), search), grouping.factor()));

        // handle signs
        boolean allNegative = remainders.stream().allMatch(term -> term.sign() == TermSign.NEGATIVE);
        TermSign groupSign = allNegative ? TermSign.NEGATIVE : TermSign.POSITIVE;
        List<Term> inner = allNegative ? flipped(remainders) : remainders;

        List<List<Term>> variants = new ArrayList<>();

        for (List<Term> innerVariant : groupVariants(inner, search)) {
            TokenNode innerSum = BalancedTreeBuilder.buildBalanced(new Chain(OperatorValue.PLUS, innerVariant));
            TokenNode groupNode = grouping.factor().sign() == TermSign.POSITIVE
                    ? NodeFactory.operation(OperatorValue.MULTIPLY, grouping.factor().node(), innerSum)
                    : NodeFactory.operation(OperatorValue.DIVISION, innerSum, grouping.factor().node());

            variants.add(insertGroup(terms, grouping.indexes(), new Term(groupNode, groupSign)));

            if (variants.size() >= VARIANT_LIMIT)
                break;
        }

        return variants;
    }

    private String orderedSignature(List<Term> terms, SearchContext search) {
        List<String> keys = new ArrayList<>(terms.size());

        for (Term term : terms)
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node(), search));

        return String.join(";", keys);
    }

    private List<Grouping> options(List<Term> terms, SearchContext search) {
        // get factors per term
        List<List<Factor>> termFactors = new ArrayList<>();
        for (Term term : terms)
            termFactors.add(factorsOf(term.node(), search));

        List<Grouping> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (List<Factor> factors : termFactors) {
            for (Factor candidate : factors) {
                // skip seen
                if (!seen.add(candidate.sign() + "|" + candidate.norm()))
                    continue;

                // find matches
                List<Integer> indexes = matchingIndexes(termFactors, candidate);
                if (indexes.size() >= 2 && !allNeutral(terms, termFactors, indexes, candidate, search))
                    result.add(new Grouping(indexes, candidate));
            }
        }

        return result;
    }

    // find matching term indexes
    private List<Integer> matchingIndexes(List<List<Factor>> termFactors, Factor candidate) {
        List<Integer> indexes = new ArrayList<>();

        for (int i = 0; i < termFactors.size(); i++) {
            if (termFactors.get(i).stream().anyMatch(factor -> provides(factor, candidate)))
                indexes.add(i);
        }

        return indexes;
    }

    // check if grouping is neutral
    private boolean allNeutral(List<Term> terms, List<List<Factor>> termFactors, List<Integer> indexes, Factor candidate, SearchContext search) {
        return indexes.stream().allMatch(index -> {
            Term remainder = remainderTerm(terms.get(index), termFactors.get(index), candidate);
            return isUnit(remainder) || key(remainder.node(), search).equals(key(terms.get(index).node(), search));
        });
    }

    private List<Term> applyGrouping(List<Term> terms, Grouping grouping, SearchContext search) {
        List<Term> remainders = new ArrayList<>();
        for (int index : grouping.indexes())
            remainders.add(remainderTerm(terms.get(index), factorsOf(terms.get(index).node(), search), grouping.factor()));

        // handle signs
        boolean allNegative = remainders.stream().allMatch(term -> term.sign() == TermSign.NEGATIVE);
        TermSign groupSign = allNegative ? TermSign.NEGATIVE : TermSign.POSITIVE;
        List<Term> inner = allNegative ? flipped(remainders) : remainders;

        // factorize inner
        inner = groupFactors(inner, search);

        // build group
        TokenNode innerSum = BalancedTreeBuilder.buildBalanced(new Chain(OperatorValue.PLUS, inner));
        TokenNode groupNode = grouping.factor().sign() == TermSign.POSITIVE
                ? NodeFactory.operation(OperatorValue.MULTIPLY, grouping.factor().node(), innerSum)
                : NodeFactory.operation(OperatorValue.DIVISION, innerSum, grouping.factor().node());

        return insertGroup(terms, grouping.indexes(), new Term(groupNode, groupSign));
    }

    private Term remainderTerm(Term term, List<Factor> factors, Factor target) {
        List<Factor> remaining = new ArrayList<>();
        TermSign sign = term.sign();
        boolean removed = false;

        for (Factor factor : factors) {
            if (!removed && provides(factor, target)) {
                if (opposite(factor, target))
                    sign = flip(sign);

                removed = true;
                continue;
            }

            remaining.add(factor);
        }

        return new Term(buildRemainder(remaining), sign);
    }

    private boolean provides(Factor factor, Factor candidate) {
        return equivalent(factor, candidate) || opposite(factor, candidate);
    }

    private boolean equivalent(Factor factor, Factor candidate) {
        return factor.sign() == candidate.sign() && factor.norm().equals(candidate.norm());
    }

    private boolean opposite(Factor factor, Factor candidate) {
        if (factor.sign() != candidate.sign())
            return false;

        return candidate.flip() != null && factor.norm().equals(candidate.flip())
                || factor.flip() != null && candidate.norm().equals(factor.flip());
    }

    private TokenNode buildRemainder(List<Factor> factors) {
        if (factors.isEmpty())
            return NodeFactory.value(oneToken());

        List<Term> terms = factors.stream()
                .map(factor -> new Term(factor.node(), factor.sign()))
                .toList();

        return BalancedTreeBuilder.buildBalanced(new Chain(OperatorValue.MULTIPLY, terms));
    }

    // insert group into list
    private List<Term> insertGroup(List<Term> terms, List<Integer> indexes, Term group) {
        boolean[] consumed = new boolean[terms.size()];
        for (int index : indexes)
            consumed[index] = true;

        List<Term> result = new ArrayList<>();
        boolean inserted = false;

        for (int i = 0; i < terms.size(); i++) {
            if (i == indexes.getFirst() && !inserted) {
                inserted = true;
                result.add(group);
            }

            if (!consumed[i])
                result.add(terms.get(i));
        }

        return result;
    }

    // flip all terms to positive
    private List<Term> flipped(List<Term> terms) {
        return terms.stream().map(term -> new Term(term.node(), TermSign.POSITIVE)).toList();
    }

    private boolean isUnit(Term term) {
        return term.node() instanceof ValueNode value && ArithmeticUtils.isOne(value.getToken());
    }

    private TermSign flip(TermSign sign) {
        return sign == TermSign.POSITIVE ? TermSign.NEGATIVE : TermSign.POSITIVE;
    }

    // get factors of node
    private List<Factor> factorsOf(TokenNode node, SearchContext search) {
        List<Factor> cached = search.factors.get(node);
        if (cached != null)
            return cached;

        List<Factor> factors;
        if (TreeWalker.chainFamily(node) != OperatorValue.MULTIPLY) {
            factors = List.of(factorOf(node, TermSign.POSITIVE, search));
        } else {
            factors = ChainAssembler.assemble(node, Map.of()).terms().stream()
                    .map(term -> factorOf(term.node(), term.sign(), search))
                    .toList();
        }

        search.factors.put(node, factors);
        return factors;
    }

    private Factor factorOf(TokenNode node, TermSign sign, SearchContext search) {
        Keys keys = keysOf(node, search);
        return new Factor(node, sign, keys.norm(), keys.flip());
    }

    private int cost(List<Term> terms) {
        int cost = 0;
        int positives = 0;
        int negatives = 0;

        for (Term term : terms) {
            cost += term.node().getWeight();

            if (term.sign() == TermSign.POSITIVE)
                positives++;
            else
                negatives++;
        }

        if (positives > 1)
            cost += positives - 1;

        if (negatives > 1)
            cost += negatives - 1;

        if (negatives > 0)
            cost += 1;

        return cost;
    }

    private String signature(List<Term> terms, SearchContext search) {
        List<String> keys = new ArrayList<>();

        for (Term term : terms)
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node(), search));

        Collections.sort(keys);
        return String.join(";", keys);
    }

    private Keys keysOf(TokenNode node, SearchContext search) {
        Keys cached = search.keys.get(node);
        if (cached != null)
            return cached;

        Keys keys = new Keys(computeKey(node, search), flipKey(node, search));
        search.keys.put(node, keys);
        return keys;
    }

    private String key(TokenNode node, SearchContext search) {
        return keysOf(node, search).norm();
    }

    private String computeKey(TokenNode node, SearchContext search) {
        OperatorValue family = TreeWalker.chainFamily(node);

        if (family == OperatorValue.PLUS)
            return sumKey(node, false, search);

        if (family == OperatorValue.MULTIPLY)
            return productKey(node, search);

        if (node instanceof FunctionNode function) {
            StringBuilder builder = new StringBuilder(function.getToken().value()).append('(');

            for (int i = 0; i < function.getArguments().size(); i++) {
                if (i > 0)
                    builder.append(',');

                builder.append(key(function.getArguments().get(i), search));
            }

            return builder.append(')').toString();
        }

        if (node instanceof OperationNode operation)
            return "(" + key(operation.getLeft(), search) + operation.getToken().value() + key(operation.getRight(), search) + ")";

        if (node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return valueKey(value.getToken());

        return node.getToken().value();
    }

    private String flipKey(TokenNode node, SearchContext search) {
        if (TreeWalker.chainFamily(node) == OperatorValue.PLUS)
            return sumKey(node, true, search);

        if (node instanceof ValueNode value && ArithmeticUtils.isNumeric(value.getToken()))
            return valueKey(negated(value.getToken()));

        return null;
    }

    private String sumKey(TokenNode node, boolean invert, SearchContext search) {
        List<String> keys = new ArrayList<>();

        for (Term term : ChainAssembler.assemble(node, Map.of()).terms()) {
            boolean negative = term.sign() == TermSign.POSITIVE;

            if (invert)
                negative = !negative;

            keys.add((negative ? "-" : "+") + key(term.node(), search));
        }

        Collections.sort(keys);
        return "{" + String.join(",", keys) + "}";
    }

    private String productKey(TokenNode node, SearchContext search) {
        List<String> keys = new ArrayList<>();

        for (Term term : ChainAssembler.assemble(node, Map.of()).terms())
            keys.add((term.sign() == TermSign.POSITIVE ? "+" : "-") + key(term.node(), search));

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

    private Token oneToken() {
        return new Token(NumberConstant.ONE, TokenType.INTEGER, 0);
    }

    private record Factor(TokenNode node, TermSign sign, String norm, String flip) {
    }

    private record Grouping(List<Integer> indexes, Factor factor) {
    }

    private record Keys(String norm, String flip) {
    }

    private static final class SearchContext {
        private final Map<String, List<Term>> memo = new HashMap<>();
        private final Map<String, List<List<Term>>> variantMemo = new HashMap<>();
        private final Map<TokenNode, Keys> keys = new WeakHashMap<>();
        private final Map<TokenNode, List<Factor>> factors = new WeakHashMap<>();
        private int budget = SEARCH_BUDGET;
        private int variantBudget = VARIANT_SEARCH_BUDGET;
    }
}
