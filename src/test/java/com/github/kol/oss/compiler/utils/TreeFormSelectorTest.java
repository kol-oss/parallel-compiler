package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeFormSelectorTest {
    private static final TokenNode ORIGINAL = plus(variable("a"), variable("b"));
    private static final TokenNode BALANCED = plus(plus(variable("a"), variable("b")), plus(variable("c"), variable("d")));
    private static final TokenNode NARROW = plus(plus(variable("a"), variable("b")), variable("c"));
    private static final TokenNode DEEP = plus(plus(plus(variable("a"), variable("b")), variable("c")), variable("d"));

    @Test
    public void givenCandidates_whenSelectRanked_thenMinimalHeightAndMaximalWidthGoFirst() {
        List<TokenNode> ranked = TreeFormSelector.selectRanked(List.of(DEEP, NARROW, BALANCED, ORIGINAL), 10);

        assertEquals(
                keys(ORIGINAL, BALANCED, NARROW, DEEP),
                keys(ranked)
        );
    }

    @Test
    public void givenDuplicates_whenSelectRanked_thenEachFormAppearsOnce() {
        TokenNode duplicate = plus(plus(variable("a"), variable("b")), plus(variable("c"), variable("d")));

        List<TokenNode> ranked = TreeFormSelector.selectRanked(List.of(ORIGINAL, BALANCED, duplicate), 10);

        assertEquals(keys(ORIGINAL, BALANCED), keys(ranked));
    }

    @Test
    public void givenLimit_whenSelectRanked_thenOnlyBestFormsAreReturned() {
        assertEquals(keys(ORIGINAL, BALANCED), keys(TreeFormSelector.selectRanked(List.of(DEEP, NARROW, BALANCED, ORIGINAL), 2)));
        assertTrue(TreeFormSelector.selectRanked(List.of(DEEP, NARROW, BALANCED, ORIGINAL), 0).isEmpty());
    }

    @Test
    public void givenMandatoryForm_whenSelectRanked_thenMandatoryFormSurvivesTheLimit() {
        List<TokenNode> ranked = TreeFormSelector.selectRanked(
                List.of(DEEP, NARROW, BALANCED, ORIGINAL),
                2,
                Set.of(TreeWalker.structuralKey(DEEP))
        );

        assertEquals(2, ranked.size());
        assertTrue(keys(ranked).contains(TreeWalker.structuralKey(DEEP)));
        assertTrue(keys(ranked).contains(TreeWalker.structuralKey(ORIGINAL)));
    }

    @Test
    public void givenUnknownMandatoryKey_whenSelectRanked_thenLimitIsUnchanged() {
        List<TokenNode> ranked = TreeFormSelector.selectRanked(
                List.of(DEEP, NARROW, BALANCED, ORIGINAL),
                2,
                Set.of("unknown")
        );

        assertEquals(keys(ORIGINAL, BALANCED), keys(ranked));
    }

    private static List<String> keys(TokenNode... nodes) {
        return java.util.Arrays.stream(nodes).map(TreeWalker::structuralKey).toList();
    }

    private static List<String> keys(List<TokenNode> nodes) {
        return nodes.stream().map(TreeWalker::structuralKey).toList();
    }

    private static TokenNode variable(String name) {
        return NodeFactory.value(new Token(name, TokenType.VARIABLE, 0));
    }

    private static TokenNode plus(TokenNode left, TokenNode right) {
        return NodeFactory.operation(OperatorValue.PLUS, left, right);
    }
}
