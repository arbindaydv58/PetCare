package com.example.petcare

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.petcare.data.local.PetCareDatabase
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.repository.PetCareRepository
import com.example.petcare.ui.viewmodel.HealthcareFormState
import com.example.petcare.ui.viewmodel.HealthcareRecordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthcareAndroidTest {
    private lateinit var context: Context
    private lateinit var repository: PetCareRepository
    private lateinit var database: PetCareDatabase
    private lateinit var viewModel: HealthcareRecordViewModel

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        repository = PetCareRepository.getInstance(context)
        database = PetCareDatabase.getInstance(context)
        database.clearAllTables()
        repository.insertPet(
            PetEntity(id = 1L, userId = TEST_USER_ID, name = "Max", species = "Dog")
        )
        viewModel = HealthcareRecordViewModel(context.applicationContext as Application)
        viewModel.setCurrentUserIdForTesting(TEST_USER_ID)
    }

    @After
    fun tearDown() = runBlocking {
        database.clearAllTables()
    }

    @Test
    fun repository_InsertAndObserveHealthcareRecords() = runBlocking {
        val record = HealthcareRecordEntity(
            userId = TEST_USER_ID,
            petId = 1L,
            type = "VACCINATION",
            title = "Rabies Shot",
            date = System.currentTimeMillis()
        )

        repository.insertHealthcareRecord(record)
        val records = repository.observeHealthcareRecords(TEST_USER_ID).first()

        assertEquals(1, records.size)
        assertEquals("Rabies Shot", records.first().title)

        val otherUserRecords = repository.observeHealthcareRecords("other-user").first()
        assertTrue(otherUserRecords.isEmpty())
    }

    @Test
    fun viewModel_SavesRecordAndSetsSuccessMessage() = runTest {
        viewModel.updateForm(HealthcareFormState(
            type = "MEDICATION",
            title = "Daily Vitamin",
            medicationName = "PetVits",
            dosage = "1 pill",
            frequency = "Daily"
        ))

        viewModel.saveRecord(petId = 1L)
        advanceUntilIdle()

        assertEquals("Record added successfully", viewModel.uiState.value.successMessage)
        val records = viewModel.records.value
        assertFalse(records.isEmpty())
        assertEquals("Daily Vitamin", records.first().title)
    }

    @Test
    fun viewModel_ValidatesMissingTitle() = runTest {
        viewModel.updateForm(HealthcareFormState(title = ""))
        viewModel.saveRecord(petId = 1L)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Title is required"))
    }

    companion object {
        private const val TEST_USER_ID = "healthcare-test-user"
    }
}
