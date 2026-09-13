package com.example.petcare.ui.screens.expenses

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.petcare.R
import com.example.petcare.ui.screens.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class ExpensesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_expenses)

        if (savedInstanceState == null) {
            val expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L).takeIf { it > 0 }
            val newExpensePetId = intent.getLongExtra(EXTRA_NEW_EXPENSE_PET_ID, -1L).takeIf { it > 0 }
            val petId = intent.getLongExtra(EXTRA_PET_ID, -1L).takeIf { it > 0 }

            val initialFragment: Fragment = when {
                expenseId != null -> ExpenseDetailsFragment.newInstance(expenseId)
                newExpensePetId != null -> ExpenseFormFragment.newInstance(expenseId = null, presetPetId = newExpensePetId)
                else -> ExpensesListFragment.newInstance(petId)
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.expenses_container, initialFragment)
                .commit()
        }
    }

    fun pushFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.expenses_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val EXTRA_PET_ID = "pet_id"
        const val EXTRA_EXPENSE_ID = "expense_id"
        const val EXTRA_NEW_EXPENSE_PET_ID = "new_expense_pet_id"
    }
}
