package com.personal.ai.shivai.core.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shiv_memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val content: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shiv_chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
