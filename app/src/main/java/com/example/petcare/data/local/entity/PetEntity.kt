package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pets",
    indices = [Index(value = ["userId"]), Index(value = ["userId", "name"])]
)
data class PetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val species: String,
    val breed: String = "",
    val dateOfBirth: Long? = null,
    val weight: Double = 0.0,
    val gender: String = "",
    val dietaryPreferences: String = "",
    val allergies: String = "",
    val vaccinationHistory: String = "",
    val favouriteToys: String = "",
    val medicalNotes: String = "",
    val generalNotes: String = "",
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
