package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EduFlowDao {
    // Subjects
    @Query("SELECT * FROM subjects ORDER BY createdAt ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubjectById(subjectId: Long)

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    // Note Pages
    @Query("SELECT * FROM note_pages ORDER BY updatedAt DESC")
    fun observeAllNotes(): Flow<List<NotePageEntity>>

    @Query("SELECT * FROM note_pages WHERE id = :noteId LIMIT 1")
    fun observeNoteById(noteId: Long): Flow<NotePageEntity?>

    @Query("SELECT * FROM note_pages WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): NotePageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NotePageEntity): Long

    @Update
    suspend fun updateNote(note: NotePageEntity)

    @Query("UPDATE definitions SET subjectId = :newSubjectId WHERE noteId = :noteId")
    suspend fun updateDefinitionsSubjectForNote(noteId: Long, newSubjectId: Long)

    @Query("DELETE FROM note_pages WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    // Definitions
    @Query("SELECT * FROM definitions ORDER BY createdAt DESC")
    fun observeAllDefinitions(): Flow<List<DefinitionEntity>>

    @Query("SELECT * FROM definitions WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun observeDefinitionsForNote(noteId: Long): Flow<List<DefinitionEntity>>

    @Query("SELECT * FROM definitions WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun observeDefinitionsForSubject(subjectId: Long): Flow<List<DefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefinition(definition: DefinitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefinitions(definitions: List<DefinitionEntity>)

    @Update
    suspend fun updateDefinition(definition: DefinitionEntity)

    @Query("DELETE FROM definitions WHERE id = :definitionId")
    suspend fun deleteDefinitionById(definitionId: Long)
}
