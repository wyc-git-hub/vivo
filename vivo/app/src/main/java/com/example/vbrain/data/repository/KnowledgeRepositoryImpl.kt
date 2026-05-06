package com.example.vbrain.data.repository

import com.example.vbrain.data.local.dao.KnowledgeDao
import com.example.vbrain.data.local.dao.TodoDao
import com.example.vbrain.data.local.dao.TodoWithSnippet
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.local.entity.TodoItem
import com.example.vbrain.domain.repository.KnowledgeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class KnowledgeRepositoryImpl @Inject constructor(
    private val dao: KnowledgeDao,
    private val todoDao: TodoDao
) : KnowledgeRepository {

    override suspend fun addSnippet(snippet: KnowledgeSnippet): Long {
        return dao.insertSnippet(snippet)
    }

    override suspend fun addSnippets(snippets: List<KnowledgeSnippet>) = dao.insertSnippets(snippets)

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

    override suspend fun getUnsummarizedSnippets(): List<KnowledgeSnippet> {
        return dao.getUnsummarizedSnippets()
    }

    // --- Todo Methods ---
    override suspend fun addTodoItem(todo: TodoItem) {
        todoDao.insertTodo(todo)
    }

    override suspend fun addTodoItems(todos: List<TodoItem>) {
        todoDao.insertTodos(todos)
    }

    override suspend fun deleteTodoItem(todo: TodoItem) {
        todoDao.deleteTodo(todo)
    }

    override suspend fun toggleTodoCompletion(todoId: Long, isCompleted: Boolean) {
        todoDao.updateTodoCompletion(todoId, isCompleted)
    }

    override fun getTodosBySnippetId(snippetId: Long): Flow<List<TodoItem>> {
        return todoDao.getTodosBySnippetId(snippetId)
    }

    override fun getAllTodos(): Flow<List<TodoItem>> {
        return todoDao.getAllTodos()
    }

    override fun getAllTodosWithSnippet(): Flow<List<TodoWithSnippet>> {
        return todoDao.getAllTodosWithSnippet()
    }
}
