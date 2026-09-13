package com.example.petcare.ui.screens.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()
    private var binding: FragmentRegisterBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRegisterBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        viewModel.setMode(AuthMode.REGISTER)

        binding.username.doOnTextChanged { text, _, _, _ ->
            if (binding.username.isFocused) viewModel.updateUsername(text?.toString() ?: "")
        }
        binding.email.doOnTextChanged { text, _, _, _ ->
            if (binding.email.isFocused) viewModel.updateEmail(text?.toString() ?: "")
        }
        binding.password.doOnTextChanged { text, _, _, _ ->
            if (binding.password.isFocused) viewModel.updatePassword(text?.toString() ?: "")
        }
        binding.confirmPassword.doOnTextChanged { text, _, _, _ ->
            if (binding.confirmPassword.isFocused) viewModel.updateConfirmPassword(text?.toString() ?: "")
        }

        binding.register.setOnClickListener { viewModel.submit() }
        binding.login.setOnClickListener { viewModel.setMode(AuthMode.LOGIN) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.mode == AuthMode.REGISTER) render(state)
                }
            }
        }
    }

    private fun render(state: AuthUiState) {
        val binding = binding ?: return
        binding.register.isEnabled = !state.isLoading
        binding.login.isEnabled = !state.isLoading
        binding.loading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        if (!state.errorMessage.isNullOrBlank()) {
            binding.errorMessage.text = state.errorMessage
            binding.errorMessage.visibility = View.VISIBLE
        } else {
            binding.errorMessage.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
