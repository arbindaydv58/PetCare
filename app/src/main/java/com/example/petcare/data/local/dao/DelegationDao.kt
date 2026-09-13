package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.petcare.data.local.entity.DelegationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DelegationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelegation(delegation: DelegationEntity): Long

    @Delete
    suspend fun deleteDelegation(delegation: DelegationEntity)

    @Query("SELECT * FROM delegations WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeDelegationsByUserId(userId: String): Flow<List<DelegationEntity>>

    @Query("SELECT * FROM delegations WHERE userId = :userId AND petId = :petId ORDER BY createdAt DESC")
    fun observeDelegationsByPet(userId: String, petId: Long): Flow<List<DelegationEntity>>
}
