package com.example.vbrain.data.remote

// 模型：请求体
data class LLMChatRequest(
    // 【修改点】：将 "gpt-3.5-turbo" 替换为通义千问的 "qwen-turbo"
    val model: String = "qwen-turbo",
    val messages: List<LLMMessage>,
    val temperature: Double = 0.3,
    val response_format: ResponseFormat? = null
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