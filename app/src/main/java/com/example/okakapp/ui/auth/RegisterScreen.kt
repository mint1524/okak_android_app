package com.example.okakapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Регистрация", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.padding(top = 24.dp)
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 12.dp)
        )

        OutlinedTextField(
            value = state.passwordRepeat,
            onValueChange = vm::onPasswordRepeatChange,
            label = { Text("Повторите пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 12.dp)
        )

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
