package com.example.okakapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.okakapp.OkakApp
import com.example.okakapp.data.repository.ApiException
import com.example.okakapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val passwordRepeat: String = "",
    val verificationCode: String = "",
    val codeSent: Boolean = false,
    val isSendingCode: Boolean = false,
    val info: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onPasswordRepeatChange(value: String) = _state.update { it.copy(passwordRepeat = value, error = null) }
    fun onVerificationCodeChange(value: String) = _state.update {
        it.copy(verificationCode = value.filter(Char::isDigit).take(6), error = null)
    }
    fun resetSuccess() = _state.update { it.copy(success = false) }

    fun login() {
        val s = _state.value
        if (!validateBasic(s.email, s.password)) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repo.login(s.email, s.password)
            handleResult(result)
        }
    }

    fun register() {
        val s = _state.value
        if (!validateBasic(s.email, s.password)) return
        if (s.password != s.passwordRepeat) {
            _state.update { it.copy(error = "пароли не совпадают") }
            return
        }
        if (s.codeSent && s.verificationCode.length != 6) {
            _state.update { it.copy(error = "введите 6-значный код из письма") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repo.register(s.email, s.password, s.verificationCode)
            handleResult(result)
        }
    }

    fun requestRegistrationCode() {
        val email = _state.value.email
        if (email.isBlank() || !email.contains("@")) {
            _state.update { it.copy(error = "введите корректный email") }
            return
        }
        _state.update { it.copy(isSendingCode = true, error = null, info = null) }
        viewModelScope.launch {
            repo.requestRegistrationCode(email)
                .onSuccess { response ->
                    val debugSuffix = response.debugCode?.let { " Код для dev: $it" }.orEmpty()
                    val text = if (response.sent) {
                        "Код отправлен на почту. Он действует ${response.expiresInMinutes} минут.$debugSuffix"
                    } else {
                        "Почтовый сервис не настроен. Попробуйте создать аккаунт без кода.$debugSuffix"
                    }
                    _state.update {
                        it.copy(isSendingCode = false, codeSent = response.sent || response.debugCode != null, info = text)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isSendingCode = false, error = e.message ?: "не удалось отправить код")
                    }
                }
        }
    }

    private fun validateBasic(email: String, password: String): Boolean {
        if (email.isBlank() || !email.contains("@")) {
            _state.update { it.copy(error = "введите корректный email") }
            return false
        }
        if (password.length < 6) {
            _state.update { it.copy(error = "пароль слишком короткий") }
            return false
        }
        return true
    }

    private fun handleResult(result: Result<Unit>) {
        result
            .onSuccess { _state.update { it.copy(isLoading = false, success = true, error = null) } }
            .onFailure { e ->
                val msg = when (e) {
                    is ApiException -> e.message
                    else -> e.message ?: "ошибка"
                }
                _state.update { it.copy(isLoading = false, error = msg) }
            }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(OkakApp.get().authRepo) as T
            }
        }
    }
}
