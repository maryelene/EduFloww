package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.DefinitionEntity

private data class TextSegment(
    val text: String,
    val matchedDefinition: DefinitionEntity?
)

/**
 * Renders extracted note text in a clean notebook surface where students can tap any sentence/line
 * right on their phone screen to highlight it as a key definition, or tap existing highlights to edit.
 */
@Composable
fun InteractiveHighlighterCard(
    extractedText: String,
    definitions: List<DefinitionEntity>,
    onSegmentTapped: (snippet: String, existingDef: DefinitionEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(extractedText, definitions) {
        splitIntoInteractiveSegments(extractedText, definitions)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tap any line to highlight a definition",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (segments.isEmpty()) {
                Text(
                    text = "No text yet. Scan a page, upload a photo, or switch to Edit Text to type your notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                segments.forEachIndexed { index, segment ->
                    val def = segment.matchedDefinition
                    val isHighlighted = def != null
                    val bgColor = if (def != null) {
                        highlightFillColor(def.highlightColor)
                    } else {
                        Color.Transparent
                    }
                    val borderColor = if (def != null) {
                        highlightBorderColor(def.highlightColor).copy(alpha = 0.65f)
                    } else {
                        Color.Transparent
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSegmentTapped(segment.text, def) }
                            .testTag("highlight_segment_$index"),
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor,
                        border = if (isHighlighted) BorderStroke(1.dp, borderColor) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = if (isHighlighted) 10.dp else 4.dp,
                                vertical = 6.dp
                            ),
                            verticalAlignment = Alignment.Top
                        ) {
                            val styledText = remember(segment.text, def) {
                                buildAnnotatedString {
                                    if (def != null) {
                                        val termIdx = segment.text.indexOf(def.term, ignoreCase = true)
                                        if (termIdx >= 0) {
                                            append(segment.text.substring(0, termIdx))
                                            withStyle(
                                                SpanStyle(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF831843)
                                                )
                                            ) {
                                                append(
                                                    segment.text.substring(
                                                        termIdx,
                                                        termIdx + def.term.length
                                                    )
                                                )
                                            }
                                            append(segment.text.substring(termIdx + def.term.length))
                                        } else {
                                            append(segment.text)
                                        }
                                    } else {
                                        append(segment.text)
                                    }
                                }
                            }
                            Text(
                                text = styledText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isHighlighted) Color(0xFF2B131D) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (def != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .background(
                                            color = Color.White.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit highlighted definition",
                                        tint = Color(0xFF831843),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun splitIntoInteractiveSegments(
    text: String,
    definitions: List<DefinitionEntity>
): List<TextSegment> {
    if (text.isBlank()) return emptyList()

    val rawLines = text.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val segments = mutableListOf<String>()
    for (line in rawLines) {
        if (line.length <= 130) {
            segments.add(line)
        } else {
            // Split longer paragraphs into crisp sentences so each is easy to tap & highlight
            val sentences = line.split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            segments.addAll(sentences)
        }
    }

    return segments.map { seg ->
        val match = definitions.firstOrNull { def ->
            (def.matchedSnippet.isNotBlank() &&
                (seg.contains(def.matchedSnippet, ignoreCase = true) ||
                    def.matchedSnippet.contains(seg, ignoreCase = true))) ||
                (def.term.length >= 3 && seg.contains(def.term, ignoreCase = true))
        }
        TextSegment(text = seg, matchedDefinition = match)
    }
}
