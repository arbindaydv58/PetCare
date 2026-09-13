package com.example.petcare.ui.screens.healthcare

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.R
import com.example.petcare.data.local.entity.HealthcareRecordEntity
import com.example.petcare.databinding.FragmentHealthcareDetailsBinding
import com.example.petcare.ui.viewmodel.HealthcareRecordViewModel
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

class HealthcareDetailsFragment : Fragment() {

    private val viewModel: HealthcareRecordViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentHealthcareDetailsBinding? = null

    private var recordId: Long = 0L
    private var imageLoadJob: Job? = null
    private var currentRecord: HealthcareRecordEntity? = null

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHealthcareDetailsBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        recordId = arguments?.getLong(ARG_RECORD_ID) ?: 0L

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnEditHealthcare.setOnClickListener {
            (requireActivity() as? HealthcareActivity)?.pushFragment(HealthcareFormFragment.newInstance(recordId, null))
        }
        binding.btnDeleteHealthcare.setOnClickListener { confirmDelete() }

        viewModel.loadRecord(recordId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> render(state.selectedRecord, state.isLoading) } }
                launch { petViewModel.pets.collect { render(currentRecord, viewModel.uiState.value.isLoading) } }
            }
        }
    }

    private fun render(record: HealthcareRecordEntity?, isLoading: Boolean) {
        val binding = binding ?: return
        currentRecord = record

        binding.healthcareDetailsLoading.isVisible = isLoading && record == null
        binding.healthcareDetailsError.isVisible = !isLoading && record == null
        binding.healthcareDetailsContent.isVisible = record != null
        binding.healthcareDetailsActions.isVisible = record != null
        if (record == null) return

        binding.tvHealthcareType.text = humanizeHealthcareType(record.type)
        binding.tvHealthcareTitle.text = record.title
        binding.healthcareDetailsIcon.setImageResource(healthcareIconFor(record.type))
        binding.healthcareDetailsIconContainer.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), healthcareColorFor(record.type))
        )

        val petName = petViewModel.pets.value.find { it.id == record.petId }?.name ?: getString(R.string.unknown_pet)
        bindInfoRow(binding.rowHealthcarePet.root, R.string.detail_label_pet_name, petName)
        bindInfoRow(binding.rowHealthcareDate.root, R.string.detail_label_visit_date, dateFormat.format(Date(record.date)))

        val time = record.time
        binding.rowHealthcareTimeContainer.isVisible = time != null
        if (time != null) {
            bindInfoRow(binding.rowHealthcareTime.root, R.string.detail_label_visit_time, timeFormat.format(Date(time)))
        }

        val nextDue = record.nextDueDate
        binding.rowHealthcareNextDueContainer.isVisible = nextDue != null
        if (nextDue != null) {
            bindInfoRow(binding.rowHealthcareNextDue.root, R.string.detail_label_next_due_date, dateFormat.format(Date(nextDue)))
            val valueColor = if (nextDue < System.currentTimeMillis()) R.color.error_red else R.color.primary_coral
            binding.rowHealthcareNextDue.value.setTextColor(ContextCompat.getColor(requireContext(), valueColor))
        }

        val hasVet = !record.veterinarianName.isNullOrBlank()
        val hasClinic = !record.clinicName.isNullOrBlank()
        val hasMedication = !record.medicationName.isNullOrBlank()
        binding.sectionDetailsMedical.isVisible = hasVet || hasClinic || hasMedication

        binding.rowHealthcareVetContainer.isVisible = hasVet
        if (hasVet) bindInfoRow(binding.rowHealthcareVet.root, R.string.label_veterinarian, record.veterinarianName)

        binding.rowHealthcareClinicContainer.isVisible = hasClinic
        if (hasClinic) bindInfoRow(binding.rowHealthcareClinic.root, R.string.detail_label_clinic, record.clinicName)

        binding.rowHealthcareMedicationContainer.isVisible = hasMedication
        if (hasMedication) {
            val notSpecified = getString(R.string.not_specified)
            bindInfoRow(binding.rowHealthcareMedication.root, R.string.detail_label_medication, record.medicationName)
            bindInfoRow(binding.rowHealthcareDosage.root, R.string.label_dosage, record.dosage?.takeIf { it.isNotBlank() } ?: notSpecified)
            bindInfoRow(binding.rowHealthcareFrequency.root, R.string.label_frequency, record.frequency?.takeIf { it.isNotBlank() } ?: notSpecified)
            bindInfoRow(binding.rowHealthcareDuration.root, R.string.label_duration, record.duration?.takeIf { it.isNotBlank() } ?: notSpecified)
        }

        val hasDescription = !record.description.isNullOrBlank()
        val hasNotes = !record.notes.isNullOrBlank()
        binding.sectionDetailsNotes.isVisible = hasDescription || hasNotes
        binding.tvHealthcareDescription.isVisible = hasDescription
        if (hasDescription) binding.tvHealthcareDescription.text = record.description
        binding.tvHealthcareNotes.isVisible = hasNotes
        if (hasNotes) binding.tvHealthcareNotes.text = record.notes

        renderImage(record.imageUri)
    }

    private fun bindInfoRow(row: View, labelRes: Int, value: String) {
        row.findViewById<android.widget.TextView>(R.id.label).text = getString(labelRes)
        row.findViewById<android.widget.TextView>(R.id.value).text = value
    }

    private fun renderImage(imageUri: String?) {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        if (imageUri.isNullOrBlank()) {
            binding.ivHealthcareImage.isVisible = false
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
                binding.ivHealthcareImage.setImageBitmap(bitmap)
                binding.ivHealthcareImage.isVisible = true
            } else {
                binding.ivHealthcareImage.isVisible = false
            }
        }
    }

    private fun confirmDelete() {
        val record = currentRecord ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_healthcare_dialog_title)
            .setMessage(R.string.delete_healthcare_dialog_message)
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                viewModel.deleteRecord(record)
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
        private const val ARG_RECORD_ID = "record_id"

        fun newInstance(recordId: Long): HealthcareDetailsFragment = HealthcareDetailsFragment().apply {
            arguments = Bundle().apply { putLong(ARG_RECORD_ID, recordId) }
        }
    }
}
