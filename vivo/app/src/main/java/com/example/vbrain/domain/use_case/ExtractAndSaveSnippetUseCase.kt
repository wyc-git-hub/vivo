package com.example.vbrain.domain.use_case

import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.remote.LLMApiService
import com.example.vbrain.data.remote.LLMChatRequest
import com.example.vbrain.data.remote.LLMMessage
import com.example.vbrain.data.remote.LLMResult
import com.example.vbrain.data.remote.ResponseFormat
import com.example.vbrain.domain.repository.KnowledgeRepository
import com.google.gson.Gson
import javax.inject.Inject

class ExtractAndSaveSnippetUseCase @Inject constructor(
    private val repository: KnowledgeRepository,
    private val llmApiService: LLMApiService
) {
    private val gson = Gson()

    suspend operator fun invoke(originalText: String, source: String = "系统分享") {
        var summary = ""
        var tags = listOf("未分类")

        try {
            val systemPrompt = """
                你是一个端侧知识提取助手。请阅读用户提供的文本，提取不超过50字的精炼摘要 (summary) 和 1-3 个核心标签 (tags)。
                你必须严格返回纯 JSON 格式数据，不要有任何额外的 Markdown 标记或解释。JSON 格式为：{"summary": "...", "tags": ["...", "..."]}
            """.trimIndent()

            val request = LLMChatRequest(
                messages = listOf(
                    LLMMessage(role = "system", content = systemPrompt),
                    LLMMessage(role = "user", content = originalText)
                ),
                response_format = ResponseFormat(type = "json_object")
            )

            val response = llmApiService.getCompletions(request)
            val content = response.choices.firstOrNull()?.message?.content
            
            if (!content.isNullOrBlank()) {
                // 如果模型带有 ```json 包装，先做简单清理
                val cleanJson = content.replace("```json", "").replace("```", "").trim()
                val result = gson.fromJson(cleanJson, LLMResult::class.java)
                summary = result.summary
                tags = result.tags
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 解析或网络请求失败，使用默认值 ("未分类")
        }

        // 构建 KnowledgeSnippet 落库
        val snippet = KnowledgeSnippet(
            originalText = originalText,
            summary = summary,
            tags = tags,
            source = source,
            timestamp = System.currentTimeMillis()
        )
        repository.addSnippet(snippet)
    }
}
