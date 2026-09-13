package com.example.petcare.ui.screens.pet

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.R
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.databinding.ItemCareRoutineBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoutineAdapter(
    private val onRoutineClick: (CareRoutineEntity) -> Unit,
    private val onEditClick: (CareRoutineEntity) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder>() {

    private var routines: List<CareRoutineEntity> = emptyList()
    private var petNames: Map<Long, String> = emptyMap()

    fun submitList(newRoutines: List<CareRoutineEntity>, newPetNames: Map<Long, String>) {
        routines = newRoutines
        petNames = newPetNames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemCareRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        val routine = routines[position]
        holder.bind(routine, petNames[routine.petId] ?: holder.itemView.context.getString(R.string.bottom_nav_pets), onRoutineClick, onEditClick)
    }

    override fun getItemCount(): Int = routines.size

    override fun onViewRecycled(holder: RoutineViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelImageLoad()
    }

    class RoutineViewHolder(private val binding: ItemCareRoutineBinding) : RecyclerView.ViewHolder(binding.root) {
        private var imageJob: Job? = null
        private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

        fun bind(
            routine: CareRoutineEntity,
            petName: String,
            onRoutineClick: (CareRoutineEntity) -> Unit,
            onEditClick: (CareRoutineEntity) -> Unit
        ) {
            val context = binding.root.context
            binding.tvRoutineName.text = routine.name
            binding.tvRoutinePet.text = petName
            binding.tvRoutineFrequency.text = routine.frequency
            val (pillBackground, pillText) = frequencyPillColors(routine.frequency)
            binding.pillRoutineFrequency.setCardBackgroundColor(ContextCompat.getColor(context, pillBackground))
            binding.tvRoutineFrequency.setTextColor(ContextCompat.getColor(context, pillText))

            if (routine.description.isNotBlank()) {
                binding.tvRoutineDescription.isVisible = true
                binding.tvRoutineDescription.text = routine.description
            } else {
                binding.tvRoutineDescription.isVisible = false
            }

            val hasDates = routine.startDate > 0 || routine.endDate != null
            binding.routineDatesRow.isVisible = hasDates
            binding.tvRoutineStartDate.text = "Starts ${dateFormat.format(Date(routine.startDate))}"
            binding.tvRoutineEndDate.isVisible = routine.endDate != null
            routine.endDate?.let { binding.tvRoutineEndDate.text = "• Ends ${dateFormat.format(Date(it))}" }

            binding.root.setOnClickListener { onRoutineClick(routine) }
            binding.btnEditRoutineItem.setOnClickListener { onEditClick(routine) }

            cancelImageLoad()
            val imageUri = routine.imageUri
            if (imageUri.isNullOrBlank()) {
                showPlaceholder()
            } else {
                imageJob = CoroutineScope(Dispatchers.Main).launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        runCatching {
                            if (imageUri.startsWith("/")) {
                                BitmapFactory.decodeFile(imageUri)
                            } else {
                                context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            }
                        }.getOrNull()
                    }
                    if (bitmap != null) {
                        binding.ivRoutineImage.setImageBitmap(bitmap)
                        binding.ivRoutineImage.isVisible = true
                        binding.ivRoutinePlaceholderIcon.isVisible = false
                    } else {
                        showPlaceholder()
                    }
                }
            }
        }

        private fun showPlaceholder() {
            binding.ivRoutineImage.isVisible = false
            binding.ivRoutinePlaceholderIcon.isVisible = true
        }

        fun cancelImageLoad() {
            imageJob?.cancel()
            imageJob = null
        }

        private fun frequencyPillColors(frequency: String): Pair<Int, Int> = when (frequency) {
            "DAILY" -> R.color.soft_coral to R.color.dark_coral
            "WEEKLY" -> R.color.soft_blue to R.color.info_blue
            "MONTHLY" -> R.color.soft_yellow to R.color.warning_yellow
            else -> R.color.soft_green to R.color.success_green
        }
    }
}
