package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.OptimizationProcessor;
import com.github.kol.oss.compiler.processor.TreeProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptimizationProcessorTest {
    private final TreeProcessor treeProcessor = new TreeProcessor();

    private TokenNode optimize(String expression) {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        List<Token> tokens = new LexicalProcessor(exceptionHandler).process(expression);

        return new OptimizationProcessor(exceptionHandler)
                .process(treeProcessor.buildTree(tokens));
    }

    private OptimizationProcessor processorFor(String expression) {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        List<Token> tokens = new LexicalProcessor(exceptionHandler).process(expression);
        OptimizationProcessor processor = new OptimizationProcessor(exceptionHandler);

        processor.process(treeProcessor.buildTree(tokens));
        return processor;
    }

    @Test
    @DisplayName("Multiply by zero collapses to zero")
    public void givenMultiplyByZero_whenOptimized_thenZero() {
        TokenNode result = optimize("a * 0");
        assertInstanceOf(ValueNode.class, result);
        assertEquals("0", result.getToken().value());
    }

    @Test
    @DisplayName("Add zero is removed")
    public void givenAddZero_whenOptimized_thenOperandKept() {
        TokenNode result = optimize("a + 0");
        assertEquals("a", result.getToken().value());
    }

    @Test
    @DisplayName("Multiply by one is removed")
    public void givenMultiplyByOne_whenOptimized_thenOperandKept() {
        TokenNode result = optimize("a * 1");
        assertEquals("a", result.getToken().value());
    }

    @Test
    @DisplayName("Division by one is removed")
    public void givenDivideByOne_whenOptimized_thenOperandKept() {
        TokenNode result = optimize("a / 1");
        assertEquals("a", result.getToken().value());
    }

    @Test
    @DisplayName("Zero divided by variable collapses to zero")
    public void givenZeroDivision_whenOptimized_thenZero() {
        TokenNode result = optimize("0 / a");
        assertInstanceOf(ValueNode.class, result);
        assertEquals("0", result.getToken().value());
    }

    @Test
    @DisplayName("Constants are folded")
    public void givenConstantFold_whenOptimized_thenFolded() {
        TokenNode result = optimize("2 + 3 * 4");
        assertInstanceOf(ValueNode.class, result);
        assertEquals("14", result.getToken().value());
    }

    @Test
    @DisplayName("Constant chain collapses to a single value")
    public void givenConstantChain_whenOptimized_thenCollapsed() {
        // a + 1 + 2 + 3 + 4  ->  a + 10
        TokenNode result = optimize("a + 1 + 2 + 3 + 4");
        OperationNode root = assertInstanceOf(OperationNode.class, result);

        assertEquals("+", root.getToken().value());
        assertEquals("a", root.getLeft().getToken().value());
        assertEquals("10", root.getRight().getToken().value());
    }

    @Test
    @DisplayName("Multiplication chain collapses to a single value")
    public void givenMultiplicationChain_whenOptimized_thenCollapsed() {
        // a * 2 * 3 * 4  ->  a * 24
        TokenNode result = optimize("a * 2 * 3 * 4");
        OperationNode root = assertInstanceOf(OperationNode.class, result);

        assertEquals("*", root.getToken().value());
        assertEquals("24", root.getRight().getToken().value());
    }

    @Test
    @DisplayName("Applied optimizations are recorded")
    public void givenOptimizableExpression_whenProcessed_thenLogPopulated() {
        assertTrue(processorFor("a * 1 + 0").getOptimizations().size() >= 2);
    }

    @Test
    @DisplayName("No optimizations for plain expression leaves empty log")
    public void givenPlainExpression_whenProcessed_thenEmptyLog() {
        assertTrue(processorFor("a + b * c").getOptimizations().isEmpty());
    }

    @Test
    @DisplayName("Regression: subtraction/division chains do not crash")
    public void givenMinusAndDivisionChains_whenOptimized_thenNoException() {
        assertInstanceOf(OperationNode.class, optimize("a - b - c - d"));
        assertInstanceOf(OperationNode.class, optimize("f / g / h / q"));
        assertInstanceOf(OperationNode.class, optimize("a + b * c + k - x - d - e - f / g / h / q"));
    }
}
