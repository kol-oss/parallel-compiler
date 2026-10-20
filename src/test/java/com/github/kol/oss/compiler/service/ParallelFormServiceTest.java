package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.github.kol.oss.compiler.utils.TokenNodeFormatter.toKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ParallelFormServiceTest {
    private LexicalAnalysisService lexicalAnalysisService;
    private SyntaxValidationService syntaxValidationService;

    private TreeBuildService treeBuildService;
    private OptimizationService optimizationService;
    private CommutativeService commutativeService;

    private ParallelFormService parallelFormService;

    private static Stream<Arguments> provideSumChainCases() {
        return Stream.of(
                Arguments.of("a+b", "(a+b)"),
                Arguments.of("a+b+c", "(a+(b+c))"),
                Arguments.of("a+b+c+d", "((a+b)+(c+d))"),
                Arguments.of("a+b+c+d+e", "((a+b)+(c+(d+e)))")
        );
    }

    private static Stream<Arguments> provideProductChainCases() {
        return Stream.of(
                Arguments.of("a*b", "(a*b)"),
                Arguments.of("a*b*c", "(a*(b*c))"),
                Arguments.of("a*b*c*d", "((a*b)*(c*d))"),
                Arguments.of("a*b*c*d*e", "((a*b)*(c*(d*e)))")
        );
    }

    private static Stream<Arguments> provideSubtractionChainCases() {
        return Stream.of(
                Arguments.of("a-b", "(a-b)"),
                Arguments.of("a-b-c", "(a-(b+c))"),
                Arguments.of("a-b-c-d", "(a-(b+(c+d)))"),
                Arguments.of("a-b-c-d-e", "(a-((b+c)+(d+e)))")
        );
    }

    private static Stream<Arguments> provideDivisionChainCases() {
        return Stream.of(
                Arguments.of("a/b", "(a/b)"),
                Arguments.of("a/b/c", "(a/(b*c))"),
                Arguments.of("a/b/c/d", "(a/(b*(c*d)))"),
                Arguments.of("a/b/c/d/e", "(a/((b*c)*(d*e)))")
        );
    }

    @BeforeEach
    public void setUp() {
        lexicalAnalysisService = new LexicalAnalysisService();
        syntaxValidationService = new SyntaxValidationService();

        treeBuildService = new TreeBuildService();
        optimizationService = new OptimizationService();
        commutativeService = new CommutativeService();

        parallelFormService = new ParallelFormService();
    }

    @ParameterizedTest
    @MethodSource("provideSumChainCases")
    public void givenSumChain_whenBuildParallelTree_thenBalancedSumForm(String expression, String expectedStructureKey) {
        TokenNode parallel = buildParallelForm(expression);

        assertNotNull(parallel);
        assertEquals(expectedStructureKey, toKey(parallel));
    }

    @ParameterizedTest
    @MethodSource("provideProductChainCases")
    public void givenProductChain_whenBuildParallelTree_thenBalancedProductForm(String expression, String expectedStructureKey) {
        TokenNode parallel = buildParallelForm(expression);

        assertNotNull(parallel);
        assertEquals(expectedStructureKey, toKey(parallel));
    }

    @ParameterizedTest
    @MethodSource("provideSubtractionChainCases")
    public void givenSubtractionChain_whenBuildParallelTree_thenParallelSubtractionForm(String expression, String expectedStructureKey) {
        TokenNode parallel = buildParallelForm(expression);

        assertNotNull(parallel);
        assertEquals(expectedStructureKey, toKey(parallel));
    }

    @ParameterizedTest
    @MethodSource("provideDivisionChainCases")
    public void givenDivisionChain_whenBuildParallelTree_thenParallelDivisionForm(String expression, String expectedStructureKey) {
        TokenNode parallel = buildParallelForm(expression);

        assertNotNull(parallel);
        assertEquals(expectedStructureKey, toKey(parallel));
    }

    private TokenNode buildParallelForm(String expression) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);

        TokenNode tree = treeBuildService.buildTree(tokens);
        TokenNode optimized = optimizationService.apply(tree).tree();
        TokenNode commuted = commutativeService.apply(optimized).tree();

        return parallelFormService.apply(commuted).tree();
    }
}
