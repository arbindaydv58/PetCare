package com.example.petcare.ui.screens.settings

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.data.local.entity.NotificationSettingsEntity
import com.example.petcare.databinding.FragmentNotificationSettingsBinding
import com.example.petcare.ui.viewmodel.NotificationSettingsUiState
import com.example.petcare.ui.viewmodel.NotificationSettingsViewModel
import kotlinx.coroutines.launch

class NotificationSettingsFragment : Fragment() {

    private val viewModel: NotificationSettingsViewModel by viewModels()
    private var binding: FragmentNotificationSettingsBinding? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        binding.permissionWarningCard.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        binding.btnEnableNotifications.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> render(state) } }
            }
        }
    }

    private fun render(state: NotificationSettingsUiState) {
        val binding = binding ?: return
        val settings = state.settings

        binding.notificationSettingsLoading.isVisible = settings == null
        binding.notificationSettingsContent.isVisible = settings != null
        if (settings == null) return

        binding.switchAllReminders.setOnCheckedChangeListener(null)
        binding.switchAllReminders.isChecked = settings.remindersEnabled
        binding.switchAllReminders.setOnCheckedChangeListener { _, checked ->
            viewModel.updateSettings(settings.copy(remindersEnabled = checked))
        }

        binding.switchTaskReminders.setOnCheckedChangeListener(null)
        binding.switchTaskReminders.isChecked = settings.taskRemindersEnabled
        binding.switchTaskReminders.isEnabled = settings.remindersEnabled
        binding.switchTaskReminders.setOnCheckedChangeListener { _, checked ->
            viewModel.updateSettings(settings.copy(taskRemindersEnabled = checked))
        }

        binding.switchHealthcareAlerts.setOnCheckedChangeListener(null)
        binding.switchHealthcareAlerts.isChecked = settings.healthcareRemindersEnabled
        binding.switchHealthcareAlerts.isEnabled = settings.remindersEnabled
        binding.switchHealthcareAlerts.setOnCheckedChangeListener { _, checked ->
            viewModel.updateSettings(settings.copy(healthcareRemindersEnabled = checked))
        }

        applyDependentRowStyle(settings)
    }

    private fun applyDependentRowStyle(settings: NotificationSettingsEntity) {
        val binding = binding ?: return
        val alpha = if (settings.remindersEnabled) 1f else 0.4f
        binding.labelTaskRemindersToggle.alpha = alpha
        binding.descriptionTaskRemindersToggle.alpha = alpha
        binding.labelHealthcareAlertsToggle.alpha = alpha
        binding.descriptionHealthcareAlertsToggle.alpha = alpha
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
