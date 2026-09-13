package com.example.petcare

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.petcare.data.local.PetCareDatabase
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.repository.PetCareRepository
import com.example.petcare.ui.viewmodel.PetFormState
import com.example.petcare.ui.viewmodel.PetViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetManagementAndroidTest {
    private lateinit var context: Context
    private lateinit var repository: PetCareRepository
    private lateinit var database: PetCareDatabase
    private lateinit var viewModel: PetViewModel

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        repository = PetCareRepository.getInstance(context)
        database = PetCareDatabase.getInstance(context)
        database.clearAllTables()
        viewModel = PetViewModel(context.applicationContext as Application)
        viewModel.setCurrentUserIdForTesting(TEST_USER_ID)
    }

    @After
    fun tearDown() = runBlocking {
        database.clearAllTables()
        viewModel.clearCurrentUserIdOverride()
    }

    @Test
    fun repositoryInsertGetUpdateAndDeletePet_forCurrentUserOnly() = runBlocking {
        val insertedId = repository.insertPet(
            PetEntity(
                userId = TEST_USER_ID,
                name = "Max",
                species = "Dog",
                breed = "Golden Retriever",
                weight = 28.0,
                gender = "Male"
            )
        )

        val userPets = repository.observePets(TEST_USER_ID).first()
        assertEquals(1, userPets.size)
        assertEquals(insertedId, userPets.first().id)
        assertTrue(repository.observePets("other-user").first().isEmpty())

        val pet = repository.getPet(TEST_USER_ID, insertedId)
        assertNotNull(pet)
        assertEquals("Max", pet!!.name)

        repository.updatePet(pet.copy(name = "Maximus", weight = 30.0))
        val updatedPet = repository.getPet(TEST_USER_ID, insertedId)
        assertEquals("Maximus", updatedPet!!.name)
        assertEquals(30.0, updatedPet.weight, 0.0)

        repository.deletePet(updatedPet)
        assertTrue(repository.observePets(TEST_USER_ID).first().isEmpty())
    }

    @Test
    fun viewModel_addsUpdatesAndDeletesPet() = runTest {
        viewModel.updateForm(
            PetFormState(
                name = "Luna",
                species = "Cat",
                breed = "Tabby",
                weight = "4.5",
                gender = "Female",
                dietaryPreferences = "Fish",
                allergies = "None",
                vaccinationHistory = "Updated",
                favouriteToys = "Ball",
                medicalNotes = "Healthy",
                generalNotes = "Loves naps"
            )
        )

        val added = viewModel.savePet()
        advanceUntilIdle()
        assertTrue(added)
        assertEquals("Pet added successfully", viewModel.petUiState.value.successMessage)
        assertEquals(1, viewModel.pets.value.size)

        viewModel.updateForm(
            viewModel.formState.value.copy(
                name = "Luna updated",
                weight = "5.2"
            )
        )
        val updated = viewModel.savePet()
        advanceUntilIdle()
        assertTrue(updated)
        assertEquals("Pet updated successfully", viewModel.petUiState.value.successMessage)

        val updatedPet = viewModel.pets.value.first()
        assertEquals("Luna updated", updatedPet.name)
        assertEquals(5.2, updatedPet.weight, 0.0)

        viewModel.deletePet(updatedPet.id)
        advanceUntilIdle()
        assertEquals("Pet deleted successfully", viewModel.petUiState.value.successMessage)
        assertTrue(viewModel.pets.value.isEmpty())
    }

    @Test
    fun viewModel_setsErrorStateWhenValidationFails() = runTest {
        viewModel.updateForm(
            PetFormState(
                name = "",
                species = "Dog",
                weight = "-1"
            )
        )

        val result = viewModel.savePet()
        advanceUntilIdle()
        assertTrue(result == false)
        assertTrue(
            viewModel.petUiState.value.errorMessage?.contains("cannot be empty") == true ||
                viewModel.petUiState.value.errorMessage?.contains("positive number") == true
        )
    }

    companion object {
        private const val TEST_USER_ID = "android-test-user"
    }
}

class MainDispatcherRule : TestWatcher() {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
