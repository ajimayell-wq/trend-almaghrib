package ma.trend.almaghrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.trend.almaghrib.data.AppState
import ma.trend.almaghrib.engine.Topic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    st: AppState,
    onRefresh: () -> Unit,
    onTopic: (Int) -> Unit,
    onAllTrends: () -> Unit,
    onAnalyst: () -> Unit,
    onCoverage: () -> Unit,
) {
    PullToRefreshBox(isRefreshing = st.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color(0xFF14101C), 0.35f to C.bg, 1f to C.bg)
            ),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { HomeHeader(st, onRefresh, onAnalyst) }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    st.error?.let { Spacer(Modifier.height(8.dp)); Notice(it, C.red) }
                    if (!st.loaded || (st.topics.isEmpty() && st.refreshing)) {
                        EmptyBlock("جارٍ رصد المصادر وتحليل ما يتداوله المغاربة…")
                    } else if (st.topics.isEmpty()) {
                        EmptyBlock("لا توجد بيانات بعد. اسحب للأسفل أو اضغط زر التحديث.")
                    }
                }
            }
            if (st.topics.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader("الأكثر تداولاً الآن", Icons.Filled.LocalFireDepartment, C.red, "الكل", onAllTrends)
                    }
                }
                itemsIndexed(st.topics.take(5), key = { _, t -> "top${t.id}" }) { i, t ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) { TopicRow(i + 1, t, { onTopic(t.id) }) }
                }
                val rising = st.topics.drop(5).filter { it.emerging || it.trend == "nouveau" || (it.progression ?: 0) >= 50 }
                    .sortedByDescending { it.progression ?: 1000 }.take(8)
                if (rising.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader("ترند صاعد", Icons.Filled.RocketLaunch, C.gold2, trailing = "مواضيع تنمو بسرعة")
                        }
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(rising, key = { "r${it.id}" }) { t -> RisingCard(t) { onTopic(t.id) } }
                        }
                    }
                }
                item { Box(Modifier.padding(16.dp)) { CoverageCard(st, onCoverage) } }
            }
        }
    }
}

@Composable
private fun HomeHeader(st: AppState, onRefresh: () -> Unit, onAnalyst: () -> Unit) {
    Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(Icons.Filled.Search, "بحث", onAnalyst)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Logo()
                Spacer(Modifier.width(8.dp))
                GoldText("ترند المغرب", MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp))
            }
            if (st.refreshing) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = C.gold2, strokeWidth = 2.dp) }
            } else GlassIcon(Icons.Filled.Refresh, "تحديث الآن", onRefresh)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(C.red.copy(alpha = .16f)).border(1.dp, C.red.copy(alpha = .45f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveDot(); Spacer(Modifier.width(6.dp))
                Text("مباشر · آخر 24 ساعة", style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFFF8F91)))
            }
            Spacer(Modifier.width(8.dp))
            st.lastRefresh?.let { Text("تحديث ${Fmt.ago(it)}", style = MaterialTheme.typography.labelSmall.copy(color = C.mut)) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            buildAnnotatedString {
                append("ماذا يحدث في ")
                withStyle(SpanStyle(brush = C.goldBrush)) { append("المغرب الآن؟") }
            },
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x0EFFFFFF)).border(1.dp, Color(0x17FFFFFF), RoundedCornerShape(16.dp))
                .padding(vertical = 10.dp),
        ) {
            val ok = st.sources.count { it.ok }
            Stat(Fmt.compact(st.posts24h), "منشوراً محللاً", highlight = true)
            Stat(Fmt.compact(st.topics.size), "موضوعاً مرصوداً", divider = true)
            Stat("$ok/${st.sources.size.coerceAtLeast(ok)}", "مصادر نشطة", divider = true)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(C.gold.copy(alpha = .15f), C.gold.copy(alpha = .04f))))
                .border(1.dp, C.gold2.copy(alpha = .4f), RoundedCornerShape(14.dp))
                .clickable(onClick = onAnalyst).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(C.goldBrush), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFF1A1405), modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(9.dp))
            Text(
                buildAnnotatedString {
                    append("اسأل ")
                    withStyle(SpanStyle(color = C.gold2, fontWeight = FontWeight.Bold)) { append("محلل الترند") }
                    append(": ماذا يتداول المغاربة اليوم؟")
                },
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCFC9BB)), modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.Search, null, tint = C.gold2, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun RisingCard(t: Topic, onClick: () -> Unit) {
    NCard(Modifier.width(220.dp), border = C.green.copy(alpha = .25f), padding = PaddingValues(7.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumb(t.imageUrl, Modifier.size(50.dp), t.categories.firstOrNull(), corner = 10.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.RocketLaunch, null, tint = C.green, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Delta(t.progression, isNew = true, small = true)
                    Spacer(Modifier.width(6.dp))
                    Text(Fmt.ago(t.firstSeen).let { "ظهر $it" }, style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoverageCard(st: AppState, onClick: () -> Unit) {
    NCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("تغطية المصادر", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("التفاصيل ‹", style = MaterialTheme.typography.labelMedium.copy(color = C.gold2))
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            st.coverage.filterValues { !it.unavailable }.forEach { (p, c) ->
                val col = if (c.active == c.total) C.green else if (c.active > 0) C.gold2 else C.red
                Pill("${Fmt.PLATFORM[p] ?: p} ${c.active}/${c.total}", col.copy(alpha = .12f), col)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "غير مغطاة: فيسبوك، إنستغرام، تيك توك، X — لا تتيح وصولاً قانونياً لتطبيقات الرصد. الأرقام المعروضة تخص المصادر المتاحة فقط ولا تمثل كل المغاربة.",
            style = MaterialTheme.typography.labelSmall.copy(color = C.mut, lineHeight = 16.sp),
        )
    }
}
