package ma.trend.almaghrib.ui

import androidx.compose.ui.graphics.Color
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val hm = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault())
    private val dm = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault())

    /** Nombres compacts en chiffres latins : 1.2K, 3.4M. */
    fun compact(n: Long?): String {
        if (n == null) return "—"
        return when {
            n >= 1_000_000 -> String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0).replace(".0M", "M")
            n >= 10_000 -> "${n / 1000}K"
            n >= 1_000 -> String.format(Locale.ROOT, "%.1fK", n / 1000.0).replace(".0K", "K")
            else -> n.toString()
        }
    }

    fun compact(n: Int): String = compact(n.toLong())

    fun ago(t: Instant?, now: Instant = Instant.now()): String {
        if (t == null) return ""
        val m = Duration.between(t, now).toMinutes().coerceAtLeast(0)
        return when {
            m < 1 -> "الآن"
            m < 60 -> "قبل $m د"
            m < 24 * 60 -> "قبل ${m / 60} س"
            else -> "قبل ${m / (24 * 60)} يوم"
        }
    }

    fun time(t: Instant?): String = t?.let { hm.format(it) } ?: ""
    fun dateTime(t: Instant?): String = t?.let { dm.format(it) } ?: ""

    val CATEGORY = mapOf(
        "politique" to "سياسة", "economie" to "اقتصاد", "societe" to "مجتمع", "sport" to "رياضة",
        "art" to "فن", "technologie" to "تكنولوجيا", "international" to "دولي",
    )

    fun catColors(cat: String): Pair<Color, Color> = when (cat) {
        "politique" -> Color(0x2E4678FF) to Color(0xFF9FB8FF)
        "sport" -> Color(0x292FD27A) to Color(0xFF6EE7A4)
        "societe" -> Color(0x2EA06EFF) to Color(0xFFC6A8FF)
        "economie" -> Color(0x29FFAA28) to Color(0xFFFFC46B)
        "art" -> Color(0x29FF50A0) to Color(0xFFFF9CC8)
        "technologie" -> Color(0x2916C1D6) to Color(0xFF7FE3F0)
        "international" -> Color(0x26FFFFFF) to Color(0xFFD9D4C8)
        "controverse" -> Color(0x2EE5383B) to Color(0xFFFF8A8C)
        "video" -> Color(0x2916C1D6) to Color(0xFF7FE3F0)
        else -> Color(0x1FFFFFFF) to Color(0xFFCFC9BB)
    }

    val PLATFORM = mapOf(
        "media" to "صحافة", "institution" to "مصدر رسمي", "aggregator" to "أخبار Google", "google_trends" to "Google Trends",
        "youtube" to "يوتيوب", "reddit" to "Reddit", "facebook" to "فيسبوك", "instagram" to "إنستغرام",
        "tiktok" to "تيك توك", "x" to "X",
    )

    fun platformColor(p: String): Color = when (p) {
        "youtube" -> Color(0xFFFF3B3B)
        "reddit" -> Color(0xFFFF6A2B)
        "google_trends" -> Color(0xFF4C8DFF)
        "aggregator" -> Color(0xFF7FB3FF)
        "institution" -> C.gold2
        else -> C.mut
    }

    val RELIABILITY = mapOf(
        "confirme" to "مؤكد من عدة مصادر",
        "source_unique" to "مصدر واحد — بانتظار التأكيد",
        "non_verifie" to "غير مؤكد (اهتمام بدون مصدر إعلامي)",
    )
}
