package ma.trend.almaghrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ma.trend.almaghrib.data.AppState
import ma.trend.almaghrib.data.Repository
import ma.trend.almaghrib.engine.Assistant
import java.time.Instant

private val EXAMPLES = listOf(
    "ماذا يتداول المغاربة اليوم؟",
    "ما هي القضايا التي تثير الجدل؟",
    "ما هي الفيديوهات الرائجة؟",
    "آخر قرارات الحكومة",
    "أخبار الرياضة في آخر 6 ساعات",
    "شنو كاين فالاقتصاد؟",
)

/** « محلل الترند » : questions en langage naturel, réponses tirées des sujets collectés. */
@Composable
fun AnalystScreen(st: AppState, repo: Repository, onBack: () -> Unit, onTopic: (Int) -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    var asked by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(repo.history) }
    val focus = LocalFocusManager.current
    val answer = remember(asked, st.topics) {
        if (asked.isBlank()) null else Assistant.answer(asked, st.topics, Fmt.CATEGORY, Instant.now())
    }
    fun ask(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        q = t; asked = t
        history = (listOf(t) + history.filter { it != t }).take(10)
        repo.history = history
        focus.clearFocus()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", onBack)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(30.dp).clip(CircleShape).background(C.goldBrush), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFF1A1405), modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("محلل الترند", style = MaterialTheme.typography.headlineSmall)
        }
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("اسأل عن أي موضوع…") },
            trailingIcon = { IconButton(onClick = { ask(q) }) { Icon(Icons.AutoMirrored.Filled.Send, "إرسال", tint = C.gold2) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { ask(q) }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.gold2, unfocusedBorderColor = C.gold.copy(alpha = .35f), cursorColor = C.gold2),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize()) {
            if (answer == null) {
                item { Text("أمثلة", style = MaterialTheme.typography.titleSmall.copy(color = C.mut)) }
                EXAMPLES.forEach { e -> item { SuggestionRow(e, Icons.Filled.AutoAwesome) { ask(e) } } }
                if (history.isNotEmpty()) {
                    item { Text("عمليات البحث السابقة", style = MaterialTheme.typography.titleSmall.copy(color = C.mut), modifier = Modifier.padding(top = 10.dp)) }
                    history.forEach { h -> item { SuggestionRow(h, Icons.Filled.History) { ask(h) } } }
                }
                item {
                    Text(
                        "يجيب المحلل من المواضيع التي رصدها التطبيق فعلياً، مع مصادرها. لا يؤلف أخباراً ولا يقدّر أرقاماً غير منشورة.",
                        style = MaterialTheme.typography.labelSmall.copy(color = C.mut2), modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                item {
                    NCard(border = C.gold2.copy(alpha = .3f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = C.gold2, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("جواب محلل الترند", style = MaterialTheme.typography.titleSmall.copy(color = C.gold2))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(answer.intro, style = MaterialTheme.typography.bodyMedium)
                        if (answer.filters.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("الفهم: " + answer.filters.joinToString(" · "), style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                        }
                    }
                }
                answer.notes.forEach { n -> item { Notice(n) } }
                if (st.topics.isEmpty()) item { Notice("لا توجد بيانات بعد: حدّث من الرئيسية أولاً.", C.red) }
                itemsIndexed(answer.topics, key = { _, t -> t.id }) { i, t -> TopicRow(i + 1, t, { onTopic(t.id) }, detailed = true) }
            }
        }
    }
}

@Composable
private fun SuggestionRow(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.card).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = C.gold2, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
