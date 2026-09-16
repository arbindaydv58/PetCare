package com.example.petcare.ui.screens.checklist

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.R
import com.example.petcare.data.local.entity.CareTaskEntity
import com.example.petcare.databinding.ItemCareTaskBinding
import com.example.petcare.databinding.ItemTaskCategoryHeaderBinding
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class TaskListItem {
    data class Header(val label: String) : TaskListItem()
    data class Task(val taskUi: CareTaskUiModel) : TaskListItem()
}

class TaskAdapter(
    private val onTaskClick: (CareTaskUiModel) -> Unit,
    private val onEditClick: (CareTaskUiModel) -> Unit,
    private val onDelegateClick: (CareTaskUiModel) -> Unit,
    private val onDeleteClick: (CareTaskUiModel) -> Unit,
    private val onToggleComplete: (CareTaskUiModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<TaskListItem> = emptyList()
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun submitList(newItems: List<TaskListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun taskAt(position: Int): CareTaskUiModel? = (items.getOrNull(position) as? TaskListItem.Task)?.taskUi

    fun isSwipeable(position: Int): Boolean = items.getOrNull(position) is TaskListItem.Task

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is TaskListItem.Header -> VIEW_TYPE_HEADER
        is TaskListItem.Task -> VIEW_TYPE_TASK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemTaskCategoryHeaderBinding.inflate(inflater, parent, false))
        } else {
            TaskViewHolder(ItemCareTaskBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TaskListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TaskListItem.Task -> (holder as TaskViewHolder).bind(
                item.taskUi, timeFormat, onTaskClick, onEditClick, onDelegateClick, onDeleteClick, onToggleComplete
            )
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(private val binding: ItemTaskCategoryHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: TaskListItem.Header) {
            binding.categoryHeaderText.text = header.label
        }
    }

    class TaskViewHolder(private val binding: ItemCareTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            taskUi: CareTaskUiModel,
            timeFormat: SimpleDateFormat,
            onTaskClick: (CareTaskUiModel) -> Unit,
            onEditClick: (CareTaskUiModel) -> Unit,
            onDelegateClick: (CareTaskUiModel) -> Unit,
            onDeleteClick: (CareTaskUiModel) -> Unit,
            onToggleComplete: (CareTaskUiModel) -> Unit
        ) {
            val context = binding.root.context
            val task = taskUi.task

            binding.taskItemTitle.text = task.title
            binding.taskItemSubtitle.text = context.getString(
                R.string.routine_pet_frequency_format,
                taskUi.petName,
                timeFormat.format(Date(task.dueAt))
            )
            binding.taskItemRoutineBadge.isVisible = task.routineId != null

            val hasNotes = task.notes.isNotBlank()
            binding.taskItemNotes.isVisible = hasNotes
            if (hasNotes) {
                binding.taskItemNotes.text = task.notes
            }

            binding.taskItemCompletedIcon.isVisible = taskUi.isCompleted
            binding.checkboxComplete.setOnCheckedChangeListener(null)
            binding.checkboxComplete.isChecked = taskUi.isCompleted
            binding.checkboxComplete.setOnCheckedChangeListener { _, _ -> onToggleComplete(taskUi) }

            val completionAlpha = if (taskUi.isCompleted) 0.55f else 1f
            binding.taskItemTitle.alpha = completionAlpha
            binding.taskItemSubtitle.alpha = completionAlpha
            binding.taskItemRoutineBadge.alpha = completionAlpha
            binding.taskItemNotes.alpha = completionAlpha
            binding.taskItemTitle.paintFlags = if (taskUi.isCompleted) {
                binding.taskItemTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.taskItemTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.root.setCardBackgroundColor(
                ContextCompat.getColor(context, if (taskUi.isCompleted) R.color.soft_green else R.color.white)
            )

            binding.root.setOnClickListener { onTaskClick(taskUi) }
            binding.btnEditTaskItem.setOnClickListener { onEditClick(taskUi) }
            binding.btnDelegateTaskItem.setOnClickListener { onDelegateClick(taskUi) }
            binding.btnDeleteTaskItem.setOnClickListener { onDeleteClick(taskUi) }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TASK = 1
    }
}

fun attachTaskSwipeGestures(
    recyclerView: RecyclerView,
    adapter: TaskAdapter,
    onToggleComplete: (CareTaskUiModel) -> Unit,
    onDeleteRequested: (CareTaskEntity) -> Unit
) {
    val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val position = viewHolder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION || !adapter.isSwipeable(position)) return 0
            return super.getSwipeDirs(recyclerView, viewHolder)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            val taskUi = adapter.taskAt(position) ?: return
            adapter.notifyItemChanged(position)
            if (direction == ItemTouchHelper.RIGHT) {
                onToggleComplete(taskUi)
            } else {
                onDeleteRequested(taskUi.task)
            }
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                val itemView = viewHolder.itemView
                val paint = Paint().apply {
                    color = ContextCompat.getColor(
                        itemView.context,
                        if (dX > 0) R.color.success_green else R.color.error_red
                    )
                }
                val background = if (dX > 0) {
                    RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat())
                } else {
                    RectF(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                }
                val radius = itemView.resources.getDimension(R.dimen.corner_radius_card_small)
                c.drawRoundRect(background, radius, radius, paint)
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
    ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
}
