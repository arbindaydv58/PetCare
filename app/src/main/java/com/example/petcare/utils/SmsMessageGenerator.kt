package com.example.petcare.utils

import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsMessageGenerator {
    fun generateDelegationMessage(
        recipientName: String?,
        petName: String,
        dateMillis: Long,
        selectedTasks: List<CareTaskUiModel>,
        pet: PetEntity? = null,
        healthRecords: List<HealthcareRecordEntity> = emptyList(),
        routines: List<CareRoutineEntity> = emptyList()
    ): String {
        if (selectedTasks.isEmpty()) return ""

        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val recordDateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(dateMillis))

        val builder = StringBuilder()

        if (!recipientName.isNullOrBlank()) {
            builder.append("Hi $recipientName,\n\n")
            builder.append("You have been asked to help care for $petName on $formattedDate.\n\n")
        } else {
            builder.append("PetCare Delegation\n\n")
            builder.append("Please help care for $petName on $formattedDate.\n\n")
        }

        val allergies = pet?.allergies?.trim().orEmpty()
        val medicalNotes = pet?.medicalNotes?.trim().orEmpty()
        if (allergies.isNotBlank() || medicalNotes.isNotBlank()) {
            builder.append("Health Info:\n")
            if (allergies.isNotBlank()) builder.append("• Allergies: $allergies\n")
            if (medicalNotes.isNotBlank()) builder.append("• Medical notes: $medicalNotes\n")
            builder.append("\n")
        }

        if (healthRecords.isNotEmpty()) {
            builder.append("Health Records:\n")
            healthRecords.forEach { record ->
                val recordDate = recordDateFormat.format(Date(record.date))
                val detail = when {
                    !record.clinicName.isNullOrBlank() -> " (${record.clinicName})"
                    !record.medicationName.isNullOrBlank() -> " (${record.medicationName})"
                    else -> ""
                }
                builder.append("• ${humanizeRecordType(record.type)}: ${record.title}$detail — $recordDate\n")
                if (record.nextDueDate != null) {
                    builder.append("  Next due: ${recordDateFormat.format(Date(record.nextDueDate))}\n")
                }
            }
            builder.append("\n")
        }

        if (routines.isNotEmpty()) {
            builder.append("Care Routine:\n")
            routines.forEach { routine ->
                val supplies = routine.requiredSupplies.trim()
                val suppliesText = if (supplies.isNotBlank()) " — Supplies: $supplies" else ""
                builder.append("• ${routine.name} (${routine.frequency.lowercase(Locale.getDefault())})$suppliesText\n")
            }
            builder.append("\n")
        }

        builder.append("Tasks:\n")
        selectedTasks.forEach { taskUi ->
            val time = timeFormat.format(Date(taskUi.task.dueAt))
            builder.append("• ${taskUi.task.title} — $time\n")
        }

        builder.append("\nThank you!")

        return builder.toString()
    }

    private fun humanizeRecordType(type: String): String =
        type.replace("_", " ").lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
}
