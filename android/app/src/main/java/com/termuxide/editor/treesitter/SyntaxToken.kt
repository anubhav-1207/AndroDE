package com.termuxide.editor.treesitter

/**
 * High-performance concrete syntax tree (CST) token generated during Tree-sitter parsing.
 */
enum class TokenType {
    KEYWORD,
    FUNCTION,
    TYPE,
    STRING,
    NUMBER,
    COMMENT,
    OPERATOR,
    VARIABLE,
    PUNCTUATION,
    PLAIN
}

data class SyntaxToken(
    val start: Int,
    val end: Int,
    val type: TokenType,
    val scope: String = ""
)
