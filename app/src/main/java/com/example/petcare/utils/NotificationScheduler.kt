package com.example.petcare.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.receivers.NotificationReceiver

object NotificationScheduler {

    private fun generateNotificationId(id: Long, typePrefix: Int): Int {
        return (typePrefix * 1000000 + (id % 1000000)).toInt()
    }

    const val TASK_PREFIX = 1
    const val HEALTHCARE_PREFIX = 2

    fun scheduleCareTaskReminder(context: Context, task: CareTaskEntity) {
        if (!task.reminderEnabled) {
            cancelCareTaskReminder(context, task)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val nextOccurrence = calculateNextOccurrence(task)
        if (nextOccurrence == null) return

        val reminderTime = nextOccurrence - (task.reminderMinutesBefore * 60 * 1000)

        if (reminderTime < System.currentTimeMillis()) {
            return
        }

        val notificationId = generateNotificationId(task.id, TASK_PREFIX)
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_SHOW_TASK_REMINDER"
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_TITLE", task.title)
            putExtra("EXTRA_PET_ID", task.petId)
            putExtra("EXTRA_USER_ID", task.userId)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            putExtra("EXTRA_OCCURRENCE_DATE", nextOccurrence)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

    fun cancelCareTaskReminder(context: Context, task: CareTaskEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationId = generateNotificationId(task.id, TASK_PREFIX)
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleHealthcareReminder(context: Context, record: HealthcareRecordEntity) {
        if (!record.reminderEnabled) {
            cancelHealthcareReminder(context, record)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderTime = record.nextDueDate ?: record.date

        if (reminderTime < System.currentTimeMillis()) return

        val notificationId = generateNotificationId(record.id, HEALTHCARE_PREFIX)
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_SHOW_HEALTHCARE_REMINDER"
            putExtra("EXTRA_RECORD_ID", record.id)
            putExtra("EXTRA_RECORD_TITLE", record.title)
            putExtra("EXTRA_PET_ID", record.petId)
            putExtra("EXTRA_USER_ID", record.userId)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

    fun cancelHealthcareReminder(context: Context, record: HealthcareRecordEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationId = generateNotificationId(record.id, HEALTHCARE_PREFIX)
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calculateNextOccurrence(task: CareTaskEntity): Long? {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = task.dueAt }

        if (task.frequency == "ONCE") {
            return if (task.dueAt > now) task.dueAt else null
        }

        while (calendar.timeInMillis < now) {
            when (task.frequency) {
                "DAILY" -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> calendar.add(java.util.Calendar.MONTH, 1)
                else -> return null
            }
        }
        return calendar.timeInMillis
    }
}
