package com.example.petcare.ui.screens.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = FirebaseAuth.getInstance().currentUser != null
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _uiState.update {
            it.copy(
                isAuthenticated = firebaseAuth.currentUser != null,
                isLoading = false
            )
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun setMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                username = "",
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null, successMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value.trim(), errorMessage = null, successMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, successMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null, successMessage = null) }
    }

    fun submit() {
        val current = _uiState.value
        val emailValue = current.email.trim()

        when (current.mode) {
            AuthMode.LOGIN -> login(emailValue, current.password)
            AuthMode.REGISTER -> register(
                usernameValue = current.username.trim(),
                emailValue = emailValue,
                passwordValue = current.password,
                confirmPasswordValue = current.confirmPassword
            )
            AuthMode.FORGOT_PASSWORD -> sendPasswordResetEmail(emailValue)
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.update {
            it.copy(
                email = "",
                password = "",
                confirmPassword = "",
                errorMessage = null,
                successMessage = "Signed out successfully",
                isAuthenticated = false,
                mode = AuthMode.LOGIN
            )
        }
    }

    private fun login(emailValue: String, passwordValue: String) {
        val error = validateLogin(emailValue, passwordValue)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error, successMessage = null) }
            return
        }

        setLoading(true)
        try {
            auth.signInWithEmailAndPassword(emailValue, passwordValue)
                .addOnSuccessListener {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            errorMessage = null,
                            successMessage = "Welcome back!"
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Unable to sign in. Please try again.",
                            successMessage = null
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.localizedMessage}",
                    successMessage = null
                )
            }
        }
    }

    private fun register(
        usernameValue: String,
        emailValue: String,
        passwordValue: String,
        confirmPasswordValue: String
    ) {
        val error = validateRegistration(usernameValue, emailValue, passwordValue, confirmPasswordValue)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error, successMessage = null) }
            return
        }

        setLoading(true)
        try {
            auth.createUserWithEmailAndPassword(emailValue, passwordValue)
                .addOnSuccessListener { result ->
                    val user = result.user
                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                        displayName = usernameValue
                    }

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    errorMessage = null,
                                    successMessage = "Account created successfully!"
                                )
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Unable to create account. Please try again.",
                            successMessage = null
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.localizedMessage}",
                    successMessage = null
                )
            }
        }
    }

    private fun sendPasswordResetEmail(emailValue: String) {
        if (emailValue.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Please enter your email address.", successMessage = null)
            }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            _uiState.update {
                it.copy(errorMessage = "Please enter a valid email address.", successMessage = null)
            }
            return
        }

        setLoading(true)
        try {
            auth.sendPasswordResetEmail(emailValue)
                .addOnSuccessListener {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            successMessage = "Password reset link sent to your email."
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Unable to send password reset email.",
                            successMessage = null
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.localizedMessage}",
                    successMessage = null
                )
            }
        }
    }

    private fun validateLogin(emailValue: String, passwordValue: String): String? {
        if (emailValue.isBlank()) return "Email is required."
        if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) return "Please enter a valid email address."
        if (passwordValue.isBlank()) return "Password is required."
        if (passwordValue.length < 6) return "Password must be at least 6 characters long."
        return null
    }

    private fun validateRegistration(
        usernameValue: String,
        emailValue: String,
        passwordValue: String,
        confirmPasswordValue: String
    ): String? {
        if (usernameValue.isBlank()) return "Username is required."
        if (usernameValue.length < 2) return "Username must be at least 2 characters."
        if (usernameValue.length > 30) return "Username must be less than 30 characters."

        if (emailValue.isBlank()) return "Email is required."
        if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) return "Please enter a valid email address."
        if (passwordValue.isBlank()) return "Password is required."
        if (passwordValue.length < 6) return "Password must be at least 6 characters long."
        if (confirmPasswordValue.isBlank()) return "Please confirm your password."
        if (passwordValue != confirmPasswordValue) return "Passwords do not match."
        return null
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }
}
