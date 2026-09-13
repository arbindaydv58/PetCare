package com.example.petcare.ui.screens.delegation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.petcare.R
import com.example.petcare.ui.screens.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class DelegationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_delegation)

        if (savedInstanceState == null) {
            val showHistory = intent.getBooleanExtra(EXTRA_SHOW_HISTORY, false)
            val petId = intent.getLongExtra(EXTRA_PET_ID, -1L).takeIf { it > 0 }
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it > 0 }

            val initialFragment: Fragment = if (showHistory) {
                DelegationHistoryFragment()
            } else {
                DelegationFormFragment.newInstance(petId, taskId)
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.delegation_container, initialFragment)
                .commit()
        }
    }

    fun pushFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.delegation_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val EXTRA_PET_ID = "pet_id"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_SHOW_HISTORY = "show_history"
    }
}
