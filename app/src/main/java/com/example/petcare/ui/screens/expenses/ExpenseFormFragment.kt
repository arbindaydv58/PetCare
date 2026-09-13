package com.example.petcare.ui.screens.expenses

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.R
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentExpenseFormBinding
import com.example.petcare.ui.viewmodel.ExpenseFormState
import com.example.petcare.ui.viewmodel.ExpenseUiState
import com.example.petcare.ui.viewmodel.ExpenseViewModel
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

class ExpenseFormFragment : Fragment() {

    private val viewModel: ExpenseViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentExpenseFormBinding? = null

    private var expenseId: Long? = null
    private var presetPetId: Long? = null
    private var effectivePetId: Long? = null
    private var fieldsPopulatedForEdit = false
    private var successHandled = false
    private var imageLoadJob: Job? = null
    private var petsBuilt = false

    private var selectedCategory: String = "Food"
    private var selectedPaymentMethod: String = "Cash"
    private var dateMillis: Long = System.currentTimeMillis()
    private var imageUri: String? = null

    private val categories = listOf("Food", "Veterinary", "Medication", "Grooming", "Toys", "Accessories", "Other")
    private val paymentMethods = listOf("Cash", "Card", "Bank Transfer", "Other")

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
        val binding = FragmentExpenseFormBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        expenseId = arguments?.getLong(ARG_EXPENSE_ID, -1L)?.takeIf { it > 0 }
        presetPetId = arguments?.getLong(ARG_PRESET_PET_ID, -1L)?.takeIf { it > 0 }
        effectivePetId = presetPetId

        binding.expenseFormTitle.text = getString(if (expenseId != null) R.string.edit_expense_title else R.string.add_expense_title)
        binding.btnSaveExpense.text = getString(if (expenseId != null) R.string.action_save_changes else R.string.action_log_expense)

        if (expenseId != null) {
            viewModel.loadExpense(expenseId!!)
        } else {
            viewModel.resetForm()
            fieldsPopulatedForEdit = true
        }

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnSelectExpenseImage.setOnClickListener { photoPicker.launch("image/*") }
        binding.btnExpenseDate.setOnClickListener { showDatePicker() }

        setupCategoryChips(binding)
        setupPaymentMethodDropdown(binding)
        binding.btnSaveExpense.setOnClickListener { save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { petViewModel.pets.collect { pets -> onPetsChanged(pets) } }
                launch { viewModel.uiState.collect { state -> onUiStateChanged(state) } }
            }
        }

        renderDate()
        renderImage()
    }

    private fun setupCategoryChips(binding: FragmentExpenseFormBinding) {
        binding.spExpenseCategory.removeAllViews()
        val chipContext = android.view.ContextThemeWrapper(requireContext(), R.style.Widget_PetCare_Chip_Filter)
        categories.forEach { category ->
            val chip = Chip(chipContext).apply {
                text = category
                tag = category
                isCheckable = true
                isChecked = category == selectedCategory
                setOnClickListener {
                    selectedCategory = category
                    for (i in 0 until binding.spExpenseCategory.childCount) {
                        (binding.spExpenseCategory.getChildAt(i) as Chip).isChecked = binding.spExpenseCategory.getChildAt(i).tag == selectedCategory
                    }
                }
            }
            binding.spExpenseCategory.addView(chip)
        }
    }

    private fun refreshCategoryChips(binding: FragmentExpenseFormBinding) {
        for (i in 0 until binding.spExpenseCategory.childCount) {
            val chip = binding.spExpenseCategory.getChildAt(i) as Chip
            chip.isChecked = chip.tag == selectedCategory
        }
    }

    private fun setupPaymentMethodDropdown(binding: FragmentExpenseFormBinding) {
        binding.spExpensePaymentMethod.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, paymentMethods))
        binding.spExpensePaymentMethod.setText(selectedPaymentMethod, false)
        binding.spExpensePaymentMethod.setOnItemClickListener { _, _, position, _ ->
            selectedPaymentMethod = paymentMethods[position]
        }
    }

    private fun onPetsChanged(pets: List<PetEntity>) {
        val binding = binding ?: return
        val labels = listOf(getString(R.string.general_pet_expense_label)) + pets.map { it.name }
        if (petsBuilt && binding.spExpensePet.adapter?.count == labels.size) {
            renderSelectedPet(pets)
            return
        }
        binding.spExpensePet.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
        binding.spExpensePet.setOnItemClickListener { _, _, position, _ ->
            effectivePetId = if (position == 0) null else pets[position - 1].id
        }
        renderSelectedPet(pets)
        petsBuilt = true
    }

    private fun renderSelectedPet(pets: List<PetEntity>) {
        val binding = binding ?: return
        val name = effectivePetId?.let { id -> pets.find { it.id == id }?.name } ?: getString(R.string.general_pet_expense_label)
        binding.spExpensePet.setText(name, false)
    }

    private fun onUiStateChanged(state: ExpenseUiState) {
        val binding = binding ?: return
        binding.btnSaveExpense.isEnabled = !state.isLoading
        binding.expenseFormLoading.isVisible = state.isLoading

        binding.errorExpenseForm.isVisible = state.errorMessage != null
        binding.errorExpenseForm.text = state.errorMessage ?: ""

        val id = expenseId
        if (id != null && !fieldsPopulatedForEdit) {
            val expense = state.selectedExpense
            if (expense != null && expense.id == id) {
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

    private fun populateFieldsForEdit(form: ExpenseFormState) {
        val binding = binding ?: return
        binding.etExpenseTitle.setText(form.title)
        binding.etExpenseAmount.setText(form.amount)
        binding.etExpenseVendor.setText(form.vendor)
        binding.etExpenseNotes.setText(form.notes)

        selectedCategory = form.category
        selectedPaymentMethod = form.paymentMethod
        dateMillis = form.date
        effectivePetId = form.petId
        imageUri = form.imageUri

        refreshCategoryChips(binding)
        binding.spExpensePaymentMethod.setText(selectedPaymentMethod, false)
        renderSelectedPet(petViewModel.pets.value)
        renderDate()
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
                renderDate()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun renderDate() {
        val binding = binding ?: return
        binding.btnExpenseDate.setText(dateFormat.format(Date(dateMillis)))
    }

    private fun renderImage() {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        val uri = imageUri
        if (uri.isNullOrBlank()) {
            binding.ivExpenseImage.isVisible = false
            binding.ivExpenseImagePlaceholderIcon.isVisible = true
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
                binding.ivExpenseImage.setImageBitmap(bitmap)
                binding.ivExpenseImage.isVisible = true
                binding.ivExpenseImagePlaceholderIcon.isVisible = false
            } else {
                binding.ivExpenseImage.isVisible = false
                binding.ivExpenseImagePlaceholderIcon.isVisible = true
            }
        }
    }

    private fun save() {
        val binding = binding ?: return
        val form = ExpenseFormState(
            title = binding.etExpenseTitle.text?.toString()?.trim().orEmpty(),
            description = "",
            amount = binding.etExpenseAmount.text?.toString()?.trim().orEmpty(),
            category = selectedCategory,
            date = dateMillis,
            petId = effectivePetId,
            paymentMethod = selectedPaymentMethod,
            vendor = binding.etExpenseVendor.text?.toString()?.trim().orEmpty(),
            notes = binding.etExpenseNotes.text?.toString()?.trim().orEmpty(),
            imageUri = imageUri
        )
        viewModel.updateForm(form)
        viewModel.saveExpense()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageLoadJob?.cancel()
        binding = null
    }

    companion object {
        private const val ARG_EXPENSE_ID = "expense_id"
        private const val ARG_PRESET_PET_ID = "preset_pet_id"

        fun newInstance(expenseId: Long?, presetPetId: Long?): ExpenseFormFragment =
            ExpenseFormFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_EXPENSE_ID, expenseId ?: -1L)
                    putLong(ARG_PRESET_PET_ID, presetPetId ?: -1L)
                }
            }
    }
}
