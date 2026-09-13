package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "care_tasks",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CareRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["petId"]), Index(value = ["routineId"])]
)
data class CareTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val petId: Long,
    val routineId: Long? = null,
    val title: String,
    val description: String = "",
    val category: String = "OTHER",
    val dueAt: Long,
    val frequency: String = "ONCE",
    val requiredSupplies: String = "",
    val notes: String = "",
    val imageUri: String? = null,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
