package com.notes.vault.util

import com.notes.vault.data.model.ChecklistItem
import kotlinx.serialization.json.Json

object MarkdownHelper {
    private val json = Json { ignoreUnknownKeys = true }

    fun wrapBold(text: String): String = "**$text**"
    fun wrapItalic(text: String): String = "_${text}_"
    fun prefixHeading(text: String, level: Int): String {
        val prefix = "#".repeat(level.coerceIn(1, 3)) + " "
        return if (text.startsWith("#")) text else prefix + text
    }
    fun prefixBullet(text: String): String = if (text.startsWith("- ")) text else "- $text"
    fun prefixNumbered(text: String): String = if (text.matches(Regex("^\\d+\\. .*"))) text else "1. $text"

    fun parseChecklist(jsonString: String): List<ChecklistItem> {
        return runCatching { json.decodeFromString<List<ChecklistItem>>(jsonString) }.getOrDefault(emptyList())
    }
}
