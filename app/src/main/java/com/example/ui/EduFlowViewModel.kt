package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.EduFlowAiService
import com.example.data.DefinitionEntity
import com.example.data.EduFlowDatabase
import com.example.data.EduFlowRepository
import com.example.data.NotePageEntity
import com.example.data.PhotoStorageHelper
import com.example.data.SubjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Loading(val message: String) : AiTaskState
    data class Success(val message: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

sealed interface RecapState {
    data object Idle : RecapState
    data object Loading : RecapState
    data class Ready(val subjectId: Long, val bullets: List<String>) : RecapState
    data class Error(val message: String) : RecapState
}

class EduFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EduFlowRepository by lazy {
        val db = EduFlowDatabase.getInstance(application)
        EduFlowRepository(
            dao = db.eduFlowDao(),
            photoHelper = PhotoStorageHelper(application),
            aiService = EduFlowAiService()
        )
    }

    val subjects: StateFlow<List<SubjectEntity>> = repository.subjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NotePageEntity>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDefinitions: StateFlow<List<DefinitionEntity>> = repository.definitions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHomeSubjectId = MutableStateFlow<Long?>(null)
    val selectedHomeSubjectId: StateFlow<Long?> = _selectedHomeSubjectId.asStateFlow()

    private val _selectedReviewerSubjectId = MutableStateFlow<Long?>(null)
    val selectedReviewerSubjectId: StateFlow<Long?> = _selectedReviewerSubjectId.asStateFlow()

    val filteredNotes: StateFlow<List<NotePageEntity>> = combine(
        allNotes,
        _selectedHomeSubjectId
    ) { notes, subjectFilter ->
        if (subjectFilter == null) notes else notes.filter { it.subjectId == subjectFilter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiTaskState: StateFlow<AiTaskState> = _aiTaskState.asStateFlow()

    private val _recapState = MutableStateFlow<RecapState>(RecapState.Idle)
    val recapState: StateFlow<RecapState> = _recapState.asStateFlow()

    private var pendingCameraUri: Uri? = null

    fun selectHomeSubjectFilter(subjectId: Long?) {
        _selectedHomeSubjectId.value = subjectId
    }

    fun selectReviewerSubject(subjectId: Long) {
        _selectedReviewerSubjectId.value = subjectId
        val current = _recapState.value
        if (current is RecapState.Ready && current.subjectId != subjectId) {
            _recapState.value = RecapState.Idle
        }
    }

    fun prepareCameraUri(): Uri {
        val uri = repository.createTempCameraUri()
        pendingCameraUri = uri
        return uri
    }

    fun consumePendingCameraUri(): Uri? = pendingCameraUri

    fun importPhotoAndOpenNote(
        uri: Uri,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val targetSubjectId = _selectedHomeSubjectId.value
                ?: subjects.value.firstOrNull()?.id
                ?: repository.ensureDefaultSubjectId()
            val noteId = repository.createNoteFromPhotoUri(uri, targetSubjectId)
            if (noteId != null) {
                onCreated(noteId)
                extractFromPhoto(noteId)
            } else {
                _aiTaskState.value = AiTaskState.Error("Could not read that photo. Please try another image.")
            }
        }
    }

    fun createBlankNoteAndOpen(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val targetSubjectId = _selectedHomeSubjectId.value
                ?: subjects.value.firstOrNull()?.id
                ?: repository.ensureDefaultSubjectId()
            val noteId = repository.createBlankNote(targetSubjectId)
            _aiTaskState.value = AiTaskState.Idle
            onCreated(noteId)
        }
    }

    fun observeNote(noteId: Long) = repository.observeNote(noteId)

    fun observeDefinitionsForNote(noteId: Long) = repository.observeDefinitionsForNote(noteId)

    fun extractFromPhoto(noteId: Long) {
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Reading printed text & handwriting with Firebase AI...")
            val result = repository.extractTextAndDefinitionsFromNotePhoto(noteId)
            result.onSuccess { extracted ->
                val defCount = extracted.definitions.size
                _aiTaskState.value = AiTaskState.Success(
                    if (defCount > 0) {
                        "Extracted text & highlighted $defCount key definition${if (defCount == 1) "" else "s"}!"
                    } else {
                        "Extracted editable text! Tap any line below to highlight a definition."
                    }
                )
            }.onFailure { err ->
                _aiTaskState.value = AiTaskState.Error(
                    err.localizedMessage ?: "Failed to extract text from photo."
                )
            }
        }
    }

    fun autoDetectDefinitions(noteId: Long) {
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Finding key definitions with Firebase AI...")
            val result = repository.autoDetectDefinitionsForNote(noteId)
            result.onSuccess { defs ->
                _aiTaskState.value = if (defs.isEmpty()) {
                    AiTaskState.Success("No extra definitions found—tap any sentence to highlight manually!")
                } else {
                    AiTaskState.Success("Highlighted ${defs.size} key definition${if (defs.size == 1) "" else "s"}!")
                }
            }.onFailure { err ->
                _aiTaskState.value = AiTaskState.Error(
                    err.localizedMessage ?: "Could not detect key definitions."
                )
            }
        }
    }

    fun clearAiTaskState() {
        _aiTaskState.value = AiTaskState.Idle
    }

    fun saveNoteEdits(
        note: NotePageEntity,
        newTitle: String,
        newText: String,
        newSubjectId: Long
    ) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(
                    title = newTitle.trim().ifBlank { "Study Note" },
                    extractedText = newText,
                    subjectId = newSubjectId
                )
            )
        }
    }

    fun deleteNote(note: NotePageEntity, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(note)
            onDeleted()
        }
    }

    fun saveDefinition(
        existingId: Long?,
        noteId: Long,
        subjectId: Long,
        term: String,
        definition: String,
        matchedSnippet: String,
        highlightColor: String
    ) {
        if (term.isBlank() || definition.isBlank()) return
        viewModelScope.launch {
            if (existingId != null && existingId > 0L) {
                repository.updateDefinition(
                    DefinitionEntity(
                        id = existingId,
                        noteId = noteId,
                        subjectId = subjectId,
                        term = term.trim(),
                        definition = definition.trim(),
                        matchedSnippet = matchedSnippet.trim(),
                        highlightColor = highlightColor
                    )
                )
            } else {
                repository.addDefinition(
                    noteId = noteId,
                    subjectId = subjectId,
                    term = term,
                    definition = definition,
                    matchedSnippet = matchedSnippet,
                    highlightColor = highlightColor
                )
            }
        }
    }

    fun deleteDefinition(definitionId: Long) {
        viewModelScope.launch {
            repository.deleteDefinition(definitionId)
        }
    }

    fun addSubject(name: String, iconKey: String, colorIndex: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addSubject(name, iconKey, colorIndex)
        }
    }

    fun deleteSubject(subjectId: Long) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
            if (_selectedHomeSubjectId.value == subjectId) {
                _selectedHomeSubjectId.value = null
            }
            if (_selectedReviewerSubjectId.value == subjectId) {
                _selectedReviewerSubjectId.value = null
            }
        }
    }

    fun generateSubjectRecap(subject: SubjectEntity, defs: List<DefinitionEntity>) {
        viewModelScope.launch {
            _recapState.value = RecapState.Loading
            val result = repository.generateSubjectRecap(subject.name, defs)
            result.onSuccess { bullets ->
                _recapState.value = RecapState.Ready(subject.id, bullets)
            }.onFailure { err ->
                _recapState.value = RecapState.Error(
                    err.localizedMessage ?: "Could not generate study recap."
                )
            }
        }
    }

    fun clearRecap() {
        _recapState.value = RecapState.Idle
    }
}
