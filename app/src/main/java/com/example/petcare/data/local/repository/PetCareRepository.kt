package com.example.petcare.data.local.repository

import android.content.Context
import com.example.petcare.data.local.PetCareDatabase
import com.example.petcare.data.local.entity.BudgetEntity
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.data.local.entity.DelegationEntity
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.NotificationSettingsEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.entity.TaskCompletionEntity
import com.example.petcare.data.local.relationship.PetWithAllRelations
import com.example.petcare.data.remote.RetrofitClient
import com.example.petcare.data.remote.model.ExternalPetRecord
import kotlinx.coroutines.flow.Flow

class PetCareRepository private constructor(
    private val database: PetCareDatabase
) {
    fun observePets(userId: String): Flow<List<PetEntity>> =
        database.petDao().observePetsByUserId(userId)

    suspend fun getPet(userId: String, petId: Long): PetEntity? =
        database.petDao().getPetById(userId, petId)

    fun observePetDetails(userId: String, petId: Long): Flow<PetWithAllRelations?> =
        database.petDao().observePetWithAllRelations(userId, petId)

    suspend fun insertPet(pet: PetEntity): Long = database.petDao().insertPet(pet)

    suspend fun updatePet(pet: PetEntity) = database.petDao().updatePet(pet)

    suspend fun deletePet(pet: PetEntity) = database.petDao().deletePet(pet)

    fun observeCareRoutines(userId: String): Flow<List<CareRoutineEntity>> =
        database.careRoutineDao().observeRoutinesByUserId(userId)

    fun observeCareRoutinesForPet(userId: String, petId: Long): Flow<List<CareRoutineEntity>> =
        database.careRoutineDao().observeRoutinesByPet(userId, petId)

    suspend fun insertCareRoutine(routine: CareRoutineEntity): Long =
        database.careRoutineDao().insertRoutine(routine)

    suspend fun updateCareRoutine(routine: CareRoutineEntity) =
        database.careRoutineDao().updateRoutine(routine)

    suspend fun deleteCareRoutine(routine: CareRoutineEntity) =
        database.careRoutineDao().deleteRoutine(routine)

    fun observeCareTasks(userId: String): Flow<List<CareTaskEntity>> =
        database.careTaskDao().observeTasksByUserId(userId)

    fun observeCareTasksForPet(userId: String, petId: Long): Flow<List<CareTaskEntity>> =
        database.careTaskDao().observeTasksByPet(userId, petId)

    fun observeCareTasksByRoutine(userId: String, routineId: Long): Flow<List<CareTaskEntity>> =
        database.careTaskDao().observeTasksByRoutine(userId, routineId)

    fun observePendingCareTasks(userId: String): Flow<List<CareTaskEntity>> =
        database.careTaskDao().observePendingTasks(userId)

    suspend fun getCareTaskById(userId: String, taskId: Long): CareTaskEntity? =
        database.careTaskDao().getTaskById(userId, taskId)

    suspend fun insertCareTask(task: CareTaskEntity): Long = database.careTaskDao().insertTask(task)

    suspend fun updateCareTask(task: CareTaskEntity) = database.careTaskDao().updateTask(task)

    suspend fun deleteCareTask(task: CareTaskEntity) = database.careTaskDao().deleteTask(task)

    suspend fun getTaskCountForRoutine(userId: String, routineId: Long): Int =
        database.careTaskDao().countTasksForRoutine(userId, routineId)

    fun observeCompletionsForDate(userId: String, date: Long): Flow<List<TaskCompletionEntity>> =
        database.taskCompletionDao().observeCompletionsForDate(userId, date)

    fun observeCompletionsInRange(userId: String, start: Long, end: Long): Flow<List<TaskCompletionEntity>> =
        database.taskCompletionDao().observeCompletionsInRange(userId, start, end)

    suspend fun markTaskComplete(userId: String, taskId: Long, date: Long) {
        val completion = TaskCompletionEntity(userId = userId, taskId = taskId, occurrenceDate = date)
        database.taskCompletionDao().insertCompletion(completion)
    }

    suspend fun markTaskIncomplete(userId: String, taskId: Long, date: Long) {
        database.taskCompletionDao().deleteCompletion(userId, taskId, date)
    }

    fun observeHealthcareRecords(userId: String): Flow<List<HealthcareRecordEntity>> =
        database.healthcareRecordDao().observeRecordsByUserId(userId)

    fun observeHealthcareRecordsForPet(userId: String, petId: Long): Flow<List<HealthcareRecordEntity>> =
        database.healthcareRecordDao().observeRecordsByPet(userId, petId)

    fun observeHealthcareRecordsByType(userId: String, petId: Long, type: String): Flow<List<HealthcareRecordEntity>> =
        database.healthcareRecordDao().observeRecordsByType(userId, petId, type)

    fun observeUpcomingHealthcare(userId: String, currentDate: Long): Flow<List<HealthcareRecordEntity>> =
        database.healthcareRecordDao().observeUpcomingRecords(userId, currentDate)

    fun observeUpcomingHealthcareByPet(userId: String, petId: Long, currentDate: Long): Flow<List<HealthcareRecordEntity>> =
        database.healthcareRecordDao().observeUpcomingRecordsByPet(userId, petId, currentDate)

    suspend fun getHealthcareRecordById(userId: String, recordId: Long): HealthcareRecordEntity? =
        database.healthcareRecordDao().getRecordById(userId, recordId)

    suspend fun insertHealthcareRecord(record: HealthcareRecordEntity): Long =
        database.healthcareRecordDao().insertRecord(record)

    suspend fun updateHealthcareRecord(record: HealthcareRecordEntity) =
        database.healthcareRecordDao().updateRecord(record)

    suspend fun deleteHealthcareRecord(record: HealthcareRecordEntity) =
        database.healthcareRecordDao().deleteRecord(record)

    suspend fun fetchExternalVeterinaryRecords(): List<ExternalPetRecord> =
        RetrofitClient.vetApiService.getPets()

    suspend fun healthcareRecordExists(userId: String, petId: Long, type: String, title: String, date: Long): Boolean =
        database.healthcareRecordDao().countMatchingRecords(userId, petId, type, title, date) > 0

    fun observeExpenses(userId: String): Flow<List<ExpenseEntity>> =
        database.expenseDao().observeExpensesByUserId(userId)

    fun observeExpensesForPet(userId: String, petId: Long): Flow<List<ExpenseEntity>> =
        database.expenseDao().observeExpensesByPet(userId, petId)

    fun observeExpensesInRange(userId: String, start: Long, end: Long): Flow<List<ExpenseEntity>> =
        database.expenseDao().observeExpensesInRange(userId, start, end)

    fun observeTotalSpending(userId: String): Flow<Double?> =
        database.expenseDao().observeTotalSpending(userId)

    fun observeSpendingInRange(userId: String, start: Long, end: Long): Flow<Double?> =
        database.expenseDao().observeSpendingInRange(userId, start, end)

    suspend fun getExpenseById(userId: String, expenseId: Long): ExpenseEntity? =
        database.expenseDao().getExpenseById(userId, expenseId)

    suspend fun insertExpense(expense: ExpenseEntity): Long = database.expenseDao().insertExpense(expense)

    suspend fun updateExpense(expense: ExpenseEntity) = database.expenseDao().updateExpense(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = database.expenseDao().deleteExpense(expense)

    fun observeBudget(userId: String): Flow<BudgetEntity?> =
        database.budgetDao().observeBudgetByUserId(userId)

    suspend fun getBudget(userId: String): BudgetEntity? =
        database.budgetDao().getBudgetByUserId(userId)

    suspend fun insertBudget(budget: BudgetEntity): Long = database.budgetDao().insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) = database.budgetDao().updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) = database.budgetDao().deleteBudget(budget)

    fun observeDelegations(userId: String): Flow<List<DelegationEntity>> =
        database.delegationDao().observeDelegationsByUserId(userId)

    fun observeDelegationsForPet(userId: String, petId: Long): Flow<List<DelegationEntity>> =
        database.delegationDao().observeDelegationsByPet(userId, petId)

    suspend fun insertDelegation(delegation: DelegationEntity): Long =
        database.delegationDao().insertDelegation(delegation)

    suspend fun deleteDelegation(delegation: DelegationEntity) =
        database.delegationDao().deleteDelegation(delegation)

    fun observeNotificationSettings(userId: String): Flow<NotificationSettingsEntity?> =
        database.notificationSettingsDao().observeSettings(userId)

    suspend fun getNotificationSettings(userId: String): NotificationSettingsEntity? =
        database.notificationSettingsDao().getSettings(userId)

    suspend fun insertNotificationSettings(settings: NotificationSettingsEntity): Long =
        database.notificationSettingsDao().insertSettings(settings)

    suspend fun updateNotificationSettings(settings: NotificationSettingsEntity) =
        database.notificationSettingsDao().updateSettings(settings)

    companion object {
        @Volatile
        private var INSTANCE: PetCareRepository? = null

        fun getInstance(context: Context): PetCareRepository {
            return INSTANCE ?: synchronized(this) {
                val repo = PetCareRepository(PetCareDatabase.getInstance(context))
                INSTANCE = repo
                repo
            }
        }
    }
}
