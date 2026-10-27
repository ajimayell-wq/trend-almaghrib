package ma.trend.almaghrib.ui

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.trend.almaghrib.data.AppState
import ma.trend.almaghrib.data.Repository
import ma.trend.almaghrib.engine.Comment
import ma.trend.almaghrib.engine.Topic

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpinionScreen(st: AppState, repo: Repository, topicId: Int?, onBack: (() -> Unit)?, onPick: (Int) -> Unit) {
    val topic: Topic? = topicId?.let { id -> st.topics.firstOrNull { it.id == id } }
        ?: st.topics.take(20).firstOrNull { it.controversial } ?: st.topics.firstOrNull()
    if (topic == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopTitle("ماذا يقول المغاربة؟", onBack)
            EmptyBlock("لا توجد مواضيع بعد. ارجع إلى الرئيسية واضغط زر التحديث.")
        }
        return
    }
    val rank = st.topics.indexOfFirst { it.id == topic.id } + 1
    var result by remember(topic.id) { mutableStateOf<Repository.OpinionResult?>(null) }
    var loading by remember(topic.id) { mutableStateOf(true) }
    LaunchedEffect(topic.id) {
        loading = true
        result = runCatching { repo.opinion(topic) }.getOrNull()
        loading = false
    }
    val uri = LocalUriHandler.current
    val ctx = LocalContext.current

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(230.dp)) {
                Thumb(topic.imageUrl, Modifier.fillMaxSize(), topic.categories.firstOrNull(), corner = 0.dp)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x99000000), 0.35f to Color.Transparent, 1f to C.bg)))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) GlassIcon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", onBack) else Spacer(Modifier.size(40.dp))
                    Text("ماذا يقول المغاربة؟", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                    GlassIcon(Icons.Filled.Share, "مشاركة", {
                        val link = topic.items.firstOrNull { it.platform != "google_trends" }?.url ?: topic.items.first().url
                        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${topic.title}\n$link")
                        ctx.startActivity(Intent.createChooser(i, "مشاركة"))
                    })
                }
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (topic.controversial) Pill("جدل", C.red, Color.White)
                        if (rank > 0) Pill("ترند رقم $rank", Color(0x99000000), C.gold2)
                        topic.categories.firstOrNull()?.let { CatPill(it) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(topic.title, maxLines = 4, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp))
                }
            }
        }
        if (onBack == null) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    items(st.topics.take(12), key = { it.id }) { t ->
                        val on = t.id == topic.id
                        Text(
                            t.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium.copy(color = if (on) Color(0xFF1A1405) else Color(0xFFD9D4C8)),
                            modifier = Modifier.width(150.dp).clip(RoundedCornerShape(50))
                                .then(if (on) Modifier.background(C.goldBrush) else Modifier.background(Color(0x0DFFFFFF)))
                                .clickable { onPick(t.id) }.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "${topic.breakdown.publications} منشوراً · ${topic.mediaCount} مصدر إعلامي · " +
                        topic.platforms.mapNotNull { Fmt.PLATFORM[it] }.distinct().joinToString("، ") + " · ظهر ${Fmt.ago(topic.firstSeen)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = C.mut),
                )
                Spacer(Modifier.height(8.dp))
                SummaryCard(topic)
                Spacer(Modifier.height(10.dp))
                WhyCard(topic)
                Spacer(Modifier.height(10.dp))
                val r = result
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 14.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = C.gold2, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("جارٍ جمع التعليقات العامة وتحليلها…", style = MaterialTheme.typography.bodySmall.copy(color = C.mut))
                    }
                    r == null -> Notice("تعذر جمع التعليقات الآن. تحقق من الاتصال.", C.red)
                    else -> OpinionBody(r, uri::openUri)
                }
                SectionHeader("المصادر", trailing = "${topic.items.size} منشوراً")
                topic.items.take(15).forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { uri.openUri(a.url) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Fmt.platformColor(a.platform)))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            Text("${a.sourceName} · ${Fmt.dateTime(a.publishedAt)}", style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, "فتح", tint = C.mut, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "طريقة الترتيب: النقاط = ${"%.1f".format(topic.score)} (مصادر مختلفة، عدد المنشورات، تنوع المنصات، عمليات البحث، المشاهدات، الحداثة والسرعة). كل الأرقام مأخوذة من المصادر مباشرة.",
                    style = MaterialTheme.typography.labelSmall.copy(color = C.mut2, lineHeight = 16.sp),
                )
            }
        }
    }
}

@Composable
private fun TopTitle(title: String, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) { GlassIcon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", onBack); Spacer(Modifier.width(10.dp)) }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun SummaryCard(t: Topic) {
    NCard(border = C.gold2.copy(alpha = .25f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ملخص النقاش", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.AutoAwesome, null, tint = C.gold2, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("محلل الترند", style = MaterialTheme.typography.labelSmall.copy(color = C.gold2))
        }
        Spacer(Modifier.height(4.dp))
        if (t.summary.isNotBlank()) {
            Text(t.summary, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFDDD7CA)))
            Text("مقتطف من: ${t.summarySource}", style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
        } else {
            Text("لا يتوفر مقتطف نصي لهذا الموضوع؛ راجع المصادر أدناه.", style = MaterialTheme.typography.bodySmall.copy(color = C.mut))
        }
        Spacer(Modifier.height(8.dp))
        val (icon, col) = when (t.reliability) {
            "confirme" -> Icons.Filled.Verified to C.green
            "source_unique" -> Icons.Filled.Warning to Color(0xFFFFC46B)
            else -> Icons.Filled.Warning to C.red
        }
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(col.copy(alpha = .12f)).padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = col, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(Fmt.RELIABILITY[t.reliability] ?: "", style = MaterialTheme.typography.labelMedium.copy(color = col))
        }
    }
}

/** « لماذا يتحدث عنه الناس؟ » : uniquement des faits observés. */
@Composable
private fun WhyCard(t: Topic) {
    val reasons = buildList {
        if (t.mediaCount >= 2) add("تناقلته ${t.mediaCount} مصادر إعلامية مختلفة")
        t.googleTraffic?.let { add("ظهر في عمليات البحث على Google بالمغرب (أكثر من ${Fmt.compact(it)} بحث)") }
        t.totalViews?.let { add("فيديوهات مرتبطة به حصدت ${Fmt.compact(it)} مشاهدة على يوتيوب") }
        val rd = t.items.count { it.platform == "reddit" }
        if (rd > 0) add("نوقش في $rd ${if (rd == 1) "منشور" else "منشورات"} على Reddit")
        if (t.last6h > 0) add("${t.last6h} منشوراً جديداً خلال آخر 6 ساعات" + (if (t.prev6h > 0) " مقابل ${t.prev6h} قبلها" else ""))
        if (t.controversial) add("عناوين التغطية تتضمن مفردات الجدل أو الانتقاد")
    }
    if (reasons.isEmpty()) return
    NCard {
        Text("لماذا يتحدث عنه الناس؟", style = MaterialTheme.typography.titleMedium)
        reasons.forEach {
            Row(Modifier.padding(top = 5.dp)) {
                Text("•", style = MaterialTheme.typography.bodySmall.copy(color = C.gold2))
                Spacer(Modifier.width(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD9D4C8)))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpinionBody(r: Repository.OpinionResult, open: (String) -> Unit) {
    val rep = r.report
    r.notes.forEach { Notice(it); Spacer(Modifier.height(8.dp)) }
    if (rep.sampleSize >= 5) {
        NCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Donut(
                    listOf(rep.negative.toFloat() to C.red, rep.positive.toFloat() to C.green, rep.neutral.toFloat() to Color(0xFF7D7A72)),
                    Fmt.compact(rep.sampleSize), "تعليق محلل",
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("اتجاهات الآراء", style = MaterialTheme.typography.titleSmall)
                    listOf(Triple("سلبي", rep.negative, C.red), Triple("إيجابي", rep.positive, C.green), Triple("محايد", rep.neutral, Color(0xFF7D7A72))).forEach { (l, v, c) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Text(l, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(44.dp))
                            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x12FFFFFF))) {
                                Box(Modifier.fillMaxWidth(rep.pct(v) / 100f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(c))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("${rep.pct(v)}%", style = MaterialTheme.typography.labelLarge.copy(color = c, fontWeight = FontWeight.Black))
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "عينة من ${rep.sampleSize} تعليقاً عاماً (${rep.platforms.joinToString("، ") { Fmt.PLATFORM[it] ?: it }}). تصنيف آلي تقريبي بالكلمات — لا يمثل رأي كل المغاربة.",
                style = MaterialTheme.typography.labelSmall.copy(color = C.gold2, lineHeight = 16.sp),
            )
        }
    } else {
        Notice(
            if (rep.sampleSize == 0) "لا توجد تعليقات عامة متاحة لهذا الموضوع في المصادر المغطاة، لذلك لا يُعرض تحليل للآراء."
            else "عدد التعليقات المتاحة (${rep.sampleSize}) غير كافٍ لعرض نسب موثوقة.",
            C.mut,
        )
    }
    if (rep.topWords.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        NCard {
            Text("أكثر الكلمات تكراراً", style = MaterialTheme.typography.titleSmall)
            Text(if (rep.wordsFromComments) "في التعليقات" else "في عناوين التغطية", style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rep.topWords.forEachIndexed { i, (w, n) ->
                    val strong = i < 3
                    Text(
                        "$w · $n",
                        style = MaterialTheme.typography.labelLarge.copy(color = if (strong) C.gold2 else Color(0xFFCFC9BB), fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.Medium),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (strong) C.gold.copy(alpha = .16f) else Color(0x0DFFFFFF)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    NCard {
        Row {
            Text("كيف تطور النقاش؟", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            rep.peakHoursAgo?.let { Text(if (it == 0) "الذروة: الساعة الأخيرة" else "الذروة: قبل ${Fmt.hours(it.toLong())}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFF8A8C))) }
        }
        Spacer(Modifier.height(6.dp))
        AreaChart(rep.hourly)
        Text("منشورات وتعليقات مؤرخة في كل ساعة", style = MaterialTheme.typography.labelSmall.copy(color = C.mut2))
    }
    if (rep.supportive.isNotEmpty() || rep.opposing.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuoteColumn("الآراء المؤيدة", C.green, rep.supportive, Modifier.weight(1f))
            QuoteColumn("الآراء المعارضة", C.red, rep.opposing, Modifier.weight(1f))
        }
    }
    if (rep.highlights.isNotEmpty()) {
        SectionHeader("أبرز التعليقات", trailing = "تعليقات عامة · بدون أسماء")
        rep.highlights.forEach { c -> CommentCard(c); Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun QuoteColumn(title: String, color: Color, list: List<Comment>, modifier: Modifier) {
    NCard(modifier, border = color.copy(alpha = .35f)) {
        Text(title, style = MaterialTheme.typography.titleSmall.copy(color = color))
        if (list.isEmpty()) Text("لا توجد تعليقات واضحة", style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
        list.forEach {
            Text("« ${it.text.take(140)} »", maxLines = 4, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD9D4C8)), modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun CommentCard(c: Comment) {
    NCard(padding = PaddingValues(10.dp)) {
        Row {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF2A2A34)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.FormatQuote, null, tint = C.mut, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(Fmt.PLATFORM[c.platform], Fmt.ago(c.publishedAt).ifBlank { null }, c.likes?.let { "${Fmt.compact(it)} إعجاب" }).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall.copy(color = C.mut),
                )
                Text(c.text, maxLines = 5, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
