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
import com.example.petcare.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()
    private var binding: FragmentLoginBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLoginBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        viewModel.setMode(AuthMode.LOGIN)

        binding.email.doOnTextChanged { text, _, _, _ ->
            if (binding.email.isFocused) viewModel.updateEmail(text?.toString() ?: "")
        }
        binding.password.doOnTextChanged { text, _, _, _ ->
            if (binding.password.isFocused) viewModel.updatePassword(text?.toString() ?: "")
        }

        binding.login.setOnClickListener { viewModel.submit() }
        binding.forgotPassword.setOnClickListener { viewModel.setMode(AuthMode.FORGOT_PASSWORD) }
        binding.createAccount.setOnClickListener { viewModel.setMode(AuthMode.REGISTER) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.mode == AuthMode.LOGIN) render(state)
                }
            }
        }
    }

    private fun render(state: AuthUiState) {
        val binding = binding ?: return
        binding.login.isEnabled = !state.isLoading
        binding.forgotPassword.isEnabled = !state.isLoading
        binding.createAccount.isEnabled = !state.isLoading
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
