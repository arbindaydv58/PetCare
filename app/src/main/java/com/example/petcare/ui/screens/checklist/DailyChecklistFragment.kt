package com.example.petcare.ui.screens.checklist

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petcare.R
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.databinding.FragmentDailyChecklistBinding
import com.example.petcare.ui.screens.delegation.DelegationActivity
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import com.example.petcare.ui.viewmodel.CareTaskViewModel
import com.example.petcare.ui.viewmodel.ChecklistUiState
import com.example.petcare.ui.viewmodel.ResetChecklistResult
import com.example.petcare.utils.ShakeEventListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyChecklistFragment : Fragment() {

    private val viewModel: CareTaskViewModel by viewModels()
    private var binding: FragmentDailyChecklistBinding? = null
    private var adapter: TaskAdapter? = null

    private var sensorManager: SensorManager? = null
    private var shakeListener: ShakeEventListener? = null

    private val dateHeaderFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDailyChecklistBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnAddTaskToolbar.setOnClickListener { openAddTask() }
        binding.fabAddTask.setOnClickListener { openAddTask() }

        val adapter = TaskAdapter(
            onTaskClick = { taskUi -> openEditTask(taskUi.task.id) },
            onEditClick = { taskUi -> openEditTask(taskUi.task.id) },
            onDelegateClick = { taskUi -> openDelegate(taskUi) },
            onDeleteClick = { taskUi -> confirmDelete(taskUi.task) },
            onToggleComplete = { taskUi -> toggleComplete(taskUi) }
        )
        this.adapter = adapter
        binding.rvCareTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCareTasks.adapter = adapter
        attachTaskSwipeGestures(
            recyclerView = binding.rvCareTasks,
            adapter = adapter,
            onToggleComplete = { taskUi -> toggleComplete(taskUi) },
            onDeleteRequested = { task -> confirmDelete(task) }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.checklistState.collect { state -> render(state) } }
            }
        }
    }

    private fun render(state: ChecklistUiState) {
        val binding = binding ?: return
        val adapter = adapter ?: return

        binding.dailyTasksLoading.isVisible = false
        binding.dailyDateLabel.text = dateHeaderFormat.format(Date(state.selectedDate))

        val progress = if (state.totalCount > 0) (state.completedCount * 100 / state.totalCount) else 0
        binding.dailyProgress.setProgressCompat(progress, true)
        binding.dailyProgressLabel.text = getString(R.string.checklist_progress_format, state.completedCount, state.totalCount)

        if (state.tasks.isEmpty()) {
            binding.rvCareTasks.isVisible = false
            binding.emptyDailyTasks.isVisible = true
            adapter.submitList(emptyList())
            return
        }

        binding.emptyDailyTasks.isVisible = false
        binding.rvCareTasks.isVisible = true

        val items = mutableListOf<TaskListItem>()
        state.tasks.groupBy { it.task.category }.forEach { (category, tasks) ->
            val icon = when (category) {
                "FEEDING" -> "🍽"
                "EXERCISE" -> "🏃"
                "MEDICATION" -> "💊"
                else -> "✨"
            }
            items.add(TaskListItem.Header("$icon ${category.replaceFirstChar { it.uppercase() }}"))
            tasks.forEach { items.add(TaskListItem.Task(it)) }
        }
        adapter.submitList(items)
    }

    private fun toggleComplete(taskUi: CareTaskUiModel) {
        viewModel.toggleTaskCompletion(taskUi, viewModel.selectedDate.value)
        val message = if (!taskUi.isCompleted) {
            getString(R.string.task_completed_toast, taskUi.task.title)
        } else {
            getString(R.string.task_pending_toast, taskUi.task.title)
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(task: CareTaskEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_task_dialog_title)
            .setMessage(getString(R.string.delete_task_dialog_message, task.title))
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                viewModel.deleteTask(task)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
            .setOnCancelListener { adapter?.notifyDataSetChanged() }
            .setOnDismissListener { adapter?.notifyDataSetChanged() }
            .show()
    }

    private fun openAddTask() {
        (requireActivity() as? TasksActivity)?.pushFragment(TaskFormFragment.newInstance(taskId = null, presetPetId = null, presetRoutineId = null))
    }

    private fun openEditTask(taskId: Long) {
        (requireActivity() as? TasksActivity)?.pushFragment(TaskFormFragment.newInstance(taskId = taskId, presetPetId = null, presetRoutineId = null))
    }

    private fun openDelegate(taskUi: CareTaskUiModel) {
        startActivity(
            Intent(requireContext(), DelegationActivity::class.java).apply {
                putExtra(DelegationActivity.EXTRA_PET_ID, taskUi.task.petId)
                putExtra(DelegationActivity.EXTRA_TASK_ID, taskUi.task.id)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        val manager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val listener = ShakeEventListener { onShakeDetected() }
        sensorManager = manager
        shakeListener = listener
        manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        shakeListener?.let { sensorManager?.unregisterListener(it) }
        shakeListener = null
        sensorManager = null
    }

    private fun onShakeDetected() {
        viewModel.resetTodayChecklist { result ->
            if (isAdded) showShakeResultDialog(result)
        }
    }

    private fun showShakeResultDialog(result: ResetChecklistResult) {
        val title: Int
        val message: String
        when (result) {
            is ResetChecklistResult.Reset -> {
                title = R.string.shake_reset_success_title
                message = getString(R.string.shake_reset_success_message)
            }
            ResetChecklistResult.AlreadyClear -> {
                title = R.string.shake_reset_already_clear_title
                message = getString(R.string.shake_reset_already_clear_message)
            }
            is ResetChecklistResult.Error -> {
                title = R.string.shake_reset_error_title
                message = result.message.ifBlank { getString(R.string.shake_reset_error_fallback) }
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.action_ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        binding = null
    }
}
