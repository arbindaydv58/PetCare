package com.example.petcare.ui.screens.expenses

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.R
import com.example.petcare.data.local.entity.ExpenseEntity
import com.example.petcare.databinding.ItemExpenseBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DESIGN SYSTEM v2: "Health" was never an actual category option in
// ExpenseFormFragment's category list (Food/Veterinary/Medication/Grooming/
// Toys/Accessories/Other) - Veterinary and Medication expenses always fell
// through to the generic default icon. Added their own cases; "Health" kept
// for backward compatibility with any pre-existing rows using that value.
fun expenseIconFor(category: String): Int = when (category) {
    "Food" -> R.drawable.ic_restaurant
    "Veterinary" -> R.drawable.ic_vaccines
    "Medication", "Health" -> R.drawable.ic_medical
    "Grooming" -> R.drawable.ic_content_cut
    "Toys" -> R.drawable.ic_toys
    else -> R.drawable.ic_receipt_long
}

// DESIGN SYSTEM v2: per-category icon-container tint so expense category is
// scannable at a glance (matching the same pattern already used for
// Healthcare record types), computed purely from expense.category - no new
// data.
fun expenseColorFor(category: String): Int = when (category) {
    "Food" -> R.color.soft_yellow
    "Veterinary", "Health" -> R.color.soft_green
    "Medication" -> R.color.soft_blue
    "Grooming" -> R.color.soft_lavender
    "Toys" -> R.color.soft_coral
    "Accessories" -> R.color.soft_pink
    else -> R.color.petcare_surface_variant
}

fun expenseCurrencyFormatter(): NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "NP"))

class ExpenseAdapter(
    private val onExpenseClick: (ExpenseEntity) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var expenses: List<ExpenseEntity> = emptyList()
    private var petNames: Map<Long, String> = emptyMap()
    private val currencyFormatter = expenseCurrencyFormatter()
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

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
        holder.bind(expense, petName, currencyFormatter, dateFormat, onExpenseClick)
    }

    override fun getItemCount(): Int = expenses.size

    class ExpenseViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            expense: ExpenseEntity,
            petName: String,
            currencyFormatter: NumberFormat,
            dateFormat: SimpleDateFormat,
            onExpenseClick: (ExpenseEntity) -> Unit
        ) {
            val context = binding.root.context
            binding.expenseItemTitle.text = expense.title
            binding.expenseItemSubtitle.text = context.getString(R.string.routine_pet_frequency_format, petName, expense.category)
            binding.expenseItemAmount.text = currencyFormatter.format(expense.amount)
            binding.expenseItemDate.text = dateFormat.format(Date(expense.date))
            binding.expenseItemIcon.setImageResource(expenseIconFor(expense.category))
            binding.expenseItemIconContainer.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, expenseColorFor(expense.category))
            )
            binding.root.setOnClickListener { onExpenseClick(expense) }
        }
    }
}
