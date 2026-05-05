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
        if (snippetId != -1L) {
            // 模式 1：查看或编辑现有的知识碎片
            viewModelScope.launch {
                repository.getSnippetById(snippetId).collect {
                    _snippet.value = it
                }
            }
        } else {
            // 模式 2：初始化一个空的碎片，用于手动录入
            _snippet.value = KnowledgeSnippet(
                originalText = "",
                summary = "",
                tags = emptyList(),
                source = "手动录入"
            )
        }
    }

    // 统一保存逻辑（无论是新增还是修改，都调用这个方法）
    fun saveSnippet(summary: String, tags: List<String>, originalText: String) {
        viewModelScope.launch {
            val current = _snippet.value ?: return@launch
            val updated = current.copy(
                summary = summary,
                tags = tags,
                originalText = originalText
            )

            if (snippetId == -1L) {
                repository.addSnippet(updated)
            } else {
                repository.updateSnippet(updated)
            }
        }
    }

    // 删除当前知识碎片
    fun deleteSnippet() {
        val current = _snippet.value ?: return
        viewModelScope.launch {
            repository.deleteSnippet(current)
        }
    }
}