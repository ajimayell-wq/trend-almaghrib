package ma.trend.almaghrib.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.trend.almaghrib.engine.Topic
import kotlin.math.cos
import kotlin.math.sin

/** Logo : étoile à cinq branches dorée dans un cercle, point rouge au centre. */
@Composable
fun Logo(size: Int = 34) {
    Canvas(Modifier.size(size.dp)) {
        val r = this.size.minDimension / 2
        val c = center
        drawCircle(C.goldBrush, r - 1.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
        val pts = (0 until 5).map { i ->
            val a = Math.toRadians((-90 + i * 144).toDouble())
            Offset(c.x + (r * .68f) * cos(a).toFloat(), c.y + (r * .68f) * sin(a).toFloat())
        }
        val p = Path().apply { moveTo(pts[0].x, pts[0].y); for (i in 1..4) lineTo(pts[i].x, pts[i].y); close() }
        drawPath(p, C.goldBrush, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
        drawCircle(C.red, 2.6.dp.toPx(), c)
    }
}

/** Petit badge de rang posé sur l'image (laisse toute la largeur au titre). */
@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val gold = rank <= 3
    Box(
        modifier.padding(4.dp).size(22.dp).clip(RoundedCornerShape(7.dp))
            .then(if (gold) Modifier.background(C.goldBrush) else Modifier.background(Color(0xD9101014)))
            .border(1.dp, if (gold) Color.Transparent else Color(0x33FFFFFF), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (gold) Color(0xFF1A1405) else C.txt, fontWeight = FontWeight.Black, fontSize = 12.sp,
                textDirection = TextDirection.Ltr,
            ),
        )
    }
}

/** Source principale du sujet + nombre d'autres sources + fraîcheur. */
fun sourceLine(t: Topic): String {
    val names = t.items.map { it.sourceName.removePrefix("YouTube · ") }.distinct()
    val lead = t.items.firstOrNull { it.platform in ma.trend.almaghrib.engine.Sources.NEWS }?.sourceName
        ?: names.firstOrNull().orEmpty()
    val others = names.size - 1
    val who = when {
        others <= 0 -> lead
        others == 1 -> "$lead ومصدر آخر"
        others == 2 -> "$lead ومصدران آخران"
        others <= 10 -> "$lead و$others مصادر أخرى"
        else -> "$lead و$others مصدراً آخر"
    }
    return listOf(who, Fmt.ago(t.lastSeen)).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Ligne d'un sujet : image avec rang, titre complet, source, rubrique, mesures réelles, courbe et progression. */
@Composable
fun TopicRow(rank: Int, t: Topic, onClick: () -> Unit, detailed: Boolean = false) {
    NCard(
        padding = PaddingValues(8.dp),
        border = if (rank <= 3) C.gold.copy(alpha = .22f) else C.line,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Thumb(t.imageUrl, Modifier.size(width = 76.dp, height = 76.dp), t.categories.firstOrNull(), corner = 12.dp) {
                RankBadge(rank, Modifier.align(Alignment.TopStart))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, maxLines = if (detailed) 4 else 3, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp, lineHeight = 21.sp),
                )
                Text(
                    sourceLine(t), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(color = C.mut), modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    t.categories.firstOrNull()?.let { CatPill(it) }
                    if (t.controversial) ControversyPill()
                    Metric(Icons.Filled.Layers, Fmt.compact(t.breakdown.publications))
                    val views = t.totalViews
                    val gt = t.googleTraffic
                    if (views != null) Metric(Icons.Filled.Visibility, Fmt.compact(views))
                    else if (gt != null) Metric(Icons.Filled.Search, "+" + Fmt.compact(gt))
                    Spacer(Modifier.weight(1f))
                    val up = (t.progression ?: 0) >= 0
                    Sparkline(t.hourly, Modifier.size(width = 40.dp, height = 16.dp), color = if (up) C.green else C.red, fill = false)
                    Delta(t.progression, isNew = t.prev6h == 0 && t.last6h > 0, small = true)
                }
            }
        }
    }
}

@Composable
fun Metric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = C.mut, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(value, style = MaterialTheme.typography.labelMedium.copy(color = androidx.compose.ui.graphics.Color(0xFFE9E4D6), fontWeight = FontWeight.Bold, textDirection = TextDirection.Ltr))
    }
}

@Composable
fun ScreenPadding(content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) { content() }
}
