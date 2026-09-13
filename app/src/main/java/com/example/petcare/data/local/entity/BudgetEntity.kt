package com.example.petcare.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val amount: Double,
    val period: String = "MONTHLY",
    val startDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
