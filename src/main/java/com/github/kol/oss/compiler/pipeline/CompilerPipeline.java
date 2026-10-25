package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.service.NormalizationService;
import com.github.kol.oss.compiler.utils.TreeMetrics;

import java.util.ArrayList;
import java.util.List;

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

            int currentHeight = TreeMetrics.height(NormalizationService.normalize(current));
            int candidateHeight = TreeMetrics.height(NormalizationService.normalize(result.tree()));

            if (candidateHeight > currentHeight) {
                results.add(new TransformationResult(service.name(), current, List.of(), List.of()));
                continue;
            }

            results.add(result);
            current = result.tree();
        }

        return results;
    }
}
