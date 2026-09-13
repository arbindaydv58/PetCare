package com.example.petcare.ui.screens.expenses

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petcare.R
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentExpensesListBinding
import com.example.petcare.ui.viewmodel.ExpenseUiState
import com.example.petcare.ui.viewmodel.ExpenseViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import kotlinx.coroutines.launch

class ExpensesListFragment : Fragment() {

    private val viewModel: ExpenseViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentExpensesListBinding? = null
    private var adapter: ExpenseAdapter? = null

    private var petId: Long? = null
    private var latestExpenses: List<ExpenseEntity> = emptyList()
    private var latestPets: List<PetEntity> = emptyList()
    private var latestUiState: ExpenseUiState = ExpenseUiState()
    private var hasLoadedOnce = false

    private val currencyFormatter = expenseCurrencyFormatter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentExpensesListBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        petId = arguments?.getLong(ARG_PET_ID, -1L)?.takeIf { it > 0 }

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnAddExpense.setOnClickListener { openAddExpense() }
        binding.btnAddBudget.setOnClickListener { openBudgetForm() }
        binding.btnEditBudget.setOnClickListener { openBudgetForm() }

        val adapter = ExpenseAdapter(onExpenseClick = { expense -> openDetails(expense.id) })
        this.adapter = adapter
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpenses.adapter = adapter

        val expensesFlow = petId?.let { viewModel.observeExpensesForPet(it) } ?: viewModel.expenses

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { expensesFlow.collect { expenses -> latestExpenses = expenses; hasLoadedOnce = true; render() } }
                launch { petViewModel.pets.collect { pets -> latestPets = pets; render() } }
                launch { viewModel.uiState.collect { state -> latestUiState = state; render() } }
            }
        }
    }

    private fun render() {
        val binding = binding ?: return
        val adapter = adapter ?: return

        binding.progressExpenses.isVisible = !hasLoadedOnce
        binding.expensesScrollContent.isVisible = hasLoadedOnce
        if (!hasLoadedOnce) return

        binding.tvTotalExpenses.text = currencyFormatter.format(latestUiState.monthlySpending)

        val budget = latestUiState.budget
        if (budget != null && budget.amount > 0) {
            val progress = (latestUiState.monthlySpending / budget.amount * 100).toInt().coerceIn(0, 100)
            val remaining = budget.amount - latestUiState.monthlySpending
            binding.budgetProgressSection.isVisible = true
            binding.progressBudget.setProgressCompat(progress, true)
            binding.tvBudgetPercent.text = getString(R.string.budget_percent_format, progress)
            binding.tvBudgetRemaining.text = getString(R.string.budget_left_format, currencyFormatter.format(remaining))
            binding.btnAddBudget.isVisible = false
        } else {
            binding.budgetProgressSection.isVisible = false
            binding.btnAddBudget.isVisible = true
        }

        if (latestExpenses.isEmpty()) {
            binding.emptyExpensesContainer.isVisible = true
            binding.expensesListSection.isVisible = false
        } else {
            binding.emptyExpensesContainer.isVisible = false
            binding.expensesListSection.isVisible = true
            binding.btnEditBudget.isVisible = budget != null && budget.amount > 0
            val petNames = latestPets.associate { it.id to it.name }
            adapter.submitList(latestExpenses, petNames)
        }
    }

    private fun openAddExpense() {
        (requireActivity() as? ExpensesActivity)?.pushFragment(ExpenseFormFragment.newInstance(expenseId = null, presetPetId = petId))
    }

    private fun openDetails(expenseId: Long) {
        (requireActivity() as? ExpensesActivity)?.pushFragment(ExpenseDetailsFragment.newInstance(expenseId))
    }

    private fun openBudgetForm() {
        (requireActivity() as? ExpensesActivity)?.pushFragment(BudgetFormFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        binding = null
    }

    companion object {
        private const val ARG_PET_ID = "pet_id"

        fun newInstance(petId: Long?): ExpensesListFragment = ExpensesListFragment().apply {
            arguments = Bundle().apply { putLong(ARG_PET_ID, petId ?: -1L) }
        }
    }
}
