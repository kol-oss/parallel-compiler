package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.change.TransformationResult;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.exception.PositionedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class OptimizationServiceTest {
    private ExceptionHandler exceptionHandler;

    private LexicalAnalysisService lexicalAnalysisService;
    private SyntaxValidationService syntaxValidationService;

    private TreeBuildService treeBuildService;
    private OptimizationService optimizationService;

    private static Stream<Arguments> provideConstantCases() {
        return Stream.of(
                Arguments.of("1+2", "3"),
                Arguments.of("5-2", "3"),
                Arguments.of("2*3", "6"),
                Arguments.of("6/2", "3"),
                Arguments.of("2^3", "8"),
                Arguments.of("(1+2)*(3+4)", "21"),
                Arguments.of("1+2*3", "7")
        );
    }

    private static Stream<Arguments> provideChainCases() {
        return Stream.of(
                Arguments.of("1+2+3", "6"),
                Arguments.of("5+2-1", "6"),
                Arguments.of("2*3*4", "24"),
                Arguments.of("24/2/3", "4"),
                Arguments.of("10-1-2-3", "4")
        );
    }

    private static Stream<Arguments> provideNeutralElementCases() {
        return Stream.of(
                Arguments.of("a+0", ValueNode.class, "a"),
                Arguments.of("0+a", ValueNode.class, "a"),
                Arguments.of("a*1", ValueNode.class, "a"),
                Arguments.of("1*a", ValueNode.class, "a"),
                Arguments.of("a+0+b", OperationNode.class, "+"),
                Arguments.of("a*1*b", OperationNode.class, "*")
        );
    }

    private static Stream<Arguments> provideDivisionByZeroCases() {
        return Stream.of(
                Arguments.of("a/0", 1),
                Arguments.of("a/b", 0),
                Arguments.of("5/0", 1),
                Arguments.of("a/b/0/c", 1)
        );
    }

    @BeforeEach
    public void setUp() {
        exceptionHandler = new ExceptionHandler();

        lexicalAnalysisService = new LexicalAnalysisService(exceptionHandler);
        syntaxValidationService = new SyntaxValidationService(exceptionHandler);

        treeBuildService = new TreeBuildService();
        optimizationService = new OptimizationService(exceptionHandler);
    }

    @ParameterizedTest
    @MethodSource("provideConstantCases")
    public void givenExpressionWithConstants_whenOptimize_thenFoldsToValue(String expression, String expectedValue) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TransformationResult result = optimizationService.optimize(tree);

        TokenNode optimized = result.tree();

        assertNotNull(optimized);
        assertInstanceOf(ValueNode.class, optimized);
        assertEquals(expectedValue, optimized.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideChainCases")
    public void givenChainWithConstants_whenOptimize_thenConstantsFolded(String expression, String expectedValue) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TransformationResult result = optimizationService.optimize(tree);

        TokenNode optimized = result.tree();

        assertNotNull(optimized);
        assertInstanceOf(ValueNode.class, optimized);
        assertEquals(expectedValue, optimized.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideNeutralElementCases")
    public void givenExpressionWithNeutralElement_whenOptimize_thenNeutralRemoved(String expression, Class<?> expectedRootClass, String expectedRootSymbol) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TransformationResult result = optimizationService.optimize(tree);

        TokenNode optimized = result.tree();

        assertNotNull(optimized);
        assertInstanceOf(expectedRootClass, optimized);
        assertEquals(expectedRootSymbol, optimized.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideDivisionByZeroCases")
    public void givenDivisionByZero_whenOptimize_thenErrorReported(String expression, int expectedErrorCount) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        optimizationService.optimize(tree);

        List<PositionedException> errors = exceptionHandler.getAllAndClear();

        assertEquals(expectedErrorCount, errors.size());
    }
}
