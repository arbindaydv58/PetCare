package com.example.petcare.ui.screens.expenses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.R
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.databinding.ItemExpenseBinding
import java.text.NumberFormat
import java.util.Locale

fun expenseIconFor(category: String): Int = when (category) {
    "Food" -> R.drawable.ic_restaurant
    "Health" -> R.drawable.ic_medical
    "Grooming" -> R.drawable.ic_content_cut
    "Toys" -> R.drawable.ic_toys
    else -> R.drawable.ic_receipt_long
}

fun expenseCurrencyFormatter(): NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "NP"))

class ExpenseAdapter(
    private val onExpenseClick: (ExpenseEntity) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var expenses: List<ExpenseEntity> = emptyList()
    private var petNames: Map<Long, String> = emptyMap()
    private val currencyFormatter = expenseCurrencyFormatter()

    fun submitList(newExpenses: List<ExpenseEntity>, newPetNames: Map<Long, String>) {
        expenses = newExpenses
        petNames = newPetNames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        val petName = expense.petId?.let { petNames[it] } ?: holder.itemView.context.getString(R.string.general_pet_expense_label)
        holder.bind(expense, petName, currencyFormatter, onExpenseClick)
    }

    override fun getItemCount(): Int = expenses.size

    class ExpenseViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            expense: ExpenseEntity,
            petName: String,
            currencyFormatter: NumberFormat,
            onExpenseClick: (ExpenseEntity) -> Unit
        ) {
            val context = binding.root.context
            binding.expenseItemTitle.text = expense.title
            binding.expenseItemSubtitle.text = context.getString(R.string.routine_pet_frequency_format, petName, expense.category)
            binding.expenseItemAmount.text = currencyFormatter.format(expense.amount)
            binding.expenseItemIcon.setImageResource(expenseIconFor(expense.category))
            binding.root.setOnClickListener { onExpenseClick(expense) }
        }
    }
}
