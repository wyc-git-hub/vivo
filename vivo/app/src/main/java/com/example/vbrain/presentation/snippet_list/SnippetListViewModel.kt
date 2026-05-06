package com.example.vbrain.presentation.snippet_list
import com.example.vbrain.data.remote.ResponseFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.remote.LLMApiService
import com.example.vbrain.data.remote.LLMChatRequest
import com.example.vbrain.data.remote.LLMMessage
import com.example.vbrain.data.remote.LLMResult
import com.example.vbrain.domain.repository.KnowledgeRepository
import com.example.vbrain.domain.use_case.ExtractAndSaveSnippetUseCase
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.Uri
import android.widget.Toast
import javax.inject.Inject

@HiltViewModel
class SnippetListViewModel @Inject constructor(
    private val repository: KnowledgeRepository,
    private val llmApiService: LLMApiService,
    private val extractAndSaveSnippetUseCase: ExtractAndSaveSnippetUseCase
) : ViewModel() {

    private val gson = Gson()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // 🌟 新增：多选模式状态
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    // 🌟 新增：记录选中的卡片 ID 集合
    private val _selectedSnippetIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSnippetIds = _selectedSnippetIds.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput = _chatInput.asStateFlow()

    private val _chatReply = MutableStateFlow("")
    val chatReply = _chatReply.asStateFlow()

    private val _isChatSheetVisible = MutableStateFlow(false)
    val isChatSheetVisible = _isChatSheetVisible.asStateFlow()

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
            filteredList = filteredList.filter { snippet ->
                snippet.originalText.contains(query, ignoreCase = true) ||
                        snippet.summary.contains(query, ignoreCase = true) ||
                        snippet.tags.any { t -> t.contains(query, ignoreCase = true) }
            }
        }
        filteredList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- 🌟 新增：多选与批量删除核心逻辑 ---
    fun enterSelectionMode(initialId: Long) {
        _isSelectionMode.value = true
        _selectedSnippetIds.value = setOf(initialId)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedSnippetIds.value = emptySet()
    }

    fun toggleSnippetSelection(id: Long) {
        val current = _selectedSnippetIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedSnippetIds.value = current
        if (current.isEmpty()) exitSelectionMode()
    }

    fun selectAllSnippets() {
        _selectedSnippetIds.value = snippets.value.map { it.id }.toSet()
    }

    fun deleteSelectedSnippets() {
        viewModelScope.launch {
            val idsToDelete = _selectedSnippetIds.value
            val toDelete = snippets.value.filter { idsToDelete.contains(it.id) }
            toDelete.forEach { repository.deleteSnippet(it) }
            exitSelectionMode()
        }
    }

    fun clearCurrentList() {
        viewModelScope.launch {
            snippets.value.forEach { repository.deleteSnippet(it) }
            exitSelectionMode()
        }
    }
    // -------------------------------------

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onChatInputChange(input: String) {
        _chatInput.value = input
    }

    fun toggleChatSheet(visible: Boolean) {
        _isChatSheetVisible.value = visible
        if (!visible) {
            _chatInput.value = ""
            _chatReply.value = ""
        }
    }

    // 🌟 新增：生成每日复习卡片 (Spaced Repetition)
    fun generateDailyReviewCard() {
        viewModelScope.launch {
            _isChatSheetVisible.value = true
            _chatReply.value = "正在洗牌你的知识库，抽取今日复习卡片..."
            _isLoading.value = true
            
            try {
                val allSnippets = snippets.value
                if (allSnippets.isEmpty()) {
                    _chatReply.value = "你的知识库当前为空，快去收集第一条碎片吧！"
                    return@launch
                }
                
                // 随机抽取一条历史知识（真实场景可根据算法计算艾宾浩斯记忆曲线）
                val targetSnippet = allSnippets.random()
                val textToReview = targetSnippet.formattedText.takeIf { it.isNotBlank() } ?: targetSnippet.originalText
                
                val prompt = """
                    # Role: 认知心理学导师与记忆力教练
                    你现在的任务是根据用户过去保存的一段知识片段，生成一张用于“间隔重复（Spaced Repetition）”的每日回顾复习卡片。

                    # Workflow:
                    1. 分析提供的【历史知识片段】，提取其最核心的 1 个关键概念。
                    2. 生成一段极简的“知识唤醒”总结（不超过 3 句话）。
                    3. 设计 1 个极具启发性的“测试问题（Quiz）”或“行动思考题”，引导用户回忆具体细节或思考如何将该知识应用到现实生活中。

                    # Output Format (严格遵循此格式输出):
                    > 💡 **核心重温**：[在此填入你的 3 句话极简总结]

                    > ❓ **灵魂拷问**：[在此填入你的启发式问题]

                    # Input Data:
                    【历史知识片段】:
                    $textToReview
                """.trimIndent()

                val request = LLMChatRequest(
                    messages = listOf(
                        LLMMessage(role = "system", content = "你是一个优秀的认知心理学导师。"),
                        LLMMessage(role = "user", content = prompt)
                    )
                )

                val response = llmApiService.getCompletions(request)
                val content = response.choices.firstOrNull()?.message?.content

                if (!content.isNullOrBlank()) {
                    _chatReply.value = content.trim()
                } else {
                    _chatReply.value = "生成复习卡片失败，网络返回为空。"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _chatReply.value = "生成出错：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun askQuestion() {
        val question = _chatInput.value.trim()
        if (question.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _chatReply.value = "V-Brain 正在检索记忆并思考..."
            try {
                val queryToken = question.split(" ").firstOrNull() ?: question
                val topSnippets = repository.searchSnippets(queryToken).first().take(5)

                val contextText = if (topSnippets.isEmpty()) {
                    "无相关内容"
                } else {
                    topSnippets.joinToString("\n- ") { snippet ->
                        snippet.summary.ifEmpty { snippet.originalText }.take(100)
                    }
                }

                val prompt = """
                    基于以下用户的个人知识库片段：
                    - $contextText
                    
                    请回答用户的问题：$question
                    如果上下文中没有答案，请明确回答‘你的碎片库中暂无相关记录’。
                """.trimIndent()

                val request = LLMChatRequest(
                    messages = listOf(
                        LLMMessage(role = "system", content = "你是 V-Brain，一个智能的端侧知识提取助手。"),
                        LLMMessage(role = "user", content = prompt)
                    )
                )

                val response = llmApiService.getCompletions(request)

                val content = response.choices.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) {
                    try {
                        val result = gson.fromJson(content, LLMResult::class.java)
                        _chatReply.value = result.summary ?: result.title ?: "无法提取摘要"
                    } catch(e: Exception) {
                        _chatReply.value = content.replace("```json", "").replace("```", "")
                    }
                } else {
                    _chatReply.value = "未获取到有效回答"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _chatReply.value = "思考失败，请检查网络：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onTagSelect(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
        exitSelectionMode() // 🌟 切换标签时退出多选模式
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
                _isLoading.value = true
                val pendingSnippets = repository.getUnsummarizedSnippets()
                for (snippet in pendingSnippets) {
                    processSnippetWithRetry(snippet)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importFiles(uris: List<Uri>, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            var successCount = 0
            try {
                for (uri in uris) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val text = stream.bufferedReader().readText()
                            if (text.isNotBlank()) {
                                extractAndSaveSnippetUseCase(originalText = text, source = "本地文件导入")
                                successCount++
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } finally {
                _isLoading.value = false
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "批量导入完成，成功处理 ${successCount} 个文件", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun processSnippetWithRetry(snippet: KnowledgeSnippet, maxRetries: Int = 3) {
        var currentAttempt = 0
        while (currentAttempt < maxRetries) {
            try {
                val systemPrompt = "你是一个端侧多模态知识提取助手。请对输入文本进行处理：1. 提取不超过50字的精炼摘要 (title) 和 1-3个核心关键词 (tags)。" +
                        "2. 剔除噪音并输出排版优美的 Markdown 文本到 content。务必返回纯JSON格式数据：{\"title\": \"...\", \"tags\": [\"...\"], \"content\": \"...\"}" +
                        "请注意是精炼摘要 (title)不超过50字，正文内容要详细，且剔除无关内容，不要加入与输入文本无关的内容，正文对输入文本进行详细处理，如果有评论内容，请突出重点，列举关键评论"

                val request = LLMChatRequest(
                    messages = listOf(
                        LLMMessage(role = "system", content = systemPrompt),
                        LLMMessage(role = "user", content = snippet.originalText)
                    ),
//                    response_format = ResponseFormat(type = "json_object")
                )

                val response = llmApiService.getCompletions(request)

                val content = response.choices.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) {
                    val cleanJson = content.replace("```json", "").replace("```", "").trim()
                    val result = gson.fromJson(cleanJson, LLMResult::class.java)

                    val updatedSnippet = snippet.copy(
                        summary = result.summary ?: result.title ?: snippet.summary,
                        tags = result.tags ?: snippet.tags,
                        formattedText = result.content ?: snippet.formattedText
                    )

                    repository.updateSnippet(updatedSnippet)
                    break
                }
            } catch (e: Exception) {
                currentAttempt++
                e.printStackTrace()
                if (currentAttempt < maxRetries) {
                    delay(1000L * currentAttempt)
                }
            }
        }
    }
}