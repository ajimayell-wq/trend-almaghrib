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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.trend.almaghrib.data.AppState
import ma.trend.almaghrib.data.VideoItem
import ma.trend.almaghrib.engine.Article
import java.time.Duration
import java.time.Instant

/** Les plateformes de l'écran « الأكثر رواجاً اليوم ». */
enum class TopTab(val label: String, val icon: ImageVector, val color: Color) {
    GOOGLE("بحث Google", Icons.Filled.Search, Color(0xFF4C8DFF)),
    YOUTUBE("يوتيوب Top 10", Icons.Filled.PlayArrow, Color(0xFFFF3B3B)),
}

/** Tuiles d'accès rapide, affichées sur l'accueil. */
@Composable
fun TopTiles(st: AppState, onOpen: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TopTab.entries.forEachIndexed { i, t ->
            val sub = when (t) {
                TopTab.GOOGLE -> if (st.searches.isEmpty()) "—" else "${st.searches.size} بحثاً رائجاً"
                TopTab.YOUTUBE -> if (st.videos.any { it.chartRank != null }) "ترتيب يوتيوب" else "${top10(st).size} فيديوهات"
            }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(t.color.copy(alpha = .10f))
                    .border(1.dp, t.color.copy(alpha = .35f), RoundedCornerShape(14.dp))
                    .clickable { onOpen(i) }.padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(t.icon, null, tint = t.color, modifier = Modifier.size(22.dp))
                Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                Text(sub, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
            }
        }
    }
}

/** Top 10 YouTube : la liste officielle des tendances au Maroc (clé), sinon les vidéos du jour les plus vues des chaînes suivies. */
fun top10(st: AppState): List<VideoItem> {
    val chart = st.videos.filter { it.chartRank != null }.sortedBy { it.chartRank }
    if (chart.isNotEmpty()) return chart.take(10)
    val day = Instant.now().minus(Duration.ofHours(24))
    return st.videos.filter { it.article.publishedAt?.isAfter(day) == true && "views" in it.article.metrics }
        .sortedByDescending { it.article.metrics["views"] ?: 0 }.take(10)
}

@Composable
fun TopScreen(st: AppState, startTab: Int, onBack: () -> Unit, onSettings: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", onBack)
            Spacer(Modifier.width(10.dp))
            Text("الأكثر رواجاً اليوم", style = MaterialTheme.typography.headlineSmall)
        }
        FilterChips(TopTab.entries.map { it.label }, tab, { tab = it })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            when (TopTab.entries[tab]) {
                TopTab.GOOGLE -> {
                    item {
                        Notice("عمليات البحث التي ترتفع بسرعة على Google في المغرب خلال آخر 24 ساعة. الرقم حدّ أدنى تقريبي تنشره Google نفسها.", C.mut)
                    }
                    if (st.searches.isEmpty()) item { EmptyBlock("لا توجد بيانات بعد. حدّث من الرئيسية.") }
                    itemsIndexed(st.searches, key = { _, a -> "g" + a.url }) { i, a -> SearchRow(i + 1, a) { uri.openUri(a.url) } }
                }
                TopTab.YOUTUBE -> {
                    val list = top10(st)
                    val chart = list.any { it.chartRank != null }
                    item {
                        Notice(
                            if (chart) "الترتيب الرسمي لـ YouTube للفيديوهات الرائجة في المغرب الآن، مع المشاهدات الحقيقية."
                            else "الفيديوهات الأكثر مشاهدة اليوم في القنوات المغربية التي يتابعها التطبيق. لعرض الترتيب الرسمي لـ YouTube في المغرب، أضف مفتاح YouTube المجاني في «المزيد» (اضغط هنا).",
                            if (chart) C.gold2 else C.mut,
                            if (chart) Modifier else Modifier.clickable(onClick = onSettings),
                        )
                    }
                    if (list.isEmpty()) item { EmptyBlock("لا توجد فيديوهات منشورة اليوم بعد. حدّث من الرئيسية.") }
                    itemsIndexed(list, key = { _, v -> "y" + v.article.url }) { i, v -> YoutubeRow(v.chartRank ?: (i + 1), v) { uri.openUri(v.article.url) } }
                }
            }
        }
    }
}

@Composable
private fun RankCircle(rank: Int, color: Color) {
    Box(Modifier.size(34.dp).clip(CircleShape).then(if (rank <= 3) Modifier.background(C.goldBrush) else Modifier.background(color.copy(alpha = .15f))),
        contentAlignment = Alignment.Center) {
        Text("$rank", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black,
            color = if (rank <= 3) Color(0xFF1A1405) else C.txt, textDirection = TextDirection.Ltr))
    }
}

@Composable
private fun SearchRow(rank: Int, a: Article, onClick: () -> Unit) {
    NCard(padding = PaddingValues(10.dp), onClick = onClick, border = if (rank <= 3) C.gold.copy(alpha = .25f) else C.line) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RankCircle(rank, TopTab.GOOGLE.color)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 3.dp)) {
                    a.metrics["approx_traffic"]?.let { Pill("+${Fmt.compact(it)} بحث", TopTab.GOOGLE.color.copy(alpha = .15f), Color(0xFF9FC2FF)) }
                    Text("بدأ ${Fmt.ago(a.publishedAt)}", style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                }
                a.relatedTitles.take(2).forEach {
                    Text("• $it", maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCFC9BB)), modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun YoutubeRow(rank: Int, v: VideoItem, onClick: () -> Unit) {
    NCard(padding = PaddingValues(8.dp), onClick = onClick, border = if (rank <= 3) C.gold.copy(alpha = .25f) else C.line) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumb(v.article.imageUrl, Modifier.size(width = 120.dp, height = 68.dp), v.categories.firstOrNull(), corner = 10.dp) {
                RankBadge(rank, Modifier.align(Alignment.TopStart))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(v.article.title, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                Text(v.article.sourceName.removePrefix("YouTube · "), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(color = C.mut), modifier = Modifier.padding(top = 2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 3.dp)) {
                    v.article.metrics["views"]?.let { Metric(Icons.Filled.Visibility, Fmt.compact(it)) }
                    Text(Fmt.ago(v.article.publishedAt), style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                }
            }
        }
    }
}
