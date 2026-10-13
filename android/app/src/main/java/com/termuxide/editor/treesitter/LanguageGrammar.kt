package com.termuxide.editor.treesitter

/**
 * Grammar definitions and scope mapping for Tree-sitter supported languages.
 */
object LanguageGrammar {

    val RUST_KEYWORDS = setOf(
        "fn", "let", "mut", "pub", "struct", "enum", "impl", "trait", "match",
        "if", "else", "loop", "while", "for", "in", "return", "use", "mod", "crate",
        "async", "await", "where", "type", "const", "static", "unsafe", "self", "Self"
    )

    val PYTHON_KEYWORDS = setOf(
        "def", "class", "import", "from", "as", "return", "if", "elif", "else",
        "for", "while", "try", "except", "finally", "with", "raise", "yield",
        "lambda", "pass", "break", "continue", "and", "or", "not", "is", "in", "True", "False", "None"
    )

    val CPP_KEYWORDS = setOf(
        "auto", "bool", "break", "case", "catch", "char", "class", "const", "constexpr",
        "continue", "default", "delete", "do", "double", "else", "enum", "explicit",
        "export", "extern", "false", "float", "for", "friend", "goto", "if", "inline",
        "int", "long", "mutable", "namespace", "new", "noexcept", "nullptr", "operator",
        "private", "protected", "public", "register", "return", "short", "signed",
        "sizeof", "static", "struct", "switch", "template", "this", "throw", "true",
        "try", "typedef", "typeid", "typename", "union", "unsigned", "using", "virtual",
        "void", "volatile", "while"
    )

    val JS_KEYWORDS = setOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "finally", "for", "function",
        "if", "import", "in", "instanceof", "new", "return", "super", "switch",
        "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
        "let", "static", "async", "await", "null", "undefined", "true", "false"
    )

    fun getKeywords(lang: String): Set<String> = when (lang.lowercase()) {
        "rust", "rs" -> RUST_KEYWORDS
        "python", "py" -> PYTHON_KEYWORDS
        "cpp", "c", "c++", "h", "hpp" -> CPP_KEYWORDS
        "javascript", "js", "typescript", "ts" -> JS_KEYWORDS
        else -> RUST_KEYWORDS
    }
}
