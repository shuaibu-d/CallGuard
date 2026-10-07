package com.callguard.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocking_rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pattern: String,
    val matchType: MatchType = MatchType.PREFIX,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MatchType {
    PREFIX, EXACT, CONTAINS
}
