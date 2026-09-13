package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthcareRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HealthcareRecordEntity): Long

    @Update
    suspend fun updateRecord(record: HealthcareRecordEntity)

    @Delete
    suspend fun deleteRecord(record: HealthcareRecordEntity)

    @Query("SELECT * FROM healthcare_records WHERE userId = :userId ORDER BY date DESC")
    fun observeRecordsByUserId(userId: String): Flow<List<HealthcareRecordEntity>>

    @Query("SELECT * FROM healthcare_records WHERE userId = :userId AND petId = :petId ORDER BY date DESC")
    fun observeRecordsByPet(userId: String, petId: Long): Flow<List<HealthcareRecordEntity>>

    @Query("SELECT * FROM healthcare_records WHERE userId = :userId AND id = :recordId LIMIT 1")
    suspend fun getRecordById(userId: String, recordId: Long): HealthcareRecordEntity?

    @Query("SELECT * FROM healthcare_records WHERE userId = :userId AND petId = :petId AND type = :type ORDER BY date DESC")
    fun observeRecordsByType(userId: String, petId: Long, type: String): Flow<List<HealthcareRecordEntity>>

    @Query("SELECT * FROM healthcare_records WHERE userId = :userId AND (date >= :currentDate OR nextDueDate >= :currentDate) ORDER BY date ASC")
    fun observeUpcomingRecords(userId: String, currentDate: Long): Flow<List<HealthcareRecordEntity>>

    @Query("SELECT * FROM healthcare_records WHERE userId = :userId AND petId = :petId AND (date >= :currentDate OR nextDueDate >= :currentDate) ORDER BY date ASC")
    fun observeUpcomingRecordsByPet(userId: String, petId: Long, currentDate: Long): Flow<List<HealthcareRecordEntity>>
}
