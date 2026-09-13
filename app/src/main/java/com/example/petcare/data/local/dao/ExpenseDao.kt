package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petcare.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    fun observeExpensesByUserId(userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND petId = :petId ORDER BY date DESC")
    fun observeExpensesByPet(userId: String, petId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND id = :expenseId LIMIT 1")
    suspend fun getExpenseById(userId: String, expenseId: Long): ExpenseEntity?

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId")
    fun observeTotalSpending(userId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    fun observeSpendingInRange(userId: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun observeExpensesInRange(userId: String, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>
}
