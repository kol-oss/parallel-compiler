package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.pipeline.PassResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class OptimizationServiceTest {
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

    private static Stream<Arguments> provideSubtractionIdentityCases() {
        return Stream.of(
                Arguments.of("x-x", ValueNode.class, "0"),
                Arguments.of("a-a+b", ValueNode.class, "b"),
                Arguments.of("5-5", ValueNode.class, "0")
        );
    }

    private static Stream<Arguments> provideDivisionIdentityCases() {
        return Stream.of(
                Arguments.of("a/a", ValueNode.class, "1"),
                Arguments.of("(a+b)/(a+b)", ValueNode.class, "1"),
                Arguments.of("sin(x+1)/sin(x+1)", ValueNode.class, "1"),
                Arguments.of("a*b/(a*b)", ValueNode.class, "1"),
                Arguments.of("a/a/b", OperationNode.class, "/"),
                Arguments.of("a/a*b", ValueNode.class, "b")
        );
    }

    private static Stream<Arguments> provideNestedDivisionByZeroCases() {
        return Stream.of(
                Arguments.of("b/(b+b*0-1*b)", List.of(7)),
                Arguments.of("a*2/0+b/(b+b*0-1*b)-1/(c*2*4.76*(1-2+1))", List.of(4, 13, 33))
        );
    }

    @BeforeEach
    public void setUp() {
        lexicalAnalysisService = new LexicalAnalysisService();
        syntaxValidationService = new SyntaxValidationService();

        treeBuildService = new TreeBuildService();
        optimizationService = new OptimizationService();
    }

    @ParameterizedTest
    @MethodSource("provideConstantCases")
    public void givenExpressionWithConstants_whenOptimize_thenFoldsToValue(String expression, String expectedValue) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        PassResult result = optimizationService.apply(tree);

        TokenNode optimized = result.tree();

        assertNotNull(optimized);
        assertInstanceOf(ValueNode.class, optimized);
        assertEquals(expectedValue, optimized.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideChainCases")
    public void givenChainWithConstants_whenOptimize_thenConstantsFolded(String expression, String expectedValue) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        PassResult result = optimizationService.apply(tree);

        TokenNode optimized = result.tree();

        assertNotNull(optimized);
        assertInstanceOf(ValueNode.class, optimized);
        assertEquals(expectedValue, optimized.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideNeutralElementCases")
    public void givenExpressionWithNeutralElement_whenOptimize_thenNeutralRemoved(String expression, Class<?> expectedRootClass, String expectedRootSymbol) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        PassResult result = optimizationService.apply(tree);

        TokenNode optimized = result.tree();

        assertNotNull(optimized);
        assertInstanceOf(expectedRootClass, optimized);
        assertEquals(expectedRootSymbol, optimized.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideDivisionByZeroCases")
    public void givenDivisionByZero_whenOptimize_thenErrorReported(String expression, int expectedErrorCount) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);

        List<PositionedException> errors = optimizationService.apply(tree).diagnostics();

        assertEquals(expectedErrorCount, errors.size());
    }

    @ParameterizedTest
    @MethodSource("provideSubtractionIdentityCases")
    public void givenSubtractingSameTerm_whenOptimize_thenTermsCancel(String expression, Class<?> expectedRootClass, String expectedRootSymbol) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        PassResult result = optimizationService.apply(tree);

        TokenNode optimized = result.tree();

        assertNotNull(optimized);
        assertInstanceOf(expectedRootClass, optimized);
        assertEquals(expectedRootSymbol, optimized.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideDivisionIdentityCases")
    public void givenSameOperandsInFraction_whenOptimize_thenFractionCollapsesToOne(String expression, Class<?> expectedRootClass, String expectedRootSymbol) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        PassResult result = optimizationService.apply(tree);

        TokenNode optimized = result.tree();

        assertNotNull(optimized);
        assertInstanceOf(expectedRootClass, optimized);
        assertEquals(expectedRootSymbol, optimized.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideNestedDivisionByZeroCases")
    public void givenDivisorCollapsingToZero_whenOptimize_thenEveryDivisorReported(String expression, List<Integer> expectedErrorIndexes) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);

        List<PositionedException> errors = optimizationService.apply(tree).diagnostics();

        assertEquals(expectedErrorIndexes, errors.stream().map(PositionedException::getIndex).toList());
    }
}
