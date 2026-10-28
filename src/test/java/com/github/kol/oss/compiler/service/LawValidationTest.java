package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.pipeline.TransformationResult;
import com.github.kol.oss.compiler.utils.TreeMetrics;
import com.github.kol.oss.compiler.utils.TreeWalker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.kol.oss.compiler.utils.TokenNodeFormatter.toKey;
import static com.github.kol.oss.compiler.utils.TreeEvaluator.evaluate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Strict checks against the official examples from the course memo:
// K1-K5 for the commutative law and A1-A7 for the associative law.
public class LawValidationTest {
    private static final Map<String, Double> BASE_VALUES = Map.ofEntries(
            Map.entry("a", 2.3), Map.entry("b", 3.1), Map.entry("c", 1.7),
            Map.entry("d", 2.9), Map.entry("e", 1.3), Map.entry("f", 2.1),
            Map.entry("g", 1.9), Map.entry("h", 3.7), Map.entry("i", 1.1),
            Map.entry("j", 2.5), Map.entry("k", 1.4), Map.entry("L", 2.7),
            Map.entry("m", 1.6), Map.entry("n", 2.2), Map.entry("p", 3.3),
            Map.entry("q", 1.2), Map.entry("r", 2.8), Map.entry("s", 1.8),
            Map.entry("t", 3.5), Map.entry("w", 0.9), Map.entry("x", 2.4),
            Map.entry("y", 1.5), Map.entry("A", 2.0), Map.entry("B", 1.6),
            Map.entry("D", 2.6), Map.entry("H", 3.2), Map.entry("J", 0.8),
            Map.entry("P", 1.9), Map.entry("Q", 2.9), Map.entry("T", 1.7),
            Map.entry("dt", 1.9), Map.entry("pi", 3.14)
    );

    private LexicalAnalysisService lexicalAnalysisService;
    private SyntaxValidationService syntaxValidationService;
    private TreeBuildService treeBuildService;
    private OptimizationService optimizationService;
    private CommutativeService commutativeService;
    private AssociativeService associativeService;

    private static Stream<Arguments> provideCommutativeCases() {
        return Stream.of(
                Arguments.of("K1", "a-b*c+k"),
                Arguments.of("K2", "A - B*c - L*k*2 + D*t/d*y - H + Q*3 - J*(w-1)/r + P"),
                Arguments.of("K3", "A-B*c-J*(d*t*j-u*t+c*r-1+w-k/q+m*(n-k*s+z*(y+u*p-y/r-5)+x+t/2))/r+P"),
                Arguments.of("K4", "a + b*c + d + e*f*g + h*i + j*(k + L + m*(n-p*q+r) - s*t)"),
                Arguments.of("K5", "exp(sin(a+b/2-pi)+a*cos(a*pi+b*pi/3-w+k*t)-5+log(2.72)/T-1)+2048+a+b*c+log(t-1)-2*log(Q)-8*d/dt*exp(t/2+H)-sin(a)/cos(a)")
        );
    }

    private static Stream<Arguments> provideAssociativeCases() {
        return Stream.of(
                Arguments.of("A1", "a*b+a*c+b*c", "((a*(b+c))+(b*c))"),
                Arguments.of("A2", "a*(b-2)+c*(b-2)", "((b-2)*(a+c))"),
                Arguments.of("A3", "a*b-2*a+b*c-c*2", "((b-2)*(a+c))"),
                Arguments.of("A4", "a/b-c/b+2/b", "(((a+2)-c)/b)"),
                Arguments.of("A5", "a/(b-1)-c/(b-1)+2/(b-1)-t", "((((a+2)-c)/(b-1))-t)"),
                Arguments.of("A6", "a-b*k+b*t-f*f*5.9+f*q+g*f*5.9-g*q-f/(d+q-w)-g/(d+q-w)",
                        "((((g-f)*((5.9*f)-q))+(b*(t-k)))+(a-((f+g)/((d+q)-w))))"),
                Arguments.of("A7", "a-b*k+b*t-b*f*f*5.9+b*f*q+b*g*f*5.9-b*g*q-b*w/p+b*y*m/p-b*y/p-x*x/(d+q-w)+3*x/(d+q-w)+3*x/(d+q-w)+3*3/(d+q-w)",
                        null)
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCommutativeCases")
    public void givenOfficialCommutativeCase_whenApplyCommutative_thenFormsAreUniqueEquivalentAndOptimal(String name, String expression) {
        TokenNode input = optimized(expression);
        TransformationResult result = commutativeService.apply(input);

        assertStrictFormSet(result.forms(), name);
        assertFormsTie(result.forms(), name);
        // the commutative rewrite is a heuristic: when it is optimal it is part of the set,
        // otherwise the permutation search must return a strictly better (or equal) form
        assertTrue(TreeMetrics.height(result.forms().getFirst()) <= TreeMetrics.height(result.tree()),
                name + ": no form is better than the heuristic primary");
        assertTrue(TreeMetrics.height(result.tree()) <= TreeMetrics.height(input),
                name + ": commutative transformation increased the tree height");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideAssociativeCases")
    public void givenOfficialAssociativeCase_whenApplyAssociative_thenFormsAreUniqueEquivalentAndRanked(String name, String expression, String expectedFormKey) {
        TokenNode input = commutative(expression);
        TransformationResult result = associativeService.apply(input);

        assertStrictFormSet(result.forms(), name);
        assertRanked(result.forms(), name);
        assertTrue(result.forms().stream()
                .anyMatch(form -> TreeWalker.structuralKey(form).equals(TreeWalker.structuralKey(result.tree()))),
                name + ": primary tree is missing from the equivalent forms");
        assertTrue(result.forms().stream()
                .anyMatch(form -> TreeWalker.structuralKey(form).equals(TreeWalker.structuralKey(input))),
                name + ": the incoming associative form is missing from the equivalent forms");

        if (expectedFormKey != null)
            assertTrue(result.forms().stream().anyMatch(form -> toKey(form).equals(expectedFormKey)),
                    name + ": expected form " + expectedFormKey + " is missing");
    }

    @Test
    public void givenA7_whenApplyAssociative_thenBestFormIsBalancedAndSetIsCapped() {
        String expression = "a-b*k+b*t-b*f*f*5.9+b*f*q+b*g*f*5.9-b*g*q-b*w/p+b*y*m/p-b*y/p"
                + "-x*x/(d+q-w)+3*x/(d+q-w)+3*x/(d+q-w)+3*3/(d+q-w)";
        TransformationResult result = associativeService.apply(commutative(expression));

        assertTrue(result.forms().size() <= 500);
        assertTrue(result.forms().size() > 1);
        assertEquals(6, TreeMetrics.height(result.forms().getFirst()));
    }

    @Test
    public void givenFactorSharedBySeveralTerms_whenApplyAssociative_thenPartialFactoringsAreAvailable() {
        TransformationResult result = associativeService.apply(commutative("a*b+a*c+a*d+e"));

        assertStrictFormSet(result.forms(), "partial subset factoring");
        assertTrue(result.forms().size() > 1, "only the complete factoring was generated");
        assertTrue(result.forms().stream().anyMatch(form -> countValue(form, "a") >= 2),
                "no form leaves some terms unfactored, so subset factoring is missing");
    }

    private TokenNode optimized(String expression) {
        return optimizationService.apply(parse(expression)).tree();
    }

    private TokenNode commutative(String expression) {
        return commutativeService.apply(optimized(expression)).tree();
    }

    private TokenNode parse(String expression) {
        List<Token> tokens = lexicalAnalysisService.tokenize(expression).tokens();
        syntaxValidationService.validate(tokens);
        return treeBuildService.buildTree(tokens);
    }

    private void assertStrictFormSet(List<TokenNode> forms, String label) {
        assertNotNull(forms, label);
        assertFalse(forms.isEmpty(), label + ": no equivalent forms");

        Set<String> keys = new HashSet<>();
        for (TokenNode form : forms) {
            assertNotNull(form, label + ": null form");
            String key = TreeWalker.structuralKey(form);

            assertTrue(keys.add(key), label + ": duplicate form " + toKey(form));
            assertEquivalentToFirst(form, forms.getFirst(), label);
        }
    }

    private void assertEquivalentToFirst(TokenNode form, TokenNode first, String label) {
        for (int round = 0; round < 3; round++) {
            Map<String, Double> values = shiftedValues(round);
            double actual = evaluate(form, values);
            double reference = evaluate(first, values);
            double tolerance = 1e-8 * Math.max(1.0, Math.max(Math.abs(reference), Math.abs(actual)));

            assertTrue(Double.isFinite(actual), label + ": non-finite form " + toKey(form));
            assertEquals(reference, actual, tolerance, label + ": not equivalent " + toKey(form));
        }
    }

    private Map<String, Double> shiftedValues(int round) {
        Map<String, Double> values = new HashMap<>();

        for (Map.Entry<String, Double> entry : BASE_VALUES.entrySet())
            values.put(entry.getKey(), entry.getValue() + round * 0.07);

        return values;
    }

    // commutative balancing must make every returned form equally optimal
    private void assertFormsTie(List<TokenNode> forms, String label) {
        int height = TreeMetrics.height(forms.getFirst());
        int width = TreeMetrics.width(forms.getFirst());

        for (TokenNode form : forms) {
            assertEquals(height, TreeMetrics.height(form), label + ": height differs for " + toKey(form));
            assertEquals(width, TreeMetrics.width(form), label + ": width differs for " + toKey(form));
        }
    }

    // associative forms are ordered from the shortest to the widest
    private void assertRanked(List<TokenNode> forms, String label) {
        int previousHeight = -1;
        int previousWidth = Integer.MAX_VALUE;

        for (TokenNode form : forms) {
            int height = TreeMetrics.height(form);
            int width = TreeMetrics.width(form);

            assertTrue(height > previousHeight || height == previousHeight && width <= previousWidth,
                    label + ": form is out of order " + toKey(form));

            previousHeight = height;
            previousWidth = width;
        }
    }

    private int countValue(TokenNode node, String name) {
        if (node instanceof ValueNode value)
            return value.getToken().value().equals(name) ? 1 : 0;

        if (node instanceof FunctionNode function)
            return function.getArguments().stream().mapToInt(argument -> countValue(argument, name)).sum();

        OperationNode operation = (OperationNode) node;
        return countValue(operation.getLeft(), name) + countValue(operation.getRight(), name);
    }
}
