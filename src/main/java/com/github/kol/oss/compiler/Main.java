package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.LexicalResult;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.pipeline.CompilerPipeline;
import com.github.kol.oss.compiler.pipeline.TransformationResult;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.service.*;
import com.github.kol.oss.compiler.view.ConsoleView;

import java.util.*;

public class Main {
    private static final String END_COMMAND = "end";
    private static final String TOKEN_COMMAND = "token";
    private static final String COMMUTATIVE_COMMAND = "commutative";
    private static final String ASSOCIATIVE_COMMAND = "associative";

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();

        LexicalAnalysisService lexicalAnalysisService = new LexicalAnalysisService();
        SyntaxValidationService syntaxValidationService = new SyntaxValidationService();
        TreeConstructionService treeConstructionService = new TreeConstructionService();
        NormalizationService normalizationService = new NormalizationService();
        AssociativeService associativeService = new AssociativeService();

        List<TransformationService> services = List.of(
                new OptimizationService(),
                new ParallelizationService(),
                new CommutativeService(),
                associativeService,
                normalizationService
        );

        Set<String> disabledServices = new HashSet<>();
        boolean isTokenOutput = false;

        while (true) {
            view.printInputLine();
            if (!view.hasNextLine())
                break;

            String value = view.readLine();
            if (value.equalsIgnoreCase(END_COMMAND))
                break;

            if (value.equalsIgnoreCase(TOKEN_COMMAND)) {
                isTokenOutput = !isTokenOutput;
                continue;
            }

            if (value.equalsIgnoreCase(COMMUTATIVE_COMMAND)) {
                toggle(disabledServices, TransformationService.COMMUTATIVE);
                continue;
            }

            if (value.equalsIgnoreCase(ASSOCIATIVE_COMMAND)) {
                toggle(disabledServices, TransformationService.ASSOCIATIVE);
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

                TokenNode tree = treeConstructionService.buildTree(lexical.tokens());
                CompilerPipeline pipeline = new CompilerPipeline(activeServices(services, disabledServices));

                for (TransformationResult transformation : pipeline.run(tree)) {
                    if (!transformation.diagnostics().isEmpty()) {
                        view.printErrors(value, transformation.diagnostics());
                        break;
                    }

                    switch (transformation.name()) {
                        case TransformationService.OPTIMIZATIONS -> view.printOptimizations(transformation.applied());
                        case TransformationService.COMMUTATIVE -> {
                            view.printCommutativeTransformations(transformation.applied());
                            view.printEquivalentForms(transformation.forms());
                        }
                        case TransformationService.ASSOCIATIVE -> {
                            view.printAssociativeTransformations(transformation.applied());
                            view.printEquivalentForms(transformation.forms());
                        }
                        case TransformationService.NORMALIZATION -> {
                            TokenNode normalized = normalizationService.fold(transformation.tree());
                            view.printEquivalentForm(normalized);
                            normalizationService.evaluate(normalized).ifPresent(view::printResult);
                            view.printTree(normalized);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                view.printFailure(value, exception);
            }
        }
    }

    private static List<TransformationService> activeServices(
            List<TransformationService> services,
            Set<String> disabledServices
    ) {
        return services.stream()
                .filter(service -> !disabledServices.contains(service.name()))
                .toList();
    }

    private static void toggle(Set<String> disabledServices, String serviceName) {
        if (!disabledServices.remove(serviceName))
            disabledServices.add(serviceName);
    }

    private static List<PositionedException> collectErrors(LexicalResult lexical, SyntaxValidationService syntaxValidationService) {
        List<PositionedException> errors = new ArrayList<>(lexical.diagnostics());
        errors.addAll(syntaxValidationService.validate(lexical.tokens()));

        errors.sort(Comparator.comparingInt(PositionedException::getIndex));
        return errors;
    }
}
