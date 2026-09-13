package com.example.petcare

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.petcare.data.local.PetCareDatabase
import com.example.petcare.data.local.entity.DelegationEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.repository.PetCareRepository
import com.example.petcare.ui.viewmodel.DelegationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DelegationAndroidTest {
    private lateinit var context: Context
    private lateinit var repository: PetCareRepository
    private lateinit var database: PetCareDatabase
    private lateinit var viewModel: DelegationViewModel

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
        viewModel = DelegationViewModel(context.applicationContext as Application)
    }

    @After
    fun tearDown() = runBlocking {
        database.clearAllTables()
    }

    @Test
    fun repository_InsertAndObserveDelegations() = runBlocking {
        val delegation = DelegationEntity(
            userId = TEST_USER_ID,
            petId = 1L,
            recipientName = "Asmita",
            recipientPhone = "+9779800000000",
            taskTitles = "Feeding, Walk",
            dateMillis = System.currentTimeMillis(),
            message = "Please help"
        )

        repository.insertDelegation(delegation)
        val history = repository.observeDelegations(TEST_USER_ID).first()

        assertEquals(1, history.size)
        assertEquals("Asmita", history.first().recipientName)

        assertTrue(repository.observeDelegations("other").first().isEmpty())
    }

    companion object {
        private const val TEST_USER_ID = "delegation-test-user"
    }
}
