package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.VectorSystemConfig;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.schedule.ExecutionSchedule;
import com.github.kol.oss.compiler.dto.schedule.ScheduleEntry;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.*;

public class VectorSystemService {
    public ExecutionSchedule execute(TokenNode root) {
        List<TokenNode> operations = TreeWalker.postOrder(root).stream()
                .filter(OperationNode.class::isInstance)
                .toList();
        int sequentialTime = operations.stream()
                .mapToInt(this::duration)
                .sum();

        if (operations.isEmpty())
            return new ExecutionSchedule(List.of(), sequentialTime, 0, VectorSystemConfig.TOTAL_BLOCKS);

        Map<TokenNode, Integer> bottomLevels = bottomLevels(root);
        Map<TokenNode, List<TokenNode>> dependencies = new IdentityHashMap<>();
        Map<TokenNode, List<TokenNode>> dependents = new IdentityHashMap<>();
        Map<TokenNode, Integer> pending = new IdentityHashMap<>();
        PriorityQueue<TokenNode> ready = new PriorityQueue<>(readyComparator(bottomLevels));

        for (TokenNode node : operations) {
            List<TokenNode> nodeDependencies = dependencies((OperationNode) node);
            dependencies.put(node, nodeDependencies);
            pending.put(node, nodeDependencies.size());

            for (TokenNode dependency : nodeDependencies)
                dependents.computeIfAbsent(dependency, _ -> new ArrayList<>()).add(node);

            if (nodeDependencies.isEmpty())
                ready.add(node);
        }

        Map<TokenNode, Integer> completion = new IdentityHashMap<>();
        Map<OperatorValue, int[]> units = createUnits();
        List<ScheduleEntry> entries = new ArrayList<>();
        int currentTime = 0;

        while (!ready.isEmpty()) {
            List<TokenNode> deferred = new ArrayList<>();
            boolean scheduled = false;

            while (!ready.isEmpty()) {
                TokenNode node = ready.poll();

                if (earliestStart(node, dependencies, completion, units) > currentTime) {
                    deferred.add(node);
                    continue;
                }

                OperatorValue operator = operator(node);
                int[] availability = units.get(operator);
                int block = earliestBlock(availability);
                int finish = currentTime + duration(node);

                availability[block] = finish;
                completion.put(node, finish);
                entries.add(new ScheduleEntry(node, block, currentTime, finish));
                releaseDependents(node, dependents, pending, ready);
                scheduled = true;
            }

            ready.addAll(deferred);

            if (!scheduled)
                currentTime = nextTime(ready, dependencies, completion, units, currentTime);
        }

        int parallelTime = completion.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        return new ExecutionSchedule(entries, sequentialTime, parallelTime, VectorSystemConfig.TOTAL_BLOCKS);
    }

    private List<TokenNode> dependencies(OperationNode operation) {
        List<TokenNode> result = new ArrayList<>();
        result.addAll(frontier(operation.getLeft()));
        result.addAll(frontier(operation.getRight()));
        return result;
    }

    private List<TokenNode> frontier(TokenNode node) {
        if (node instanceof OperationNode)
            return List.of(node);

        if (node instanceof FunctionNode function) {
            List<TokenNode> result = new ArrayList<>();

            for (TokenNode argument : function.getArguments())
                result.addAll(frontier(argument));

            return result;
        }

        return List.of();
    }

    private Map<TokenNode, Integer> bottomLevels(TokenNode root) {
        Map<TokenNode, Integer> levels = new IdentityHashMap<>();

        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (node instanceof OperationNode operation) {
                int childTime = Math.max(
                        levels.getOrDefault(operation.getLeft(), 0),
                        levels.getOrDefault(operation.getRight(), 0)
                );

                levels.put(node, duration(node) + childTime);
                continue;
            }

            if (node instanceof FunctionNode function) {
                int childTime = function.getArguments().stream()
                        .mapToInt(argument -> levels.getOrDefault(argument, 0))
                        .max()
                        .orElse(0);

                levels.put(node, childTime);
                continue;
            }

            levels.put(node, 0);
        }

        return levels;
    }

    private Comparator<TokenNode> readyComparator(Map<TokenNode, Integer> bottomLevels) {
        return Comparator
                .comparingInt((TokenNode node) -> bottomLevels.get(node))
                .reversed()
                .thenComparing(Comparator.comparingInt(this::duration).reversed())
                .thenComparingInt(node -> node.getToken().position());
    }

    private void releaseDependents(
            TokenNode node,
            Map<TokenNode, List<TokenNode>> dependents,
            Map<TokenNode, Integer> pending,
            PriorityQueue<TokenNode> ready
    ) {
        for (TokenNode dependent : dependents.getOrDefault(node, List.of())) {
            int remaining = pending.merge(dependent, -1, Integer::sum);

            if (remaining == 0)
                ready.add(dependent);
        }
    }

    private int earliestStart(
            TokenNode node,
            Map<TokenNode, List<TokenNode>> dependencies,
            Map<TokenNode, Integer> completion,
            Map<OperatorValue, int[]> units
    ) {
        int dependencyTime = dependencies.get(node).stream()
                .mapToInt(dependency -> completion.getOrDefault(dependency, 0))
                .max()
                .orElse(0);
        int[] availability = units.get(operator(node));

        return Math.max(dependencyTime, availability[earliestBlock(availability)]);
    }

    private int nextTime(
            PriorityQueue<TokenNode> ready,
            Map<TokenNode, List<TokenNode>> dependencies,
            Map<TokenNode, Integer> completion,
            Map<OperatorValue, int[]> units,
            int currentTime
    ) {
        int nextTime = ready.stream()
                .mapToInt(node -> earliestStart(node, dependencies, completion, units))
                .min()
                .orElse(currentTime);

        if (nextTime <= currentTime)
            throw new IllegalStateException("Unable to schedule operations");

        return nextTime;
    }

    private Map<OperatorValue, int[]> createUnits() {
        Map<OperatorValue, int[]> units = new EnumMap<>(OperatorValue.class);
        units.put(OperatorValue.PLUS, new int[VectorSystemConfig.ADDERS]);
        units.put(OperatorValue.MINUS, new int[VectorSystemConfig.SUBTRACTORS]);
        units.put(OperatorValue.MULTIPLY, new int[VectorSystemConfig.MULTIPLIERS]);
        units.put(OperatorValue.DIVISION, new int[VectorSystemConfig.DIVIDERS]);

        return units;
    }

    private int earliestBlock(int[] availability) {
        if (availability.length == 0)
            throw new IllegalStateException("No functional units configured");

        int block = 0;

        for (int i = 1; i < availability.length; i++)
            if (availability[i] < availability[block])
                block = i;

        return block;
    }

    private OperatorValue operator(TokenNode node) {
        return OperatorValue.fromSymbol(node.getToken().value());
    }

    private int duration(TokenNode node) {
        if (!(node instanceof OperationNode))
            return 0;

        return switch (operator(node)) {
            case PLUS -> VectorSystemConfig.ADDITION_DURATION;
            case MINUS -> VectorSystemConfig.SUBTRACTION_DURATION;
            case MULTIPLY -> VectorSystemConfig.MULTIPLICATION_DURATION;
            case DIVISION -> VectorSystemConfig.DIVISION_DURATION;
            default -> throw new IllegalArgumentException("Unsupported operator: " + node.getToken().value());
        };
    }
}
