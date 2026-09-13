package com.example.petcare

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.petcare.data.local.PetCareDatabase
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.data.local.entity.TaskCompletionEntity
import com.example.petcare.data.local.repository.PetCareRepository
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import com.example.petcare.ui.viewmodel.CareTaskViewModel
import com.example.petcare.ui.viewmodel.ResetChecklistResult
import com.example.petcare.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CareTaskGestureAndroidTest {
    private lateinit var context: Context
    private lateinit var repository: PetCareRepository
    private lateinit var database: PetCareDatabase
    private lateinit var viewModel: CareTaskViewModel

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        repository = PetCareRepository.getInstance(context)
        database = PetCareDatabase.getInstance(context)
        database.clearAllTables()
        repository.insertPet(
            PetEntity(id = PET_ID, userId = TEST_USER_ID, name = "Max", species = "Dog")
        )
        viewModel = CareTaskViewModel(context.applicationContext as Application)
        viewModel.setCurrentUserIdForTesting(TEST_USER_ID)
    }

    @After
    fun tearDown() = runBlocking {
        database.clearAllTables()
        viewModel.clearCurrentUserIdOverride()
    }

    @Test
    fun swipeRight_completesOnlyTodaysOccurrence_recurringTaskStaysPendingTomorrow() = runTest {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val tomorrow = today + TimeUnit.DAYS.toMillis(1)

        val taskId = repository.insertCareTask(dailyTask("Feed Max"))
        val taskUi = CareTaskUiModel(
            task = repository.getCareTaskById(TEST_USER_ID, taskId)!!,
            isCompleted = false
        )

        viewModel.toggleTaskCompletion(taskUi, today)
        advanceUntilIdle()

        val todaysCompletions = repository.observeCompletionsForDate(TEST_USER_ID, today).first()
        assertEquals(1, todaysCompletions.size)
        assertEquals(taskId, todaysCompletions.first().taskId)
        assertEquals(TEST_USER_ID, todaysCompletions.first().userId)

        val tomorrowsCompletions = repository.observeCompletionsForDate(TEST_USER_ID, tomorrow).first()
        assertTrue("Recurring task must not be permanently completed for future days", tomorrowsCompletions.isEmpty())

        val taskAfter = repository.getCareTaskById(TEST_USER_ID, taskId)!!
        assertFalse(taskAfter.isCompleted)
    }

    @Test
    fun swipeRight_onceTask_usesPermanentFlagCorrectly() = runTest {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val taskId = repository.insertCareTask(onceTask("Vet visit", dueAt = today))
        val taskUi = CareTaskUiModel(task = repository.getCareTaskById(TEST_USER_ID, taskId)!!, isCompleted = false)

        viewModel.toggleTaskCompletion(taskUi, today)
        advanceUntilIdle()

        val taskAfter = repository.getCareTaskById(TEST_USER_ID, taskId)!!
        assertTrue(taskAfter.isCompleted)
        assertTrue(repository.observeCompletionsForDate(TEST_USER_ID, today).first().isEmpty())
    }

    @Test
    fun swipeLeft_deleteFlow_removesTaskAndCascadesCompletions_andCancelsReminder() = runTest {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val taskId = repository.insertCareTask(dailyTask("Walk Max", reminderEnabled = true))
        repository.markTaskComplete(TEST_USER_ID, taskId, today)

        val task = repository.getCareTaskById(TEST_USER_ID, taskId)!!
        viewModel.deleteTask(task)
        advanceUntilIdle()

        assertNull(repository.getCareTaskById(TEST_USER_ID, taskId))
        assertTrue(
            "TaskCompletionEntity rows must cascade-delete with their task",
            repository.observeCompletionsForDate(TEST_USER_ID, today).first().none { it.taskId == taskId }
        )
    }

    @Test
    fun swipeLeft_deletesOnlyTheSwipedTask_othersRemain() = runTest {
        val keepId = repository.insertCareTask(dailyTask("Feed Max"))
        val deleteId = repository.insertCareTask(dailyTask("Walk Max"))

        viewModel.deleteTask(repository.getCareTaskById(TEST_USER_ID, deleteId)!!)
        advanceUntilIdle()

        assertNull(repository.getCareTaskById(TEST_USER_ID, deleteId))
        assertTrue(repository.getCareTaskById(TEST_USER_ID, keepId) != null)
    }

    @Test
    fun shake_resetTodayChecklist_clearsOnlyTodaysCompletionsForCurrentUser() = runTest {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val yesterday = today - TimeUnit.DAYS.toMillis(1)
        val tomorrow = today + TimeUnit.DAYS.toMillis(1)

        val feedMaxId = repository.insertCareTask(dailyTask("Feed Max"))
        val walkMaxId = repository.insertCareTask(dailyTask("Walk Max"))

        repository.markTaskComplete(TEST_USER_ID, feedMaxId, today)
        repository.markTaskComplete(TEST_USER_ID, walkMaxId, today)
        repository.markTaskComplete(TEST_USER_ID, feedMaxId, yesterday)
        repository.markTaskComplete(TEST_USER_ID, feedMaxId, tomorrow)
        val otherUserTaskId = repository.insertCareTask(dailyTask("Feed Rex", userId = OTHER_USER_ID))
        repository.markTaskComplete(OTHER_USER_ID, otherUserTaskId, today)

        var result: ResetChecklistResult? = null
        viewModel.resetTodayChecklist { result = it }
        advanceUntilIdle()

        assertTrue(result is ResetChecklistResult.Reset)
        assertEquals(2, (result as ResetChecklistResult.Reset).count)

        assertTrue("Today's completions must be cleared", repository.observeCompletionsForDate(TEST_USER_ID, today).first().isEmpty())
        assertEquals("Yesterday must be unaffected", 1, repository.observeCompletionsForDate(TEST_USER_ID, yesterday).first().size)
        assertEquals("Tomorrow must be unaffected", 1, repository.observeCompletionsForDate(TEST_USER_ID, tomorrow).first().size)
        assertEquals("Other users must be unaffected", 1, repository.observeCompletionsForDate(OTHER_USER_ID, today).first().size)

        val feedMaxAfter = repository.getCareTaskById(TEST_USER_ID, feedMaxId)!!
        assertEquals("DAILY", feedMaxAfter.frequency)
        val walkMaxAfter = repository.getCareTaskById(TEST_USER_ID, walkMaxId)!!
        assertEquals("DAILY", walkMaxAfter.frequency)
    }

    @Test
    fun shake_resetTodayChecklist_resetsOnceTaskCompletedToday_withoutDeletingIt() = runTest {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val taskId = repository.insertCareTask(onceTask("Vet visit", dueAt = today, isCompleted = true))

        viewModel.resetTodayChecklist { }
        advanceUntilIdle()

        val taskAfter = repository.getCareTaskById(TEST_USER_ID, taskId)!!
        assertFalse(taskAfter.isCompleted)
        assertNull(taskAfter.completedAt)
    }

    @Test
    fun shake_resetTodayChecklist_whenNothingCompleted_reportsAlreadyClear() = runTest {
        repository.insertCareTask(dailyTask("Feed Max"))

        var result: ResetChecklistResult? = null
        viewModel.resetTodayChecklist { result = it }
        advanceUntilIdle()

        assertTrue(result is ResetChecklistResult.AlreadyClear)
    }

    @Test
    fun routineDetailsChecklistState_reflectsActualCompletion_notHardcodedFalse() = runTest {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val routineId = repository.insertCareRoutine(
            CareRoutineEntity(
                userId = TEST_USER_ID,
                petId = PET_ID,
                name = "Morning Routine",
                frequency = "DAILY",
                startDate = today
            )
        )
        repository.insertCareTask(dailyTask("Feed Max", routineId = routineId))
        advanceUntilIdle()

        var tasksInRoutine = viewModel.checklistState.value.tasks.filter { it.task.routineId == routineId }
        assertEquals(1, tasksInRoutine.size)
        assertFalse(tasksInRoutine.first().isCompleted)

        viewModel.toggleTaskCompletion(tasksInRoutine.first(), today)
        advanceUntilIdle()

        tasksInRoutine = viewModel.checklistState.value.tasks.filter { it.task.routineId == routineId }
        assertTrue("Routine details must reflect the real occurrence completion state", tasksInRoutine.first().isCompleted)
    }

    private fun dailyTask(
        title: String,
        userId: String = TEST_USER_ID,
        routineId: Long? = null,
        reminderEnabled: Boolean = false
    ) = CareTaskEntity(
        userId = userId,
        petId = PET_ID,
        routineId = routineId,
        title = title,
        category = "FEEDING",
        dueAt = System.currentTimeMillis(),
        frequency = "DAILY",
        reminderEnabled = reminderEnabled
    )

    private fun onceTask(
        title: String,
        dueAt: Long,
        isCompleted: Boolean = false
    ) = CareTaskEntity(
        userId = TEST_USER_ID,
        petId = PET_ID,
        title = title,
        category = "HEALTHCARE",
        dueAt = dueAt,
        frequency = "ONCE",
        isCompleted = isCompleted,
        completedAt = if (isCompleted) System.currentTimeMillis() else null
    )

    companion object {
        private const val TEST_USER_ID = "gesture-test-user"
        private const val OTHER_USER_ID = "other-gesture-test-user"
        private const val PET_ID = 1L
    }
}
