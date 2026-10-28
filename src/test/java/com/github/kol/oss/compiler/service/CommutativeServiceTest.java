package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.pipeline.TransformationResult;
import com.github.kol.oss.compiler.utils.TreeMetrics;
import com.github.kol.oss.compiler.utils.TreeWalker;
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
        assertFormsTie(result.forms());
    }

    @Test
    public void givenTwoTerms_whenApplyCommutative_thenOnlySwappedFormReturned() {
        TransformationResult result = applyCommutativeResult("a+b");

        assertEquals("(a+b)", toKey(result.tree()));
        assertEquals(1, result.forms().size());
        assertEquals("(b+a)", toKey(result.forms().getFirst()));
    }

    @Test
    public void givenSumOfFourTerms_whenApplyCommutative_thenEveryReturnedFormIsOptimal() {
        TransformationResult result = applyCommutativeResult("a+b+c+d");

        assertEquals(24, result.forms().size());
        assertFormsTie(result.forms());
        assertEquals(2, TreeMetrics.height(result.forms().getFirst()));
        assertEquals(2, TreeMetrics.width(result.forms().getFirst()));
    }

    @Test
    public void givenK4_whenApplyCommutative_thenFormsReachMinimalHeightAndMaximalWidth() {
        String expression = "a + b*c + d + e*f*g + h*i + j*(k + L + m*(n - p*q + r) - s*t)";
        String ideal = "j * (m * ((n + r) - p * q) + ((k + L) - s * t)) + (((a + d) + b * c) + (e * (f * g) + h * i))";

        TransformationResult result = applyCommutativeResult(expression);
        TokenNode idealTree = parseExpression(ideal);

        assertEquals(6, TreeMetrics.height(result.tree()));
        assertEquals(6, TreeMetrics.width(result.tree()));
        assertFormsTie(result.forms());
        assertEquals(6, TreeMetrics.height(result.forms().getFirst()));
        assertEquals(6, TreeMetrics.width(result.forms().getFirst()));
        assertTrue(result.forms().stream()
                .anyMatch(form -> TreeWalker.structuralKey(form).equals(TreeWalker.structuralKey(idealTree))));
    }

    @Test
    public void givenExpression_whenApplyCommutative_thenInputFormExcludedFromForms() {
        TokenNode input = commutativeInput("a+b+c+d");
        TransformationResult result = commutativeService.apply(input);
        String inputKey = TreeWalker.structuralKey(input);

        assertTrue(result.forms().stream()
                .noneMatch(form -> TreeWalker.structuralKey(form).equals(inputKey)));
    }

    private TokenNode applyCommutative(String expression) {
        return applyCommutativeResult(expression).tree();
    }

    private TransformationResult applyCommutativeResult(String expression) {
        return commutativeService.apply(commutativeInput(expression));
    }

    private TokenNode commutativeInput(String expression) {
        return optimizationService.apply(parseExpression(expression)).tree();
    }

    private TokenNode parseExpression(String expression) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        return treeBuildService.buildTree(tokens);
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

    // every returned form must share the same optimal height and width
    private void assertFormsTie(List<TokenNode> forms) {
        int height = TreeMetrics.height(forms.getFirst());
        int width = TreeMetrics.width(forms.getFirst());

        for (TokenNode form : forms) {
            assertEquals(height, TreeMetrics.height(form), toKey(form));
            assertEquals(width, TreeMetrics.width(form), toKey(form));
        }
    }
}
