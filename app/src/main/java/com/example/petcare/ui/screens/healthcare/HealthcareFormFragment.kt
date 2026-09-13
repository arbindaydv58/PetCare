package com.example.petcare.ui.screens.healthcare

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
import com.example.petcare.databinding.FragmentHealthcareFormBinding
import com.example.petcare.ui.viewmodel.HealthcareFormState
import com.example.petcare.ui.viewmodel.HealthcareRecordViewModel
import com.example.petcare.ui.viewmodel.HealthcareUiState
import com.example.petcare.ui.viewmodel.PetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HealthcareFormFragment : Fragment() {

    private val viewModel: HealthcareRecordViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentHealthcareFormBinding? = null

    private var recordId: Long? = null
    private var presetPetId: Long? = null
    private var effectivePetId: Long? = null
    private var fieldsPopulatedForEdit = false
    private var successHandled = false
    private var imageLoadJob: Job? = null

    private var selectedType: String = "VETERINARY_APPOINTMENT"
    private var dateMillis: Long = System.currentTimeMillis()
    private var timeMillis: Long? = null
    private var nextDueMillis: Long? = null
    private var reminderEnabled: Boolean = false
    private var createCareTask: Boolean = false
    private var imageUri: String? = null

    private val recordTypes = listOf(
        "VETERINARY_APPOINTMENT", "VACCINATION", "MEDICATION",
        "MEDICAL_CONDITION", "ALLERGY", "HEALTH_CHECK", "TREATMENT", "OTHER"
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
        val binding = FragmentHealthcareFormBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        recordId = arguments?.getLong(ARG_RECORD_ID, -1L)?.takeIf { it > 0 }
        presetPetId = arguments?.getLong(ARG_PRESET_PET_ID, -1L)?.takeIf { it > 0 }
        effectivePetId = presetPetId

        binding.healthcareFormTitle.text = getString(if (recordId != null) R.string.edit_healthcare_title else R.string.add_healthcare_title)
        binding.btnSaveHealthcare.text = getString(if (recordId != null) R.string.action_update_record else R.string.action_save_medical_record)

        if (recordId != null) {
            viewModel.loadRecord(recordId!!)
        } else {
            viewModel.resetForm()
            fieldsPopulatedForEdit = true
        }

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnSelectHealthcareImage.setOnClickListener { photoPicker.launch("image/*") }
        binding.btnHealthcareDate.setOnClickListener { showDatePicker() }
        binding.btnHealthcareTime.setOnClickListener { showTimePicker() }
        binding.btnHealthcareNextDue.setOnClickListener { showNextDuePicker() }
        binding.checkboxNextDueReminder.setOnCheckedChangeListener { _, checked -> reminderEnabled = checked }
        binding.checkboxCreateCareTask.setOnCheckedChangeListener { _, checked -> createCareTask = checked }

        setupTypeDropdown(binding)
        binding.btnSaveHealthcare.setOnClickListener { save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { petViewModel.pets.collect { renderPetBanner() } }
                launch { viewModel.uiState.collect { state -> onUiStateChanged(state) } }
            }
        }

        renderDateTime()
        renderImage()
        applyTypeVisibility()
    }

    private fun setupTypeDropdown(binding: FragmentHealthcareFormBinding) {
        val labels = recordTypes.map { humanizeHealthcareType(it) }
        binding.spHealthcareType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
        binding.spHealthcareType.setText(humanizeHealthcareType(selectedType), false)
        binding.spHealthcareType.setOnItemClickListener { _, _, position, _ ->
            selectedType = recordTypes[position]
            applyTypeVisibility()
        }
    }

    private fun applyTypeVisibility() {
        val binding = binding ?: return
        binding.sectionMedicationInfo.isVisible = selectedType == "MEDICATION"
        binding.sectionVisitInfo.isVisible = selectedType == "VETERINARY_APPOINTMENT"
        binding.createCareTaskRow.isVisible = recordId == null && selectedType == "MEDICATION"
    }

    private fun renderPetBanner() {
        val binding = binding ?: return
        val pet = effectivePetId?.let { id -> petViewModel.pets.value.find { it.id == id } }
        if (pet != null) {
            binding.recordingForCard.isVisible = true
            binding.recordingForText.text = getString(R.string.recording_for_format, pet.name)
        } else {
            binding.recordingForCard.isVisible = false
        }
    }

    private fun onUiStateChanged(state: HealthcareUiState) {
        val binding = binding ?: return
        binding.btnSaveHealthcare.isEnabled = !state.isLoading
        binding.healthcareFormLoading.isVisible = state.isLoading

        val id = recordId
        if (id != null && !fieldsPopulatedForEdit) {
            val record = state.selectedRecord
            if (record != null && record.id == id) {
                effectivePetId = record.petId
                populateFieldsForEdit(viewModel.formState.value)
                fieldsPopulatedForEdit = true
            }
        }

        if (state.successMessage != null && !successHandled) {
            successHandled = true
            viewModel.clearMessages()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun populateFieldsForEdit(form: HealthcareFormState) {
        val binding = binding ?: return

        binding.etHealthcareTitle.setText(form.title)
        binding.etHealthcareMedication.setText(form.medicationName)
        binding.etHealthcareDosage.setText(form.dosage)
        binding.etHealthcareFrequency.setText(form.frequency)
        binding.etHealthcareDuration.setText(form.duration)
        binding.etHealthcareVeterinarian.setText(form.veterinarianName)
        binding.etHealthcareClinic.setText(form.clinicName)
        binding.etHealthcareNotes.setText(form.notes)

        selectedType = form.type
        dateMillis = form.date
        timeMillis = form.time
        nextDueMillis = form.nextDueDate
        reminderEnabled = form.reminderEnabled
        imageUri = form.imageUri

        binding.spHealthcareType.setText(humanizeHealthcareType(selectedType), false)
        binding.checkboxNextDueReminder.setOnCheckedChangeListener(null)
        binding.checkboxNextDueReminder.isChecked = reminderEnabled
        binding.checkboxNextDueReminder.setOnCheckedChangeListener { _, checked -> reminderEnabled = checked }

        applyTypeVisibility()
        renderPetBanner()
        renderDateTime()
        renderImage()
    }

    private fun showDatePicker() {
        val current = Calendar.getInstance().apply { timeInMillis = dateMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                cal.set(year, month, dayOfMonth)
                dateMillis = cal.timeInMillis
                renderDateTime()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val current = Calendar.getInstance().apply { timeInMillis = timeMillis ?: System.currentTimeMillis() }
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply { timeInMillis = timeMillis ?: System.currentTimeMillis() }
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                timeMillis = cal.timeInMillis
                renderDateTime()
            },
            current.get(Calendar.HOUR_OF_DAY),
            current.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showNextDuePicker() {
        val current = Calendar.getInstance().apply { timeInMillis = nextDueMillis ?: System.currentTimeMillis() }
        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply { timeInMillis = nextDueMillis ?: System.currentTimeMillis() }
                cal.set(year, month, dayOfMonth)
                nextDueMillis = cal.timeInMillis
                renderDateTime()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.action_clear)) { _, _ ->
            nextDueMillis = null
            reminderEnabled = false
            renderDateTime()
        }
        dialog.show()
    }

    private fun renderDateTime() {
        val binding = binding ?: return
        binding.btnHealthcareDate.setText(dateFormat.format(Date(dateMillis)))
        binding.btnHealthcareTime.setText(timeMillis?.let { timeFormat.format(Date(it)) } ?: getString(R.string.time_not_set))
        binding.btnHealthcareNextDue.setText(nextDueMillis?.let { dateFormat.format(Date(it)) } ?: "")
        binding.reminderRow.isVisible = nextDueMillis != null
    }

    private fun renderImage() {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        val uri = imageUri
        if (uri.isNullOrBlank()) {
            binding.ivHealthcareImage.isVisible = false
            binding.ivHealthcareImagePlaceholderIcon.isVisible = true
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
                binding.ivHealthcareImage.setImageBitmap(bitmap)
                binding.ivHealthcareImage.isVisible = true
                binding.ivHealthcareImagePlaceholderIcon.isVisible = false
            } else {
                binding.ivHealthcareImage.isVisible = false
                binding.ivHealthcareImagePlaceholderIcon.isVisible = true
            }
        }
    }

    private fun save() {
        val binding = binding ?: return
        binding.errorHealthcareTitle.isVisible = false

        val petId = effectivePetId
        if (petId == null) {
            return
        }
        val title = binding.etHealthcareTitle.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            binding.errorHealthcareTitle.text = getString(R.string.error_healthcare_title_required)
            binding.errorHealthcareTitle.isVisible = true
            return
        }

        val form = HealthcareFormState(
            type = selectedType,
            title = title,
            description = "",
            date = dateMillis,
            time = timeMillis,
            veterinarianName = binding.etHealthcareVeterinarian.text?.toString()?.trim().orEmpty(),
            clinicName = binding.etHealthcareClinic.text?.toString()?.trim().orEmpty(),
            medicationName = binding.etHealthcareMedication.text?.toString()?.trim().orEmpty(),
            dosage = binding.etHealthcareDosage.text?.toString()?.trim().orEmpty(),
            frequency = binding.etHealthcareFrequency.text?.toString()?.trim().orEmpty(),
            duration = binding.etHealthcareDuration.text?.toString()?.trim().orEmpty(),
            nextDueDate = nextDueMillis,
            notes = binding.etHealthcareNotes.text?.toString()?.trim().orEmpty(),
            imageUri = imageUri,
            reminderEnabled = reminderEnabled,
            createCareTask = createCareTask
        )
        viewModel.updateForm(form)
        viewModel.saveRecord(petId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageLoadJob?.cancel()
        binding = null
    }

    companion object {
        private const val ARG_RECORD_ID = "record_id"
        private const val ARG_PRESET_PET_ID = "preset_pet_id"

        fun newInstance(recordId: Long?, presetPetId: Long?): HealthcareFormFragment =
            HealthcareFormFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_RECORD_ID, recordId ?: -1L)
                    putLong(ARG_PRESET_PET_ID, presetPetId ?: -1L)
                }
            }
    }
}
