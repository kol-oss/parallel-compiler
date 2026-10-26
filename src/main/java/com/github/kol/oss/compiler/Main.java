package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.LexicalResult;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.pipeline.CompilerPipeline;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.pipeline.TransformationResult;
import com.github.kol.oss.compiler.service.*;
import com.github.kol.oss.compiler.utils.TreeMetrics;
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
                new CommutativeService(),
                associativeService,
                new ParallelFormService(),
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
                            view.printEquivalentForm(currentTree);
                        }
                        case TransformationService.ASSOCIATIVE -> {
                            if (transformation.applied().isEmpty())
                                printSkippedAssociative(associativeService, normalizationService, currentTree, view);
                            else
                                view.printAssociativeTransformations(transformation.applied());

                            currentTree = normalizationService.fold(currentTree);
                            view.printEquivalentForm(currentTree);
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

    private static void printSkippedAssociative(
            AssociativeService associativeService,
            NormalizationService normalizationService,
            TokenNode tree,
            ConsoleView view
    ) {
        TransformationResult candidate = associativeService.apply(tree);

        if (candidate.diagnostics().isEmpty() && !candidate.applied().isEmpty()) {
            int currentHeight = TreeMetrics.height(NormalizationService.normalize(tree));
            int candidateHeight = TreeMetrics.height(NormalizationService.normalize(candidate.tree()));

            if (candidateHeight > currentHeight) {
                view.printSkippedAssociativeForm(
                        normalizationService.fold(candidate.tree()),
                        currentHeight,
                        candidateHeight
                );
                return;
            }
        }

        view.printAssociativeTransformations(List.of());
    }

    private static List<PositionedException> collectErrors(LexicalResult lexical, SyntaxValidationService syntaxValidationService) {
        List<PositionedException> errors = new ArrayList<>(lexical.diagnostics());
        errors.addAll(syntaxValidationService.validate(lexical.tokens()));

        errors.sort(Comparator.comparingInt(PositionedException::getIndex));
        return errors;
    }
}
