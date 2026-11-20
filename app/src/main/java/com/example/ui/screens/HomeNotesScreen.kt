package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DefinitionEntity
import com.example.data.NotePageEntity
import com.example.data.SubjectEntity
import com.example.ui.components.subjectColorForIndex
import com.example.ui.components.subjectIconForKey
import java.io.File

@Composable
fun HomeNotesScreen(
    subjects: List<SubjectEntity>,
    notes: List<NotePageEntity>,
    allDefinitions: List<DefinitionEntity>,
    selectedSubjectId: Long?,
    onSelectSubjectFilter: (Long?) -> Unit,
    onScanWithCamera: () -> Unit,
    onPickPhoto: () -> Unit,
    onCreateBlankNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onAddSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Hero Banner Card
            item {
                PinkHeroBanner(
                    totalNotes = notes.size,
                    totalDefinitions = allDefinitions.size,
                    onScanWithCamera = onScanWithCamera,
                    onPickPhoto = onPickPhoto,
                    onCreateBlankNote = onCreateBlankNote
                )
            }

            // Subject Filter Chips
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Subjects",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onAddSubjectClick() }
                                .testTag("quick_add_subject_chip"),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add new subject",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Subject",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSubjectId == null,
                                onClick = { onSelectSubjectFilter(null) },
                                label = { Text("All Notes") },
                                modifier = Modifier.testTag("filter_all_subjects"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        items(subjects, key = { it.id }) { subject ->
                            val isSelected = selectedSubjectId == subject.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onSelectSubjectFilter(if (isSelected) null else subject.id)
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
                                modifier = Modifier.testTag("filter_subject_${subject.id}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (selectedSubjectId == null) "Recent Class Notes" else "Filtered Notes",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${notes.size} note${if (notes.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty state or Note List
            if (notes.isEmpty()) {
                item {
                    EmptyNotesCard(
                        onScanWithCamera = onScanWithCamera,
                        onPickPhoto = onPickPhoto
                    )
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    val subject = subjects.firstOrNull { it.id == note.subjectId }
                    val defCount = allDefinitions.count { it.noteId == note.id }
                    NotePageCard(
                        note = note,
                        subject = subject,
                        definitionCount = defCount,
                        onClick = { onOpenNote(note.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun PinkHeroBanner(
    totalNotes: Int,
    totalDefinitions: Int,
    onScanWithCamera: () -> Unit,
    onPickPhoto: () -> Unit,
    onCreateBlankNote: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_study_banner),
                    contentDescription = "Pink study notebook illustration",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x33831843),
                                    Color(0xCC831843)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "No Sign-In Needed • Firebase AI Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home_banner_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "$totalNotes Notes  •  $totalDefinitions Highlighted Definitions",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFCE7F3)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onScanWithCamera,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                        .testTag("hero_scan_camera_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_scan_page),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_pick_photo_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_pick_photo),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onCreateBlankNote,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("hero_blank_note_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = "Create typed note",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotePageCard(
    note: NotePageEntity,
    subject: SubjectEntity?,
    definitionCount: Int,
    onClick: () -> Unit
) {
    val subjectColor = subjectColorForIndex(subject?.colorIndex ?: 0)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .testTag("note_card_${note.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!note.photoPath.isNullOrBlank() && File(note.photoPath).exists()) {
                AsyncImage(
                    model = File(note.photoPath),
                    contentDescription = "Photo for ${note.title}",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (subject != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = subjectColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = subjectColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val sourceLabel = when (note.sourceType) {
                        "handwritten" -> "Handwriting"
                        "typed" -> "Typed"
                        else -> "Printed"
                    }
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = note.extractedText.ifBlank { "Tap to extract text from this photo..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$definitionCount highlight${if (definitionCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            note.updatedAt,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotesCard(
    onScanWithCamera: () -> Unit,
    onPickPhoto: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your study shelf is ready!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Take a photo of a textbook page or tidy handwritten notes. EduFlow will extract editable text and let you highlight key definitions into a subject reviewer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanWithCamera,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("empty_scan_button")
                ) {
                    Text("Scan a Page")
                }
                FilledTonalButton(
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("empty_upload_button")
                ) {
                    Text("Pick Photo")
                }
            }
        }
    }
}
