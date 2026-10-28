package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.dto.node.TokenNode;

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
            results.add(result);

            if (!result.diagnostics().isEmpty())
                break;

            current = result.tree();
        }

        return results;
    }
}
