package com.example.petcare.ui.screens.healthcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.petcare.R
import com.example.petcare.ui.screens.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class HealthcareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_healthcare)

        if (savedInstanceState == null) {
            val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L).takeIf { it > 0 }
            val newRecordPetId = intent.getLongExtra(EXTRA_NEW_RECORD_PET_ID, -1L).takeIf { it > 0 }
            val petId = intent.getLongExtra(EXTRA_PET_ID, -1L).takeIf { it > 0 }

            val initialFragment: Fragment = when {
                recordId != null -> HealthcareDetailsFragment.newInstance(recordId)
                newRecordPetId != null -> HealthcareFormFragment.newInstance(recordId = null, presetPetId = newRecordPetId)
                else -> HealthcareListFragment.newInstance(petId)
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.healthcare_container, initialFragment)
                .commit()
        }
    }

    fun pushFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.healthcare_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val EXTRA_PET_ID = "pet_id"
        const val EXTRA_RECORD_ID = "record_id"
        const val EXTRA_NEW_RECORD_PET_ID = "new_record_pet_id"
    }
}
