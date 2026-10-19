package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TreeBuildServiceTest {
    private ExceptionHandler exceptionHandler;

    private LexicalAnalysisService lexicalAnalysisService;
    private SyntaxValidationService syntaxValidationService;

    private TreeBuildService treeBuildService;

    private static Stream<Arguments> provideOperatorCases() {
        return Stream.of(
                Arguments.of("a+b", OperatorValue.PLUS),
                Arguments.of("a-b", OperatorValue.MINUS),
                Arguments.of("a*b", OperatorValue.MULTIPLY),
                Arguments.of("a/b", OperatorValue.DIVISION),
                Arguments.of("a^b", OperatorValue.EXPONENTIATION)
        );
    }

    private static Stream<Arguments> provideStructureCases() {
        return Stream.of(
                Arguments.of("1+2*3", "+", 2),
                Arguments.of("(1+2)*3", "*", 2),
                Arguments.of("a^b^c", "^", 2),
                Arguments.of("sin(x)", "sin", 1),
                Arguments.of("f(a,b)", "f", 2),
                Arguments.of("f(a,b,c)", "f", 3),
                Arguments.of("a*b+c", "+", 2),
                Arguments.of("a+b*c", "+", 2)
        );
    }

    private static Stream<Arguments> provideLeafCases() {
        return Stream.of(
                Arguments.of("5", "5"),
                Arguments.of("123", "123"),
                Arguments.of("5.2", "5.2"),
                Arguments.of("x", "x")
        );
    }

    @BeforeEach
    public void setUp() {
        exceptionHandler = new ExceptionHandler();

        lexicalAnalysisService = new LexicalAnalysisService(exceptionHandler);
        syntaxValidationService = new SyntaxValidationService(exceptionHandler);

        treeBuildService = new TreeBuildService();
    }

    @ParameterizedTest
    @MethodSource("provideOperatorCases")
    public void givenSimpleExpression_whenBuildTree_thenOperationNodeWithExpectedOperator(String expression, OperatorValue expectedRootOperator) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode root = treeBuildService.buildTree(tokens);

        assertNotNull(root);
        assertInstanceOf(OperationNode.class, root);
        assertEquals(expectedRootOperator.toSymbol(), root.getToken().value());
    }

    @ParameterizedTest
    @MethodSource("provideStructureCases")
    public void givenExpression_whenBuildTree_thenRootAndChildrenMatch(String expression, String expectedRootSymbol, int expectedChildrenCount) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode root = treeBuildService.buildTree(tokens);

        assertNotNull(root);
        assertEquals(expectedRootSymbol, root.getToken().value());

        if (root instanceof OperationNode) {
            assertNotNull(((OperationNode) root).getLeft());
            assertNotNull(((OperationNode) root).getRight());
        }

        if (root instanceof FunctionNode) {
            assertEquals(expectedChildrenCount, ((FunctionNode) root).getArguments().size());
        }
    }

    @ParameterizedTest
    @MethodSource("provideLeafCases")
    public void givenLeafExpression_whenBuildTree_thenValueNode(String expression, String expectedValue) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression);
        syntaxValidationService.validate(tokens);

        TokenNode root = treeBuildService.buildTree(tokens);

        assertNotNull(root);
        assertInstanceOf(ValueNode.class, root);
        assertEquals(expectedValue, root.getToken().value());
    }
}
