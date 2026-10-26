package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.pipeline.TransformationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.kol.oss.compiler.utils.TokenNodeFormatter.toKey;
import static com.github.kol.oss.compiler.utils.TreeEvaluator.evaluate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommutativeServiceTest {
    private LexicalAnalysisService lexicalAnalysisService;
    private SyntaxValidationService syntaxValidationService;

    private TreeBuildService treeBuildService;
    private OptimizationService optimizationService;
    private CommutativeService commutativeService;

    private static Stream<Arguments> provideHeavierTermsReorderedCases() {
        return Stream.of(
                Arguments.of("a+b*c", "((b*c)+a)"),
                Arguments.of("x + y*z + w", "((y*z)+(x+w))"),
                Arguments.of("1 + a*b + c", "((a*b)+(1+c))")
        );
    }

    private static Stream<Arguments> provideHeavierFactorsReorderedCases() {
        return Stream.of(
                Arguments.of("a*(b+c)", "((b+c)*a)"),
                Arguments.of("x * (y+z) * w", "((y+z)*(x*w))"),
                Arguments.of("2 * (a+b)", "((a+b)*2)")
        );
    }

    private static Stream<Arguments> providePositivesAndNegativesSeparatedCases() {
        return Stream.of(
                Arguments.of("a - b + c", "((a+c)-b)"),
                Arguments.of("a + b - c - d", "((a+b)-(c+d))"),
                Arguments.of("x*y/z", "((x*y)/z)")
        );
    }

    private static Stream<Arguments> provideCancelledTermsCases() {
        return Stream.of(
                Arguments.of("a + x - x + b*c", "((b*c)+a)"),
                Arguments.of("a - b + b - c*d", "(a-(c*d))")
        );
    }

    @BeforeEach
    public void setUp() {
        lexicalAnalysisService = new LexicalAnalysisService();
        syntaxValidationService = new SyntaxValidationService();

        treeBuildService = new TreeBuildService();
        optimizationService = new OptimizationService();
        commutativeService = new CommutativeService();
    }

    @ParameterizedTest
    @MethodSource("provideHeavierTermsReorderedCases")
    public void givenSumWithDifferentWeights_whenApplyCommutative_thenHeavierTermsReordered(String expression, String expectedExpressionFormKey) {
        TokenNode commuted = applyCommutative(expression);

        assertNotNull(commuted);
        assertEquals(expectedExpressionFormKey, toKey(commuted));
    }

    @ParameterizedTest
    @MethodSource("provideHeavierFactorsReorderedCases")
    public void givenProductWithDifferentWeights_whenApplyCommutative_thenHeavierFactorsReordered(String expression, String expectedExpressionFormKey) {
        TokenNode commuted = applyCommutative(expression);

        assertNotNull(commuted);
        assertEquals(expectedExpressionFormKey, toKey(commuted));
    }

    @ParameterizedTest
    @MethodSource("providePositivesAndNegativesSeparatedCases")
    public void givenSignedChain_whenApplyCommutative_thenPositivesAndNegativesSeparated(String expression, String expectedExpressionFormKey) {
        TokenNode commuted = applyCommutative(expression);

        assertNotNull(commuted);
        assertEquals(expectedExpressionFormKey, toKey(commuted));
    }

    @ParameterizedTest
    @MethodSource("provideCancelledTermsCases")
    public void givenSumWithCancelledTerms_whenApplyCommutative_thenRemainingTermsReordered(String expression, String expectedExpressionFormKey) {
        TokenNode commuted = applyCommutative(expression);

        assertNotNull(commuted);
        assertEquals(expectedExpressionFormKey, toKey(commuted));
    }

    @Test
    public void givenExpression_whenApplyCommutative_thenMultipleEquivalentFormsReturned() {
        TransformationResult result = applyCommutativeResult("a-b*c+k");

        assertTrue(result.forms().size() > 1);
        assertSame(result.tree(), result.forms().getFirst());
        assertEquivalent(result.forms());
    }

    private TokenNode applyCommutative(String expression) {
        return applyCommutativeResult(expression).tree();
    }

    private TransformationResult applyCommutativeResult(String expression) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TransformationResult optimized = optimizationService.apply(tree);

        return commutativeService.apply(optimized.tree());
    }

    private void assertEquivalent(List<TokenNode> forms) {
        Map<String, Double> values = Map.of(
                "a", 2.0,
                "b", 3.0,
                "c", 5.0,
                "k", 7.0
        );

        double expected = evaluate(forms.getFirst(), values);

        for (TokenNode form : forms)
            assertEquals(expected, evaluate(form, values), 1e-9);
    }
}
