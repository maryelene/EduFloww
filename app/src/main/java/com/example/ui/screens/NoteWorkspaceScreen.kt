package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DefinitionEntity
import com.example.data.NotePageEntity
import com.example.data.SubjectEntity
import com.example.ui.AiTaskState
import com.example.ui.components.InteractiveHighlighterCard
import com.example.ui.components.highlightBorderColor
import com.example.ui.components.highlightFillColor
import com.example.ui.components.subjectColorForIndex
import com.example.ui.components.subjectIconForKey
import com.example.ui.components.suggestTermAndDefinition
import java.io.File

private data class HighlightDialogState(
    val existingId: Long?,
    val term: String,
    val definition: String,
    val matchedSnippet: String,
    val colorKey: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteWorkspaceScreen(
    note: NotePageEntity?,
    subjects: List<SubjectEntity>,
    definitions: List<DefinitionEntity>,
    aiTaskState: AiTaskState,
    onBack: () -> Unit,
    onSaveNote: (NotePageEntity, String, String, Long) -> Unit,
    onExtractWithAi: (Long) -> Unit,
    onAutoDetectDefinitions: (Long) -> Unit,
    onClearAiState: () -> Unit,
    onSaveDefinition: (Long?, Long, Long, String, String, String, String) -> Unit,
    onDeleteDefinition: (Long) -> Unit,
    onDeleteNote: (NotePageEntity) -> Unit
) {
    if (note == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var titleText by remember(note.id, note.title) { mutableStateOf(note.title) }
    var editableBody by remember(note.id, note.extractedText) { mutableStateOf(note.extractedText) }
    var selectedSubjectId by remember(note.id, note.subjectId) { mutableStateOf(note.subjectId) }
    var selectedModeIndex by remember { mutableIntStateOf(0) } // 0=Highlight, 1=Edit, 2=Definitions
    var showPhotoZoom by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var highlightDialog by remember { mutableStateOf<HighlightDialogState?>(null) }

    // Keep synced when AI populates extractedText/title
    LaunchedEffect(note.extractedText) {
        if (note.extractedText != editableBody && editableBody.isBlank()) {
            editableBody = note.extractedText
        }
    }
    LaunchedEffect(note.title) {
        if (titleText == "Scanned Page" && note.title != "Scanned Page") {
            titleText = note.title
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText.ifBlank { "Note Workspace" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onSaveNote(note, titleText, editableBody, selectedSubjectId)
                            onBack()
                        },
                        modifier = Modifier.testTag("workspace_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to notes"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("workspace_delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Status Banner
                item {
                    AnimatedVisibility(visible = aiTaskState !is AiTaskState.Idle) {
                        AiStatusBanner(
                            state = aiTaskState,
                            onDismiss = onClearAiState,
                            onRetry = { onExtractWithAi(note.id) }
                        )
                    }
                }

                // Title & Subject Tagging Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            OutlinedTextField(
                                value = titleText,
                                onValueChange = {
                                    titleText = it
                                    onSaveNote(note, it, editableBody, selectedSubjectId)
                                },
                                label = { Text("Note Title") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("note_title_input"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Tag Subject",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(subjects, key = { it.id }) { subject ->
                                    val isSelected = selectedSubjectId == subject.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedSubjectId = subject.id
                                            onSaveNote(note, titleText, editableBody, subject.id)
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
                                        modifier = Modifier.testTag("workspace_subject_${subject.id}"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Tagged Photo Card + Firebase AI Extract CTA
                if (!note.photoPath.isNullOrBlank() && File(note.photoPath).exists()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { showPhotoZoom = true }
                                        .testTag("tagged_photo_preview")
                                ) {
                                    AsyncImage(
                                        model = File(note.photoPath),
                                        contentDescription = "Tagged page photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp),
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ZoomIn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "View Page",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { onExtractWithAi(note.id) },
                                    enabled = aiTaskState !is AiTaskState.Loading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("extract_ai_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (editableBody.isBlank()) {
                                            stringResource(R.string.action_extract_ai)
                                        } else {
                                            "Re-Extract Text & Definitions (AI)"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Segmented Mode Switcher: Highlight | Edit Text | Definitions
                item {
                    val options = listOf(
                        "Highlight" to Icons.Rounded.Highlight,
                        "Edit Text" to Icons.Rounded.Edit,
                        "Saved (${definitions.size})" to Icons.Rounded.BookmarkAdd
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEachIndexed { index, (label, icon) ->
                            SegmentedButton(
                                selected = selectedModeIndex == index,
                                onClick = { selectedModeIndex = index },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("workspace_mode_$index")
                            ) {
                                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Active Mode Content
                when (selectedModeIndex) {
                    0 -> {
                        // Highlight Mode
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        highlightDialog = HighlightDialogState(
                                            existingId = null,
                                            term = "",
                                            definition = "",
                                            matchedSnippet = "",
                                            colorKey = "yellow"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("add_manual_definition_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Definition")
                                }

                                OutlinedButton(
                                    onClick = {
                                        onSaveNote(note, titleText, editableBody, selectedSubjectId)
                                        onAutoDetectDefinitions(note.id)
                                    },
                                    enabled = editableBody.isNotBlank() && aiTaskState !is AiTaskState.Loading,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("ai_find_definitions_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Highlight")
                                }
                            }
                        }

                        item {
                            InteractiveHighlighterCard(
                                extractedText = editableBody,
                                definitions = definitions,
                                onSegmentTapped = { snippet, existing ->
                                    if (existing != null) {
                                        highlightDialog = HighlightDialogState(
                                            existingId = existing.id,
                                            term = existing.term,
                                            definition = existing.definition,
                                            matchedSnippet = existing.matchedSnippet,
                                            colorKey = existing.highlightColor
                                        )
                                    } else {
                                        val (suggestedTerm, suggestedDef) = suggestTermAndDefinition(snippet)
                                        highlightDialog = HighlightDialogState(
                                            existingId = null,
                                            term = suggestedTerm,
                                            definition = suggestedDef,
                                            matchedSnippet = snippet,
                                            colorKey = "yellow"
                                        )
                                    }
                                }
                            )
                        }
                    }

                    1 -> {
                        // Edit Text Mode
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Edit Extracted Text",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editableBody,
                                        onValueChange = { editableBody = it },
                                        placeholder = {
                                            Text("Type or edit your extracted class notes here...")
                                        },
                                        minLines = 8,
                                        maxLines = 20,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("editable_note_body_input"),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            onSaveNote(note, titleText, editableBody, selectedSubjectId)
                                            selectedModeIndex = 0
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("save_edited_text_button"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save & Highlight Text")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Saved Definitions List
                        if (definitions.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No highlighted definitions yet",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Switch to Highlight mode and tap any line to highlight key terms into your reviewer.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(definitions, key = { it.id }) { def ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            highlightDialog = HighlightDialogState(
                                                existingId = def.id,
                                                term = def.term,
                                                definition = def.definition,
                                                matchedSnippet = def.matchedSnippet,
                                                colorKey = def.highlightColor
                                            )
                                        }
                                        .testTag("note_definition_card_${def.id}"),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, highlightBorderColor(def.highlightColor))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(highlightFillColor(def.highlightColor))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = def.term,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = def.definition,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteDefinition(def.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Remove definition",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    // Full-screen photo zoom dialog
    if (showPhotoZoom && !note.photoPath.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showPhotoZoom = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(16.dp)
            ) {
                AsyncImage(
                    model = File(note.photoPath),
                    contentDescription = "Full page photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showPhotoZoom = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close photo view",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Highlight / Edit Definition Dialog
    highlightDialog?.let { dialog ->
        var termInput by remember(dialog) { mutableStateOf(dialog.term) }
        var defInput by remember(dialog) { mutableStateOf(dialog.definition) }
        var colorKey by remember(dialog) { mutableStateOf(dialog.colorKey) }

        AlertDialog(
            onDismissRequest = { highlightDialog = null },
            title = {
                Text(
                    text = if (dialog.existingId != null) "Edit Highlight" else "Highlight Definition",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = termInput,
                        onValueChange = { termInput = it },
                        label = { Text("Key Term / Concept") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("highlight_term_input")
                    )
                    OutlinedTextField(
                        value = defInput,
                        onValueChange = { defInput = it },
                        label = { Text("Definition / Explanation") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("highlight_definition_input")
                    )
                    Text(
                        text = "Highlighter Marker Color",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            "yellow" to "Sunny",
                            "pink" to "Blush",
                            "peach" to "Peach"
                        ).forEach { (key, label) ->
                            val selected = colorKey == key
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { colorKey = key }
                                    .testTag("marker_color_$key"),
                                shape = RoundedCornerShape(12.dp),
                                color = highlightFillColor(key),
                                border = BorderStroke(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else highlightBorderColor(key)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color(0xFF2B131D)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveDefinition(
                            dialog.existingId,
                            note.id,
                            selectedSubjectId,
                            termInput,
                            defInput,
                            dialog.matchedSnippet.ifBlank { defInput },
                            colorKey
                        )
                        highlightDialog = null
                    },
                    enabled = termInput.isNotBlank() && defInput.isNotBlank(),
                    modifier = Modifier.testTag("save_highlight_button")
                ) {
                    Text("Save Highlight")
                }
            },
            dismissButton = {
                Row {
                    if (dialog.existingId != null) {
                        TextButton(
                            onClick = {
                                onDeleteDefinition(dialog.existingId)
                                highlightDialog = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove")
                        }
                    }
                    TextButton(onClick = { highlightDialog = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Delete Note Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this note?") },
            text = { Text("This will remove the note, tagged photo, and its highlighted definitions.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteNote(note)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Keep")
                }
            }
        )
    }
}

@Composable
private fun AiStatusBanner(
    state: AiTaskState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val containerColor = when (state) {
        is AiTaskState.Error -> MaterialTheme.colorScheme.errorContainer
        is AiTaskState.Success -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = if (state is AiTaskState.Error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val text = when (state) {
                        is AiTaskState.Loading -> state.message
                        is AiTaskState.Success -> state.message
                        is AiTaskState.Error -> state.message
                        AiTaskState.Idle -> ""
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (state is AiTaskState.Error) {
                    TextButton(onClick = onRetry) {
                        Text("Retry")
                    }
                } else if (state is AiTaskState.Success) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss message",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (state is AiTaskState.Loading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}
