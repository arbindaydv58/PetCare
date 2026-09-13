package com.example.petcare.ui.screens.expenses

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
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.databinding.FragmentExpenseDetailsBinding
import com.example.petcare.ui.viewmodel.ExpenseViewModel
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

class ExpenseDetailsFragment : Fragment() {

    private val viewModel: ExpenseViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentExpenseDetailsBinding? = null

    private var expenseId: Long = 0L
    private var imageLoadJob: Job? = null
    private var currentExpense: ExpenseEntity? = null

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val currencyFormatter = expenseCurrencyFormatter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentExpenseDetailsBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        expenseId = arguments?.getLong(ARG_EXPENSE_ID) ?: 0L

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnEditExpense.setOnClickListener {
            (requireActivity() as? ExpensesActivity)?.pushFragment(ExpenseFormFragment.newInstance(expenseId, null))
        }
        binding.btnDeleteExpense.setOnClickListener { confirmDelete() }

        viewModel.loadExpense(expenseId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> render(state.selectedExpense, state.isLoading) } }
                launch { petViewModel.pets.collect { render(currentExpense, viewModel.uiState.value.isLoading) } }
            }
        }
    }

    private fun render(expense: ExpenseEntity?, isLoading: Boolean) {
        val binding = binding ?: return
        currentExpense = expense

        binding.expenseDetailsLoading.isVisible = isLoading && expense == null
        binding.expenseDetailsError.isVisible = !isLoading && expense == null
        binding.expenseDetailsContent.isVisible = expense != null
        binding.expenseDetailsActions.isVisible = expense != null
        if (expense == null) return

        binding.tvExpenseCategory.text = expense.category.uppercase(Locale.getDefault())
        binding.tvExpenseTitle.text = expense.title
        binding.tvExpenseAmount.text = currencyFormatter.format(expense.amount)
        binding.expenseDetailsIcon.setImageResource(expenseIconFor(expense.category))

        val petName = expense.petId?.let { id -> petViewModel.pets.value.find { it.id == id }?.name }
            ?: getString(R.string.general_pet_expense_label)
        bindInfoRow(binding.rowExpensePet.root, R.string.detail_label_pet, petName)
        bindInfoRow(binding.rowExpenseDate.root, R.string.label_date, dateFormat.format(Date(expense.date)))
        bindInfoRow(binding.rowExpensePayment.root, R.string.detail_label_payment, expense.paymentMethod.ifBlank { getString(R.string.not_specified) })
        bindInfoRow(binding.rowExpenseVendor.root, R.string.detail_label_vendor, expense.vendor.ifBlank { getString(R.string.not_specified) })

        val hasDescription = expense.description.isNotBlank()
        val hasNotes = expense.notes.isNotBlank()
        binding.sectionExpenseNotes.isVisible = hasDescription || hasNotes
        binding.tvExpenseDescription.isVisible = hasDescription
        if (hasDescription) binding.tvExpenseDescription.text = expense.description
        binding.tvExpenseNotes.isVisible = hasNotes
        if (hasNotes) binding.tvExpenseNotes.text = expense.notes

        renderImage(expense.imageUri)
    }

    private fun bindInfoRow(row: View, labelRes: Int, value: String) {
        row.findViewById<android.widget.TextView>(R.id.label).text = getString(labelRes)
        row.findViewById<android.widget.TextView>(R.id.value).text = value
    }

    private fun renderImage(imageUri: String?) {
        val binding = binding ?: return
        imageLoadJob?.cancel()
        if (imageUri.isNullOrBlank()) {
            binding.ivExpenseImage.isVisible = false
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
                binding.ivExpenseImage.setImageBitmap(bitmap)
                binding.ivExpenseImage.isVisible = true
            } else {
                binding.ivExpenseImage.isVisible = false
            }
        }
    }

    private fun confirmDelete() {
        val expense = currentExpense ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_expense_dialog_title)
            .setMessage(R.string.delete_expense_dialog_message)
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                viewModel.deleteExpense(expense)
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
        private const val ARG_EXPENSE_ID = "expense_id"

        fun newInstance(expenseId: Long): ExpenseDetailsFragment = ExpenseDetailsFragment().apply {
            arguments = Bundle().apply { putLong(ARG_EXPENSE_ID, expenseId) }
        }
    }
}
