package com.example.petcare.ui.screens.checklist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.petcare.R
import com.example.petcare.ui.screens.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class TasksActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_tasks)

        if (savedInstanceState == null) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it > 0 }
            val newTaskPetId = intent.getLongExtra(EXTRA_NEW_TASK_PET_ID, -1L).takeIf { it > 0 }
            val newTaskRoutineId = intent.getLongExtra(EXTRA_NEW_TASK_ROUTINE_ID, -1L).takeIf { it > 0 }
            val showWeekly = intent.getBooleanExtra(EXTRA_SHOW_WEEKLY, false)

            val initialFragment: Fragment = when {
                taskId != null -> TaskFormFragment.newInstance(taskId = taskId, presetPetId = null, presetRoutineId = null)
                newTaskPetId != null || newTaskRoutineId != null ->
                    TaskFormFragment.newInstance(taskId = null, presetPetId = newTaskPetId, presetRoutineId = newTaskRoutineId)
                showWeekly -> WeeklyChecklistFragment()
                else -> DailyChecklistFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.tasks_container, initialFragment)
                .commit()
        }
    }

    fun pushFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.tasks_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_NEW_TASK_PET_ID = "new_task_pet_id"
        const val EXTRA_NEW_TASK_ROUTINE_ID = "new_task_routine_id"
        const val EXTRA_SHOW_WEEKLY = "show_weekly"
    }
}
