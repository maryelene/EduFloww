package com.example.data

import android.net.Uri
import com.example.ai.AiExtractedDefinition
import com.example.ai.AiPageExtractionResult
import com.example.ai.EduFlowAiService
import kotlinx.coroutines.flow.Flow

class EduFlowRepository(
    private val dao: EduFlowDao,
    private val photoHelper: PhotoStorageHelper,
    private val aiService: EduFlowAiService
) {
    val subjects: Flow<List<SubjectEntity>> = dao.observeSubjects()
    val notes: Flow<List<NotePageEntity>> = dao.observeAllNotes()
    val definitions: Flow<List<DefinitionEntity>> = dao.observeAllDefinitions()

    fun observeNote(noteId: Long): Flow<NotePageEntity?> = dao.observeNoteById(noteId)

    fun observeDefinitionsForNote(noteId: Long): Flow<List<DefinitionEntity>> =
        dao.observeDefinitionsForNote(noteId)

    fun observeDefinitionsForSubject(subjectId: Long): Flow<List<DefinitionEntity>> =
        dao.observeDefinitionsForSubject(subjectId)

    fun createTempCameraUri(): Uri = photoHelper.createTempCameraUri()

    suspend fun ensureDefaultSubjectId(): Long {
        if (dao.getSubjectCount() == 0) {
            return dao.insertSubject(
                SubjectEntity(name = "General", iconKey = "book", colorIndex = 0)
            )
        }
        return 1L
    }

    suspend fun addSubject(name: String, iconKey: String, colorIndex: Int): Long {
        return dao.insertSubject(
            SubjectEntity(
                name = name.trim(),
                iconKey = iconKey,
                colorIndex = colorIndex
            )
        )
    }

    suspend fun deleteSubject(subjectId: Long) {
        dao.deleteSubjectById(subjectId)
    }

    suspend fun createNoteFromPhotoUri(uri: Uri, subjectId: Long): Long? {
        val savedPath = photoHelper.savePhotoToInternalStorage(uri) ?: return null
        val now = System.currentTimeMillis()
        return dao.insertNote(
            NotePageEntity(
                subjectId = subjectId,
                title = "Scanned Page",
                photoPath = savedPath,
                extractedText = "",
                sourceType = "printed",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun createBlankNote(subjectId: Long, title: String = "Quick Study Note"): Long {
        val now = System.currentTimeMillis()
        return dao.insertNote(
            NotePageEntity(
                subjectId = subjectId,
                title = title,
                photoPath = null,
                extractedText = "",
                sourceType = "typed",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateNote(note: NotePageEntity) {
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        dao.updateNote(updated)
        dao.updateDefinitionsSubjectForNote(note.id, note.subjectId)
    }

    suspend fun deleteNote(note: NotePageEntity) {
        photoHelper.deletePhotoFile(note.photoPath)
        dao.deleteNoteById(note.id)
    }

    suspend fun addDefinition(
        noteId: Long,
        subjectId: Long,
        term: String,
        definition: String,
        matchedSnippet: String,
        highlightColor: String = "yellow"
    ): Long {
        return dao.insertDefinition(
            DefinitionEntity(
                noteId = noteId,
                subjectId = subjectId,
                term = term.trim(),
                definition = definition.trim(),
                matchedSnippet = matchedSnippet.trim(),
                highlightColor = highlightColor
            )
        )
    }

    suspend fun updateDefinition(definition: DefinitionEntity) {
        dao.updateDefinition(definition)
    }

    suspend fun deleteDefinition(definitionId: Long) {
        dao.deleteDefinitionById(definitionId)
    }

    suspend fun extractTextAndDefinitionsFromNotePhoto(noteId: Long): Result<AiPageExtractionResult> {
        val note = dao.getNoteById(noteId)
            ?: return Result.failure(IllegalStateException("Note not found"))
        val photoPath = note.photoPath
            ?: return Result.failure(IllegalStateException("This note does not have a photo attached."))
        val bitmap = photoHelper.loadBitmapForAi(photoPath)
            ?: return Result.failure(IllegalStateException("Could not load the photo from storage."))

        val result = try {
            aiService.extractNoteFromBitmap(bitmap)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }

        result.onSuccess { extracted ->
            val updatedNote = note.copy(
                title = if (note.title == "Scanned Page" || note.title.isBlank()) {
                    extracted.suggestedTitle
                } else {
                    note.title
                },
                extractedText = extracted.extractedText,
                sourceType = extracted.sourceType,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateNote(updatedNote)

            if (extracted.definitions.isNotEmpty()) {
                val colors = listOf("yellow", "pink", "peach")
                val entities = extracted.definitions.mapIndexed { idx, def ->
                    DefinitionEntity(
                        noteId = note.id,
                        subjectId = note.subjectId,
                        term = def.term,
                        definition = def.definition,
                        matchedSnippet = def.matchedSnippet,
                        highlightColor = colors[idx % colors.size]
                    )
                }
                dao.insertDefinitions(entities)
            }
        }
        return result
    }

    suspend fun autoDetectDefinitionsForNote(noteId: Long): Result<List<AiExtractedDefinition>> {
        val note = dao.getNoteById(noteId)
            ?: return Result.failure(IllegalStateException("Note not found"))
        val result = aiService.findDefinitionsInText(note.extractedText)
        result.onSuccess { defs ->
            if (defs.isNotEmpty()) {
                val colors = listOf("yellow", "pink", "peach")
                val entities = defs.mapIndexed { idx, def ->
                    DefinitionEntity(
                        noteId = note.id,
                        subjectId = note.subjectId,
                        term = def.term,
                        definition = def.definition,
                        matchedSnippet = def.matchedSnippet,
                        highlightColor = colors[idx % colors.size]
                    )
                }
                dao.insertDefinitions(entities)
            }
        }
        return result
    }

    suspend fun generateSubjectRecap(
        subjectName: String,
        definitions: List<DefinitionEntity>
    ): Result<List<String>> {
        val summary = definitions.take(25).joinToString("\n") { "- ${it.term}: ${it.definition}" }
        return aiService.generateSubjectRecap(subjectName, summary)
    }
}
