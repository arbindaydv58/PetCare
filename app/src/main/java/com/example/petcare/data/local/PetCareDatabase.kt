package com.example.petcare.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.petcare.data.local.dao.BudgetDao
import com.example.petcare.data.local.dao.CareRoutineDao
import com.example.petcare.data.local.dao.CareTaskDao
import com.example.petcare.data.local.dao.DelegationDao
import com.example.petcare.data.local.dao.ExpenseDao
import com.example.petcare.data.local.dao.HealthcareRecordDao
import com.example.petcare.data.local.dao.NotificationSettingsDao
import com.example.petcare.data.local.dao.PetDao
import com.example.petcare.data.local.dao.TaskCompletionDao
import com.example.petcare.data.local.entity.BudgetEntity
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.data.local.entity.DelegationEntity
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.NotificationSettingsEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.entity.TaskCompletionEntity

@Database(
    entities = [
        PetEntity::class,
        CareRoutineEntity::class,
        CareTaskEntity::class,
        HealthcareRecordEntity::class,
        ExpenseEntity::class,
        TaskCompletionEntity::class,
        BudgetEntity::class,
        DelegationEntity::class,
        NotificationSettingsEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class PetCareDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun careRoutineDao(): CareRoutineDao
    abstract fun careTaskDao(): CareTaskDao
    abstract fun healthcareRecordDao(): HealthcareRecordDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun delegationDao(): DelegationDao
    abstract fun notificationSettingsDao(): NotificationSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: PetCareDatabase? = null

        fun getInstance(context: Context): PetCareDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        PetCareDatabase::class.java,
                        "petcare_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    throw e
                }
            }
        }

        fun getTestInstance(context: Context): PetCareDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                PetCareDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
