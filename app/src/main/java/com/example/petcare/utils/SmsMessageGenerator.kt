package com.example.petcare.utils

import com.example.petcare.ui.viewmodel.CareTaskUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsMessageGenerator {
    fun generateDelegationMessage(
        recipientName: String?,
        petName: String,
        dateMillis: Long,
        selectedTasks: List<CareTaskUiModel>
    ): String {
        if (selectedTasks.isEmpty()) return ""

        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(dateMillis))

        val builder = StringBuilder()

        if (!recipientName.isNullOrBlank()) {
            builder.append("Hi $recipientName,\n\n")
            builder.append("You have been asked to help care for $petName on $formattedDate.\n\n")
        } else {
            builder.append("PetCare Delegation\n\n")
            builder.append("Please help care for $petName on $formattedDate.\n\n")
        }

        builder.append("Tasks:\n")
        selectedTasks.forEach { taskUi ->
            val time = timeFormat.format(Date(taskUi.task.dueAt))
            builder.append("• ${taskUi.task.title} — $time\n")
        }

        builder.append("\nThank you!")

        return builder.toString()
    }
}
