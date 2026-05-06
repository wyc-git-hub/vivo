package com.example.vbrain.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeSnippet::class,
            parentColumns = ["id"],
            childColumns = ["snippetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("snippetId")]
)
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val snippetId: Long,
    val taskContent: String,
    val isCompleted: Boolean = false
)

