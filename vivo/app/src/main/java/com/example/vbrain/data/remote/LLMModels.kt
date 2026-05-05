package com.example.vbrain.data.remote

// 模型：请求体
data class LLMChatRequest(
    // 【修改点】：将 "gpt-3.5-turbo" 替换为通义千问的 "qwen-turbo"
    val model: String = "qwen-turbo",
    val messages: List<LLMMessage>,
    val temperature: Double = 0.3,
    val response_format: ResponseFormat? = null,
    val stream: Boolean = false
)

data class LLMMessage(
    val role: String,
    val content: String
)

//data class ResponseFormat(
//    val type: String = "json_object"
//)

// 模型：响应体
data class LLMChatResponse(
    val id: String,
    val choices: List<LLMChoice>
)

data class LLMChoice(
    val message: LLMMessage,
    val finish_reason: String?
)

data class StreamingDelta(
    val content: String?
)

data class StreamingChoice(
    val delta: StreamingDelta
)

data class StreamingChunk(
    val choices: List<StreamingChoice>
)

// 模型：预期的 JSON 结果
data class LLMResult(
    val title: String,
    val content: String,
    val tags: List<String>
)

data class ResponseFormat(
    val type: String
)