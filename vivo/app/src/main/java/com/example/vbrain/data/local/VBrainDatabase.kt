package com.example.vbrain.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vbrain.data.local.dao.KnowledgeDao
import com.example.vbrain.data.local.entity.KnowledgeSnippet

@Database(
    entities = [KnowledgeSnippet::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VBrainDatabase : RoomDatabase() {
    
    abstract val knowledgeDao: KnowledgeDao

    companion object {
        const val DATABASE_NAME = "v_brain_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE knowledge_snippets ADD COLUMN formattedText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE knowledge_snippets ADD COLUMN imagePath TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE knowledge_snippets ADD COLUMN imagePaths TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE knowledge_snippets ADD COLUMN sourceUrl TEXT")
            }
        }
    }
}
