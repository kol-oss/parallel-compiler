package com.github.kol.oss.compiler.dto.chain;

import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.dto.node.TokenNode;

public record Factor(TokenNode node, TermSign sign, String norm, String flip) {
}
