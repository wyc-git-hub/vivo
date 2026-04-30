package com.example.vbrain.domain.repository

import com.example.vbrain.data.local.entity.KnowledgeSnippet
import kotlinx.coroutines.flow.Flow

interface KnowledgeRepository {
    suspend fun addSnippet(snippet: KnowledgeSnippet): Long
    
    suspend fun updateSnippet(snippet: KnowledgeSnippet)
    
    suspend fun deleteSnippet(snippet: KnowledgeSnippet)
    
    fun getAllSnippets(): Flow<List<KnowledgeSnippet>>
    
    fun getSnippetById(id: Long): Flow<KnowledgeSnippet?>
    
    fun searchSnippets(query: String): Flow<List<KnowledgeSnippet>>
    suspend fun getUnsummarizedSnippets(): List<KnowledgeSnippet>
}
