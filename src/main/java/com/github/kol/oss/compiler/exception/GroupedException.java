package com.github.kol.oss.compiler.exception;

import java.util.List;

public class GroupedException extends RuntimeException {
    private final List<? extends Exception> children;

    public GroupedException(List<? extends Exception> children) {
        this.children = children;
    }

    public <T extends Exception> List<T> getChildren(Class<T> type) {
        return children.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}