package ma.trend.almaghrib.ui

import androidx.compose.foundation.Canvas
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

@Composable
fun RankNumber(rank: Int, big: Boolean = true) {
    val st = MaterialTheme.typography.headlineSmall.copy(fontSize = if (big) 25.sp else 20.sp, textAlign = TextAlign.Center, textDirection = TextDirection.Ltr)
    val m = Modifier.width(26.dp)
    when {
        rank == 1 -> GoldText("$rank", st, m)
        rank <= 3 -> Text("$rank", style = st.copy(color = C.gold2), modifier = m)
        else -> Text("$rank", style = st.copy(color = androidx.compose.ui.graphics.Color(0xFFCFC9BB)), modifier = m)
    }
}

/** Ligne d'un sujet : rang, image, titre, rubrique, mesures réelles, courbe et progression. */
@Composable
fun TopicRow(rank: Int, t: Topic, onClick: () -> Unit, detailed: Boolean = false) {
    NCard(
        padding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        border = if (rank <= 3) C.gold.copy(alpha = .22f) else C.line,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RankNumber(rank, big = rank <= 3)
            Spacer(Modifier.width(8.dp))
            Thumb(t.imageUrl, Modifier.size(width = 60.dp, height = 44.dp), t.categories.firstOrNull(), corner = 10.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, maxLines = if (detailed) 2 else 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    t.categories.firstOrNull()?.let { CatPill(it) }
                    if (t.controversial) ControversyPill()
                    Metric(Icons.Filled.Layers, Fmt.compact(t.breakdown.publications))
                    val views = t.totalViews
                    val gt = t.googleTraffic
                    if (views != null) Metric(Icons.Filled.Visibility, Fmt.compact(views))
                    else if (gt != null && detailed) Metric(Icons.Filled.Search, "+" + Fmt.compact(gt))
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val up = (t.progression ?: 0) >= 0
                Sparkline(t.hourly, Modifier.size(width = 58.dp, height = 24.dp), color = if (up) C.green else C.red)
                Delta(t.progression, isNew = t.prev6h == 0 && t.last6h > 0, small = true)
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
