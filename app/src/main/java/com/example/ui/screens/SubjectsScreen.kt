package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.DefinitionEntity
import com.example.data.NotePageEntity
import com.example.data.SubjectEntity
import com.example.ui.components.SubjectIconOptions
import com.example.ui.components.subjectColorForIndex
import com.example.ui.components.subjectIconForKey
import com.example.ui.theme.SubjectSwatches

@Composable
fun SubjectsScreen(
    subjects: List<SubjectEntity>,
    notes: List<NotePageEntity>,
    definitions: List<DefinitionEntity>,
    onOpenSubjectReviewer: (Long) -> Unit,
    onAddSubjectClick: () -> Unit,
    onDeleteSubject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteSubject by remember { mutableStateOf<SubjectEntity?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Subjects",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Organize notes and open subject reviewers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onAddSubjectClick,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("subjects_add_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New")
                    }
                }
            }

            items(subjects, key = { it.id }) { subject ->
                val noteCount = notes.count { it.subjectId == subject.id }
                val defCount = definitions.count { it.subjectId == subject.id }
                val accent = subjectColorForIndex(subject.colorIndex)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .clickable { onOpenSubjectReviewer(subject.id) }
                        .testTag("subject_card_${subject.id}"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = subjectIconForKey(subject.iconKey),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$noteCount note${if (noteCount == 1) "" else "s"} • $defCount definition${if (defCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (subjects.size > 1) {
                            IconButton(
                                onClick = { pendingDeleteSubject = subject },
                                modifier = Modifier.testTag("delete_subject_${subject.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete ${subject.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open ${subject.name} reviewer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    pendingDeleteSubject?.let { subject ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSubject = null },
            title = { Text("Delete ${subject.name}?") },
            text = { Text("All notes and highlighted definitions tagged to ${subject.name} will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSubject(subject.id)
                        pendingDeleteSubject = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSubject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onCreateSubject: (name: String, iconKey: String, colorIndex: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf(SubjectIconOptions.first().key) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Subject",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g., Chemistry)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_subject_name_input")
                )

                Text(
                    text = "Pick an Icon",
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SubjectIconOptions) { option ->
                        val isSelected = selectedIconKey == option.key
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedIconKey = option.key },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Pick a Pink / Pastel Accent",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SubjectSwatches.forEachIndexed { index, color ->
                        val isSelected = selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF2B131D) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateSubject(name, selectedIconKey, selectedColorIndex)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_subject_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
