package com.github.kol.oss.compiler.view;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class TreeViewRenderer {
    public String renderStructure(TokenNode root) {
        if (root == null)
            return "";

        StringBuilder out = new StringBuilder();
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root, "", true));

        while (!stack.isEmpty()) {
            Frame frame = stack.pop();
            out.append(frame.indent())
                    .append(frame.closing() ? "└── " : "├── ")
                    .append(frame.node().getToken().value())
                    .append('\n');

            String childIndent = frame.indent() + (frame.closing() ? "    " : "│   ");
            List<TokenNode> children = TreeWalker.children(frame.node());

            for (int i = children.size() - 1; i >= 0; i--)
                stack.push(new Frame(children.get(i), childIndent, i == children.size() - 1));
        }

        return out.toString();
    }

    private record Frame(TokenNode node, String indent, boolean closing) {
    }
}