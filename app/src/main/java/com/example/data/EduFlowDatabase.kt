package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [SubjectEntity::class, NotePageEntity::class, DefinitionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EduFlowDatabase : RoomDatabase() {
    abstract fun eduFlowDao(): EduFlowDao

    companion object {
        @Volatile
        private var INSTANCE: EduFlowDatabase? = null

        fun getInstance(context: Context): EduFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EduFlowDatabase::class.java,
                    "eduflow_notes_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).eduFlowDao()
                                if (dao.getSubjectCount() == 0) {
                                    seedDefaultSubjects(dao)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultSubjects(dao: EduFlowDao) {
            val defaults = listOf(
                SubjectEntity(name = "Biology", iconKey = "biotech", colorIndex = 0),
                SubjectEntity(name = "History", iconKey = "history", colorIndex = 1),
                SubjectEntity(name = "English", iconKey = "menu_book", colorIndex = 2),
                SubjectEntity(name = "Science", iconKey = "science", colorIndex = 3)
            )
            defaults.forEach { dao.insertSubject(it) }
        }
    }
}
