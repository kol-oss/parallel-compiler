package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.change.TransformationResult;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.github.kol.oss.compiler.utils.TokenNodeFormatter.toKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AssociativeServiceTest {
    private ExceptionHandler exceptionHandler;

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

    private static Stream<Arguments> provideRecordedTransformationCases() {
        return Stream.of(
                Arguments.of("a*b+a*c+b*c", 1),
                Arguments.of("a/(b-1)-c/(b-1)+2/(b-1)-t", 1),
                Arguments.of("a+b", 0)
        );
    }

    @BeforeEach
    public void setUp() {
        exceptionHandler = new ExceptionHandler();

        lexicalAnalysisService = new LexicalAnalysisService(exceptionHandler);
        syntaxValidationService = new SyntaxValidationService(exceptionHandler);

        treeBuildService = new TreeBuildService();
        optimizationService = new OptimizationService(exceptionHandler);

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

    private TransformationResult applyAssociative(String expression) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TokenNode optimized = optimizationService.optimize(tree).tree();
        TokenNode commuted = commutativeService.applyCommutativeLaw(optimized).tree();

        return associativeService.applyAssociativeLaw(commuted);
    }
}
