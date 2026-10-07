package com.callguard.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM blocking_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM blocking_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<RuleEntity>

    @Insert
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)
}

@Dao
interface CallHistoryDao {
    @Query("SELECT * FROM call_history ORDER BY timestamp DESC LIMIT 100")
    fun observeAll(): Flow<List<CallHistoryEntity>>

    @Insert
    suspend fun insert(record: CallHistoryEntity)

    @Query("DELETE FROM call_history")
    suspend fun clear()
}
