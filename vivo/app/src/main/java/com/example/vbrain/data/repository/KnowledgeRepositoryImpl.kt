package com.example.vbrain.data.repository

import com.example.vbrain.data.local.dao.KnowledgeDao
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.domain.repository.KnowledgeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class KnowledgeRepositoryImpl @Inject constructor(
    private val dao: KnowledgeDao
) : KnowledgeRepository {

    override suspend fun addSnippet(snippet: KnowledgeSnippet): Long = dao.insertSnippet(snippet)

    override suspend fun updateSnippet(snippet: KnowledgeSnippet) {
        dao.updateSnippet(snippet)
    }

    override suspend fun deleteSnippet(snippet: KnowledgeSnippet) {
        dao.deleteSnippet(snippet)
    }

    override fun getAllSnippets(): Flow<List<KnowledgeSnippet>> {
        return dao.getAllSnippets()
    }

    override fun getSnippetById(id: Long): Flow<KnowledgeSnippet?> {
        return dao.getSnippetById(id)
    }

    override fun searchSnippets(query: String): Flow<List<KnowledgeSnippet>> = dao.searchSnippets(query)

    override suspend fun getUnsummarizedSnippets(): List<KnowledgeSnippet> = dao.getUnsummarizedSnippets()
}
