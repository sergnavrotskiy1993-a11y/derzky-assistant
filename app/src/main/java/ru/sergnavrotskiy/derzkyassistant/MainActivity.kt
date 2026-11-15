package ru.sergnavrotskiy.derzkyassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Night = Color(0xFF10131C)
private val Panel = Color(0xFF191E2A)
private val Accent = Color(0xFF8B7CFF)
private val Muted = Color(0xFFADB4C5)

private data class ChatMessage(val text: String, val fromAssistant: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = androidx.compose.material3.darkColorScheme(
                    primary = Accent,
                    onPrimary = Color.White,
                    background = Night,
                    surface = Panel,
                    onSurface = Color(0xFFF4F5FA),
                    onSurfaceVariant = Muted,
                ),
            ) {
                AssistantChat()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantChat() {
    val messageSaver = listSaver<List<ChatMessage>, String>(
        save = { messages -> messages.flatMap { listOf(it.text, it.fromAssistant.toString()) } },
        restore = { values ->
            values.chunked(2).mapNotNull { pair ->
                if (pair.size == 2) ChatMessage(pair[0], pair[1].toBoolean()) else null
            }
        },
    )
    var messages by rememberSaveable(stateSaver = messageSaver) {
        mutableStateOf(listOf(ChatMessage("Привет! Я Дерзкий ассистент. Чем помочь?", true)))
    }
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun sendMessage() {
        val text = draft.trim()
        if (text.isNotEmpty()) {
            messages = messages + ChatMessage(text, fromAssistant = false)
            draft = ""
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Night,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Header(onClear = {
                messages = listOf(ChatMessage("Привет! Я Дерзкий ассистент. Чем помочь?", true))
            })

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242039)),
            ) {
                Text(
                    text = "Демо-режим: отправленные сообщения пока не обрабатываются сервером.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    color = Color(0xFFD8D2FF),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(messages, key = { index, _ -> index }) { _, message ->
                    MessageBubble(message)
                }
            }

            Composer(
                text = draft,
                onTextChange = { draft = it },
                onSend = ::sendMessage,
                onVoiceClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Голосовой режим ещё не подключён.")
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("ДА", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Дерзкий ассистент", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Текстовый интерфейс", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onClear) {
            Text("Очистить", color = Muted)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromAssistant) Arrangement.Start else Arrangement.End,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.86f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomEnd = if (message.fromAssistant) 18.dp else 5.dp,
                bottomStart = if (message.fromAssistant) 5.dp else 18.dp,
            ),
            color = if (message.fromAssistant) Panel else Color(0xFF5146A8),
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                color = Color(0xFFF4F5FA),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun Composer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Напишите сообщение…", color = Muted) },
            shape = RoundedCornerShape(18.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        Button(
            onClick = onSend,
            enabled = text.isNotBlank(),
            modifier = Modifier.height(56.dp).semantics { contentDescription = "Отправить сообщение" },
            shape = RoundedCornerShape(18.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Text("➤", fontSize = 20.sp)
        }
    }
    TextButton(onClick = onVoiceClick, modifier = Modifier.align(Alignment.End)) {
        Text("🎙 Голосовой режим", color = Muted)
    }
}
