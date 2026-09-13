package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petcare.data.local.entity.CareRoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CareRoutineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: CareRoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: CareRoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: CareRoutineEntity)

    @Query("SELECT * FROM care_routines WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeRoutinesByUserId(userId: String): Flow<List<CareRoutineEntity>>

    @Query("SELECT * FROM care_routines WHERE userId = :userId AND petId = :petId ORDER BY updatedAt DESC")
    fun observeRoutinesByPet(userId: String, petId: Long): Flow<List<CareRoutineEntity>>

    @Query("SELECT * FROM care_routines WHERE userId = :userId AND id = :routineId LIMIT 1")
    suspend fun getRoutineById(userId: String, routineId: Long): CareRoutineEntity?
}
