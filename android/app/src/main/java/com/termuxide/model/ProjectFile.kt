package com.termuxide.model

/**
 * Representation of an in-memory or on-disk file in the TermuxIDE project.
 */
data class ProjectFile(
    val id: String,
    val name: String,
    val path: String,
    var content: String,
    val language: String,
    val encoding: String = "UTF-8",
    var isDirty: Boolean = false
) {
    fun getExtension(): String {
        return name.substringAfterLast('.', "")
    }

    fun getIcon(): String = when (getExtension().lowercase()) {
        "rs" -> "🦀"
        "py" -> "🐍"
        "cpp", "c", "h", "hpp" -> "⚡"
        "js", "ts" -> "🟨"
        "go" -> "🐹"
        "kt", "java" -> "☕"
        "json" -> "📦"
        "sh" -> "🐚"
        else -> "📄"
    }
}
