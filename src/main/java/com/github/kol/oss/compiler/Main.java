package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.change.TransformationResult;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.service.CommutativeService;
import com.github.kol.oss.compiler.service.EvaluationService;
import com.github.kol.oss.compiler.service.LexicalAnalysisService;
import com.github.kol.oss.compiler.service.OptimizationService;
import com.github.kol.oss.compiler.service.ParallelFormService;
import com.github.kol.oss.compiler.service.SyntaxValidationService;
import com.github.kol.oss.compiler.service.TreeBuildService;
import com.github.kol.oss.compiler.view.ConsoleView;

import java.util.List;

public class Main {
    private static final String STEP_VALIDATION = "Step 1. Lexical and Syntax validation";
    private static final String STEP_OPTIMIZATION = "Step 2. Optimization";
    private static final String STEP_COMMUTATIVE = "Step 3. Commutative transformation";
    private static final String STEP_PARALLEL = "Step 4. Parallel tree building";
    private static final String END_COMMAND = "end";
    private static final String TOKEN_COMMAND = "token";

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        ExceptionHandler exceptionHandler = new ExceptionHandler();

        LexicalAnalysisService lexicalAnalysisService = new LexicalAnalysisService(exceptionHandler);
        SyntaxValidationService syntaxValidationService = new SyntaxValidationService(exceptionHandler);
        TreeBuildService treeBuildService = new TreeBuildService();
        OptimizationService optimizationService = new OptimizationService(exceptionHandler);
        CommutativeService commutativeService = new CommutativeService();
        ParallelFormService parallelFormService = new ParallelFormService();
        EvaluationService evaluationService = new EvaluationService();

        boolean isTokenOutput = false;

        while (true) {
            String value = view.readLine();

            if (value.equalsIgnoreCase(END_COMMAND))
                break;

            if (value.equalsIgnoreCase(TOKEN_COMMAND)) {
                isTokenOutput = !isTokenOutput;
                continue;
            }

            if (value.isEmpty())
                continue;

            view.printStepStarted(STEP_VALIDATION);

            List<Token> tokens = lexicalAnalysisService.tokenize(value);
            if (isTokenOutput)
                view.printTokens(tokens);

            syntaxValidationService.validate(tokens);
            if (failed(view, exceptionHandler, value))
                continue;

            view.printStepPassed(STEP_VALIDATION);

            view.printStepStarted(STEP_OPTIMIZATION);

            TransformationResult optimized = optimizationService.optimize(treeBuildService.buildTree(tokens));
            view.printOptimizations(optimized.applied());

            if (failed(view, exceptionHandler, value))
                continue;

            view.printStepPassed(STEP_OPTIMIZATION);

            view.printStepStarted(STEP_COMMUTATIVE);

            TransformationResult commuted = commutativeService.applyCommutativeLaw(optimized.tree());
            view.printCommutativeTransformations(commuted.applied());

            if (failed(view, exceptionHandler, value))
                continue;

            view.printEquivalentForm(commuted.tree());
            view.printStepPassed(STEP_COMMUTATIVE);

            view.printStepStarted(STEP_PARALLEL);

            TokenNode operatorTree = parallelFormService.buildParallelTree(commuted.tree());

            if (failed(view, exceptionHandler, value))
                continue;

            view.printStepPassed(STEP_PARALLEL);

            evaluationService.evaluate(operatorTree).ifPresent(view::printResult);
            view.printTree(operatorTree);
        }
    }

    private static boolean failed(ConsoleView view, ExceptionHandler exceptionHandler, String expression) {
        List<PositionedException> errors = exceptionHandler.getAllAndClear();

        if (errors.isEmpty())
            return false;

        view.printErrors(expression, errors);
        return true;
    }
}
