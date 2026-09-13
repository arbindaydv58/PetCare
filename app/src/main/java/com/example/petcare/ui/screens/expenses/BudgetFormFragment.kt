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
import com.example.petcare.R
import com.example.petcare.databinding.FragmentBudgetFormBinding
import com.example.petcare.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

class BudgetFormFragment : Fragment() {

    private val viewModel: ExpenseViewModel by viewModels()
    private var binding: FragmentBudgetFormBinding? = null
    private var fieldPopulated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBudgetFormBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnSaveBudget.setOnClickListener { save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.budget.collect { budget ->
                        if (!fieldPopulated && budget != null) {
                            binding.etBudgetAmount.setText(budget.amount.toString())
                            fieldPopulated = true
                        }
                    }
                }
            }
        }
    }

    private fun save() {
        val binding = binding ?: return
        binding.errorBudgetAmount.isVisible = false

        val amount = binding.etBudgetAmount.text?.toString()?.trim()?.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.errorBudgetAmount.text = getString(R.string.error_budget_amount_invalid)
            binding.errorBudgetAmount.isVisible = true
            return
        }

        viewModel.setBudget(amount)
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
