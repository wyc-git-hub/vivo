package com.example.vbrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_snippets")
data class KnowledgeSnippet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalText: String,
    val summary: String,
    val tags: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String
)

