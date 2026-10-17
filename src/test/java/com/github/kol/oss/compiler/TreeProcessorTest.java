package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.TreeProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeProcessorTest {
    private final TreeProcessor treeProcessor = new TreeProcessor();

    private TokenNode variable(String value) {
        return new ValueNode(new Token(value, TokenType.VARIABLE, 0));
    }

    private TokenNode number(String value) {
        return new ValueNode(new Token(value, TokenType.INTEGER, 0));
    }

    private TokenNode op(String operator, TokenNode left, TokenNode right) {
        return new OperationNode(new Token(operator, TokenType.OPERATOR, 0), left, right);
    }

    private int height(TokenNode node) {
        if (!(node instanceof OperationNode operation))
            return 0;

        return 1 + Math.max(height(operation.getLeft()), height(operation.getRight()));
    }

    @Test
    @DisplayName("Sum chain is balanced into minimal height")
    public void givenSumChain_whenBalanced_thenMinimalHeight() {
        // 1 + 2 + 3 + 4 + 5 + 6  ->  height 3 (was height 5 as a left chain)
        TokenNode chain = number("1");
        for (int i = 2; i <= 6; i++)
            chain = op("+", chain, number(String.valueOf(i)));

        assertEquals(3, height(treeProcessor.balance(chain)));
    }

    @Test
    @DisplayName("Subtraction chain becomes parallel (a-b)-(c+d)")
    public void givenSubtractionChain_whenBalanced_thenParallelForm() {
        // a - b - c - d  ->  (a - b) - (c + d)
        TokenNode chain = op("-", op("-", op("-", variable("a"), variable("b")), variable("c")), variable("d"));

        OperationNode root = assertInstanceOf(OperationNode.class, treeProcessor.balance(chain));

        assertEquals("-", root.getToken().value());
        assertEquals(2, height(root));

        // right subtree must be a sum grouping the subtracted tail
        OperationNode right = assertInstanceOf(OperationNode.class, root.getRight());
        assertEquals("+", right.getToken().value());
    }

    @Test
    @DisplayName("Division chain becomes parallel (a/b)/(c*d)")
    public void givenDivisionChain_whenBalanced_thenParallelForm() {
        // f / g / h / q  ->  (f / g) / (h * q)
        TokenNode chain = op("/", op("/", op("/", variable("f"), variable("g")), variable("h")), variable("q"));

        OperationNode root = assertInstanceOf(OperationNode.class, treeProcessor.balance(chain));

        assertEquals("/", root.getToken().value());
        assertEquals(2, height(root));

        // divisor tail must be grouped as a product
        OperationNode right = assertInstanceOf(OperationNode.class, root.getRight());
        assertEquals("*", right.getToken().value());
    }

    @Test
    @DisplayName("Constant expression is evaluated correctly")
    public void givenConstantTree_whenEvaluated_thenReturnsValue() {
        // 2 + 3 * 4 = 14
        TokenNode tree = op("+", number("2"), op("*", number("3"), number("4")));

        Optional<Token> result = treeProcessor.evaluate(tree);
        assertTrue(result.isPresent());
        assertEquals("14", result.get().value());
        assertEquals(TokenType.INTEGER, result.get().type());
    }

    @Test
    @DisplayName("Expression with variables is not evaluated")
    public void givenVariableTree_whenEvaluated_thenEmpty() {
        assertTrue(treeProcessor.evaluate(op("+", number("2"), variable("a"))).isEmpty());
    }

    @Test
    @DisplayName("Fractional result is typed as FLOAT")
    public void givenFractionalResult_whenEvaluated_thenFloatType() {
        Optional<Token> result = treeProcessor.evaluate(op("/", number("1"), number("2")));

        assertTrue(result.isPresent());
        assertEquals("0.5", result.get().value());
        assertEquals(TokenType.FLOAT, result.get().type());
    }

    @Test
    @DisplayName("Non-associative operators keep their structure")
    public void givenExponentiation_whenBalanced_thenNotFlattened() {
        TokenNode balanced = treeProcessor.balance(op("^", number("2"), number("3")));
        assertEquals("^", assertInstanceOf(OperationNode.class, balanced).getToken().value());
    }

    private TokenNode build(String expression) {
        List<Token> tokens = new LexicalProcessor(new ExceptionHandler()).process(expression);
        return treeProcessor.buildTree(tokens);
    }

    @Test
    @DisplayName("Function call keeps its name and argument")
    public void givenFunctionCall_whenTreeBuilt_thenFunctionNode() {
        FunctionNode function = assertInstanceOf(FunctionNode.class, build("sin(a+b)"));

        assertEquals("sin", function.getToken().value());
        assertEquals(1, function.getArguments().size());

        OperationNode argument = assertInstanceOf(OperationNode.class, function.getArguments().getFirst());
        assertEquals("+", argument.getToken().value());
    }

    @Test
    @DisplayName("Multi-argument function keeps every argument")
    public void givenMultiArgumentFunction_whenTreeBuilt_thenAllArgumentsKept() {
        FunctionNode function = assertInstanceOf(FunctionNode.class, build("sum(10, 20)"));

        assertEquals(2, function.getArguments().size());
        assertEquals("10", function.getArguments().get(0).getToken().value());
        assertEquals("20", function.getArguments().get(1).getToken().value());
    }

    @Test
    @DisplayName("Function with nested parenthesis argument wraps the whole argument")
    public void givenNestedFunctionArgument_whenTreeBuilt_thenArgumentComplete() {
        FunctionNode function = assertInstanceOf(FunctionNode.class, build("foo((a+3)*b)"));

        assertEquals(1, function.getArguments().size());
        OperationNode argument = assertInstanceOf(OperationNode.class, function.getArguments().getFirst());
        assertEquals("*", argument.getToken().value());
    }

    @Test
    @DisplayName("Balance recurses into function arguments")
    public void givenFunctionWithChainArgument_whenBalanced_thenArgumentBalanced() {
        FunctionNode function = assertInstanceOf(FunctionNode.class, treeProcessor.balance(build("sin(a+b+c)")));

        OperationNode argument = assertInstanceOf(OperationNode.class, function.getArguments().getFirst());
        assertEquals(2, height(argument));
    }

    @Test
    @DisplayName("Function expression is not evaluated")
    public void givenFunctionTree_whenEvaluated_thenEmpty() {
        assertTrue(treeProcessor.evaluate(build("sin(1)")).isEmpty());
    }
}
