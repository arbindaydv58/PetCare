package com.example.petcare.ui.screens.pet

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.R
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.databinding.FragmentRoutineDetailsBinding
import com.example.petcare.databinding.ItemHomeCareTaskBinding
import com.example.petcare.ui.screens.checklist.TasksActivity
import com.example.petcare.ui.viewmodel.CareRoutineViewModel
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import com.example.petcare.ui.viewmodel.CareTaskViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoutineDetailsFragment : Fragment() {

    private val viewModel: CareRoutineViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private val taskViewModel: CareTaskViewModel by viewModels()
    private var binding: FragmentRoutineDetailsBinding? = null

    private var routineId: Long = 0L
    private var imageLoadJob: Job? = null
    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRoutineDetailsBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        routineId = arguments?.getLong(ARG_ROUTINE_ID) ?: 0L

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnEditRoutine.setOnClickListener {
            (requireActivity() as? RoutinesActivity)?.pushFragment(RoutineFormFragment.newInstance(routineId, null))
        }
        binding.btnDeleteRoutine.setOnClickListener { confirmDelete() }
        binding.btnAddTask.setOnClickListener {
            val routine = currentRoutine
            if (routine != null) {
                startActivity(
                    Intent(requireContext(), TasksActivity::class.java).apply {
                        putExtra(TasksActivity.EXTRA_NEW_TASK_PET_ID, routine.petId)
                        putExtra(TasksActivity.EXTRA_NEW_TASK_ROUTINE_ID, routineId)
                    }
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.routines.collect { routines -> onRoutinesChanged(routines) } }
                launch { taskViewModel.checklistState.collect { state -> renderTasks(state.tasks.filter { it.task.routineId == routineId }) } }
            }
        }
    }

    private var currentRoutine: CareRoutineEntity? = null

    private fun onRoutinesChanged(routines: List<CareRoutineEntity>) {
        val binding = binding ?: return
        val routine = routines.find { it.id == routineId }
        currentRoutine = routine

        binding.routineDetailsError.isVisible = routine == null
        binding.routineDetailsContent.isVisible = routine != null
        if (routine == null) return

        binding.routineDetailsName.text = routine.name
        val petName = petViewModel.pets.value.find { it.id == routine.petId }?.name
            ?: getString(R.string.bottom_nav_pets)
        binding.routineDetailsPet.text = "For $petName"
        binding.routineDetailsFrequency.text = routine.frequency.lowercase().replaceFirstChar { it.uppercase() }

        val datesText = buildString {
            append("Starts ${dateFormat.format(Date(routine.startDate))}")
            routine.endDate?.let { append(" • Ends ${dateFormat.format(Date(it))}") }
        }
        binding.routineDetailsDates.text = datesText

        if (routine.description.isNotBlank()) {
            binding.routineDetailsDescription.isVisible = true
            binding.routineDetailsDescription.text = routine.description
        } else {
            binding.routineDetailsDescription.isVisible = false
        }

        if (routine.requiredSupplies.isNotBlank()) {
            binding.suppliesSection.isVisible = true
            binding.suppliesContainer.removeAllViews()
            val supplies = routine.requiredSupplies.split(",").map { it.trim() }.filter { it.isNotBlank() }
            supplies.forEach { supply ->
                val row = android.widget.TextView(requireContext()).apply {
                    text = "•  $supply"
                    setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_text))
                    textSize = 14f
                    setPadding(0, 4, 0, 4)
                }
                binding.suppliesContainer.addView(row)
            }
        } else {
            binding.suppliesSection.isVisible = false
        }

        if (routine.notes.isNotBlank()) {
            binding.notesSection.isVisible = true
            binding.routineDetailsNotes.text = routine.notes
        } else {
            binding.notesSection.isVisible = false
        }

        renderImage(routine.imageUri)
    }

    private fun renderImage(imageUri: String?) {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        if (imageUri.isNullOrBlank()) {
            binding.routineDetailsImage.isVisible = false
            return
        }
        imageLoadJob = CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    if (imageUri.startsWith("/")) {
                        BitmapFactory.decodeFile(imageUri)
                    } else {
                        requireContext().contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                binding.routineDetailsImage.setImageBitmap(bitmap)
                binding.routineDetailsImage.isVisible = true
            } else {
                binding.routineDetailsImage.isVisible = false
            }
        }
    }

    private fun renderTasks(tasks: List<CareTaskUiModel>) {
        val binding = binding ?: return
        binding.routineTasksHeading.text = getString(R.string.routine_tasks_count_format, tasks.size)
        binding.routineTaskContainer.removeAllViews()
        if (tasks.isEmpty()) {
            binding.noRoutineTasksMessage.isVisible = true
            return
        }
        binding.noRoutineTasksMessage.isVisible = false
        val inflater = LayoutInflater.from(binding.root.context)
        tasks.forEach { taskUi ->
            val itemBinding = ItemHomeCareTaskBinding.inflate(inflater, binding.routineTaskContainer, false)
            itemBinding.taskTitle.text = taskUi.task.title
            itemBinding.taskTime.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(taskUi.task.dueAt))
            val iconRes = when (taskUi.task.category) {
                "FEEDING" -> R.drawable.ic_restaurant
                "EXERCISE" -> R.drawable.ic_directions_walk
                "MEDICATION" -> R.drawable.ic_medical
                else -> R.drawable.ic_assignment
            }
            val bgColorRes = when (taskUi.task.category) {
                "FEEDING" -> R.color.soft_yellow
                "EXERCISE" -> R.color.soft_green
                "MEDICATION" -> R.color.soft_blue
                else -> R.color.soft_lavender
            }
            itemBinding.taskIcon.setImageResource(iconRes)
            itemBinding.taskIconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(itemBinding.root.context, bgColorRes)
            )
            itemBinding.taskCompletionIcon.isVisible = taskUi.isCompleted
            itemBinding.taskCompletionPlaceholder.isVisible = !taskUi.isCompleted
            itemBinding.root.setOnClickListener {
                startActivity(
                    Intent(requireContext(), TasksActivity::class.java).apply {
                        putExtra(TasksActivity.EXTRA_TASK_ID, taskUi.task.id)
                    }
                )
            }
            binding.routineTaskContainer.addView(itemBinding.root)
        }
    }

    private fun confirmDelete() {
        val routine = currentRoutine ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_routine_dialog_title)
            .setMessage(getString(R.string.delete_routine_dialog_message, routine.name))
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                viewModel.deleteRoutine(routine)
                dialog.dismiss()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageLoadJob?.cancel()
        binding = null
    }

    companion object {
        private const val ARG_ROUTINE_ID = "routine_id"

        fun newInstance(routineId: Long): RoutineDetailsFragment = RoutineDetailsFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ROUTINE_ID, routineId) }
        }
    }
}
