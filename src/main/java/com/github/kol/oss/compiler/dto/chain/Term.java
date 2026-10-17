package com.github.kol.oss.compiler.dto.chain;

import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.node.TokenNode;

public record Term(TokenNode node, TermSign sign) {
}