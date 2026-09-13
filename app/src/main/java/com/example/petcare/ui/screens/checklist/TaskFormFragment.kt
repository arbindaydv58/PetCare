package com.example.petcare.ui.screens.checklist

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.R
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentTaskFormBinding
import com.example.petcare.ui.viewmodel.CareTaskViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskFormFragment : Fragment() {

    private val viewModel: CareTaskViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentTaskFormBinding? = null

    private var taskId: Long? = null
    private var presetPetId: Long? = null
    private var presetRoutineId: Long? = null
    private var fieldsPopulatedForEdit = false
    private var imageLoadJob: Job? = null
    private var petsBuilt = false

    private var selectedPetId: Long? = null
    private var selectedRoutineId: Long? = null
    private var selectedCategory: String = "OTHER"
    private var selectedFrequency: String = "ONCE"
    private var dueAtMillis: Long = System.currentTimeMillis()
    private var reminderEnabled: Boolean = false
    private var reminderMinutesBefore: Int = 0
    private var imageUri: String? = null

    private val categories = listOf("FEEDING", "EXERCISE", "GROOMING", "MEDICATION", "HEALTHCARE", "CLEANING", "OTHER")
    private val categoryLabelRes = mapOf(
        "FEEDING" to R.string.category_feeding,
        "EXERCISE" to R.string.category_exercise,
        "GROOMING" to R.string.category_grooming,
        "MEDICATION" to R.string.category_medication,
        "HEALTHCARE" to R.string.category_healthcare,
        "CLEANING" to R.string.category_cleaning,
        "OTHER" to R.string.category_other
    )

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
            renderImage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTaskFormBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        taskId = arguments?.getLong(ARG_TASK_ID, -1L)?.takeIf { it > 0 }
        presetPetId = arguments?.getLong(ARG_PRESET_PET_ID, -1L)?.takeIf { it > 0 }
        presetRoutineId = arguments?.getLong(ARG_PRESET_ROUTINE_ID, -1L)?.takeIf { it > 0 }
        selectedPetId = presetPetId
        selectedRoutineId = presetRoutineId

        binding.taskFormTitle.text = getString(if (taskId != null) R.string.edit_task_title else R.string.add_task_title)
        binding.btnSaveTask.text = getString(if (taskId != null) R.string.action_update_task else R.string.action_create_task)

        if (taskId == null) {
            fieldsPopulatedForEdit = true
        }

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnSelectTaskImage.setOnClickListener { photoPicker.launch("image/*") }
        binding.btnTaskDate.setOnClickListener { showDatePicker() }
        binding.btnTaskTime.setOnClickListener { showTimePicker() }

        setupCategoryDropdown(binding)
        setupFrequencyChips(binding)
        setupReminderControls(binding)

        binding.btnSaveTask.setOnClickListener { save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { petViewModel.pets.collect { pets -> onPetsChanged(pets) } }
                launch { viewModel.tasks.collect { tasks -> onTasksChanged(tasks) } }
            }
        }

        renderDateTime()
        renderImage()
    }

    private fun setupCategoryDropdown(binding: FragmentTaskFormBinding) {
        val labels = categories.map { getString(categoryLabelRes.getValue(it)) }
        binding.etTaskCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
        binding.etTaskCategory.setText(getString(categoryLabelRes.getValue(selectedCategory)), false)
        binding.etTaskCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
        }
    }

    private fun setupFrequencyChips(binding: FragmentTaskFormBinding) {
        val chips = mapOf(
            "ONCE" to binding.chipTaskFreqOnce,
            "DAILY" to binding.chipTaskFreqDaily,
            "WEEKLY" to binding.chipTaskFreqWeekly,
            "MONTHLY" to binding.chipTaskFreqMonthly
        )
        chips.forEach { (value, chip) ->
            chip.isChecked = value == selectedFrequency
            chip.setOnClickListener {
                selectedFrequency = value
                chips.values.forEach { it.isChecked = it === chip }
            }
        }
    }

    private fun setupReminderControls(binding: FragmentTaskFormBinding) {
        binding.switchReminderEnabled.setOnCheckedChangeListener(null)
        binding.switchReminderEnabled.isChecked = reminderEnabled
        binding.reminderOffsetSection.isVisible = reminderEnabled
        binding.switchReminderEnabled.setOnCheckedChangeListener { _, checked ->
            reminderEnabled = checked
            binding.reminderOffsetSection.isVisible = checked
        }

        val offsetChips = mapOf(
            0 to binding.chipOffset0,
            5 to binding.chipOffset5,
            15 to binding.chipOffset15,
            30 to binding.chipOffset30,
            60 to binding.chipOffset60
        )
        offsetChips.forEach { (value, chip) ->
            chip.isChecked = value == reminderMinutesBefore
            chip.setOnClickListener {
                reminderMinutesBefore = value
                offsetChips.values.forEach { it.isChecked = it === chip }
            }
        }
    }

    private fun onPetsChanged(pets: List<PetEntity>) {
        val binding = binding ?: return
        if (petsBuilt && binding.spTaskPet.childCount == pets.size) {
            for (i in 0 until binding.spTaskPet.childCount) {
                val chip = binding.spTaskPet.getChildAt(i) as Chip
                chip.isChecked = chip.tag == selectedPetId
            }
            return
        }
        binding.spTaskPet.removeAllViews()
        val chipContext = android.view.ContextThemeWrapper(requireContext(), R.style.Widget_PetCare_Chip_Filter)
        pets.forEach { pet ->
            val chip = Chip(chipContext).apply {
                text = pet.name
                tag = pet.id
                isCheckable = true
                isChecked = pet.id == selectedPetId
                setOnClickListener {
                    selectedPetId = pet.id
                    for (i in 0 until binding.spTaskPet.childCount) {
                        (binding.spTaskPet.getChildAt(i) as Chip).isChecked = binding.spTaskPet.getChildAt(i).tag == selectedPetId
                    }
                }
            }
            binding.spTaskPet.addView(chip)
        }
        petsBuilt = true
    }

    private fun onTasksChanged(tasks: List<CareTaskEntity>) {
        val id = taskId ?: return
        if (fieldsPopulatedForEdit) return
        val task = tasks.find { it.id == id } ?: return
        populateFieldsForEdit(task)
        fieldsPopulatedForEdit = true
    }

    private fun populateFieldsForEdit(task: CareTaskEntity) {
        val binding = binding ?: return
        binding.etTaskTitle.setText(task.title)
        binding.etTaskSupplies.setText(task.requiredSupplies)
        binding.etTaskNotes.setText(task.notes)

        selectedPetId = task.petId
        selectedRoutineId = task.routineId
        selectedCategory = task.category
        selectedFrequency = task.frequency
        dueAtMillis = task.dueAt
        reminderEnabled = task.reminderEnabled
        reminderMinutesBefore = task.reminderMinutesBefore
        imageUri = task.imageUri

        binding.etTaskCategory.setText(getString(categoryLabelRes.getValue(selectedCategory)), false)
        setupFrequencyChips(binding)
        setupReminderControls(binding)
        onPetsChanged(petViewModel.pets.value)
        renderDateTime()
        renderImage()
    }

    private fun showDatePicker() {
        val current = Calendar.getInstance().apply { timeInMillis = dueAtMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply { timeInMillis = dueAtMillis }
                cal.set(year, month, dayOfMonth)
                dueAtMillis = cal.timeInMillis
                renderDateTime()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val current = Calendar.getInstance().apply { timeInMillis = dueAtMillis }
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply { timeInMillis = dueAtMillis }
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                dueAtMillis = cal.timeInMillis
                renderDateTime()
            },
            current.get(Calendar.HOUR_OF_DAY),
            current.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun renderDateTime() {
        val binding = binding ?: return
        binding.btnTaskDate.setText(dateFormat.format(Date(dueAtMillis)))
        binding.btnTaskTime.setText(timeFormat.format(Date(dueAtMillis)))
    }

    private fun renderImage() {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        val uri = imageUri
        if (uri.isNullOrBlank()) {
            binding.ivTaskImage.isVisible = false
            binding.ivTaskImagePlaceholderIcon.isVisible = true
            return
        }
        imageLoadJob = CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    if (uri.startsWith("/")) {
                        BitmapFactory.decodeFile(uri)
                    } else {
                        requireContext().contentResolver.openInputStream(Uri.parse(uri))?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                binding.ivTaskImage.setImageBitmap(bitmap)
                binding.ivTaskImage.isVisible = true
                binding.ivTaskImagePlaceholderIcon.isVisible = false
            } else {
                binding.ivTaskImage.isVisible = false
                binding.ivTaskImagePlaceholderIcon.isVisible = true
            }
        }
    }

    private fun save() {
        val binding = binding ?: return
        binding.errorTaskPet.isVisible = false
        binding.errorTaskTitle.isVisible = false

        val petId = selectedPetId
        if (petId == null) {
            binding.errorTaskPet.text = getString(R.string.error_select_pet)
            binding.errorTaskPet.isVisible = true
            return
        }
        val title = binding.etTaskTitle.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            binding.errorTaskTitle.text = getString(R.string.error_task_title_required)
            binding.errorTaskTitle.isVisible = true
            return
        }

        val task = CareTaskEntity(
            id = taskId ?: 0,
            userId = "",
            petId = petId,
            routineId = selectedRoutineId,
            title = title,
            category = selectedCategory,
            dueAt = dueAtMillis,
            frequency = selectedFrequency,
            requiredSupplies = binding.etTaskSupplies.text?.toString()?.trim().orEmpty(),
            notes = binding.etTaskNotes.text?.toString()?.trim().orEmpty(),
            imageUri = imageUri,
            reminderEnabled = reminderEnabled,
            reminderMinutesBefore = reminderMinutesBefore
        )

        if (taskId != null) {
            viewModel.updateTask(task)
        } else {
            viewModel.addTask(task)
        }
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageLoadJob?.cancel()
        binding = null
    }

    companion object {
        private const val ARG_TASK_ID = "task_id"
        private const val ARG_PRESET_PET_ID = "preset_pet_id"
        private const val ARG_PRESET_ROUTINE_ID = "preset_routine_id"

        fun newInstance(taskId: Long?, presetPetId: Long?, presetRoutineId: Long?): TaskFormFragment =
            TaskFormFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TASK_ID, taskId ?: -1L)
                    putLong(ARG_PRESET_PET_ID, presetPetId ?: -1L)
                    putLong(ARG_PRESET_ROUTINE_ID, presetRoutineId ?: -1L)
                }
            }
    }
}
