package com.example.petcare.ui.screens.pet

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.R
import com.example.petcare.data.local.entity.CareRoutineEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentRoutineFormBinding
import com.example.petcare.ui.viewmodel.CareRoutineViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import com.example.petcare.ui.viewmodel.RoutineUiState
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

class RoutineFormFragment : Fragment() {

    private val viewModel: CareRoutineViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentRoutineFormBinding? = null

    private var routineId: Long? = null
    private var presetPetId: Long? = null
    private var fieldsPopulatedForEdit = false
    private var successHandled = false
    private var imageLoadJob: Job? = null

    private var selectedPetId: Long? = null
    private var selectedFrequency: String = "DAILY"
    private var startDateMillis: Long? = System.currentTimeMillis()
    private var endDateMillis: Long? = null
    private var imageUri: String? = null
    private var petsBuilt = false

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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
        val binding = FragmentRoutineFormBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        routineId = arguments?.getLong(ARG_ROUTINE_ID, -1L)?.takeIf { it > 0 }
        presetPetId = arguments?.getLong(ARG_PRESET_PET_ID, -1L)?.takeIf { it > 0 }
        selectedPetId = presetPetId

        binding.routineFormTitle.text = getString(if (routineId != null) R.string.edit_routine_title else R.string.add_routine_title)
        binding.btnSaveRoutine.text = getString(if (routineId != null) R.string.action_update_routine else R.string.action_create_routine)

        if (routineId == null) {
            fieldsPopulatedForEdit = true
        }

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnSelectRoutineImage.setOnClickListener { photoPicker.launch("image/*") }
        binding.btnStartDate.setOnClickListener { showDatePicker(isStart = true) }
        binding.btnEndDate.setOnClickListener { showDatePicker(isStart = false) }

        setupFrequencyChips(binding)

        binding.btnSaveRoutine.setOnClickListener { save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { petViewModel.pets.collect { pets -> onPetsChanged(pets) } }
                launch { viewModel.routines.collect { routines -> onRoutinesChanged(routines) } }
                launch { viewModel.uiState.collect { state -> onUiStateChanged(state) } }
            }
        }

        renderDates()
        renderImage()
    }

    private fun setupFrequencyChips(binding: FragmentRoutineFormBinding) {
        val chips = mapOf(
            "DAILY" to binding.chipFreqDaily,
            "WEEKLY" to binding.chipFreqWeekly,
            "MONTHLY" to binding.chipFreqMonthly,
            "ONCE" to binding.chipFreqOnce
        )
        chips.forEach { (value, chip) ->
            chip.isChecked = value == selectedFrequency
            chip.setOnClickListener {
                selectedFrequency = value
                chips.values.forEach { it.isChecked = it === chip }
            }
        }
    }

    private fun onPetsChanged(pets: List<PetEntity>) {
        val binding = binding ?: return
        if (petsBuilt && binding.spPet.childCount == pets.size) {
            for (i in 0 until binding.spPet.childCount) {
                val chip = binding.spPet.getChildAt(i) as Chip
                chip.isChecked = chip.tag == selectedPetId
            }
            return
        }
        binding.spPet.removeAllViews()
        val chipContext = android.view.ContextThemeWrapper(requireContext(), R.style.Widget_PetCare_Chip_Filter)
        pets.forEach { pet ->
            val chip = Chip(chipContext).apply {
                text = pet.name
                tag = pet.id
                isCheckable = true
                isChecked = pet.id == selectedPetId
                setOnClickListener {
                    selectedPetId = pet.id
                    for (i in 0 until binding.spPet.childCount) {
                        (binding.spPet.getChildAt(i) as Chip).isChecked = binding.spPet.getChildAt(i).tag == selectedPetId
                    }
                }
            }
            binding.spPet.addView(chip)
        }
        petsBuilt = true
    }

    private fun onRoutinesChanged(routines: List<CareRoutineEntity>) {
        val id = routineId ?: return
        if (fieldsPopulatedForEdit) return
        val routine = routines.find { it.id == id } ?: return
        populateFieldsForEdit(routine)
        fieldsPopulatedForEdit = true
    }

    private fun populateFieldsForEdit(routine: CareRoutineEntity) {
        val binding = binding ?: return
        binding.etRoutineName.setText(routine.name)
        binding.etRoutineDescription.setText(routine.description)
        binding.etRequiredSupplies.setText(routine.requiredSupplies)
        binding.etRoutineNotes.setText(routine.notes)
        selectedPetId = routine.petId
        selectedFrequency = routine.frequency
        startDateMillis = routine.startDate
        endDateMillis = routine.endDate
        imageUri = routine.imageUri

        setupFrequencyChips(binding)
        onPetsChanged(petViewModel.pets.value)
        renderDates()
        renderImage()
    }

    private fun showDatePicker(isStart: Boolean) {
        val current = Calendar.getInstance()
        (if (isStart) startDateMillis else endDateMillis)?.let { current.timeInMillis = it }

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (isStart) startDateMillis = selected.timeInMillis else endDateMillis = selected.timeInMillis
                renderDates()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        )
        if (!isStart) {
            dialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.action_clear)) { _, _ ->
                endDateMillis = null
                renderDates()
            }
        }
        dialog.show()
    }

    private fun renderDates() {
        val binding = binding ?: return
        binding.btnStartDate.setText(startDateMillis?.let { dateFormat.format(Date(it)) } ?: "")
        binding.btnEndDate.setText(endDateMillis?.let { dateFormat.format(Date(it)) } ?: getString(R.string.end_date_ongoing))
    }

    private fun renderImage() {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        val uri = imageUri
        if (uri.isNullOrBlank()) {
            binding.ivRoutineImage.isVisible = false
            binding.ivRoutineImagePlaceholderIcon.isVisible = true
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
                binding.ivRoutineImage.setImageBitmap(bitmap)
                binding.ivRoutineImage.isVisible = true
                binding.ivRoutineImagePlaceholderIcon.isVisible = false
            } else {
                binding.ivRoutineImage.isVisible = false
                binding.ivRoutineImagePlaceholderIcon.isVisible = true
            }
        }
    }

    private fun save() {
        val binding = binding ?: return
        binding.errorPet.isVisible = false
        binding.errorRoutineName.isVisible = false

        val petId = selectedPetId
        if (petId == null) {
            binding.errorPet.text = getString(R.string.error_select_pet)
            binding.errorPet.isVisible = true
            return
        }
        val name = binding.etRoutineName.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            binding.errorRoutineName.text = getString(R.string.error_routine_name_required)
            binding.errorRoutineName.isVisible = true
            return
        }

        val routine = CareRoutineEntity(
            id = routineId ?: 0,
            userId = "",
            petId = petId,
            name = name,
            description = binding.etRoutineDescription.text?.toString()?.trim().orEmpty(),
            frequency = selectedFrequency,
            startDate = startDateMillis ?: System.currentTimeMillis(),
            endDate = endDateMillis,
            requiredSupplies = binding.etRequiredSupplies.text?.toString()?.trim().orEmpty(),
            notes = binding.etRoutineNotes.text?.toString()?.trim().orEmpty(),
            imageUri = imageUri
        )

        if (routineId != null) {
            viewModel.updateRoutine(routine)
        } else {
            viewModel.addRoutine(routine)
        }
    }

    private fun onUiStateChanged(state: RoutineUiState) {
        val binding = binding ?: return
        binding.btnSaveRoutine.isEnabled = !state.isLoading
        binding.routineFormLoading.isVisible = state.isLoading

        if (state.successMessage != null && !successHandled) {
            successHandled = true
            viewModel.clearMessages()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageLoadJob?.cancel()
        binding = null
    }

    companion object {
        private const val ARG_ROUTINE_ID = "routine_id"
        private const val ARG_PRESET_PET_ID = "preset_pet_id"

        fun newInstance(routineId: Long?, presetPetId: Long?): RoutineFormFragment =
            RoutineFormFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ROUTINE_ID, routineId ?: -1L)
                    putLong(ARG_PRESET_PET_ID, presetPetId ?: -1L)
                }
            }
    }
}
