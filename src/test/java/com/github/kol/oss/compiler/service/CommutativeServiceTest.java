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

public class CommutativeServiceTest {
    private ExceptionHandler exceptionHandler;

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

    @BeforeEach
    public void setUp() {
        exceptionHandler = new ExceptionHandler();

        lexicalAnalysisService = new LexicalAnalysisService(exceptionHandler);
        syntaxValidationService = new SyntaxValidationService(exceptionHandler);

        treeBuildService = new TreeBuildService();
        optimizationService = new OptimizationService(exceptionHandler);
        commutativeService = new CommutativeService();
    }

    @ParameterizedTest
    @MethodSource("provideHeavierTermsReorderedCases")
    public void givenSumWithDifferentWeights_whenApplyCommutative_thenHeavierTermsReordered(String expression, String expectedExpressionFormKey) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TransformationResult optimized = optimizationService.optimize(tree);
        TransformationResult result = commutativeService.applyCommutativeLaw(optimized.tree());

        TokenNode commuted = result.tree();

        assertNotNull(commuted);
        assertEquals(expectedExpressionFormKey, toKey(commuted));
    }

    @ParameterizedTest
    @MethodSource("provideHeavierFactorsReorderedCases")
    public void givenProductWithDifferentWeights_whenApplyCommutative_thenHeavierFactorsReordered(String expression, String expectedExpressionFormKey) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TransformationResult optimized = optimizationService.optimize(tree);
        TransformationResult result = commutativeService.applyCommutativeLaw(optimized.tree());

        TokenNode commuted = result.tree();

        assertNotNull(commuted);
        assertEquals(expectedExpressionFormKey, toKey(commuted));
    }

    @ParameterizedTest
    @MethodSource("providePositivesAndNegativesSeparatedCases")
    public void givenSignedChain_whenApplyCommutative_thenPositivesAndNegativesSeparated(String expression, String expectedExpressionFormKey) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TransformationResult optimized = optimizationService.optimize(tree);
        TransformationResult result = commutativeService.applyCommutativeLaw(optimized.tree());

        TokenNode commuted = result.tree();

        assertNotNull(commuted);
        assertEquals(expectedExpressionFormKey, toKey(commuted));
    }
}