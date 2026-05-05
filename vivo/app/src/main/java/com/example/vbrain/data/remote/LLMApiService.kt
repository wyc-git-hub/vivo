package com.example.vbrain.data.remote

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface LLMApiService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun getCompletions(
        @Body request: LLMChatRequest
    ): LLMChatResponse

    @Streaming
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun getStreamingCompletions(
        @Body request: LLMChatRequest
    ): ResponseBody
}
