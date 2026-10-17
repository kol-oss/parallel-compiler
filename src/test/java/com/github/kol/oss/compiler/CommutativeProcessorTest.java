package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.processor.CommutativeProcessor;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.TreeProcessor;
import com.github.kol.oss.compiler.utils.ExpressionPrinter;
import com.github.kol.oss.compiler.utils.TreeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommutativeProcessorTest {
    private final TreeProcessor treeProcessor = new TreeProcessor();

    private TokenNode build(String expression) {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        List<Token> tokens = new LexicalProcessor(exceptionHandler).process(expression);
        assertTrue(exceptionHandler.getAllAndClear().isEmpty(), "invalid expression: " + expression);

        return treeProcessor.buildTree(tokens);
    }

    private TokenNode commute(String expression) {
        return new CommutativeProcessor().process(build(expression));
    }

    private int height(TokenNode node) {
        if (node instanceof OperationNode operation)
            return 1 + Math.max(height(operation.getLeft()), height(operation.getRight()));

        if (node instanceof FunctionNode function)
            return 1 + function.getArguments().stream().mapToInt(this::height).max().orElse(0);

        return 0;
    }

    static Stream<String> provideConstantExpressions() {
        return Stream.of(
                "2 + 3*4 + 5 - 6",
                "100 - 10*2 + 3*3*3 - 4",
                "2*3 + 4*5 - 6*7 + 8",
                "8/4/2 + 6*6 - 1",
                "10/4/5*8 + 2*9 - 30"
        );
    }

    @Test
    @DisplayName("Lab example: a + b*c + d becomes (b*c) + (a + d)")
    public void givenSumChain_whenCommuted_thenHeaviestTermFirstAndBalanced() {
        TokenNode result = commute("a + b*c + d");

        OperationNode root = assertInstanceOf(OperationNode.class, result);
        assertEquals("+", root.getToken().value());

        OperationNode left = assertInstanceOf(OperationNode.class, root.getLeft());
        assertEquals("*", left.getToken().value());
        assertEquals("b", left.getLeft().getToken().value());
        assertEquals("c", left.getRight().getToken().value());

        OperationNode right = assertInstanceOf(OperationNode.class, root.getRight());
        assertEquals("+", right.getToken().value());
        assertEquals("a", right.getLeft().getToken().value());
        assertEquals("d", right.getRight().getToken().value());

        assertEquals(2, height(result));
    }

    @Test
    @DisplayName("a - b*c + k groups positive terms: (a + k) - b*c")
    public void givenMixedSumChain_whenCommuted_thenPositivesGrouped() {
        TokenNode result = commute("a - b*c + k");

        OperationNode root = assertInstanceOf(OperationNode.class, result);
        assertEquals("-", root.getToken().value());

        OperationNode positives = assertInstanceOf(OperationNode.class, root.getLeft());
        assertEquals("+", positives.getToken().value());
        assertEquals("a", positives.getLeft().getToken().value());
        assertEquals("k", positives.getRight().getToken().value());

        OperationNode negative = assertInstanceOf(OperationNode.class, root.getRight());
        assertEquals("*", negative.getToken().value());
        assertEquals("b", negative.getLeft().getToken().value());

        assertEquals(2, height(result));
    }

    @Test
    @DisplayName("a/b*c keeps value: divisors regrouped to (a*c)/b")
    public void givenProductChain_whenCommuted_thenDivisorsGrouped() {
        TokenNode result = commute("a/b*c");

        OperationNode root = assertInstanceOf(OperationNode.class, result);
        assertEquals("/", root.getToken().value());

        OperationNode numerator = assertInstanceOf(OperationNode.class, root.getLeft());
        assertEquals("*", numerator.getToken().value());
        assertEquals("a", numerator.getLeft().getToken().value());
        assertEquals("c", numerator.getRight().getToken().value());

        assertEquals("b", root.getRight().getToken().value());
    }

    @Test
    @DisplayName("Unary minus form -x + y*z becomes y * z - x without a stray zero")
    public void givenUnaryMinusTerm_whenCommuted_thenNeutralZeroDropped() {
        assertEquals("y * z - x", ExpressionPrinter.print(commute("-x + y*z")));
    }

    @Test
    @DisplayName("Exponentiation chain is not reordered")
    public void givenExponentiation_whenCommuted_thenStructureKept() {
        TokenNode result = commute("a^b^c");

        OperationNode root = assertInstanceOf(OperationNode.class, result);
        assertEquals("^", root.getToken().value());
        assertEquals("a", root.getLeft().getToken().value());

        OperationNode right = assertInstanceOf(OperationNode.class, root.getRight());
        assertEquals("^", right.getToken().value());
    }

    @Test
    @DisplayName("Commutative transformation of the lab example is recorded")
    public void givenReorderableExpression_whenCommuted_thenLogPopulated() {
        CommutativeProcessor processor = new CommutativeProcessor();
        processor.process(build("a + b*c + d"));

        assertEquals(List.of("a + b * c + d => b * c + (a + d)"), processor.getTransformations());
    }

    @ParameterizedTest
    @DisplayName("Constant expressions keep their value after commuting")
    @MethodSource("provideConstantExpressions")
    public void givenConstantExpression_whenCommuted_thenValueUnchanged(String expression) {
        Optional<Token> before = treeProcessor.evaluate(build(expression));
        Optional<Token> after = treeProcessor.evaluate(new CommutativeProcessor().process(build(expression)));

        assertTrue(before.isPresent());
        assertTrue(after.isPresent());
        assertEquals(before.get().value(), after.get().value());
    }

    @Test
    @DisplayName("Lab test #2: heavy terms are sorted and the tree becomes shorter")
    public void givenLabExpressionTwo_whenCommuted_thenShorterBalancedTree() {
        String expression = "A - B*c - L*k*2 + D*t/d*y - H + Q*3 - J*(w-1)/r + P";
        TokenNode original = build(expression);
        TokenNode result = new CommutativeProcessor().process(build(expression));

        assertTrue(height(result) < height(original));

        OperationNode root = assertInstanceOf(OperationNode.class, result);
        assertEquals("-", root.getToken().value());

        OperationNode positives = assertInstanceOf(OperationNode.class, root.getLeft());
        assertEquals("+", positives.getToken().value());
        // heaviest positive term D*t/d*y is rebuilt as (D*t*y)/d
        assertEquals("/", positives.getLeft().getToken().value());

        OperationNode negatives = assertInstanceOf(OperationNode.class, root.getRight());
        assertEquals("+", negatives.getToken().value());
        // heaviest negative term J*(w-1)/r comes first
        assertEquals("/", negatives.getLeft().getToken().value());
    }

    @Test
    @DisplayName("Lab test #3: deeply nested expression is commuted without errors")
    public void givenLabExpressionThree_whenCommuted_thenBalancedRoot() {
        String expression = "A-B*c-J*(d*t*j-u*t+c*r-1+w-k/q+m*(n-k*s+z*(y+u*p-y/r-5)+x+t/2))/r+P";
        TokenNode original = build(expression);
        TokenNode result = new CommutativeProcessor().process(build(expression));

        assertTrue(height(result) < height(original));

        OperationNode root = assertInstanceOf(OperationNode.class, result);
        assertEquals("-", root.getToken().value());

        OperationNode positives = assertInstanceOf(OperationNode.class, root.getLeft());
        assertEquals("+", positives.getToken().value());
        assertEquals("A", positives.getLeft().getToken().value());

        OperationNode negatives = assertInstanceOf(OperationNode.class, root.getRight());
        assertEquals("+", negatives.getToken().value());
        assertEquals("/", negatives.getLeft().getToken().value());
    }

    @Test
    @DisplayName("Lab test #4: heaviest j*(...) term leads the sorted sum")
    public void givenLabExpressionFour_whenCommuted_thenHeaviestTermFirst() {
        String expression = "a + b*c + d + e*f*g + h*i + j*(k + L + m*(n-p*q+r) - s*t)";
        TokenNode original = build(expression);
        TokenNode result = new CommutativeProcessor().process(build(expression));

        assertTrue(height(result) < height(original));

        OperationNode root = assertInstanceOf(OperationNode.class, result);
        assertEquals("+", root.getToken().value());

        // the j*(...) term becomes the first branch and carries at least as much work as the rest
        OperationNode first = assertInstanceOf(OperationNode.class, root.getLeft());
        assertEquals("*", first.getToken().value());
        assertEquals("-", assertInstanceOf(OperationNode.class, first.getLeft()).getToken().value());

        assertTrue(TreeUtils.weight(root.getLeft()) >= TreeUtils.weight(root.getRight()));
    }

    @Test
    @DisplayName("Lab test #5: functional expression is commuted and keeps its functions")
    public void givenFunctionalExpression_whenCommuted_thenFunctionsPreserved() {
        String expression = "exp(sin(a+b/2-pi)+a*cos(a*pi+b*pi/3-w+k*t)-5+log(2.72)/T-1)" +
                "+2048+a+b*c+log(t-1)-2*log(Q)-8*d/dt*exp(t/2+H)-sin(a)/cos(a)";

        TokenNode result = new CommutativeProcessor().process(build(expression));
        String printed = ExpressionPrinter.print(result);

        assertInstanceOf(OperationNode.class, result);
        assertTrue(printed.contains("exp("));
        assertTrue(printed.contains("sin("));
        assertTrue(printed.contains("cos("));
        assertTrue(printed.contains("log("));
    }
}