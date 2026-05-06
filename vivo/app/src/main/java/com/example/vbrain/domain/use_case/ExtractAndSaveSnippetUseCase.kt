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
                # Role: 资深知识管理专家与文本清洗大师
                你现在的任务是对用户通过 OCR 识别、语音录入或网页抓取产生的“碎片化脏数据”进行深度清洗和结构化排版。

                # Workflow:
                1. 剔除噪音：自动识别并删除网页广告、无意义的菜单文本、乱码或 OCR 识别错误产生的冗余字符。
                2. 纠正错漏：修正语音转写带来的同音字错误，补全缺失的标点符号，将口语化的表达转化为书面语（但不要改变原意）。
                3. 结构化重组：为文本提取一个合适的大标题 (#)，并将核心观点提炼为小标题 (##) 和无序列表 (-)。

                # Strict Constraints (绝对遵守):
                - 只输出标准 Markdown 格式文本，**绝不能包含任何开场白或解释性对话**。
                - 如果原始内容太短（少于 50 字），仅做错别字修正，不要强行生成复杂的标题结构。
                - 必须保留原始内容中的关键数据（时间、地点、数字、专有名词），绝不能因精简而丢失事实。
            """.trimIndent()
            
            val userMessage = """
                【输入来源】: $source
                【原始文本】: 
                $originalText
            """.trimIndent()

            val request = LLMChatRequest(
                messages = listOf(
                    LLMMessage(role = "system", content = systemPrompt),
                    LLMMessage(role = "user", content = userMessage)
                ),
                response_format = null // 取消 JSON 限定，直接返回文本
            )

            val response = llmApiService.getCompletions(request)
            val content = response.choices.firstOrNull()?.message?.content ?: originalText
            
            formattedText = content.trim()
            
            // 第二步：提取标签和标题
            val metricPrompt = """
                # Role: 知识图谱架构师 (Knowledge Graph Architect)
                你现在的任务是对输入的文本进行自然语言处理（NLP），提取其中的核心实体、自动分配标签，并输出严格的 JSON 数据。

                # Workflow:
                1. Tags (标签): 提取 3-5 个最能代表文章垂直领域的标签（如 "人工智能", "个人成长", "理财"）。不要带 # 号。
                2. Entities (核心实体): 提取文本中出现的关键具体名词，包括人物、公司机构、专业术语、工具名称或特定地点。
                3. Summary (摘要): 生成一句不超过 30 个字的极简核心摘要。

                # Strict Constraints (绝对遵守):
                - 必须且只能输出合法的 JSON 格式字符串，不需要 ```json 的代码块标记，绝不能有任何其他文本。
                - 确保 JSON 的 Key 完全匹配下方的示例。

                # Output JSON Template:
                {
                  "tags": ["标签1", "标签2"],
                  "entities": ["实体1", "实体2", "实体3"],
                  "summary": "一句话核心摘要"
                }
            """.trimIndent()
            
            val metricRequest = LLMChatRequest(
                messages = listOf(
                    LLMMessage(role = "system", content = metricPrompt),
                    LLMMessage(role = "user", content = "【分析文本】:\n$formattedText")
                ),
                response_format = ResponseFormat(type = "json_object")
            )
            
            val metricResponse = llmApiService.getCompletions(metricRequest)
            val metricContent = metricResponse.choices.firstOrNull()?.message?.content
            
            if (!metricContent.isNullOrBlank()) {
                try {
                    val cleanJson = cleanMarkdownJson(metricContent)
                    val result = gson.fromJson(cleanJson, LLMResult::class.java)
                    summary = result.summary ?: result.title ?: ""
                    tags = result.tags ?: listOf("未分类")
                } catch (e: Exception) {
                    Log.e("ExtractAndSaveSnippet", "Metric JSON parsing failed: ${e.message}\nRaw Content: $metricContent", e)
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
