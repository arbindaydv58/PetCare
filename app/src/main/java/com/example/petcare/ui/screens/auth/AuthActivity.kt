package com.example.petcare.ui.screens.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.R
import com.example.petcare.ui.screens.home.HomeActivity
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    val viewModel: AuthViewModel by viewModels()

    private var currentMode: AuthMode? = null
    private var navigatedHome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isAuthenticated) {
                        goToHome()
                    } else {
                        showFragmentForMode(state.mode)
                    }
                }
            }
        }
    }

    private fun showFragmentForMode(mode: AuthMode) {
        if (mode == currentMode) return
        currentMode = mode
        val fragment: Fragment = when (mode) {
            AuthMode.LOGIN -> LoginFragment()
            AuthMode.REGISTER -> RegisterFragment()
            AuthMode.FORGOT_PASSWORD -> ForgotPasswordFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, fragment)
            .commitAllowingStateLoss()
    }

    private fun goToHome() {
        if (navigatedHome) return
        navigatedHome = true
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
