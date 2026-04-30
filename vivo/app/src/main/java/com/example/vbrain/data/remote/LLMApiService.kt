package com.example.vbrain.data.remote

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface LLMApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun getCompletions(
        @Body request: LLMChatRequest
    ): LLMChatResponse
}

