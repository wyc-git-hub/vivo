package com.example.vbrain.presentation.todo_center

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vbrain.data.local.dao.TodoWithSnippet
import com.example.vbrain.domain.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoCenterViewModel @Inject constructor(
    private val repository: KnowledgeRepository
) : ViewModel() {

    val allTodos: StateFlow<List<TodoWithSnippet>> = repository.getAllTodosWithSnippet()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uncompletedTodos: StateFlow<List<TodoWithSnippet>> = allTodos
        .map { todos -> todos.filter { !it.todo.isCompleted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedTodos: StateFlow<List<TodoWithSnippet>> = allTodos
        .map { todos -> todos.filter { it.todo.isCompleted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleTodoCompletion(todoId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTodoCompletion(todoId, isCompleted)
        }
    }
}
