package com.example.petcare.ui.screens.healthcare

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.R
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.databinding.ItemHealthcareRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun humanizeHealthcareType(type: String): String =
    type.replace("_", " ").lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

fun healthcareIconFor(type: String): Int = when {
    type.contains("VACCINATION") -> R.drawable.ic_vaccines
    type.contains("MEDICATION") -> R.drawable.ic_medical
    type.contains("APPOINTMENT") -> R.drawable.ic_calendar
    else -> R.drawable.ic_assignment
}

fun healthcareColorFor(type: String): Int = when {
    type.contains("VACCINATION") -> R.color.soft_green
    type.contains("MEDICATION") -> R.color.soft_blue
    type.contains("APPOINTMENT") -> R.color.soft_yellow
    else -> R.color.soft_lavender
}

class HealthcareAdapter(
    private val onRecordClick: (HealthcareRecordEntity) -> Unit
) : RecyclerView.Adapter<HealthcareAdapter.HealthcareViewHolder>() {

    private var records: List<HealthcareRecordEntity> = emptyList()
    private var petNames: Map<Long, String> = emptyMap()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun submitList(newRecords: List<HealthcareRecordEntity>, newPetNames: Map<Long, String>) {
        records = newRecords
        petNames = newPetNames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthcareViewHolder {
        val binding = ItemHealthcareRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HealthcareViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HealthcareViewHolder, position: Int) {
        val record = records[position]
        holder.bind(record, petNames[record.petId] ?: holder.itemView.context.getString(R.string.bottom_nav_pets), dateFormat, onRecordClick)
    }

    override fun getItemCount(): Int = records.size

    class HealthcareViewHolder(private val binding: ItemHealthcareRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            record: HealthcareRecordEntity,
            petName: String,
            dateFormat: SimpleDateFormat,
            onRecordClick: (HealthcareRecordEntity) -> Unit
        ) {
            val context = binding.root.context

            binding.healthcareItemType.text = humanizeHealthcareType(record.type)
            binding.healthcareItemDate.text = dateFormat.format(Date(record.date))
            binding.healthcareItemTitle.text = record.title
            binding.healthcareItemPet.text = context.getString(R.string.pet_label_format, petName)

            binding.healthcareItemIcon.setImageResource(healthcareIconFor(record.type))
            binding.healthcareItemIconContainer.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, healthcareColorFor(record.type))
            )

            val nextDue = record.nextDueDate
            if (nextDue != null) {
                val isOverdue = nextDue < System.currentTimeMillis()
                binding.healthcareItemNextDue.isVisible = true
                binding.healthcareItemNextDue.text = context.getString(R.string.healthcare_next_due_pill_format, dateFormat.format(Date(nextDue)))
                val pillColor = ContextCompat.getColor(context, if (isOverdue) R.color.error_red else R.color.primary_coral)
                binding.healthcareItemNextDue.setTextColor(pillColor)
                binding.healthcareItemNextDue.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, if (isOverdue) R.color.soft_yellow else R.color.soft_coral)
                )
            } else {
                binding.healthcareItemNextDue.isVisible = false
            }

            binding.root.setOnClickListener { onRecordClick(record) }
        }
    }
}
