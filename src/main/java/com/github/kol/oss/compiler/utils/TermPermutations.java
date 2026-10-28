package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.chain.Term;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class TermPermutations {
    public static List<List<Term>> generate(List<Term> terms, int limit) {
        List<List<Term>> result = new ArrayList<>();

        if (terms.size() <= 1) {
            result.add(List.copyOf(terms));
            return result;
        }

        List<Term> working = new ArrayList<>(terms);
        Deque<Frame> pending = new ArrayDeque<>();
        pending.push(new Frame(0, 0, false));

        while (!pending.isEmpty() && result.size() < limit) {
            Frame frame = pending.pop();

            if (frame.restore()) {
                Collections.swap(working, frame.index(), frame.value());
                continue;
            }

            if (frame.index() == working.size()) {
                result.add(List.copyOf(working));
                continue;
            }

            if (frame.value() >= working.size())
                continue;

            int index = frame.index();
            int value = frame.value();

            pending.push(new Frame(index, value + 1, false));
            pending.push(new Frame(index, value, true));

            Collections.swap(working, index, value);
            pending.push(new Frame(index + 1, index + 1, false));
        }

        return result;
    }

    private record Frame(int index, int value, boolean restore) {
    }
}
