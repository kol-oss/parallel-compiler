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
}
