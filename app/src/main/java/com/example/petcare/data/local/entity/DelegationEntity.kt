package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "delegations",
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
data class DelegationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val petId: Long,
    val recipientName: String?,
    val recipientPhone: String,
    val taskTitles: String,
    val dateMillis: Long,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)
