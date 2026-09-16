package com.example.petcare.ui.screens.delegation

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.card.MaterialCardView
import com.example.petcare.R
import com.example.petcare.data.local.entity.PetEntity
import com.example.petcare.databinding.FragmentDelegationFormBinding
import com.example.petcare.databinding.ItemDelegationTaskBinding
import com.example.petcare.ui.viewmodel.CareTaskUiModel
import com.example.petcare.ui.viewmodel.DelegationUiState
import com.example.petcare.ui.viewmodel.DelegationViewModel
import com.example.petcare.ui.viewmodel.PetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DelegationFormFragment : Fragment() {

    private val viewModel: DelegationViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private var binding: FragmentDelegationFormBinding? = null

    private var presetPetId: Long? = null
    private var presetTaskId: Long? = null
    private var selectedPetId: Long? = null
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var petsBuilt = false
    private var lastAvailableTaskIds: List<Long> = emptyList()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDelegationFormBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        presetPetId = arguments?.getLong(ARG_PRESET_PET_ID, -1L)?.takeIf { it > 0 }
        presetTaskId = arguments?.getLong(ARG_PRESET_TASK_ID, -1L)?.takeIf { it > 0 }
        selectedPetId = presetPetId

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnDelegationHistory.setOnClickListener {
            startActivity(
                Intent(requireContext(), DelegationActivity::class.java).apply {
                    putExtra(DelegationActivity.EXTRA_SHOW_HISTORY, true)
                }
            )
        }
        binding.btnDelegationDate.setOnClickListener { showDatePicker() }
        binding.btnSendSms.setOnClickListener { sendSms() }

        val recipientWatcher = { _: CharSequence?, _: Int, _: Int, _: Int ->
            viewModel.updateRecipient(
                binding.etDelegationRecipientName.text?.toString().orEmpty(),
                binding.etDelegationPhone.text?.toString().orEmpty()
            )
        }
        binding.etDelegationRecipientName.doOnTextChanged(recipientWatcher)
        binding.etDelegationPhone.doOnTextChanged(recipientWatcher)

        viewModel.setPetAndDate(selectedPetId, selectedDateMillis)
        viewModel.preselectTask(presetTaskId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { petViewModel.pets.collect { pets -> onPetsChanged(pets) } }
                launch { viewModel.uiState.collect { state -> render(state) } }
            }
        }

        renderDate()
    }

    private fun onPetsChanged(pets: List<PetEntity>) {
        val binding = binding ?: return
        val labels = pets.map { it.name }
        if (!petsBuilt || binding.spDelegationPet.adapter?.count != labels.size) {
            binding.spDelegationPet.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
            binding.spDelegationPet.setOnItemClickListener { _, _, position, _ ->
                selectedPetId = pets[position].id
                viewModel.setPetAndDate(selectedPetId, selectedDateMillis)
            }
            petsBuilt = true
        }
        val name = pets.find { it.id == selectedPetId }?.name ?: getString(R.string.select_pet_label)
        binding.spDelegationPet.setText(name, false)
    }

    private fun showDatePicker() {
        val current = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                cal.set(year, month, dayOfMonth)
                selectedDateMillis = cal.timeInMillis
                renderDate()
                viewModel.setPetAndDate(selectedPetId, selectedDateMillis)
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun renderDate() {
        val binding = binding ?: return
        binding.btnDelegationDate.setText(dateFormat.format(Date(selectedDateMillis)))
    }

    private fun render(state: DelegationUiState) {
        val binding = binding ?: return

        renderTasks(binding, state)

        binding.messagePreviewCard.isVisible = state.generatedMessage.isNotBlank()
        binding.tvDelegationMessage.text = state.generatedMessage

        binding.btnSendSms.isEnabled = state.selectedTaskIds.isNotEmpty() && state.recipientPhone.isNotBlank()
    }

    private fun renderTasks(binding: FragmentDelegationFormBinding, state: DelegationUiState) {
        val taskIds = state.availableTasks.map { it.task.id }
        binding.tvNoDelegationTasks.isVisible = state.availableTasks.isEmpty()

        if (taskIds != lastAvailableTaskIds) {
            binding.spDelegationTask.removeAllViews()
            val inflater = LayoutInflater.from(requireContext())
            state.availableTasks.forEach { taskUi ->
                val itemBinding = ItemDelegationTaskBinding.inflate(inflater, binding.spDelegationTask, false)
                bindTaskRow(itemBinding, taskUi, state)
                binding.spDelegationTask.addView(itemBinding.root)
            }
            lastAvailableTaskIds = taskIds
        } else {
            for (i in 0 until binding.spDelegationTask.childCount) {
                val row = binding.spDelegationTask.getChildAt(i)
                val checkbox = row.findViewById<CheckBox>(R.id.delegation_task_checkbox)
                val taskId = row.tag as? Long ?: continue
                checkbox.setOnCheckedChangeListener(null)
                checkbox.isChecked = state.selectedTaskIds.contains(taskId)
                checkbox.setOnCheckedChangeListener { _, _ -> viewModel.toggleTaskSelection(taskId) }
                (row as? MaterialCardView)?.isChecked = state.selectedTaskIds.contains(taskId)
            }
        }
    }

    private fun bindTaskRow(itemBinding: ItemDelegationTaskBinding, taskUi: CareTaskUiModel, state: DelegationUiState) {
        itemBinding.root.tag = taskUi.task.id
        itemBinding.delegationTaskTitle.text = taskUi.task.title
        itemBinding.delegationTaskSubtitle.text = getString(
            R.string.routine_pet_frequency_format,
            taskUi.task.category,
            timeFormat.format(Date(taskUi.task.dueAt))
        )
        itemBinding.delegationTaskCheckbox.isChecked = state.selectedTaskIds.contains(taskUi.task.id)
        itemBinding.delegationTaskCheckbox.setOnCheckedChangeListener { _, _ -> viewModel.toggleTaskSelection(taskUi.task.id) }
        itemBinding.root.isChecked = state.selectedTaskIds.contains(taskUi.task.id)
        itemBinding.root.setOnClickListener { viewModel.toggleTaskSelection(taskUi.task.id) }
    }

    private fun sendSms() {
        val binding = binding ?: return
        binding.errorDelegationPhone.isVisible = false
        binding.errorDelegationTasks.isVisible = false

        val state = viewModel.uiState.value
        val phone = state.recipientPhone.trim()

        if (phone.isBlank()) {
            binding.errorDelegationPhone.text = getString(R.string.error_recipient_phone_required)
            binding.errorDelegationPhone.isVisible = true
            return
        }
        if (!Patterns.PHONE.matcher(phone).matches()) {
            binding.errorDelegationPhone.text = getString(R.string.error_recipient_phone_invalid)
            binding.errorDelegationPhone.isVisible = true
            return
        }
        if (state.selectedTaskIds.isEmpty()) {
            binding.errorDelegationTasks.text = getString(R.string.error_no_tasks_selected)
            binding.errorDelegationTasks.isVisible = true
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", state.generatedMessage)
        }
        try {
            startActivity(intent)
            viewModel.saveDelegation()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.sms_app_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val ARG_PRESET_PET_ID = "preset_pet_id"
        private const val ARG_PRESET_TASK_ID = "preset_task_id"

        fun newInstance(presetPetId: Long?, presetTaskId: Long?): DelegationFormFragment =
            DelegationFormFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PRESET_PET_ID, presetPetId ?: -1L)
                    putLong(ARG_PRESET_TASK_ID, presetTaskId ?: -1L)
                }
            }
    }
}
