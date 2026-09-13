package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.relationship.PetWithAllRelations
import com.example.petcare.data.local.relationship.PetWithCareRoutines
import com.example.petcare.data.local.relationship.PetWithCareTasks
import com.example.petcare.data.local.relationship.PetWithExpenses
import com.example.petcare.data.local.relationship.PetWithHealthcareRecords
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity): Long

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Delete
    suspend fun deletePet(pet: PetEntity)

    @Query("SELECT * FROM pets WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observePetsByUserId(userId: String): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId LIMIT 1")
    suspend fun getPetById(userId: String, petId: Long): PetEntity?

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId LIMIT 1")
    fun observePetWithAllRelations(userId: String, petId: Long): Flow<PetWithAllRelations?>

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId LIMIT 1")
    fun observePetWithCareRoutines(userId: String, petId: Long): Flow<PetWithCareRoutines?>

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId LIMIT 1")
    fun observePetWithCareTasks(userId: String, petId: Long): Flow<PetWithCareTasks?>

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId LIMIT 1")
    fun observePetWithHealthcareRecords(userId: String, petId: Long): Flow<PetWithHealthcareRecords?>

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId LIMIT 1")
    fun observePetWithExpenses(userId: String, petId: Long): Flow<PetWithExpenses?>
}
