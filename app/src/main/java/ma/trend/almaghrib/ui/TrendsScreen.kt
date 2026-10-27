package ma.trend.almaghrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ma.trend.almaghrib.data.AppState
import ma.trend.almaghrib.engine.Topic
import java.time.Duration
import java.time.Instant

private data class TrendFilter(val label: String, val test: (Topic) -> Boolean)

private val FILTERS = listOf(
    TrendFilter("الكل") { true },
    TrendFilter("سياسة") { "politique" in it.categories },
    TrendFilter("رياضة") { "sport" in it.categories },
    TrendFilter("مجتمع") { "societe" in it.categories },
    TrendFilter("فن") { "art" in it.categories },
    TrendFilter("فيديوهات") { it.hasVideo },
    TrendFilter("جدل") { it.controversial },
    TrendFilter("اقتصاد") { "economie" in it.categories },
    TrendFilter("دولي") { "international" in it.categories },
)
private val WINDOWS = listOf(3L, 6L, 12L, 24L)

@Composable
fun TrendsScreen(st: AppState, onTopic: (Int) -> Unit, onAnalyst: () -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var window by rememberSaveable { mutableIntStateOf(3) }
    val now = Instant.now()
    val hours = WINDOWS[window]
    val list = st.topics
        .filter { t -> t.lastSeen?.let { Duration.between(it, now) <= Duration.ofHours(hours) } ?: false }
        .filter(FILTERS[filter].test)
        .take(20)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Insights, null, tint = C.red, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text("الترندات", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            GlassIcon(Icons.Filled.Search, "اسأل محلل الترند", onAnalyst)
        }
        FilterChips(FILTERS.map { it.label }, filter, { filter = it })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(C.gold.copy(alpha = .16f), C.red2.copy(alpha = .12f))))
                        .border(1.dp, C.gold2.copy(alpha = .28f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                ) {
                    Text("أكثر 20 موضوعاً تداولاً في المغرب", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "آخر ${Fmt.hours(hours)} · ${st.lastRefresh?.let { "تحديث " + Fmt.ago(it) } ?: "لم يتم التحديث بعد"}",
                        style = MaterialTheme.typography.bodySmall.copy(color = C.mut),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WINDOWS.forEachIndexed { i, h ->
                            val on = i == window
                            Text(
                                Fmt.hours(h), textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(color = if (on) Color(0xFF1A1405) else C.txt),
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .then(if (on) Modifier.background(C.goldBrush) else Modifier.background(Color(0x14FFFFFF)))
                                    .clickable { window = i }.padding(vertical = 6.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val ok = st.sources.count { it.ok }
                    val total = st.sources.size.coerceAtLeast(1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x14FFFFFF))) {
                            Box(Modifier.fillMaxWidth(ok.toFloat() / total).height(6.dp).background(C.goldBrush))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("تغطية المصادر $ok/${st.sources.size}", style = MaterialTheme.typography.labelMedium.copy(color = C.gold2))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(C.green))
                        Spacer(Modifier.width(4.dp))
                        Text("المؤشر: تغيّر عدد المنشورات بين آخر 6 ساعات والـ 6 السابقة", style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                    }
                }
            }
            if (list.isEmpty()) item { EmptyBlock(if (st.topics.isEmpty()) "لا توجد بيانات بعد. ارجع إلى الرئيسية للتحديث." else "لا توجد مواضيع لهذا التصنيف خلال آخر ${Fmt.hours(hours)}.") }
            itemsIndexed(list, key = { _, t -> t.id }) { i, t -> TopicRow(i + 1, t, { onTopic(t.id) }, detailed = true) }
        }
    }
}
