package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String = "book",
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "note_pages",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class NotePageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val photoPath: String? = null,
    val extractedText: String = "",
    val sourceType: String = "printed", // "printed", "handwritten", "typed"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "definitions",
    foreignKeys = [
        ForeignKey(
            entity = NotePageEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["subjectId"])
    ]
)
data class DefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val subjectId: Long,
    val term: String,
    val definition: String,
    val matchedSnippet: String = "",
    val highlightColor: String = "yellow", // "yellow", "pink", "peach"
    val createdAt: Long = System.currentTimeMillis()
)
