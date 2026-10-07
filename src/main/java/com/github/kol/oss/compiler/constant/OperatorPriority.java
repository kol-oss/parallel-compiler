package com.github.kol.oss.compiler.constant;

import java.util.Set;

public class OperatorPriority {
    private static final Set<String> NORMAL_PRIORITY = Set.of("+", "-", "|", "&");
    private static final Set<String> MAX_PRIORITY = Set.of("*", "/");

    public static int getPriority(String value) {
        if (NORMAL_PRIORITY.contains(value))
            return 1;

        if (MAX_PRIORITY.contains(value))
            return 2;

        return 0;
    }
}
