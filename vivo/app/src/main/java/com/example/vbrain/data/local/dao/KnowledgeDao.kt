package com.example.vbrain.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: KnowledgeSnippet): Long

    @Update
    suspend fun updateSnippet(snippet: KnowledgeSnippet)

    @Delete
    suspend fun deleteSnippet(snippet: KnowledgeSnippet)

    @Query("SELECT * FROM knowledge_snippets ORDER BY timestamp DESC")
    fun getAllSnippets(): Flow<List<KnowledgeSnippet>>

    @Query("SELECT * FROM knowledge_snippets WHERE id = :id")
    fun getSnippetById(id: Long): Flow<KnowledgeSnippet?>

    @Query("SELECT * FROM knowledge_snippets WHERE originalText LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchSnippets(query: String): Flow<List<KnowledgeSnippet>>

    @Query("SELECT * FROM knowledge_snippets WHERE summary = '' OR tags = '[]'")
    suspend fun getUnsummarizedSnippets(): List<KnowledgeSnippet>
}
