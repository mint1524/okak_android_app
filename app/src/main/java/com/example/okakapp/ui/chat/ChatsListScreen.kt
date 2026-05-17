package com.example.okakapp.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.okakapp.data.local.cache.ChatEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatsListScreen(
    onOpenChat: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    vm: ChatsListViewModel = viewModel(factory = ChatsListViewModel.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<ChatEntity?>(null) }
    var askLogout by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it)
            vm.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чаты") },
                actions = {
                    IconButton(onClick = { vm.refresh() }, enabled = !state.isRefreshing) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                        }
                    }
                    TextButton(onClick = onOpenSubscription) { Text("Подписка") }
                    TextButton(onClick = onOpenProfile) { Text("Профиль") }
                    TextButton(onClick = { askLogout = true }) { Text("Выход") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                vm.create(title = null) { id -> onOpenChat(id) }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Новый чат")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) { Snackbar(snackbarData = it) } }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            if (state.chats.isEmpty() && !state.isRefreshing) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Чатов пока нет", style = MaterialTheme.typography.titleMedium)
                        Text("Нажмите + чтобы создать новый", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.chats, key = { it.id }) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOpenChat(chat.id) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        renameTarget = chat
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(chat.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    formatDateTime(chat.updatedAt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { pendingDelete = chat.id }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить чат?") },
            text = { Text("Чат и все сообщения будут удалены безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete?.let { vm.delete(it) }
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Отмена") } }
        )
    }

    renameTarget?.let { target ->
        var newTitle by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Переименовать") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.take(120) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.rename(target.id, newTitle)
                    renameTarget = null
                }, enabled = newTitle.trim().isNotBlank()) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Отмена") } }
        )
    }

    if (askLogout) {
        AlertDialog(
            onDismissRequest = { askLogout = false },
            title = { Text("Выйти?") },
            text = { Text("Нужно будет войти заново.") },
            confirmButton = {
                TextButton(onClick = {
                    askLogout = false
                    onLogout()
                }) { Text("Выйти") }
            },
            dismissButton = { TextButton(onClick = { askLogout = false }) { Text("Отмена") } }
        )
    }
}
