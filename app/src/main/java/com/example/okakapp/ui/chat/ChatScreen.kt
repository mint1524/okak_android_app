package com.example.okakapp.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.okakapp.data.local.cache.MessageEntity
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    vm: ChatViewModel = viewModel(factory = ChatViewModel.factory(chatId))
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHost = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    val visibleCount = state.messages.size + (if (state.streamingDraft != null || state.isStreaming) 1 else 0)
    LaunchedEffect(visibleCount, state.streamingDraft?.content?.length) {
        if (visibleCount > 0) {
            listState.animateScrollToItem((visibleCount - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(state.error) {
        val err = state.error
        if (err != null) {
            snackbarHost.showSnackbar(err)
            vm.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.chatTitle.ifBlank { "Чат" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHost) { data -> Snackbar(snackbarData = data) }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.messages.isEmpty() && state.streamingDraft == null && !state.isStreaming) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Сообщений пока нет, начните разговор")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages, key = { it.id }) { msg ->
                            MessageBubble(
                                role = msg.role,
                                content = msg.content,
                                createdAt = msg.createdAt,
                                onCopy = { clipboard.setText(AnnotatedString(msg.content)) },
                                onCopyText = { clipboard.setText(AnnotatedString(it)) }
                            )
                        }
                        if (state.isStreaming) {
                            item(key = "streaming-draft") {
                                StreamingBubble(
                                    content = state.streamingDraft?.content.orEmpty(),
                                    onCopyText = { clipboard.setText(AnnotatedString(it)) }
                                )
                            }
                        }
                    }
                }
            }
            ChatInput(
                text = state.draft,
                isSending = state.isStreaming,
                onTextChange = vm::onDraftChange,
                onSend = vm::send
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    role: String,
    content: String,
    createdAt: String,
    onCopy: () -> Unit,
    onCopyText: (String) -> Unit
) {
    val isUser = role == "user"
    val haptic = LocalHapticFeedback.current
    val color = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            color = color,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCopy()
                    }
                )
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isUser) {
                    Text(content)
                } else {
                    AssistantMarkdownContent(content = content, onCopyText = onCopyText)
                }
            }
        }
        Text(
            text = formatTime(createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StreamingBubble(content: String, onCopyText: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (content.isBlank()) {
                    TypingDots()
                } else {
                    AssistantMarkdownContent(content = content, onCopyText = onCopyText)
                }
            }
        }
    }
}

@Composable
private fun AssistantMarkdownContent(content: String, onCopyText: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parseMarkdownParts(content).forEach { part ->
            when (part) {
                is MarkdownPart.TextPart -> {
                    if (part.text.isNotBlank()) {
                        Markdown(
                            content = part.text.trim(),
                            colors = markdownColor(),
                            typography = markdownTypography()
                        )
                    }
                }
                is MarkdownPart.CodeBlock -> {
                    CodeBlockCard(
                        language = part.language,
                        code = part.code.trimEnd(),
                        onCopy = { onCopyText(part.code.trimEnd()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(language: String, code: String, onCopy: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать code block")
                }
            }
            SelectionContainer {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

private sealed class MarkdownPart {
    data class TextPart(val text: String) : MarkdownPart()
    data class CodeBlock(val language: String, val code: String) : MarkdownPart()
}

private fun parseMarkdownParts(content: String): List<MarkdownPart> {
    val regex = Regex("```([A-Za-z0-9_+.#-]*)\\n([\\s\\S]*?)```")
    val parts = mutableListOf<MarkdownPart>()
    var cursor = 0
    regex.findAll(content).forEach { match ->
        if (match.range.first > cursor) {
            parts += MarkdownPart.TextPart(content.substring(cursor, match.range.first))
        }
        parts += MarkdownPart.CodeBlock(
            language = match.groupValues.getOrNull(1).orEmpty(),
            code = match.groupValues.getOrNull(2).orEmpty()
        )
        cursor = match.range.last + 1
    }
    if (cursor < content.length) {
        parts += MarkdownPart.TextPart(content.substring(cursor))
    }
    return parts.ifEmpty { listOf(MarkdownPart.TextPart(content)) }
}

@Composable
private fun TypingDots() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(0, 150, 300).forEach { delay ->
            val transition = rememberInfiniteTransition(label = "dot$delay")
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha$delay"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(8.dp)
                    .alpha(alpha)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ChatInput(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Сообщение...") },
            enabled = !isSending,
            maxLines = 4
        )
        IconButton(
            onClick = onSend,
            enabled = !isSending && text.isNotBlank()
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
            }
        }
    }
}
