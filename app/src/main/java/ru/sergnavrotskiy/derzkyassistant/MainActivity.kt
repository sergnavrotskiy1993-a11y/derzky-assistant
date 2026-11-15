package ru.sergnavrotskiy.derzkyassistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Background = Color(0xFF10131C)
private val Panel = Color(0xFF1A2030)
private val Accent = Color(0xFF8B7CFF)
private val Muted = Color(0xFFADB4C5)
private const val PREFS = "derzky_local_data"
private const val CHAT_KEY = "chat"
private const val SLIDES_KEY = "slides"

private data class Message(val text: String, val fromAssistant: Boolean)
private data class LocalApp(val title: String, val subtitle: String, val symbol: String, val action: () -> Unit)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(
                primary = Accent,
                background = Background,
                surface = Panel,
                onSurface = Color(0xFFF4F5FA),
                onSurfaceVariant = Muted,
            )) {
                DerzkyApp(
                    openAction = { action -> openDeviceAction(action) },
                    readValue = { key -> getSharedPreferences(PREFS, MODE_PRIVATE).getString(key, null) },
                    saveValue = { key, value -> getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(key, value).apply() },
                )
            }
        }
    }

    private fun openDeviceAction(action: String) {
        val intent = when (action) {
            "browser" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            "dial" -> Intent(Intent.ACTION_DIAL)
            "mail" -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            "settings" -> Intent(android.provider.Settings.ACTION_SETTINGS)
            "calendar" -> Intent(Intent.ACTION_INSERT).setData(android.provider.CalendarContract.Events.CONTENT_URI)
            else -> return
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "На устройстве нет подходящего приложения", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Действие недоступно на этом устройстве", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun DerzkyApp(
    openAction: (String) -> Unit,
    readValue: (String) -> String?,
    saveValue: (String, String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val messages = remember { mutableStateListOf<Message>().apply { addAll(decodeMessages(readValue(CHAT_KEY))) } }
    val slides = remember { mutableStateListOf<String>().apply { addAll(decodeSlides(readValue(SLIDES_KEY))) } }
    if (messages.isEmpty()) messages.add(Message("Привет! Я работаю прямо на устройстве. Могу помочь с заметкой, идеей или планом.", true))

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Чат", "Приложения", "Слайды").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(listOf("💬", "▦", "▤")[index], fontSize = 18.sp) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> ChatScreen(
                modifier = Modifier.padding(padding),
                messages = messages,
                onSend = { text ->
                    messages.add(Message(text, false))
                    messages.add(Message(localReply(text), true))
                    saveValue(CHAT_KEY, encodeMessages(messages))
                },
                onClear = {
                    messages.clear()
                    messages.add(Message("Чат очищен. Напишите, что нужно сделать.", true))
                    saveValue(CHAT_KEY, encodeMessages(messages))
                },
            )
            1 -> AppsScreen(Modifier.padding(padding), openAction)
            else -> SlidesScreen(
                modifier = Modifier.padding(padding),
                slides = slides,
                onAdd = { text -> slides.add(text); saveValue(SLIDES_KEY, encodeSlides(slides)) },
                onDelete = { index -> if (index in slides.indices) { slides.removeAt(index); saveValue(SLIDES_KEY, encodeSlides(slides)) } },
            )
        }
    }
}

@Composable
private fun ChatScreen(modifier: Modifier, messages: List<Message>, onSend: (String) -> Unit, onClear: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Header("Дерзкий ассистент", "Локальный режим · без отправки данных") {
            TextButton(onClick = onClear) { Text("Очистить", color = Muted) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages) { message -> MessageBubble(message) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Сообщение…", color = Muted) },
                shape = RoundedCornerShape(18.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val text = draft.trim(); if (text.isNotEmpty()) { onSend(text); draft = "" }
                }),
            )
            Button(
                onClick = { val text = draft.trim(); if (text.isNotEmpty()) { onSend(text); draft = "" } },
                enabled = draft.isNotBlank(),
                modifier = Modifier.height(56.dp),
            ) { Text("➤") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header(title: String, subtitle: String, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).background(Accent, CircleShape), contentAlignment = Alignment.Center) {
            Text("ДА", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

@Composable
private fun MessageBubble(message: Message) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromAssistant) Arrangement.Start else Arrangement.End) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            color = if (message.fromAssistant) Panel else Color(0xFF5146A8),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(message.text, Modifier.padding(horizontal = 15.dp, vertical = 12.dp), color = Color(0xFFF4F5FA))
        }
    }
}

@Composable
private fun AppsScreen(modifier: Modifier, openAction: (String) -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Header("Приложения", "Быстрые действия на телефоне")
        Text("Открываются установленные на устройстве приложения. Интернет используется только при выборе браузера.",
            Modifier.padding(top = 18.dp, bottom = 12.dp), color = Muted, style = MaterialTheme.typography.bodySmall)
        val apps = listOf(
            LocalApp("Браузер", "Открыть веб-страницу", "🌐") { openAction("browser") },
            LocalApp("Телефон", "Открыть набор номера", "☎") { openAction("dial") },
            LocalApp("Почта", "Создать письмо", "✉") { openAction("mail") },
            LocalApp("Календарь", "Создать событие", "▣") { openAction("calendar") },
            LocalApp("Настройки", "Настройки Android", "⚙") { openAction("settings") },
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(apps) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { app.action() },
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(app.symbol, fontSize = 24.sp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(app.title, fontWeight = FontWeight.SemiBold)
                            Text(app.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlidesScreen(modifier: Modifier, slides: List<String>, onAdd: (String) -> Unit, onDelete: (Int) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    var current by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Header("Слайды", "Локальные карточки для идей и планов")
        Text("Создавайте карточки и листайте их кнопками — данные остаются на телефоне.",
            Modifier.padding(top = 18.dp), color = Muted, style = MaterialTheme.typography.bodySmall)
        if (slides.isEmpty()) {
            Card(Modifier.fillMaxWidth().weight(1f).padding(vertical = 18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Пока слайдов нет. Добавьте первую идею ниже.", color = Muted)
                }
            }
        } else {
            val safeIndex = current.coerceIn(0, slides.lastIndex)
            Card(
                Modifier.fillMaxWidth().weight(1f).padding(vertical = 18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242039)),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("ИДЕЯ ${safeIndex + 1} / ${slides.size}", color = Accent, fontWeight = FontWeight.Bold)
                    Text(slides[safeIndex], fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { current = (safeIndex - 1).coerceAtLeast(0) }, enabled = safeIndex > 0) { Text("← Назад") }
                        TextButton(onClick = { onDelete(safeIndex); current = (safeIndex - 1).coerceAtLeast(0) }) { Text("Удалить", color = Muted) }
                        TextButton(onClick = { current = (safeIndex + 1).coerceAtMost(slides.lastIndex) }, enabled = safeIndex < slides.lastIndex) { Text("Далее →") }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Текст новой карточки…") },
                maxLines = 3,
            )
            Button(onClick = { val text = draft.trim(); if (text.isNotEmpty()) { onAdd(text); current = slides.size; draft = "" } }, enabled = draft.isNotBlank()) {
                Text("Добавить")
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private fun localReply(input: String): String {
    val text = input.trim()
    val lower = text.lowercase()
    return when {
        lower in listOf("привет", "здравствуй", "добрый день", "доброе утро") -> "Привет! Я отвечаю локально, без облачного ИИ. Сформулируй задачу — помогу составить план или заметку."
        "время" in lower || "дата" in lower -> "Сейчас ${java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm", java.util.Locale("ru")).format(java.util.Date())}."
        "план" in lower -> "Вот простой план:\n1. Сформулируй цель.\n2. Разбей её на небольшие шаги.\n3. Начни с самого короткого шага.\n\nЭто локальная подсказка, не ответ облачной модели."
        "слайд" in lower || "презентац" in lower -> "Открой вкладку «Слайды», чтобы создать карточки и листать их как мини-презентацию."
        "замет" in lower -> "Для заметок можно использовать чат или сохранить эту мысль отдельной карточкой во вкладке «Слайды»."
        else -> "Я получил: «$text». Сейчас я работаю локально: могу дать простые подсказки, но не подключён к облачной языковой модели и не выполняю интернет-поиск."
    }
}

private fun encodeMessages(messages: List<Message>): String = JSONArray().apply {
    messages.forEach { message -> put(JSONObject().put("text", message.text).put("assistant", message.fromAssistant)) }
}.toString()

private fun decodeMessages(raw: String?): List<Message> = runCatching {
    val array = JSONArray(raw ?: "[]")
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        Message(item.optString("text"), item.optBoolean("assistant"))
    }
}.getOrDefault(emptyList())

private fun encodeSlides(slides: List<String>): String = JSONArray(slides).toString()

private fun decodeSlides(raw: String?): List<String> = runCatching {
    val array = JSONArray(raw ?: "[]")
    (0 until array.length()).map { array.getString(it) }
}.getOrDefault(emptyList())
