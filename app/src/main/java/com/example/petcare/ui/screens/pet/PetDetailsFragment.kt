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
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentPetDetailsBinding
import com.example.petcare.databinding.ItemHomeCareTaskBinding
import com.example.petcare.ui.screens.checklist.TasksActivity
import com.example.petcare.ui.screens.delegation.DelegationActivity
import com.example.petcare.ui.screens.expenses.ExpensesActivity
import com.example.petcare.ui.screens.healthcare.HealthcareActivity
import com.example.petcare.ui.viewmodel.CareRoutineViewModel
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import com.example.petcare.ui.viewmodel.CareTaskViewModel
import com.example.petcare.ui.viewmodel.ExpenseViewModel
import com.example.petcare.ui.viewmodel.HealthcareRecordViewModel
import com.example.petcare.ui.viewmodel.PetUiState
import com.example.petcare.ui.viewmodel.PetViewModel
import com.example.petcare.ui.viewmodel.ageInYears
import com.example.petcare.ui.viewmodel.formattedDate
import com.example.petcare.utils.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PetDetailsFragment : Fragment() {

    private val viewModel: PetViewModel by viewModels()
    private val careTaskVm: CareTaskViewModel by viewModels()
    private val healthcareVm: HealthcareRecordViewModel by viewModels()
    private val expenseVm: ExpenseViewModel by viewModels()
    private val routineVm: CareRoutineViewModel by viewModels()

    private var binding: FragmentPetDetailsBinding? = null
    private var petId: Long = 0L
    private var imageLoadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPetDetailsBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        petId = arguments?.getLong(ARG_PET_ID) ?: 0L
        viewModel.loadPet(petId)

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnEditPet.setOnClickListener {
            (requireActivity() as? PetsActivity)?.pushFragment(PetFormFragment.newInstance(petId))
        }
        binding.btnDeletePet.setOnClickListener { confirmDelete() }
        binding.btnPetDetailsGoBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        binding.btnAddTask.setOnClickListener {
            startActivity(
                Intent(requireContext(), TasksActivity::class.java).apply {
                    putExtra(TasksActivity.EXTRA_NEW_TASK_PET_ID, petId)
                }
            )
        }
        binding.btnAddHealthcare.setOnClickListener {
            startActivity(
                Intent(requireContext(), HealthcareActivity::class.java).apply {
                    putExtra(HealthcareActivity.EXTRA_NEW_RECORD_PET_ID, petId)
                }
            )
        }
        binding.healthcareCard.setOnClickListener {
            startActivity(
                Intent(requireContext(), HealthcareActivity::class.java).apply {
                    putExtra(HealthcareActivity.EXTRA_PET_ID, petId)
                }
            )
        }
        binding.btnAddExpense.setOnClickListener {
            startActivity(
                Intent(requireContext(), ExpensesActivity::class.java).apply {
                    putExtra(ExpensesActivity.EXTRA_NEW_EXPENSE_PET_ID, petId)
                }
            )
        }
        binding.expensesCard.setOnClickListener {
            startActivity(
                Intent(requireContext(), ExpensesActivity::class.java).apply {
                    putExtra(ExpensesActivity.EXTRA_PET_ID, petId)
                }
            )
        }
        binding.btnDelegateCare.setOnClickListener {
            startActivity(
                Intent(requireContext(), DelegationActivity::class.java).apply {
                    putExtra(DelegationActivity.EXTRA_PET_ID, petId)
                }
            )
        }

        binding.btnAddRoutineFromPet.setOnClickListener {
            startActivity(
                Intent(requireContext(), RoutinesActivity::class.java).apply {
                    putExtra(RoutinesActivity.EXTRA_NEW_ROUTINE_PET_ID, petId)
                }
            )
        }
        binding.btnSeeAllRoutines.setOnClickListener {
            startActivity(Intent(requireContext(), RoutinesActivity::class.java))
        }

        val today = DateUtils.startOfDay(System.currentTimeMillis())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.petUiState.collect { state -> renderUiState(state) } }
                launch { viewModel.selectedPet.collect { pet -> renderPet(pet) } }
                launch {
                    careTaskVm.observeTasksForPet(petId, today).collect { tasks -> renderTasks(tasks) }
                }
                launch {
                    healthcareVm.observeRecordsForPet(petId).collect { records -> renderHealthcare(records) }
                }
                launch {
                    expenseVm.observeExpensesForPet(petId).collect { expenses ->
                        val total = expenses.sumOf { it.amount }
                        binding.expensesTotal.text = currencyFormatter.format(total)
                    }
                }
                launch {
                    routineVm.observeRoutinesForPet(petId).collect { routines -> renderRoutines(routines) }
                }
            }
        }
    }

    private val currencyFormatter: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale("en", "NP")) }

    private fun renderUiState(state: PetUiState) {
        val binding = binding ?: return
        val pet = viewModel.selectedPet.value
        binding.petDetailsLoading.isVisible = state.isLoading && pet == null
        binding.petDetailsError.isVisible = state.errorMessage != null && pet == null
        binding.petDetailsContent.isVisible = pet != null
    }

    private fun renderPet(pet: PetEntity?) {
        val binding = binding ?: return
        if (pet == null) return

        binding.petDetailsTitle.text = pet.name
        binding.petDetailsName.text = pet.name.uppercase()
        binding.petDetailsSpeciesBreed.text = "${pet.species} • ${pet.breed.ifBlank { getString(R.string.not_provided) }}"
        binding.petDetailsWeight.text = getString(R.string.pet_weight_kg, pet.weight.toString())
        binding.petDetailsAge.text = getString(R.string.pet_age_years, pet.ageInYears())

        bindInfoRow(binding.rowDob.root, R.string.label_birthday, pet.formattedDate())
        bindInfoRow(binding.rowGender.root, R.string.label_gender, pet.gender.ifBlank { getString(R.string.not_provided) })
        bindInfoRow(binding.rowDietary.root, R.string.label_dietary_preferences, pet.dietaryPreferences.ifBlank { getString(R.string.not_provided) })
        bindInfoRow(binding.rowAllergies.root, R.string.label_allergies, pet.allergies.ifBlank { getString(R.string.not_provided) })
        bindInfoRow(binding.rowVaccination.root, R.string.label_vaccination_history, pet.vaccinationHistory.ifBlank { getString(R.string.not_provided) })
        bindInfoRow(binding.rowToys.root, R.string.label_favourite_toys, pet.favouriteToys.ifBlank { getString(R.string.not_provided) })
        bindInfoRow(binding.rowMedical.root, R.string.label_medical_notes, pet.medicalNotes.ifBlank { getString(R.string.not_provided) })
        bindInfoRow(binding.rowGeneral.root, R.string.label_general_notes, pet.generalNotes.ifBlank { getString(R.string.not_provided) })

        renderImage(pet.imageUri)
    }

    private fun bindInfoRow(row: View, labelRes: Int, value: String) {
        row.findViewById<android.widget.TextView>(R.id.label).text = getString(labelRes)
        row.findViewById<android.widget.TextView>(R.id.value).text = value
    }

    private fun renderImage(imageUri: String?) {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        if (imageUri.isNullOrBlank()) {
            binding.petDetailsImage.isVisible = false
            binding.petDetailsImagePlaceholder.isVisible = true
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
                binding.petDetailsImage.setImageBitmap(bitmap)
                binding.petDetailsImage.isVisible = true
                binding.petDetailsImagePlaceholder.isVisible = false
            } else {
                binding.petDetailsImage.isVisible = false
                binding.petDetailsImagePlaceholder.isVisible = true
            }
        }
    }

    private fun renderTasks(tasks: List<CareTaskUiModel>) {
        val binding = binding ?: return
        binding.careTaskContainer.removeAllViews()
        if (tasks.isEmpty()) {
            binding.noTasksMessage.isVisible = true
            return
        }
        binding.noTasksMessage.isVisible = false
        val inflater = LayoutInflater.from(binding.root.context)
        tasks.take(4).forEach { taskUi ->
            val itemBinding = ItemHomeCareTaskBinding.inflate(inflater, binding.careTaskContainer, false)
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
            binding.careTaskContainer.addView(itemBinding.root)
        }
    }

    private fun renderHealthcare(records: List<HealthcareRecordEntity>) {
        val binding = binding ?: return
        if (records.isEmpty()) {
            binding.healthcareNextDue.isVisible = false
            binding.healthcareSummary.text = getString(R.string.no_medical_records)
            return
        }
        val now = System.currentTimeMillis()
        val upcoming = records.filter { it.nextDueDate != null && it.nextDueDate > now }
        if (upcoming.isNotEmpty()) {
            binding.healthcareNextDue.isVisible = true
            binding.healthcareNextDue.text = getString(R.string.healthcare_next_due_format, upcoming.first().title)
        } else {
            binding.healthcareNextDue.isVisible = false
        }
        binding.healthcareSummary.text = getString(R.string.healthcare_record_count_format, records.size)
    }

    private fun renderRoutines(routines: List<CareRoutineEntity>) {
        val binding = binding ?: return
        binding.petRoutineContainer.removeAllViews()
        binding.noPetRoutinesMessage.isVisible = routines.isEmpty()
        binding.btnSeeAllRoutines.isVisible = routines.isNotEmpty()

        val inflater = LayoutInflater.from(binding.root.context)
        routines.take(3).forEach { routine ->
            val row = inflater.inflate(R.layout.item_pet_routine_preview, binding.petRoutineContainer, false)
            row.findViewById<android.widget.TextView>(R.id.tv_routine_preview_name).text = routine.name
            row.findViewById<android.widget.TextView>(R.id.tv_routine_preview_frequency).text =
                routine.frequency.lowercase().replaceFirstChar { it.uppercase() }
            row.setOnClickListener {
                startActivity(
                    Intent(requireContext(), RoutinesActivity::class.java).apply {
                        putExtra(RoutinesActivity.EXTRA_ROUTINE_ID, routine.id)
                    }
                )
            }
            binding.petRoutineContainer.addView(row)
        }
    }

    private fun confirmDelete() {
        val pet = viewModel.selectedPet.value ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_pet_dialog_title)
            .setMessage(getString(R.string.delete_pet_dialog_message, pet.name))
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                viewModel.deletePet(pet.id) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
                dialog.dismiss()
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
        private const val ARG_PET_ID = "pet_id"

        fun newInstance(petId: Long): PetDetailsFragment = PetDetailsFragment().apply {
            arguments = Bundle().apply { putLong(ARG_PET_ID, petId) }
        }
    }
}
