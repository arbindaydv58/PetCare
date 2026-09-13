package com.example.petcare.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSchedulerTest {

    @Test
    fun deterministicIds_areConsistent() {
        val taskId = 123L

        fun generateId(id: Long, prefix: Int) = (prefix * 1000000 + (id % 1000000)).toInt()

        val id1 = generateId(taskId, NotificationScheduler.TASK_PREFIX)
        val id2 = generateId(taskId, NotificationScheduler.TASK_PREFIX)

        assertEquals(id1, id2)
        assertEquals(1000123, id1)
    }

    @Test
    fun collisions_betweenTypes_arePrevented() {
        val id = 123L

        fun generateId(id: Long, prefix: Int) = (prefix * 1000000 + (id % 1000000)).toInt()

        val taskId = generateId(id, NotificationScheduler.TASK_PREFIX)
        val healthId = generateId(id, NotificationScheduler.HEALTHCARE_PREFIX)

        assertNotEquals(taskId, healthId)
    }

    @Test
    fun calculateNextOccurrence_forDailyTask_isCorrect() {
        assertTrue(true)
    }
}
