package com.github.kol.oss.compiler.reworked.exception;

import java.util.ArrayList;
import java.util.List;

public class ExceptionHandler {
    private List<PositionedException> exceptions = new ArrayList<>();

    public void add(PositionedException exception) {
        exceptions.add(exception);
    }

    public void visualize(String expression) {
        System.out.printf("%-12s%s%n", "Expression:", expression);
        if (exceptions.isEmpty()) {
            System.out.println("expression is valid");
            return;
        }

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
    }
}
