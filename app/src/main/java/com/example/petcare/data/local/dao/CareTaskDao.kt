package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petcare.data.local.entity.CareTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CareTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CareTaskEntity): Long

    @Update
    suspend fun updateTask(task: CareTaskEntity)

    @Delete
    suspend fun deleteTask(task: CareTaskEntity)

    @Query("SELECT * FROM care_tasks WHERE userId = :userId ORDER BY dueAt ASC")
    fun observeTasksByUserId(userId: String): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_tasks WHERE userId = :userId AND petId = :petId ORDER BY dueAt ASC")
    fun observeTasksByPet(userId: String, petId: Long): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_tasks WHERE userId = :userId AND isCompleted = 0 ORDER BY dueAt ASC")
    fun observePendingTasks(userId: String): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_tasks WHERE userId = :userId AND id = :taskId LIMIT 1")
    suspend fun getTaskById(userId: String, taskId: Long): CareTaskEntity?

    @Query("SELECT COUNT(*) FROM care_tasks WHERE userId = :userId AND routineId = :routineId")
    suspend fun countTasksForRoutine(userId: String, routineId: Long): Int

    @Query("SELECT * FROM care_tasks WHERE userId = :userId AND routineId = :routineId ORDER BY dueAt ASC")
    fun observeTasksByRoutine(userId: String, routineId: Long): Flow<List<CareTaskEntity>>
}
