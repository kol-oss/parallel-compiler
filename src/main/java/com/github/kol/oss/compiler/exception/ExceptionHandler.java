package com.github.kol.oss.compiler.exception;

import java.util.ArrayList;
import java.util.List;

public class ExceptionHandler {
    private final List<PositionedException> exceptions = new ArrayList<>();

    public void add(PositionedException exception) {
        exceptions.add(exception);
    }

    private void sortByIndex() {
        exceptions.sort((o1, o2) -> {
            if (o1.getIndex() == o2.getIndex())
                return 0;

            return o1.getIndex() > o2.getIndex() ? 1 : -1;
        });
    }

    public List<PositionedException> getAllAndClear() {
        sortByIndex();

        List<PositionedException> result = new ArrayList<>(exceptions);
        exceptions.clear();
        return result;
    }

    public void printAndClear(String expression) {
        System.out.printf("%-12s%s%n", "Expression:", expression);
        if (exceptions.isEmpty()) {
            System.out.println("expression is valid");
            return;
        }

        sortByIndex();

        StringBuilder pointers = new StringBuilder();
        pointers.repeat(" ", expression.length());

        StringBuilder messages = new StringBuilder();
        for (PositionedException exception : exceptions) {
            int index = exception.getIndex();
            pointers.setCharAt(index, '^');

            messages
                    .append("> (")
                    .append(index)
                    .append("): ")
                    .append(exception.getMessage())
                    .append("\n");
        }

        System.out.printf("%-12s%s%n", "Errors:", pointers);
        System.out.println(messages);

        exceptions.clear();
    }
}
