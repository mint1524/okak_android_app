package com.example.okakapp.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    onOpenChat: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    vm: ChatsListViewModel = viewModel(factory = ChatsListViewModel.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чаты") },
                actions = {
                    TextButton(onClick = onOpenSubscription) { Text("Подписка") }
                    TextButton(onClick = onOpenProfile) { Text("Профиль") }
                    TextButton(onClick = onLogout) { Text("Выход") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                vm.create(title = null) { chat -> onOpenChat(chat.id) }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Новый чат")
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { vm.load() }) { Text("Обновить") }
                }
                state.chats.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Чатов пока нет. Нажмите +")
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.chats, key = { it.id }) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChat(chat.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(chat.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    chat.createdAt.take(19).replace('T', ' '),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { vm.delete(chat.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }
}
