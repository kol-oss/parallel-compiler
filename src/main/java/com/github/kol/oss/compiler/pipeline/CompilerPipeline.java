package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.service.NormalizationService;
import com.github.kol.oss.compiler.utils.TreeFormSelector;
import com.github.kol.oss.compiler.utils.TreeMetrics;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompilerPipeline {
    private final List<TransformationService> services;

    public CompilerPipeline(List<TransformationService> services) {
        this.services = List.copyOf(services);
    }

    public List<TransformationResult> run(TokenNode tree) {
        List<TransformationResult> results = new ArrayList<>();
        TokenNode current = tree;

        for (TransformationService service : services) {
            TransformationResult result = service.apply(current);

            if (!result.diagnostics().isEmpty()) {
                results.add(result);
                break;
            }

            List<TokenNode> bestForms = TreeFormSelector.selectBest(result.forms());
            if (bestForms.isEmpty())
                bestForms = List.of(result.tree());

            TokenNode best = bestForms.getFirst();
            int currentHeight = TreeMetrics.height(NormalizationService.normalize(current));
            int candidateHeight = TreeMetrics.height(NormalizationService.normalize(best));

            if (candidateHeight > currentHeight) {
                results.add(new TransformationResult(
                        service.name(),
                        current,
                        TreeFormSelector.selectBest(mergeForms(current, bestForms)),
                        result.applied(),
                        result.diagnostics()
                ));
                continue;
            }

            results.add(new TransformationResult(
                    service.name(),
                    best,
                    bestForms,
                    result.applied(),
                    result.diagnostics()
            ));
            current = best;
        }

        return results;
    }

    private List<TokenNode> mergeForms(TokenNode current, List<TokenNode> forms) {
        Map<String, TokenNode> merged = new LinkedHashMap<>();
        merged.put(TreeWalker.structuralKey(current), current);

        for (TokenNode form : forms)
            merged.putIfAbsent(TreeWalker.structuralKey(form), form);

        return List.copyOf(merged.values());
    }
}
