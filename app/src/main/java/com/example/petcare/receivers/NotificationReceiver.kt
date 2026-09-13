package com.example.petcare.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.petcare.data.local.repository.PetCareRepository
import com.example.petcare.utils.NotificationHelper
import com.example.petcare.utils.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)
        val userId = intent.getStringExtra("EXTRA_USER_ID").orEmpty()
        if (userId.isBlank()) return

        when (action) {
            "ACTION_SHOW_TASK_REMINDER" -> {
                val taskId = intent.getLongExtra("EXTRA_TASK_ID", 0L)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = PetCareRepository.getInstance(context)
                        val task = repository.getCareTaskById(userId, taskId)
                        val settings = repository.getNotificationSettings(userId)
                        val allowed = settings == null || (settings.remindersEnabled && settings.taskRemindersEnabled)

                        if (task != null && allowed) {
                            NotificationHelper.showNotification(
                                context = context,
                                id = notificationId,
                                title = "Reminder: ${task.title}",
                                message = "Time to take care of your pet!",
                                deepLink = "petcare://tasks/${task.id}"
                            )
                        }

                        if (task != null && task.frequency != "ONCE") {
                            NotificationScheduler.scheduleCareTaskReminder(context, task)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "ACTION_SHOW_HEALTHCARE_REMINDER" -> {
                val title = intent.getStringExtra("EXTRA_RECORD_TITLE") ?: "Healthcare Event"
                val recordId = intent.getLongExtra("EXTRA_RECORD_ID", 0L)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = PetCareRepository.getInstance(context)
                        val settings = repository.getNotificationSettings(userId)
                        val allowed = settings == null || (settings.remindersEnabled && settings.healthcareRemindersEnabled)

                        if (allowed) {
                            NotificationHelper.showNotification(
                                context = context,
                                id = notificationId,
                                title = "Upcoming: $title",
                                message = "Check your pet's healthcare schedule.",
                                deepLink = "petcare://healthcare/$recordId"
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
