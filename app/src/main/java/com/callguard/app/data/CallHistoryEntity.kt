package com.callguard.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val ruleId: Long?,
    val timestamp: Long = System.currentTimeMillis()
)
