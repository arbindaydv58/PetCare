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
import com.example.petcare.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()
    private var binding: FragmentForgotPasswordBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        viewModel.setMode(AuthMode.FORGOT_PASSWORD)

        binding.forgotPasswordEmail.doOnTextChanged { text, _, _, _ ->
            if (binding.forgotPasswordEmail.isFocused) viewModel.updateEmail(text?.toString() ?: "")
        }

        binding.sendReset.setOnClickListener { viewModel.submit() }
        binding.backToLogin.setOnClickListener { viewModel.setMode(AuthMode.LOGIN) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.mode == AuthMode.FORGOT_PASSWORD) render(state)
                }
            }
        }
    }

    private fun render(state: AuthUiState) {
        val binding = binding ?: return
        binding.sendReset.isEnabled = !state.isLoading
        binding.backToLogin.isEnabled = !state.isLoading
        binding.loading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        if (!state.errorMessage.isNullOrBlank()) {
            binding.errorMessage.text = state.errorMessage
            binding.errorMessage.visibility = View.VISIBLE
        } else {
            binding.errorMessage.visibility = View.GONE
        }

        if (!state.successMessage.isNullOrBlank()) {
            binding.successMessage.text = state.successMessage
            binding.successMessage.visibility = View.VISIBLE
        } else {
            binding.successMessage.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
