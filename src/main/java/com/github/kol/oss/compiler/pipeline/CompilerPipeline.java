package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.dto.node.TokenNode;

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
            results.add(result);

            if (!result.diagnostics().isEmpty())
                break;

            current = result.tree();
        }

        return results;
    }
}
