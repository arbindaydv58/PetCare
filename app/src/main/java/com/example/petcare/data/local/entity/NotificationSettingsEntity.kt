package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_settings",
    indices = [Index(value = ["userId"], unique = true)]
)
data class NotificationSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val remindersEnabled: Boolean = true,
    val taskRemindersEnabled: Boolean = true,
    val healthcareRemindersEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
