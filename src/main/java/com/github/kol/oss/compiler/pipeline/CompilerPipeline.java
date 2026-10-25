package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.service.NormalizationService;
import com.github.kol.oss.compiler.utils.TreeMetrics;

import java.util.ArrayList;
import java.util.List;

public class CompilerPipeline {
    private final List<Pass> passes;

    public CompilerPipeline(List<Pass> passes) {
        this.passes = List.copyOf(passes);
    }

    public List<PassResult> run(TokenNode tree) {
        List<PassResult> results = new ArrayList<>();
        TokenNode current = tree;

        for (Pass pass : passes) {
            PassResult result = pass.apply(current);

            if (!result.diagnostics().isEmpty()) {
                results.add(result);
                break;
            }

            int currentHeight = TreeMetrics.height(NormalizationService.normalize(current));
            int candidateHeight = TreeMetrics.height(NormalizationService.normalize(result.tree()));

            if (candidateHeight > currentHeight) {
                results.add(new PassResult(pass.name(), current, List.of(), List.of()));
                continue;
            }

            results.add(result);
            current = result.tree();
        }

        return results;
    }
}
