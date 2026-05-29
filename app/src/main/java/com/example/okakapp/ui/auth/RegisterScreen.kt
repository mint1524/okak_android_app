package com.example.okakapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onGoToLogin: () -> Unit,
    vm: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) {
            vm.resetSuccess()
            onRegistered()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Регистрация", style = MaterialTheme.typography.headlineMedium)

            AuthTextField(
                value = state.email,
                onValueChange = vm::onEmailChange,
                label = "Email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            OutlinedButton(
                onClick = { vm.requestRegistrationCode() },
                enabled = !state.isSendingCode && !state.isLoading,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                if (state.isSendingCode) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(if (state.codeSent) "Отправить код еще раз" else "Получить код на почту")
            }

            AuthTextField(
                value = state.verificationCode,
                onValueChange = vm::onVerificationCodeChange,
                label = "Код из письма",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            AuthTextField(
                value = state.password,
                onValueChange = vm::onPasswordChange,
                label = "Пароль",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            AuthTextField(
                value = state.passwordRepeat,
                onValueChange = vm::onPasswordRepeatChange,
                label = "Повторите пароль",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            state.info?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (state.error != null) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Button(
                onClick = { vm.register() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .width(220.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text("Создать аккаунт")
            }

            TextButton(onClick = onGoToLogin, modifier = Modifier.padding(top = 12.dp)) {
                Text("Уже есть аккаунт? Войти")
            }
        }
    }
}
