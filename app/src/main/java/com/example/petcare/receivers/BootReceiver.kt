package com.example.petcare.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.petcare.data.local.repository.PetCareRepository
import com.example.petcare.utils.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        val repository = PetCareRepository.getInstance(context)

                        val tasks = repository.observeCareTasks(userId).first()
                        tasks.filter { it.reminderEnabled }.forEach { task ->
                            NotificationScheduler.scheduleCareTaskReminder(context, task)
                        }

                        val records = repository.observeHealthcareRecords(userId).first()
                        records.filter { it.reminderEnabled }.forEach { record ->
                            NotificationScheduler.scheduleHealthcareReminder(context, record)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
