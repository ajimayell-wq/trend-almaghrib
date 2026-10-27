package ma.trend.almaghrib.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.max

val CardShape = RoundedCornerShape(18.dp)

/** Carte sombre avec bordure fine (style « verre »). */
@Composable
fun NCard(
    modifier: Modifier = Modifier,
    border: Color = C.line,
    padding: PaddingValues = PaddingValues(12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(CardShape)
            .background(C.cardBrush)
            .border(1.dp, border, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun GoldText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = style.copy(brush = C.goldBrush))
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text, maxLines = 1,
        style = MaterialTheme.typography.labelMedium.copy(color = fg, fontSize = 11.sp),
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

@Composable
fun CatPill(cat: String) {
    val (bg, fg) = Fmt.catColors(cat)
    Pill(Fmt.CATEGORY[cat] ?: cat, bg, fg)
}

@Composable
fun ControversyPill() {
    val (bg, fg) = Fmt.catColors("controverse")
    Pill("جدل", bg, fg)
}

/** Badge de la mention « données mesurées » / « estimation ». */
@Composable
fun DashedBadge(text: String, color: Color = C.gold2) {
    Text(
        text, maxLines = 1,
        style = MaterialTheme.typography.labelSmall.copy(color = color),
        modifier = Modifier
            .drawBehind {
                drawRoundRect(
                    color.copy(alpha = .5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    style = Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                )
            }
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Progression réelle entre deux fenêtres de 6 h. null + nouveau = « جديد ». */
@Composable
fun Delta(progression: Int?, isNew: Boolean = false, small: Boolean = false) {
    val st = (if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
    when {
        progression == null && isNew -> Text("جديد", style = st.copy(color = C.gold2))
        progression == null -> Text("—", style = st.copy(color = C.mut))
        progression >= 0 -> Text("▲ $progression%", style = st.copy(color = C.green, textDirection = androidx.compose.ui.text.style.TextDirection.Ltr))
        else -> Text("▼ ${-progression}%", style = st.copy(color = C.red, textDirection = androidx.compose.ui.text.style.TextDirection.Ltr))
    }
}

/** Mini-graphique : publications par heure. Les données sont tracées de gauche (ancien) à droite (récent). */
@Composable
fun Sparkline(values: IntArray, modifier: Modifier = Modifier, color: Color = C.green, fill: Boolean = true) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        // Lissage léger : somme glissante sur 3 heures pour un tracé lisible.
        val v = values.indices.map { i -> (max(0, i - 2)..i).sumOf { values[it] }.toFloat() }
        val mx = (v.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val w = size.width
        val h = size.height
        val pts = v.mapIndexed { i, y -> Offset(i * w / (v.size - 1).coerceAtLeast(1), h - 3f - (y / mx) * (h - 6f)) }
        val line = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        if (fill) {
            val area = Path().apply { addPath(line); lineTo(w, h); lineTo(0f, h); close() }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = .35f), Color.Transparent)))
        }
        drawPath(line, color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, 2.6.dp.toPx(), pts.last())
    }
}

/** Courbe d'activité sur 24 h avec repère de pic. */
@Composable
fun AreaChart(values: IntArray, modifier: Modifier = Modifier) {
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(56.dp)) {
            if (values.isEmpty()) return@Canvas
            val mx = (values.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
            val w = size.width
            val h = size.height
            for (i in 0..2) drawLine(Color(0x0FFFFFFF), Offset(0f, 4f + i * (h - 8f) / 2), Offset(w, 4f + i * (h - 8f) / 2))
            val pts = values.mapIndexed { i, y -> Offset(i * w / (values.size - 1), h - 4f - (y / mx) * (h - 10f)) }
            val line = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            val area = Path().apply { addPath(line); lineTo(w, h); lineTo(0f, h); close() }
            drawPath(area, Brush.verticalGradient(listOf(C.gold.copy(alpha = .45f), Color.Transparent)))
            drawPath(line, C.gold2, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
            val pk = values.indices.maxByOrNull { values[it] } ?: 0
            if (values[pk] > 0) {
                drawLine(C.red, Offset(pts[pk].x, pts[pk].y), Offset(pts[pk].x, h), 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                drawCircle(Color.White, 4.5.dp.toPx(), pts[pk]); drawCircle(C.red, 3.dp.toPx(), pts[pk])
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // Ordre visuel gauche → droite = ancien → récent (le graphique n'est pas inversé en RTL).
            listOf("الآن", "-6 س", "-12 س", "-18 س", "-24 س").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall.copy(color = C.mut2))
            }
        }
    }
}

/** Anneau de répartition (positif / négatif / neutre). */
@Composable
fun Donut(parts: List<Pair<Float, Color>>, center: String, sub: String, modifier: Modifier = Modifier.size(92.dp)) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 12.dp.toPx()
            val total = parts.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(0.0001f)
            var start = -90f
            val d = size.minDimension - sw
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            if (parts.all { it.first == 0f }) drawArc(C.line, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
            for ((v, c) in parts) {
                val sweep = 360f * v / total
                if (sweep > 0f) drawArc(c, start, (sweep - 2f).coerceAtLeast(0.5f), false, tl, Size(d, d), style = Stroke(sw))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(center, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
            Text(sub, style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
        }
    }
}

/** Image d'illustration (fournie par la source) avec repli graphique si absente. */
@Composable
fun Thumb(url: String?, modifier: Modifier, category: String? = null, corner: Dp = 12.dp, content: @Composable BoxScope.() -> Unit = {}) {
    val (bg, fg) = Fmt.catColors(category ?: "")
    Box(
        modifier.clip(RoundedCornerShape(corner)).background(Brush.linearGradient(listOf(C.card2, bg.copy(alpha = .9f), C.bg2))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Insights, null, tint = fg.copy(alpha = .5f), modifier = Modifier.size(22.dp))
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        content()
    }
}

@Composable
fun FilterChips(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        itemsIndexed(options) { i, o ->
            val on = i == selected
            Text(
                o, maxLines = 1,
                style = MaterialTheme.typography.labelLarge.copy(color = if (on) Color(0xFF1A1405) else Color(0xFFD9D4C8)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .then(if (on) Modifier.background(C.goldBrush) else Modifier.background(Color(0x0DFFFFFF)).border(1.dp, C.line, RoundedCornerShape(50)))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector? = null, iconTint: Color = C.red, action: String? = null, onAction: (() -> Unit)? = null, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelSmall.copy(color = C.mut))
        if (action != null && onAction != null) {
            Text("$action ‹", style = MaterialTheme.typography.labelLarge.copy(color = C.gold2), modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
        }
    }
}

@Composable
fun GlassIcon(icon: ImageVector, desc: String, onClick: () -> Unit, tint: Color = Color(0xFFE9E4D6)) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x0EFFFFFF)).border(1.dp, Color(0x17FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(21.dp)) }
}

/** Petit indicateur rouge animé « مباشر ». */
@Composable
fun LiveDot() {
    Box(Modifier.size(14.dp).clip(CircleShape).background(C.red.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(C.red))
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("الرئيسية", Icons.Filled.Home),
    TRENDS("الترندات", Icons.Filled.Insights),
    OPINION("آراء المغاربة", Icons.Filled.Forum),
    VIDEOS("الفيديوهات", Icons.Filled.PlayCircle),
    MORE("المزيد", Icons.Filled.MoreHoriz),
}

@Composable
fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xE60A0A0E), Color(0xFA08080B))))
            .drawBehind { drawLine(C.gold.copy(alpha = .2f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab.entries.forEach { t ->
                val on = t == current
                Column(
                    Modifier.weight(1f).clickable { onSelect(t) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(28.dp).height(3.dp).clip(RoundedCornerShape(3.dp)).then(if (on) Modifier.background(C.goldBrush) else Modifier))
                    Spacer(Modifier.height(6.dp))
                    Icon(t.icon, t.label, tint = if (on) C.gold2 else C.mut, modifier = Modifier.size(23.dp))
                    Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(color = if (on) C.gold2 else C.mut, fontSize = 10.5.sp))
                }
            }
        }
    }
}

/** Bandeau d'état (erreur réseau, données en cache). */
@Composable
fun Notice(text: String, color: Color = C.gold2, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.bodySmall.copy(color = color),
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .08f))
            .border(1.dp, color.copy(alpha = .3f), RoundedCornerShape(12.dp)).padding(10.dp),
    )
}

@Composable
fun EmptyBlock(text: String, modifier: Modifier = Modifier) {
    Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium.copy(color = C.mut),
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp))
}

@Composable
fun RowScope.Stat(value: String, label: String, highlight: Boolean = false, divider: Boolean = false) {
    Column(
        Modifier.weight(1f).drawBehind {
            if (divider) drawLine(Color(0x1AFFFFFF), Offset(if (layoutDirection == LayoutDirection.Rtl) size.width else 0f, 6f),
                Offset(if (layoutDirection == LayoutDirection.Rtl) size.width else 0f, size.height - 6f), 1.dp.toPx())
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = if (highlight) C.gold2 else C.txt,
            textDirection = androidx.compose.ui.text.style.TextDirection.Ltr))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall.copy(color = C.mut, fontSize = 11.sp))
    }
}
