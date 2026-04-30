package com.example.vbrain.presentation.snippet_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.remote.LLMApiService
import com.example.vbrain.data.remote.LLMChatRequest
import com.example.vbrain.data.remote.LLMMessage
import com.example.vbrain.data.remote.LLMResult
import com.example.vbrain.data.remote.ResponseFormat
import com.example.vbrain.domain.repository.KnowledgeRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnippetListViewModel @Inject constructor(
    private val repository: KnowledgeRepository,
    private val llmApiService: LLMApiService
) : ViewModel() {

    private val gson = Gson()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    // 提取所有不重复的标签
    val availableTags: StateFlow<List<String>> = repository.getAllSnippets()
        .map { snippets ->
            snippets.flatMap { it.tags }.distinct().sorted()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val snippets: StateFlow<List<KnowledgeSnippet>> = combine(
        repository.getAllSnippets(),
        _searchQuery,
        _selectedTag
    ) { snippets, query, tag ->
        var filteredList = snippets

        if (tag != null) {
            filteredList = filteredList.filter { it.tags.contains(tag) }
        }

        if (query.isNotBlank()) {
            filteredList = filteredList.filter {
                it.originalText.contains(query, ignoreCase = true) ||
                it.summary.contains(query, ignoreCase = true) ||
                it.tags.any { t -> t.contains(query, ignoreCase = true) }
            }
        }
        filteredList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTagSelect(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun insertMockSnippet(snippet: KnowledgeSnippet) {
        viewModelScope.launch { repository.addSnippet(snippet) }
    }

    fun deleteSnippet(snippet: KnowledgeSnippet) {
        viewModelScope.launch {
            repository.deleteSnippet(snippet)
        }
    }

    fun processUnsummarizedSnippets() {
        viewModelScope.launch {
            try {
                val pendingSnippets = repository.getUnsummarizedSnippets()
                for (snippet in pendingSnippets) {
                    processSnippetWithRetry(snippet)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun processSnippetWithRetry(snippet: KnowledgeSnippet, maxRetries: Int = 3) {
        var currentAttempt = 0
        while (currentAttempt < maxRetries) {
            try {
                val systemPrompt = "你是一个专业的知识提纯助手。请对用户输入的文本进行去水提纯。务必返回严格的JSON格式，包含字段：\"summary\"(不超过50字的精炼摘要) 和 \"tags\"(1-3个核心关键词的字符串数组)。不要输出任何其他内容。"
                
                val request = LLMChatRequest(
                    messages = listOf(
                        LLMMessage(role = "system", content = systemPrompt),
                        LLMMessage(role = "user", content = snippet.originalText)
                    ),
                    response_format = ResponseFormat(type = "json_object")
                )

                val response = llmApiService.getCompletions(request)
                
                val content = response.choices.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) {
                    val result = gson.fromJson(content, LLMResult::class.java)
                    
                    val updatedSnippet = snippet.copy(
                        summary = result.summary,
                        tags = result.tags
                    )
                    
                    repository.updateSnippet(updatedSnippet)
                    break // 成功则退出重试循环
                }
            } catch (e: Exception) {
                currentAttempt++
                e.printStackTrace()
                if (currentAttempt >= maxRetries) {
                    // 处理最终失败情况，如记录日志，此处简单跳过
                } else {
                    delay(1000L * currentAttempt) // 简单的指数退避
                }
            }
        }
    }
}
