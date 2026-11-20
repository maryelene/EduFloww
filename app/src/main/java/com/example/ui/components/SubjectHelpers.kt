package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.HighlightPeach
import com.example.ui.theme.HighlightPeachBorder
import com.example.ui.theme.HighlightPink
import com.example.ui.theme.HighlightPinkBorder
import com.example.ui.theme.HighlightYellow
import com.example.ui.theme.HighlightYellowBorder
import com.example.ui.theme.SubjectSwatches

data class SubjectIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

val SubjectIconOptions = listOf(
    SubjectIconOption("book", "Notes", Icons.Rounded.AutoStories),
    SubjectIconOption("biotech", "Biology", Icons.Rounded.Biotech),
    SubjectIconOption("history", "History", Icons.Rounded.HistoryEdu),
    SubjectIconOption("menu_book", "English", Icons.Rounded.Language),
    SubjectIconOption("science", "Science", Icons.Rounded.Science),
    SubjectIconOption("math", "Math", Icons.Rounded.Calculate),
    SubjectIconOption("art", "Arts", Icons.Rounded.Palette),
    SubjectIconOption("psych", "Research", Icons.Rounded.Psychology)
)

fun subjectIconForKey(key: String): ImageVector =
    SubjectIconOptions.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.AutoStories

fun subjectColorForIndex(index: Int): Color =
    SubjectSwatches[index.mod(SubjectSwatches.size)]

fun highlightFillColor(colorKey: String): Color = when (colorKey) {
    "pink" -> HighlightPink
    "peach" -> HighlightPeach
    else -> HighlightYellow
}

fun highlightBorderColor(colorKey: String): Color = when (colorKey) {
    "pink" -> HighlightPinkBorder
    "peach" -> HighlightPeachBorder
    else -> HighlightYellowBorder
}

/**
 * Splits a tapped sentence or line into a reasonable (term, definition) pair if it contains
 * common study delimiters like ":", " - ", " — ", or " is ".
 */
fun suggestTermAndDefinition(rawSnippet: String): Pair<String, String> {
    val clean = rawSnippet.trim()
    val colonIdx = clean.indexOf(':')
    if (colonIdx in 2..48) {
        val term = clean.substring(0, colonIdx).trim()
        val def = clean.substring(colonIdx + 1).trim()
        if (term.isNotEmpty() && def.isNotEmpty()) return term to def
    }
    val dashSeparators = listOf(" — ", " – ", " - ")
    for (sep in dashSeparators) {
        val idx = clean.indexOf(sep)
        if (idx in 2..48) {
            val term = clean.substring(0, idx).trim()
            val def = clean.substring(idx + sep.length).trim()
            if (term.isNotEmpty() && def.isNotEmpty()) return term to def
        }
    }
    val isIdx = clean.indexOf(" is ")
    if (isIdx in 2..40) {
        val term = clean.substring(0, isIdx).trim()
        val def = clean.substring(isIdx + 4).trim()
        if (term.isNotEmpty() && def.isNotEmpty()) return term to def
    }
    val words = clean.split(Regex("\\s+"))
    val fallbackTerm = words.take(2).joinToString(" ").trimEnd('.', ',', ';')
    return fallbackTerm to clean
}
