package com.example.okakapp.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatsListScreen(
    onOpenChat: (String) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Чаты", style = MaterialTheme.typography.headlineMedium)
                Text("Список появится позже", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onLogout, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Выйти")
                }
            }
        }
    }
}
