package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.LexicalResult;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.pipeline.CompilerPipeline;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.pipeline.TransformationResult;
import com.github.kol.oss.compiler.service.*;
import com.github.kol.oss.compiler.view.ConsoleView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main {
    private static final String END_COMMAND = "end";
    private static final String TOKEN_COMMAND = "token";

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();

        LexicalAnalysisService lexicalAnalysisService = new LexicalAnalysisService();
        SyntaxValidationService syntaxValidationService = new SyntaxValidationService();
        TreeBuildService treeBuildService = new TreeBuildService();
        NormalizationService normalizationService = new NormalizationService();
        AssociativeService associativeService = new AssociativeService();

        CompilerPipeline pipeline = new CompilerPipeline(List.of(
                new OptimizationService(),
                new ParallelFormService(),
                new CommutativeService(),
                associativeService,
                normalizationService
        ));

        boolean isTokenOutput = false;

        view.printInputLine();
        while (view.hasNextLine()) {
            String value = view.readLine();
            if (value.equalsIgnoreCase(END_COMMAND))
                break;

            if (value.equalsIgnoreCase(TOKEN_COMMAND)) {
                isTokenOutput = !isTokenOutput;
                continue;
            }

            if (value.isEmpty())
                continue;

            try {
                LexicalResult lexical = lexicalAnalysisService.tokenize(value);
                if (isTokenOutput)
                    view.printTokens(lexical.tokens());

                List<PositionedException> errors = collectErrors(lexical, syntaxValidationService);
                if (!errors.isEmpty()) {
                    view.printErrors(value, errors);
                    continue;
                }

                if (lexical.tokens().isEmpty())
                    continue;

                TokenNode tree = treeBuildService.buildTree(lexical.tokens());

                TokenNode currentTree;
                for (TransformationResult transformation : pipeline.run(tree)) {
                    if (!transformation.diagnostics().isEmpty()) {
                        view.printErrors(value, transformation.diagnostics());
                        break;
                    }

                    currentTree = transformation.tree();
                    switch (transformation.name()) {
                        case TransformationService.OPTIMIZATIONS -> view.printOptimizations(transformation.applied());
                        case TransformationService.COMMUTATIVE -> {
                            view.printCommutativeTransformations(transformation.applied());
                            view.printEquivalentForms(transformation.forms());
                        }
                        case TransformationService.ASSOCIATIVE -> {
                            view.printAssociativeTransformations(transformation.applied());
                            view.printEquivalentForms(transformation.forms());

                            currentTree = normalizationService.fold(currentTree);
                        }
                        case TransformationService.PARALLEL -> currentTree = normalizationService.fold(currentTree);
                        case TransformationService.NORMALIZATION -> {
                            currentTree = normalizationService.fold(currentTree);
                            view.printEquivalentForm(currentTree);
                            normalizationService.evaluate(currentTree).ifPresent(view::printResult);
                            view.printTree(currentTree);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                view.printFailure(value, exception);
            } finally {
                view.printInputLine();
            }
        }
    }

    private static List<PositionedException> collectErrors(LexicalResult lexical, SyntaxValidationService syntaxValidationService) {
        List<PositionedException> errors = new ArrayList<>(lexical.diagnostics());
        errors.addAll(syntaxValidationService.validate(lexical.tokens()));

        errors.sort(Comparator.comparingInt(PositionedException::getIndex));
        return errors;
    }
}
