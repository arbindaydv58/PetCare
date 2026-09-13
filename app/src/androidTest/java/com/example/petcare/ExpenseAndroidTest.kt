package com.example.petcare

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.petcare.data.local.PetCareDatabase
import com.example.petcare.data.local.entity.BudgetEntity
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.data.local.repository.PetCareRepository
import com.example.petcare.ui.viewmodel.ExpenseFormState
import com.example.petcare.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseAndroidTest {
    private lateinit var context: Context
    private lateinit var repository: PetCareRepository
    private lateinit var database: PetCareDatabase
    private lateinit var viewModel: ExpenseViewModel

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        repository = PetCareRepository.getInstance(context)
        database = PetCareDatabase.getInstance(context)
        database.clearAllTables()
        viewModel = ExpenseViewModel(context.applicationContext as Application)
        viewModel.setCurrentUserIdForTesting(TEST_USER_ID)
    }

    @After
    fun tearDown() = runBlocking {
        database.clearAllTables()
    }

    @Test
    fun repository_InsertAndObserveExpenses() = runBlocking {
        val expense = ExpenseEntity(
            userId = TEST_USER_ID,
            title = "Premium Dog Food",
            amount = 2500.0,
            category = "Food",
            date = System.currentTimeMillis()
        )

        repository.insertExpense(expense)
        val expenses = repository.observeExpenses(TEST_USER_ID).first()

        assertEquals(1, expenses.size)
        assertEquals("Premium Dog Food", expenses.first().title)
        assertEquals(2500.0, expenses.first().amount, 0.0)
    }

    @Test
    fun repository_InsertAndObserveBudget() = runBlocking {
        val budget = BudgetEntity(
            userId = TEST_USER_ID,
            amount = 10000.0
        )

        repository.insertBudget(budget)
        val observed = repository.observeBudget(TEST_USER_ID).first()

        assertNotNull(observed)
        assertEquals(10000.0, observed!!.amount, 0.0)
    }

    @Test
    fun viewModel_SavesExpenseAndUpdatesSpending() = runTest {
        viewModel.updateForm(ExpenseFormState(
            title = "Leash",
            amount = "1200",
            category = "Accessories",
            date = System.currentTimeMillis()
        ))

        viewModel.saveExpense()
        advanceUntilIdle()

        assertEquals("Expense added successfully", viewModel.uiState.value.successMessage)
        val total = repository.observeTotalSpending(TEST_USER_ID).first()
        assertEquals(1200.0, total ?: 0.0, 0.0)
        assertFalse(viewModel.expenses.value.isEmpty())
        assertEquals("Leash", viewModel.expenses.value.first().title)
    }

    @Test
    fun viewModel_RejectsInvalidAmount() = runTest {
        viewModel.updateForm(ExpenseFormState(title = "Leash", amount = "not-a-number"))
        viewModel.saveExpense()
        advanceUntilIdle()

        assertEquals("Valid amount is required", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.expenses.value.isEmpty())
    }

    companion object {
        private const val TEST_USER_ID = "expense-test-user"
    }
}
