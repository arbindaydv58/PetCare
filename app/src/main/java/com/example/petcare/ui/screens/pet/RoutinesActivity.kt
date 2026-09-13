package com.example.petcare.ui.screens.pet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.petcare.R
import com.example.petcare.ui.screens.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class RoutinesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_routines)

        if (savedInstanceState == null) {
            val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L).takeIf { it > 0 }
            val newRoutinePetId = intent.getLongExtra(EXTRA_NEW_ROUTINE_PET_ID, -1L).takeIf { it > 0 }
            val initialFragment: Fragment = when {
                routineId != null -> RoutineDetailsFragment.newInstance(routineId)
                newRoutinePetId != null -> RoutineFormFragment.newInstance(routineId = null, presetPetId = newRoutinePetId)
                else -> MyRoutinesFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.routines_container, initialFragment)
                .commit()
        }
    }

    fun pushFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.routines_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val EXTRA_ROUTINE_ID = "routine_id"
        const val EXTRA_NEW_ROUTINE_PET_ID = "new_routine_pet_id"
    }
}
