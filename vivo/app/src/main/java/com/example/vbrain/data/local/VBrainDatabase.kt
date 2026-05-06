package com.example.vbrain.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vbrain.data.local.dao.KnowledgeDao
import com.example.vbrain.data.local.dao.TodoDao
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.local.entity.TodoItem

@Database(
    entities = [KnowledgeSnippet::class, TodoItem::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VBrainDatabase : RoomDatabase() {
    
    abstract val knowledgeDao: KnowledgeDao
    abstract val todoDao: TodoDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `todos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `snippetId` INTEGER NOT NULL, 
                        `taskContent` TEXT NOT NULL, 
                        `isCompleted` INTEGER NOT NULL, 
                        FOREIGN KEY(`snippetId`) REFERENCES `knowledge_snippets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todos_snippetId` ON `todos` (`snippetId`)")
            }
        }
    }
}
