package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "healthcare_records",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["petId"])]
)
data class HealthcareRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val petId: Long,
    val type: String,
    val title: String,
    val description: String? = null,
    val date: Long,
    val time: Long? = null,
    val veterinarianName: String? = null,
    val clinicName: String? = null,
    val medicationName: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val nextDueDate: Long? = null,
    val reminderEnabled: Boolean = false,
    val notes: String? = null,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
