package com.example.petcare.ui.screens.checklist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petcare.R
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.databinding.FragmentWeeklyChecklistBinding
import com.example.petcare.databinding.ItemWeeklyDayBinding
import com.example.petcare.ui.screens.delegation.DelegationActivity
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import com.example.petcare.ui.viewmodel.CareTaskViewModel
import com.example.petcare.ui.viewmodel.ChecklistUiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeeklyChecklistFragment : Fragment() {

    private val viewModel: CareTaskViewModel by viewModels()
    private var binding: FragmentWeeklyChecklistBinding? = null
    private var adapter: TaskAdapter? = null

    private var weekDays: List<Date> = emptyList()
    private var dayViews: List<View> = emptyList()

    private val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())
    private val dateLabelFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentWeeklyChecklistBinding.inflate(inflater, container, false)
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
        binding.rvWeeklyTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWeeklyTasks.adapter = adapter
        attachTaskSwipeGestures(
            recyclerView = binding.rvWeeklyTasks,
            adapter = adapter,
            onToggleComplete = { taskUi -> toggleComplete(taskUi) },
            onDeleteRequested = { task -> confirmDelete(task) }
        )

        buildDaySelector()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedDate.collect { date ->
                        binding.weeklyDateLabel.text = dateLabelFormat.format(Date(date))
                        updateDaySelection(date)
                    }
                }
                launch { viewModel.checklistState.collect { state -> render(state) } }
            }
        }
    }

    private fun buildDaySelector() {
        val binding = binding ?: return
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val days = mutableListOf<Date>()
        for (i in 0..6) {
            days.add(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        weekDays = days

        binding.weeklyDayContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val views = mutableListOf<View>()
        days.forEach { day ->
            val cellBinding = ItemWeeklyDayBinding.inflate(inflater, binding.weeklyDayContainer, false)
            cellBinding.weeklyDayName.text = dayNameFormat.format(day).uppercase(Locale.getDefault())
            cellBinding.weeklyDayNumber.text = dayNumberFormat.format(day)
            cellBinding.root.tag = day
            cellBinding.root.setOnClickListener { viewModel.setSelectedDate(day.time) }
            binding.weeklyDayContainer.addView(cellBinding.root)
            views.add(cellBinding.root)
        }
        dayViews = views
        updateDaySelection(viewModel.selectedDate.value)
    }

    private fun updateDaySelection(selectedMillis: Long) {
        dayViews.forEach { cellView ->
            val day = cellView.tag as? Date ?: return@forEach
            val isSelected = isSameDay(day, selectedMillis)

            val background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_weekly_day_cell)?.mutate()
            if (background != null) {
                DrawableCompat.setTint(
                    background,
                    ContextCompat.getColor(requireContext(), if (isSelected) R.color.primary_coral else R.color.white)
                )
                cellView.background = background
            }

            val nameView = cellView.findViewById<android.widget.TextView>(R.id.weekly_day_name)
            val numberView = cellView.findViewById<android.widget.TextView>(R.id.weekly_day_number)
            nameView.setTextColor(
                ContextCompat.getColor(requireContext(), if (isSelected) R.color.white else R.color.secondary_text)
            )
            numberView.setTextColor(
                ContextCompat.getColor(requireContext(), if (isSelected) R.color.white else R.color.primary_text)
            )
        }
    }

    private fun isSameDay(date: Date, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun render(state: ChecklistUiState) {
        val binding = binding ?: return
        val adapter = adapter ?: return

        binding.weeklyTasksLoading.isVisible = false
        binding.weeklyTaskCountLabel.text = getString(R.string.weekly_task_count_format, state.totalCount)
        binding.weeklyProgress.isVisible = state.totalCount > 0
        if (state.totalCount > 0) {
            binding.weeklyProgress.setProgressCompat(state.completedCount * 100 / state.totalCount, true)
        }

        if (state.tasks.isEmpty()) {
            binding.rvWeeklyTasks.isVisible = false
            binding.emptyWeeklyTasks.isVisible = true
            adapter.submitList(emptyList())
        } else {
            binding.emptyWeeklyTasks.isVisible = false
            binding.rvWeeklyTasks.isVisible = true
            adapter.submitList(state.tasks.map { TaskListItem.Task(it) })
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        dayViews = emptyList()
        binding = null
    }
}
