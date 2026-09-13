package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.petcare.data.local.entity.TaskCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: TaskCompletionEntity): Long

    @Query("DELETE FROM task_completions WHERE userId = :userId AND taskId = :taskId AND occurrenceDate = :date")
    suspend fun deleteCompletion(userId: String, taskId: Long, date: Long)

    @Query("SELECT * FROM task_completions WHERE userId = :userId AND occurrenceDate = :date")
    fun observeCompletionsForDate(userId: String, date: Long): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE userId = :userId AND occurrenceDate BETWEEN :startDate AND :endDate")
    fun observeCompletionsInRange(userId: String, startDate: Long, endDate: Long): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE userId = :userId AND taskId = :taskId AND occurrenceDate = :date LIMIT 1")
    suspend fun getCompletion(userId: String, taskId: Long, date: Long): TaskCompletionEntity?
}
