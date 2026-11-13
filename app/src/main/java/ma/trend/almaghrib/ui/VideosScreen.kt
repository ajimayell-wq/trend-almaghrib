package ma.trend.almaghrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ChatBubble
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.trend.almaghrib.data.AppState
import ma.trend.almaghrib.data.VideoItem
import ma.trend.almaghrib.engine.TextNorm

private data class VFilter(val label: String, val pick: (List<VideoItem>) -> List<VideoItem>)

private val VFILTERS = listOf(
    VFilter("الأسرع انتشاراً") { l -> l.sortedByDescending { it.viewsPerHour ?: 0.0 } },
    VFilter("الأكثر مشاهدة") { l -> l.sortedByDescending { it.article.metrics["views"] ?: 0 } },
    VFilter("مباريات ورياضة") { l -> l.filter { "sport" in it.categories } },
    VFilter("جدل") { l -> l.filter { it.controversial } },
    VFilter("أخبار ومجتمع") { l -> l.filter { it.categories.any { c -> c in setOf("politique", "societe", "economie") } } },
    VFilter("فن") { l -> l.filter { "art" in it.categories } },
)

@Composable
fun VideosScreen(st: AppState, onSettings: () -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val uri = LocalUriHandler.current
    val list = VFILTERS[filter].pick(st.videos)
    val featured = list.firstOrNull()
    val rest = list.drop(1)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PlayCircle, null, tint = C.red, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text("الفيديوهات الرائجة", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        FilterChips(VFILTERS.mapIndexed { i, f -> if (i < 2) f.label else "${f.label} (${f.pick(st.videos).size})" }, filter, { filter = it })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            item {
                val chart = st.videos.any { it.fromTrendingChart }
                Notice(
                    if (chart) "المصدر: قائمة الفيديوهات الرائجة في المغرب من يوتيوب + قنوات إخبارية مغربية. الأرقام كما تنشرها يوتيوب."
                    else "المصدر: قنوات يوتيوب مغربية (آخر 48 ساعة). أضف مفتاح YouTube في «المزيد» لعرض قائمة الرائج في المغرب وعدد التعليقات.",
                    if (chart) C.gold2 else C.mut,
                    Modifier.then(if (!chart) Modifier.clickable(onClick = onSettings) else Modifier),
                )
            }
            if (featured == null) {
                item { EmptyBlock(if (st.videos.isEmpty()) "لا توجد فيديوهات بعد. حدّث من الرئيسية." else "لا توجد فيديوهات «${VFILTERS[filter].label}» في القنوات المرصودة خلال آخر 48 ساعة. اسحب للتحديث من الرئيسية أو اختر تصنيفاً آخر.") }
            } else {
                item { FeaturedVideo(featured) { uri.openUri(featured.article.url) } }
                item {
                    SectionHeader("الأكثر انتشاراً في المغرب", Icons.Filled.LocalFireDepartment, C.red)
                }
                items(rest.take(12), key = { it.article.url }) { v -> VideoRow(v) { uri.openUri(v.article.url) } }
                val similar = similarTo(featured, st.videos)
                if (similar.isNotEmpty()) {
                    item { SectionHeader("فيديوهات مشابهة") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(similar, key = { "s" + it.article.url }) { v ->
                                Column(Modifier.width(150.dp).clickable { uri.openUri(v.article.url) }) {
                                    VideoThumb(v, Modifier.fillMaxWidth().aspectRatio(16f / 9f), 26.dp)
                                    Text(v.article.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun similarTo(v: VideoItem, all: List<VideoItem>): List<VideoItem> {
    val base = TextNorm.tokenize(v.article.title).toSet()
    return all.filter { it.article.url != v.article.url }
        .map { o -> o to (TextNorm.tokenize(o.article.title).toSet() intersect base).size }
        .filter { it.second > 0 || it.first.categories.firstOrNull() == v.categories.firstOrNull() }
        .sortedByDescending { it.second }
        .take(6).map { it.first }
}

@Composable
private fun VideoThumb(v: VideoItem, modifier: Modifier, play: Dp, overlay: @Composable BoxScope.() -> Unit = {}) {
    Thumb(v.article.imageUrl, modifier, v.categories.firstOrNull(), corner = 12.dp) {
        Box(
            Modifier.size(play).clip(CircleShape).background(Color(0x40FFFFFF)).border(1.5.dp, Color(0x99FFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(play * 0.6f)) }
        overlay()
    }
}

@Composable
private fun FeaturedVideo(v: VideoItem, onWatch: () -> Unit) {
    Column {
        VideoThumb(v, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clickable(onClick = onWatch), 60.dp) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xBF000000))))
            Row(Modifier.align(Alignment.TopStart).padding(10.dp)) {
                Pill(if (v.fromTrendingChart) "رائج في المغرب" else "الأكثر انتشاراً", C.red, Color.White)
            }
            Row(Modifier.align(Alignment.BottomStart).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF3B3B)))
                Spacer(Modifier.width(5.dp))
                Text(v.article.sourceName.removePrefix("YouTube · "), style = MaterialTheme.typography.labelMedium.copy(color = Color.White))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(v.article.title, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium.copy(lineHeight = 24.sp))
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            v.article.metrics["views"]?.let { Metric(Icons.Filled.Visibility, Fmt.compact(it)) }
            v.article.metrics["comments"]?.let { Metric(Icons.Filled.ChatBubble, Fmt.compact(it)) }
            v.article.metrics["likes"]?.let { Metric(Icons.Filled.ThumbUp, Fmt.compact(it)) }
            Text(Fmt.ago(v.article.publishedAt), style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
        }
        Spacer(Modifier.height(4.dp))
        SpeedLine(v)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(C.goldBrush).clickable(onClick = onWatch),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF1A1405))
            Spacer(Modifier.width(6.dp))
            Text("شاهد الفيديو", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF1A1405), fontWeight = FontWeight.Black))
        }
    }
}

/** Vitesse de diffusion : uniquement des valeurs calculées à partir des vues publiées par YouTube. */
@Composable
private fun SpeedLine(v: VideoItem) {
    val parts = buildList {
        v.viewsPerHour?.let { add("≈ ${Fmt.compact(it.toLong())} مشاهدة/ساعة منذ النشر") }
        if (v.deltaViews != null && v.deltaMinutes != null) add("+${Fmt.compact(v.deltaViews)} منذ آخر تحديث (${v.deltaMinutes} د)")
    }
    if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelMedium.copy(color = C.green, textDirection = TextDirection.Rtl))
}

@Composable
private fun VideoRow(v: VideoItem, onClick: () -> Unit) {
    NCard(padding = PaddingValues(7.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VideoThumb(v, Modifier.size(width = 116.dp, height = 66.dp), 26.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(v.article.title, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    v.categories.firstOrNull()?.let { CatPill(it) }
                    if (v.controversial) ControversyPill()
                    Text(v.article.sourceName.removePrefix("YouTube · "), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 3.dp)) {
                    v.article.metrics["views"]?.let { Metric(Icons.Filled.Visibility, Fmt.compact(it)) }
                    v.viewsPerHour?.let { Text("${Fmt.compact(it.toLong())} مشاهدة/ساعة", style = MaterialTheme.typography.labelMedium.copy(color = C.green)) }
                    Text(Fmt.ago(v.article.publishedAt), style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                }
            }
        }
    }
}

