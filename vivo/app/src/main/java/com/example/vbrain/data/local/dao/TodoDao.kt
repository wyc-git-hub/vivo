package com.example.vbrain.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Embedded
import androidx.room.Relation
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.data.local.entity.TodoItem
import kotlinx.coroutines.flow.Flow

data class TodoWithSnippet(
    @Embedded val todo: TodoItem,
    @Relation(
        parentColumn = "snippetId",
        entityColumn = "id"
    )
    val snippet: KnowledgeSnippet
)

@Dao
interface TodoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoItem>)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("UPDATE todos SET isCompleted = :isCompleted WHERE id = :todoId")
    suspend fun updateTodoCompletion(todoId: Long, isCompleted: Boolean)

    @Query("SELECT * FROM todos WHERE snippetId = :snippetId ORDER BY id ASC")
    fun getTodosBySnippetId(snippetId: Long): Flow<List<TodoItem>>

    @Transaction
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, id DESC")
    fun getAllTodosWithSnippet(): Flow<List<TodoWithSnippet>>

    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, id DESC")
    fun getAllTodos(): Flow<List<TodoItem>>
}

