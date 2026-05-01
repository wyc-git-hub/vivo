package com.example.vbrain.presentation.snippet_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.domain.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnippetDetailViewModel @Inject constructor(
    private val repository: KnowledgeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val snippetId: Long = checkNotNull(savedStateHandle["snippetId"])

    private val _snippet = MutableStateFlow<KnowledgeSnippet?>(null)
    val snippet: StateFlow<KnowledgeSnippet?> = _snippet.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSnippetById(snippetId).filterNotNull().collect {
                _snippet.value = it
            }
        }
    }

    fun updateSnippet(summary: String, tags: List<String>) {
        val current = _snippet.value ?: return
        viewModelScope.launch {
            repository.updateSnippet(current.copy(summary = summary, tags = tags))
        }
    }

    fun deleteSnippet() {
        val current = _snippet.value ?: return
        viewModelScope.launch {
            repository.deleteSnippet(current)
        }
    }
}

