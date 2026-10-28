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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssociativeServiceTest {
    private LexicalAnalysisService lexicalAnalysisService;
    private SyntaxValidationService syntaxValidationService;

    private TreeBuildService treeBuildService;
    private OptimizationService optimizationService;

    private CommutativeService commutativeService;
    private AssociativeService associativeService;

    private static Stream<Arguments> provideCommonMultiplierCases() {
        return Stream.of(
                Arguments.of("a*b+a*c+b*c", "((a*(b+c))+(b*c))"),
                Arguments.of("a*m+n*m", "(m*(a+n))"),
                Arguments.of("x*y+x*z", "(x*(y+z))")
        );
    }

    private static Stream<Arguments> provideSharedBracketCases() {
        return Stream.of(
                Arguments.of("a*(b-2)+c*(b-2)", "((b-2)*(a+c))"),
                Arguments.of("a*b-2*a+b*c-c*2", "((b-2)*(a+c))"),
                Arguments.of("x*(a+b)+y*(a+b)", "((a+b)*(x+y))")
        );
    }

    private static Stream<Arguments> provideCommonDivisorCases() {
        return Stream.of(
                Arguments.of("a/b-c/b+2/b", "(((a+2)-c)/b)"),
                Arguments.of("x/y+z/y", "((x+z)/y)"),
                Arguments.of("a/b-c/b", "((a-c)/b)"),
                Arguments.of("a/(b-1)-c/(b-1)+2/(b-1)-t", "((((a+2)-c)/(b-1))-t)")
        );
    }

    private static Stream<Arguments> provideWithoutCommonFactorCases() {
        return Stream.of(
                Arguments.of("a+b", "(a+b)"),
                Arguments.of("a*b+c*d", "((a*b)+(c*d))"),
                Arguments.of("a/b+c/d", "((a/b)+(c/d))")
        );
    }

    private static Stream<Arguments> provideClosedParenthesesCases() {
        return Stream.of(
                Arguments.of("a-b*k+b*t-f*f*5.9+f*q+g*f*5.9-g*q-f/(d+q-w)-g/(d+q-w)",
                        "((((g-f)*((5.9*f)-q))+(b*(t-k)))+(a-((f+g)/((d+q)-w))))"),
                Arguments.of("a-b*k+b*t-b*f*f*5.9+b*f*q+b*g*f*5.9-b*g*q-b*w/p+b*y*m/p-b*y/p-x*x/(d+q-w)+3*x/(d+q-w)+3*x/(d+q-w)+3*3/(d+q-w)",
                        "((((x*((3+3)-x))+9)/((d+q)-w))+((b*((((f*5.9)-q)*(g-f))+((((y*(m-1))-w)/p)+(t-k))))+a))")
        );
    }

    private static Stream<Arguments> provideRecordedTransformationCases() {
        return Stream.of(
                Arguments.of("a*b+a*c+b*c", 1),
                Arguments.of("a/(b-1)-c/(b-1)+2/(b-1)-t", 1),
                Arguments.of("a-b*k+b*t-f*f*5.9+f*q+g*f*5.9-g*q-f/(d+q-w)-g/(d+q-w)", 1),
                Arguments.of("a+b", 0)
        );
    }

    @BeforeEach
    public void setUp() {
        lexicalAnalysisService = new LexicalAnalysisService();
        syntaxValidationService = new SyntaxValidationService();

        treeBuildService = new TreeBuildService();
        optimizationService = new OptimizationService();

        commutativeService = new CommutativeService();
        associativeService = new AssociativeService();
    }

    @ParameterizedTest
    @MethodSource("provideCommonMultiplierCases")
    public void givenSumWithCommonMultiplier_whenApplyAssociative_thenTermsGroupedByCommonFactor(String expression, String expectedExpressionFormKey) {
        TransformationResult result = applyAssociative(expression);
        TokenNode associated = result.tree();

        assertNotNull(associated);
        assertEquals(expectedExpressionFormKey, toKey(associated));
    }

    @ParameterizedTest
    @MethodSource("provideSharedBracketCases")
    public void givenSumWithSharedBracket_whenApplyAssociative_thenBracketClosedOverCommonFactor(String expression, String expectedExpressionFormKey) {
        TransformationResult result = applyAssociative(expression);
        TokenNode associated = result.tree();

        assertNotNull(associated);
        assertEquals(expectedExpressionFormKey, toKey(associated));
    }

    @ParameterizedTest
    @MethodSource("provideCommonDivisorCases")
    public void givenSumWithCommonDivisor_whenApplyAssociative_thenTermsGroupedOverCommonDivisor(String expression, String expectedExpressionFormKey) {
        TransformationResult result = applyAssociative(expression);
        TokenNode associated = result.tree();

        assertNotNull(associated);
        assertEquals(expectedExpressionFormKey, toKey(associated));
    }

    @ParameterizedTest
    @MethodSource("provideClosedParenthesesCases")
    public void givenSumWithScatteredFactors_whenApplyAssociative_thenCommonParenthesesClosed(String expression, String expectedExpressionFormKey) {
        TransformationResult result = applyAssociative(expression);
        TokenNode associated = result.tree();

        assertNotNull(associated);
        assertEquals(expectedExpressionFormKey, toKey(associated));
    }

    @ParameterizedTest
    @MethodSource("provideWithoutCommonFactorCases")
    public void givenSumWithoutCommonFactor_whenApplyAssociative_thenExpressionStaysUnchanged(String expression, String expectedExpressionFormKey) {
        TransformationResult result = applyAssociative(expression);
        TokenNode associated = result.tree();

        assertNotNull(associated);
        assertEquals(expectedExpressionFormKey, toKey(associated));
    }

    @ParameterizedTest
    @MethodSource("provideRecordedTransformationCases")
    public void givenExpression_whenApplyAssociative_thenTransformationsRecorded(String expression, int expectedTransformations) {
        TransformationResult result = applyAssociative(expression);

        assertEquals(expectedTransformations, result.applied().size());
    }

    @Test
    public void givenExpressionWithCommonFactor_whenApplyAssociative_thenRankedEquivalentFormsReturned() {
        TokenNode input = associativeInput("a*b+a*c+b*c");
        TransformationResult result = associativeService.apply(input);

        assertEquals(4, result.forms().size());
        assertEquivalent(result.forms());
        assertRanked(result.forms());
        assertEquals(3, TreeMetrics.height(result.forms().getFirst()));
        assertEquals(2, TreeMetrics.width(result.forms().getFirst()));
        assertTrue(result.forms().stream()
                .anyMatch(form -> TreeWalker.structuralKey(form).equals(TreeWalker.structuralKey(input))));
    }

    @Test
    public void givenA7_whenApplyAssociative_thenUpToLimitOfFormsRankedByBalance() {
        TokenNode input = associativeInput("a-b*k+b*t-b*f*f*5.9+b*f*q+b*g*f*5.9-b*g*q-b*w/p+b*y*m/p-b*y/p-x*x/(d+q-w)+3*x/(d+q-w)+3*x/(d+q-w)+3*3/(d+q-w)");
        TransformationResult result = associativeService.apply(input);

        List<TokenNode> forms = result.forms();

        assertFalse(forms.isEmpty());
        assertTrue(forms.size() <= 500);
        assertEquivalent(forms);
        assertRanked(forms);
        assertEquals(6, TreeMetrics.height(forms.getFirst()));
        assertEquals(14, TreeMetrics.width(forms.getFirst()));
        assertTrue(forms.stream()
                .anyMatch(form -> TreeWalker.structuralKey(form).equals(TreeWalker.structuralKey(input))));
    }

    @Test
    public void givenK4CommutativeOutput_whenApplyAssociative_thenBalancedHeightIsPreserved() {
        TokenNode input = associativeInput("a + b*c + d + e*f*g + h*i + j*(k + L + m*(n - p*q + r) - s*t)");
        TransformationResult result = associativeService.apply(input);

        assertEquals(6, TreeMetrics.height(input));
        assertEquals(6, TreeMetrics.width(input));
        assertEquals(6, TreeMetrics.height(result.tree()));
        assertEquals(6, TreeMetrics.width(result.tree()));

        // there is nothing to factor out, so the input itself is the only equivalent form
        assertEquals(1, result.forms().size());
        assertEquals(TreeWalker.structuralKey(input), TreeWalker.structuralKey(result.forms().getFirst()));
    }

    private TransformationResult applyAssociative(String expression) {
        return associativeService.apply(associativeInput(expression));
    }

    // associative service receives the primary form produced by the commutative service
    private TokenNode associativeInput(String expression) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TokenNode optimized = optimizationService.apply(tree).tree();
        return commutativeService.apply(optimized).tree();
    }

    private void assertEquivalent(List<TokenNode> forms) {
        Map<String, Double> values = Map.of(
                "a", 2.0,
                "b", 3.0,
                "c", 5.0
        );

        double expected = evaluate(forms.getFirst(), values);

        for (TokenNode form : forms)
            assertEquals(expected, evaluate(form, values), 1e-9);
    }

    // every returned form must not be worse than the previous one: height grows, then width shrinks
    private void assertRanked(List<TokenNode> forms) {
        int previousHeight = -1;
        int previousWidth = Integer.MAX_VALUE;

        for (TokenNode form : forms) {
            int height = TreeMetrics.height(form);
            int width = TreeMetrics.width(form);

            assertTrue(height > previousHeight || height == previousHeight && width <= previousWidth, toKey(form));

            previousHeight = height;
            previousWidth = width;
        }
    }
}
