package com.example.vbrain.domain.use_case

import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.remote.LLMApiService
import com.example.vbrain.data.remote.LLMChatRequest
import com.example.vbrain.data.remote.LLMMessage
import com.example.vbrain.data.remote.LLMResult
import com.example.vbrain.data.remote.ResponseFormat
import com.example.vbrain.domain.repository.KnowledgeRepository
import com.google.gson.Gson
import android.util.Log
import javax.inject.Inject

class ExtractAndSaveSnippetUseCase @Inject constructor(
    private val repository: KnowledgeRepository,
    private val llmApiService: LLMApiService
) {
    private val gson = Gson()

    private fun cleanMarkdownJson(rawContent: String): String {
        return try {
            var cleaned = rawContent.trim()
            // 匹配开头的 ``` 或 ```json
            cleaned = cleaned.replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
            // 匹配结尾的 ```
            cleaned = cleaned.replace(Regex("\\s*```$"), "")
            cleaned.trim()
        } catch (e: Exception) {
            Log.e("ExtractAndSaveSnippet", "Error cleaning JSON", e)
            rawContent
        }
    }

    suspend operator fun invoke(
        originalText: String, 
        source: String = "系统分享",
        imagePaths: List<String> = emptyList(),
        sourceUrl: String? = null
    ) {
        var summary = ""
        var tags = listOf("未分类")
        var formattedText = ""

        try {
            val systemPrompt = """
                你是一个端侧多模态知识提取助手。请阅读用户提供的文本（可能是含有噪音的OCR识别文本或凌乱的内容），进行以下处理：
                1. 提取不超过50字的精炼摘要 (title) 和 1-3 个核心标签 (tags)。
                2. 剔除无用的UI噪音和无关符号，输出结构化、排版优美的 Markdown 文本到 content 字段。
                你必须严格返回纯 JSON 格式数据，不要有任何额外的 Markdown 标记或解释。JSON 格式为：{"title": "...", "tags": ["...", "..."], "content": "..."}
                请注意是精炼摘要 (title)不超过50字，正文内容要详细，且剔除无关内容，不要加入与输入文本无关的内容，正文对输入文本进行详细处理，如果有评论内容，请突出重点，列举关键评论。
                结构要清晰，要类似笔记的形式，使用 Markdown 语法进行排版，标题使用一级标题，重要内容加粗，列表清晰分明。
                先给出一个总结的笔记，随后给出一个非常详细的笔记，最后给出一个总结性的标签列表。请严格按照这个顺序输出，并且保证输出的 JSON 格式正确。
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
                try {
                    val cleanJson = cleanMarkdownJson(content)
                    val result = gson.fromJson(cleanJson, LLMResult::class.java)
                    summary = result.title ?: ""
                    tags = result.tags ?: listOf("未分类")
                    formattedText = result.content ?: ""
                } catch (e: Exception) {
                    Log.e("ExtractAndSaveSnippet", "JSON parsing failed: ${e.message}\nRaw Content: $content", e)
                }
            }
        } catch (e: Exception) {
            Log.e("ExtractAndSaveSnippet", "API request failed", e)
            // 解析或网络请求失败，使用默认值 ("未分类")
        }

        // 构建 KnowledgeSnippet 落库
        val snippet = KnowledgeSnippet(
            originalText = originalText,
            summary = summary,
            tags = tags,
            source = source,
            timestamp = System.currentTimeMillis(),
            formattedText = formattedText,
            imagePaths = imagePaths,
            sourceUrl = sourceUrl
        )
        repository.addSnippet(snippet)
    }
}
