package com.example.petcare.ui.screens.pet

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.R
import com.example.petcare.databinding.FragmentPetFormBinding
import com.example.petcare.ui.viewmodel.PetFormState
import com.example.petcare.ui.viewmodel.PetUiState
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

class PetFormFragment : Fragment() {

    private val viewModel: PetViewModel by viewModels()
    private var binding: FragmentPetFormBinding? = null

    private var petId: Long? = null
    private var fieldsPopulatedForEdit = false
    private var successHandled = false
    private var imageLoadJob: Job? = null

    private val speciesOptions = listOf("Dog", "Cat", "Bird", "Rabbit", "Fish", "Other")
    private val genderOptions = listOf("Male", "Female", "Unknown")
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.updateForm(viewModel.formState.value.copy(imageUri = uri.toString()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPetFormBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        petId = arguments?.getLong(ARG_PET_ID, -1L)?.takeIf { it > 0 }
        binding.petFormTitle.text = getString(if (petId != null) R.string.edit_pet_title else R.string.add_pet_title)
        binding.btnSavePet.text = getString(if (petId != null) R.string.action_update_profile else R.string.action_create_profile)

        if (petId != null) {
            viewModel.loadPet(petId!!)
        } else {
            viewModel.resetForm()
            fieldsPopulatedForEdit = true
        }

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnSelectPetImage.setOnClickListener { photoPicker.launch("image/*") }

        setupDropdown(binding.etSpecies.also { it.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, speciesOptions)) }) { selected ->
            viewModel.updateForm(viewModel.formState.value.copy(species = selected))
        }
        setupDropdown(binding.etGender.also { it.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, genderOptions)) }) { selected ->
            viewModel.updateForm(viewModel.formState.value.copy(gender = selected))
        }

        binding.etDob.setOnClickListener { showDatePicker() }

        bindTextField(binding.etPetName) { form, text -> form.copy(name = text) }
        bindTextField(binding.etBreed) { form, text -> form.copy(breed = text) }
        bindTextField(binding.etWeight) { form, text -> form.copy(weight = text) }
        bindTextField(binding.etDietaryPreferences) { form, text -> form.copy(dietaryPreferences = text) }
        bindTextField(binding.etAllergies) { form, text -> form.copy(allergies = text) }
        bindTextField(binding.etVaccinationHistory) { form, text -> form.copy(vaccinationHistory = text) }
        bindTextField(binding.etFavouriteToys) { form, text -> form.copy(favouriteToys = text) }
        bindTextField(binding.etMedicalNotes) { form, text -> form.copy(medicalNotes = text) }
        bindTextField(binding.etGeneralNotes) { form, text -> form.copy(generalNotes = text) }

        binding.btnSavePet.setOnClickListener { viewModel.savePet() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.formState.collect { form -> onFormStateChanged(form) } }
                launch { viewModel.petUiState.collect { state -> onUiStateChanged(state) } }
            }
        }
    }

    private fun setupDropdown(view: android.widget.AutoCompleteTextView, onSelected: (String) -> Unit) {
        view.setOnItemClickListener { parent, _, position, _ ->
            onSelected(parent.getItemAtPosition(position) as String)
        }
    }

    private inline fun bindTextField(
        editText: com.google.android.material.textfield.TextInputEditText,
        crossinline update: (PetFormState, String) -> PetFormState
    ) {
        editText.doOnTextChanged { text, _, _, _ ->
            if (editText.isFocused) {
                viewModel.updateForm(update(viewModel.formState.value, text?.toString() ?: ""))
            }
        }
    }

    private fun onFormStateChanged(form: PetFormState) {
        val binding = binding ?: return

        if (petId != null && !fieldsPopulatedForEdit && viewModel.selectedPet.value != null) {
            populateFieldsForEdit(form)
            fieldsPopulatedForEdit = true
        }

        binding.etDob.setText(form.dateOfBirthMillis?.let { dateFormat.format(Date(it)) } ?: "")
        renderImage(form.imageUri)
    }

    private fun populateFieldsForEdit(form: PetFormState) {
        val binding = binding ?: return
        binding.etPetName.setText(form.name)
        binding.etSpecies.setText(form.species, false)
        binding.etBreed.setText(form.breed)
        binding.etWeight.setText(form.weight)
        binding.etGender.setText(form.gender, false)
        binding.etDietaryPreferences.setText(form.dietaryPreferences)
        binding.etAllergies.setText(form.allergies)
        binding.etVaccinationHistory.setText(form.vaccinationHistory)
        binding.etFavouriteToys.setText(form.favouriteToys)
        binding.etMedicalNotes.setText(form.medicalNotes)
        binding.etGeneralNotes.setText(form.generalNotes)
    }

    private fun renderImage(imageUri: String?) {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        if (imageUri.isNullOrBlank()) {
            binding.ivPetImage.isVisible = false
            binding.ivPetImagePlaceholderIcon.isVisible = true
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
                binding.ivPetImage.setImageBitmap(bitmap)
                binding.ivPetImage.isVisible = true
                binding.ivPetImagePlaceholderIcon.isVisible = false
            } else {
                binding.ivPetImage.isVisible = false
                binding.ivPetImagePlaceholderIcon.isVisible = true
            }
        }
    }

    private fun showDatePicker() {
        val current = Calendar.getInstance()
        viewModel.formState.value.dateOfBirthMillis?.let { current.timeInMillis = it }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.updateForm(viewModel.formState.value.copy(dateOfBirthMillis = selected.timeInMillis))
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setCancelable(true)
        }.show()
    }

    private fun onUiStateChanged(state: PetUiState) {
        val binding = binding ?: return
        binding.btnSavePet.isEnabled = !state.isLoading
        binding.petFormLoading.isVisible = state.isLoading

        binding.errorPetName.isVisible = false
        binding.errorSpecies.isVisible = false
        binding.errorWeight.isVisible = false
        when (state.errorMessage) {
            getString(R.string.error_pet_name_required) -> binding.errorPetName.isVisible = true
            getString(R.string.error_species_required) -> binding.errorSpecies.isVisible = true
            getString(R.string.error_weight_invalid) -> binding.errorWeight.isVisible = true
        }

        if (state.successMessage != null && !successHandled) {
            successHandled = true
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageLoadJob?.cancel()
        binding = null
    }

    companion object {
        private const val ARG_PET_ID = "pet_id"

        fun newInstance(petId: Long?): PetFormFragment = PetFormFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_PET_ID, petId ?: -1L)
            }
        }
    }
}
