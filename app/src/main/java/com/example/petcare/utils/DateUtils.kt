package com.example.petcare.utils

import com.example.petcare.data.local.entity.CareTaskEntity
import java.util.Calendar

object DateUtils {
    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isTaskActiveOnDate(task: CareTaskEntity, dateMillis: Long): Boolean {
        val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueAt }
        val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }

        if (startOfDay(task.dueAt) > dateMillis) return false

        return when (task.frequency) {
            "ONCE" -> startOfDay(task.dueAt) == dateMillis
            "DAILY" -> true
            "WEEKLY" -> taskCal.get(Calendar.DAY_OF_WEEK) == dateCal.get(Calendar.DAY_OF_WEEK)
            "MONTHLY" -> taskCal.get(Calendar.DAY_OF_MONTH) == dateCal.get(Calendar.DAY_OF_MONTH)
            else -> false
        }
    }
}
