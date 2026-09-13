package com.example.petcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petcare.ui.screens.auth.AuthActivity
import com.example.petcare.ui.screens.checklist.TasksActivity
import com.example.petcare.ui.screens.healthcare.HealthcareActivity
import com.example.petcare.ui.screens.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val deepLinkUri = intent.data
        val id = deepLinkUri?.lastPathSegment?.toLongOrNull()
        if (id != null) {
            when (deepLinkUri.host) {
                "tasks" -> {
                    startActivity(Intent(this, TasksActivity::class.java).apply {
                        putExtra(TasksActivity.EXTRA_TASK_ID, id)
                    })
                    finish()
                    return
                }
                "healthcare" -> {
                    startActivity(Intent(this, HealthcareActivity::class.java).apply {
                        putExtra(HealthcareActivity.EXTRA_RECORD_ID, id)
                    })
                    finish()
                    return
                }
            }
        }

        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
