package com.example.vbrain.presentation.snippet_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.remote.LLMApiService
import com.example.vbrain.data.remote.LLMChatRequest
import com.example.vbrain.data.remote.LLMMessage
import com.example.vbrain.data.remote.StreamingChunk
import com.example.vbrain.domain.repository.KnowledgeRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.vbrain.data.local.entity.TodoItem
import javax.inject.Inject

data class ChatMessage(val role: String, val content: String)

@HiltViewModel
class SnippetDetailViewModel @Inject constructor(
    private val repository: KnowledgeRepository,
    private val llmApiService: LLMApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val snippetId: Long = checkNotNull(savedStateHandle["snippetId"])

    private val _snippet = MutableStateFlow<KnowledgeSnippet?>(null)
    val snippet: StateFlow<KnowledgeSnippet?> = _snippet.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // --- Todo Methods ---
    val todos: StateFlow<List<TodoItem>> = snippet.flatMapLatest { snip ->
        if (snip != null && snip.id != 0L) {
            repository.getTodosBySnippetId(snip.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTodoCompletion(todoId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTodoCompletion(todoId, isCompleted)
        }
    }

    private val gson = Gson()

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
                originalText = originalText,
                formattedText = originalText // 如果用户修改了，也同步更新 formattedText
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

    fun sendMessage(userInput: String, currentSnippetContent: String) {
        if (userInput.isBlank()) return

        val trimmedContext = currentSnippetContent.take(3000) // Prevent token limit error
        val isFirstMessage = _chatMessages.value.isEmpty()

        val newUserMessage = ChatMessage(role = "user", content = userInput)
        
        // Append user message immediately
        _chatMessages.value = _chatMessages.value + newUserMessage

        viewModelScope.launch(Dispatchers.IO) {
            _isChatLoading.value = true

            // Prepare LLM messages
            val apiMessages = mutableListOf<LLMMessage>()
            
            if (isFirstMessage) {
                apiMessages.add(
                    LLMMessage(
                        role = "system",
                        content = "你是一个阅读助手。参考以下文章内容回答问题：[$trimmedContext]"
                    )
                )
            } else {
                // If it's not the first message, we can still provide context if needed, but here we assume the model remembers if we include history.
                apiMessages.add(
                    LLMMessage(
                        role = "system",
                        content = "你是一个阅读助手。参考以下文章内容回答问题：[$trimmedContext]"
                    )
                )
            }

            // Add history
            _chatMessages.value.forEach { msg ->
                apiMessages.add(LLMMessage(role = msg.role, content = msg.content))
            }

            try {
                val request = LLMChatRequest(
                    messages = apiMessages,
                    stream = true
                )

                val responseBody = llmApiService.getStreamingCompletions(request)
                
                // Add empty assistant message that we will stream into
                withContext(Dispatchers.Main) {
                    _chatMessages.value = _chatMessages.value + ChatMessage(role = "assistant", content = "")
                }

                val reader = responseBody.charStream().buffered()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("data: ") && line != "data: [DONE]") {
                        val jsonString = line.substring(6).trim()
                        if (jsonString.isNotEmpty()) {
                            try {
                                val chunk = gson.fromJson(jsonString, StreamingChunk::class.java)
                                val deltaContent = chunk.choices.firstOrNull()?.delta?.content ?: ""
                                
                                if (deltaContent.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        val currentList = _chatMessages.value.toMutableList()
                                        val lastMessage = currentList.removeLast()
                                        currentList.add(lastMessage.copy(content = lastMessage.content + deltaContent))
                                        _chatMessages.value = currentList
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _chatMessages.value = _chatMessages.value + ChatMessage(role = "assistant", content = "请求失败，请检查网络或配置")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isChatLoading.value = false
                }
            }
        }
    }
}