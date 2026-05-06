package com.example.vbrain.domain.repository

import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.local.entity.TodoItem
import kotlinx.coroutines.flow.Flow

interface KnowledgeRepository {
    suspend fun addSnippet(snippet: KnowledgeSnippet): Long
    
    suspend fun addSnippets(snippets: List<KnowledgeSnippet>)
    
    suspend fun updateSnippet(snippet: KnowledgeSnippet)
    
    suspend fun deleteSnippet(snippet: KnowledgeSnippet)
    
    fun getAllSnippets(): Flow<List<KnowledgeSnippet>>
    
    fun getSnippetById(id: Long): Flow<KnowledgeSnippet?>
    
    fun searchSnippets(query: String): Flow<List<KnowledgeSnippet>>
    suspend fun getUnsummarizedSnippets(): List<KnowledgeSnippet>

    // --- Todo Methods ---
    suspend fun addTodoItem(todo: TodoItem)
    suspend fun addTodoItems(todos: List<TodoItem>)
    suspend fun deleteTodoItem(todo: TodoItem)
    suspend fun toggleTodoCompletion(todoId: Long, isCompleted: Boolean)
    fun getTodosBySnippetId(snippetId: Long): Flow<List<TodoItem>>
    fun getAllTodos(): Flow<List<TodoItem>>
    fun getAllTodosWithSnippet(): Flow<List<com.example.vbrain.data.local.dao.TodoWithSnippet>>
}
