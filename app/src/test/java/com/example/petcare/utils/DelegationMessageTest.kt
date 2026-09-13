package com.example.petcare.utils

import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DelegationMessageTest {

    @Test
    fun generateDelegationMessage_containsEssentialInfo() {
        val date = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 5)
        }.timeInMillis

        val tasks = listOf(
            CareTaskUiModel(
                task = CareTaskEntity(
                    userId = "user1",
                    petId = 1L,
                    title = "Feed Max",
                    dueAt = date,
                    frequency = "DAILY"
                ),
                isCompleted = false,
                petName = "Max"
            )
        )

        val message = SmsMessageGenerator.generateDelegationMessage(
            recipientName = "Alice",
            petName = "Max",
            dateMillis = date,
            selectedTasks = tasks
        )

        assertTrue(message.contains("Alice"))
        assertTrue(message.contains("Max"))
        assertTrue(message.contains("Feed Max"))
    }
}
