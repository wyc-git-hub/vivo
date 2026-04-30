package com.example.vbrain.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.vbrain.data.local.dao.KnowledgeDao
import com.example.vbrain.data.local.entity.KnowledgeSnippet

@Database(
    entities = [KnowledgeSnippet::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VBrainDatabase : RoomDatabase() {
    
    abstract val knowledgeDao: KnowledgeDao

    companion object {
        const val DATABASE_NAME = "v_brain_db"
    }
}

