package com.example.petcare.ui.screens.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.petcare.R
import com.example.petcare.databinding.FragmentProfileBinding
import com.example.petcare.databinding.ItemProfileMenuBinding
import com.example.petcare.ui.screens.auth.AuthActivity
import com.example.petcare.ui.screens.delegation.DelegationActivity
import com.example.petcare.ui.screens.settings.NotificationSettingsActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val user = FirebaseAuth.getInstance().currentUser
        val userName = user?.displayName ?: getString(R.string.default_user_display_name)
        val userEmail = user?.email ?: getString(R.string.default_user_email)

        binding.tvProfileAvatar.text = userName.take(1)
        binding.tvProfileUsername.text = userName
        binding.tvProfileEmail.text = userEmail

        bindMenuItem(
            binding.itemNotificationSettings,
            iconRes = R.drawable.ic_notifications,
            titleRes = R.string.notification_settings_menu_item
        ) {
            startActivity(Intent(requireContext(), NotificationSettingsActivity::class.java))
        }

        bindMenuItem(
            binding.itemDelegationHistory,
            iconRes = R.drawable.ic_history,
            titleRes = R.string.delegation_history_title
        ) {
            startActivity(
                Intent(requireContext(), DelegationActivity::class.java).apply {
                    putExtra(DelegationActivity.EXTRA_SHOW_HISTORY, true)
                }
            )
        }

        bindMenuItem(
            binding.itemAboutPetcare,
            iconRes = R.drawable.ic_info,
            titleRes = R.string.about_petcare_menu_item
        ) { }

        bindMenuItem(
            binding.itemPrivacyPolicy,
            iconRes = R.drawable.ic_lock,
            titleRes = R.string.privacy_policy_menu_item
        ) { }

        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun bindMenuItem(itemBinding: ItemProfileMenuBinding, iconRes: Int, titleRes: Int, onClick: () -> Unit) {
        itemBinding.profileMenuIcon.setImageResource(iconRes)
        itemBinding.profileMenuTitle.text = getString(titleRes)
        itemBinding.root.setOnClickListener { onClick() }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(
            Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
