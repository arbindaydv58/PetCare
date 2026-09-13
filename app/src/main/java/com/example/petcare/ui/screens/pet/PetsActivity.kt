package com.example.petcare.ui.screens.pet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petcare.R
import com.example.petcare.ui.screens.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class PetsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_pets)

        if (savedInstanceState == null) {
            val petId = intent.getLongExtra(EXTRA_PET_ID, -1L).takeIf { it > 0 }
            val initialFragment = if (petId != null) {
                PetDetailsFragment.newInstance(petId)
            } else {
                MyPetsFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.pets_container, initialFragment)
                .commit()
        }
    }

    fun pushFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.pets_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val EXTRA_PET_ID = "pet_id"
    }
}
