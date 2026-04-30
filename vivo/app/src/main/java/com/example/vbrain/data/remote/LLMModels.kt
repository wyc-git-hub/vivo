package com.example.vbrain.data.remote

// 模型：请求体
data class LLMChatRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<LLMMessage>,
    val temperature: Double = 0.3,
    val response_format: ResponseFormat? = null // 用于强化要求返回 JSON (如果是最新版API)
)

data class LLMMessage(
    val role: String,
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

// 模型：响应体
data class LLMChatResponse(
    val id: String,
    val choices: List<LLMChoice>
)

data class LLMChoice(
    val message: LLMMessage,
    val finish_reason: String?
)

// 模型：预期的 JSON 结果
data class LLMResult(
    val summary: String,
    val tags: List<String>
)

