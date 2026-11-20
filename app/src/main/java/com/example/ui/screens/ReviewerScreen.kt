package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DefinitionEntity
import com.example.data.NotePageEntity
import com.example.data.SubjectEntity
import com.example.ui.RecapState
import com.example.ui.components.highlightBorderColor
import com.example.ui.components.highlightFillColor
import com.example.ui.components.subjectColorForIndex
import com.example.ui.components.subjectIconForKey
import java.io.File

@Composable
fun ReviewerScreen(
    subjects: List<SubjectEntity>,
    notes: List<NotePageEntity>,
    definitions: List<DefinitionEntity>,
    selectedSubjectId: Long?,
    recapState: RecapState,
    onSelectSubject: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onGenerateRecap: (SubjectEntity, List<DefinitionEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSubject = remember(subjects, selectedSubjectId) {
        subjects.firstOrNull { it.id == selectedSubjectId } ?: subjects.firstOrNull()
    }

    LaunchedEffect(activeSubject?.id) {
        if (activeSubject != null && selectedSubjectId != activeSubject.id) {
            onSelectSubject(activeSubject.id)
        }
    }

    var quizModeEnabled by remember { mutableStateOf(false) }
    val revealedCards = remember { mutableStateMapOf<Long, Boolean>() }

    val subjectNotes = remember(notes, activeSubject) {
        if (activeSubject == null) emptyList() else notes.filter { it.subjectId == activeSubject.id }
    }
    val subjectDefinitions = remember(definitions, activeSubject) {
        if (activeSubject == null) emptyList() else definitions.filter { it.subjectId == activeSubject.id }
    }
    val taggedPhotos = remember(subjectNotes) {
        subjectNotes.filter { !it.photoPath.isNullOrBlank() && File(it.photoPath).exists() }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Subject Reviewer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Scroll your highlighted definitions & tagged study photos by subject",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Subject Selector Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects, key = { it.id }) { subject ->
                        val isSelected = activeSubject?.id == subject.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                revealedCards.clear()
                                onSelectSubject(subject.id)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = subjectIconForKey(subject.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        subjectColorForIndex(subject.colorIndex)
                                    }
                                )
                            },
                            label = { Text(subject.name) },
                            modifier = Modifier.testTag("reviewer_subject_${subject.id}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (activeSubject != null) {
                val accent = subjectColorForIndex(activeSubject.colorIndex)

                // Subject Summary & Study Controls
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(accent.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = subjectIconForKey(activeSubject.iconKey),
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = activeSubject.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${subjectDefinitions.size} key definitions • ${taggedPhotos.size} tagged photos",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        quizModeEnabled = !quizModeEnabled
                                        revealedCards.clear()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reviewer_quiz_toggle"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = if (quizModeEnabled) {
                                            Icons.Rounded.Visibility
                                        } else {
                                            Icons.Rounded.VisibilityOff
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (quizModeEnabled) "Show All" else "Quiz Mode",
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = { onGenerateRecap(activeSubject, subjectDefinitions) },
                                    enabled = subjectDefinitions.isNotEmpty() && recapState !is RecapState.Loading,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reviewer_ai_recap_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Recap", maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // AI Quick Recap Card
                item {
                    AnimatedVisibility(visible = recapState !is RecapState.Idle) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Quick Study Recap",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                when (recapState) {
                                    RecapState.Loading -> {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(50))
                                        )
                                    }

                                    is RecapState.Ready -> {
                                        recapState.bullets.forEach { bullet ->
                                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                                Text(
                                                    text = "• ",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = bullet,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }

                                    is RecapState.Error -> {
                                        Text(
                                            text = recapState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    RecapState.Idle -> Unit
                                }
                            }
                        }
                    }
                }

                // Definitions Section Header
                item {
                    Text(
                        text = "Key Definitions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (subjectDefinitions.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No definitions highlighted in ${activeSubject.name} yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Scan a page or open a ${activeSubject.name} note to highlight key definitions into this reviewer.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(subjectDefinitions, key = { it.id }) { def ->
                        val sourceNote = subjectNotes.firstOrNull { it.id == def.noteId }
                        val isRevealed = !quizModeEnabled || (revealedCards[def.id] == true)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    if (quizModeEnabled) {
                                        revealedCards[def.id] = !(revealedCards[def.id] ?: false)
                                    } else if (sourceNote != null) {
                                        onOpenNote(sourceNote.id)
                                    }
                                }
                                .testTag("reviewer_def_card_${def.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, highlightBorderColor(def.highlightColor).copy(alpha = 0.7f)),
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(highlightFillColor(def.highlightColor))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = def.term,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (sourceNote != null) {
                                            Text(
                                                text = sourceNote.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (isRevealed) {
                                        Text(
                                            text = def.definition,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "Tap to reveal definition",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tagged Photos Gallery for Subject
                if (taggedPhotos.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tagged Page Photos (${taggedPhotos.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(taggedPhotos, key = { it.id }) { note ->
                                Surface(
                                    modifier = Modifier
                                        .width(156.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onOpenNote(note.id) }
                                        .testTag("reviewer_photo_${note.id}"),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column {
                                        AsyncImage(
                                            model = File(note.photoPath!!),
                                            contentDescription = "Tagged photo for ${note.title}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = note.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Tap to view",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}
