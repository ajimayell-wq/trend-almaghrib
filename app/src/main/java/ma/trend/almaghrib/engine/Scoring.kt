package ma.trend.almaghrib.engine

import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Score de tendance, transparent et fondé sur des mesures observées :
 *   base  = 1,0·ln(1+médias distincts) + 0,6·ln(1+publications) + 1,0·(plateformes − 1)
 *         + 0,7·ln(1+trafic Google/100) + 0,5·ln(1+vues YouTube/10 000) + 0,5·ln(1+discussions Reddit)
 *   score = 20 · base · fraîcheur · vélocité
 * Une mesure absente vaut 0 et reste « null » dans le détail : rien n'est estimé.
 */
object Scoring {
    data class Breakdown(
        val parts: Map<String, Double>,
        val freshness: Double,
        val velocity: Double,
        val ageHours: Double,
        val recent3h: Int,
        val before3to12h: Int,
        val distinctMedia: Int,
        val publications: Int,
        val platforms: List<String>,
        val googleTraffic: Long?,
        val youtubeViews: Long?,
    )

    private fun sumMetric(c: Cluster, platform: String, keys: List<String>): Long? {
        val vals = c.items.filter { it.platform == platform && keys.any { k -> k in it.metrics } }
            .map { keys.sumOf { k -> it.metrics[k] ?: 0L } }
        return if (vals.isEmpty()) null else vals.sum()
    }

    private fun round(x: Double, d: Int = 3): Double {
        var f = 1.0
        repeat(d) { f *= 10 }
        return Math.round(x * f) / f
    }

    fun score(c: Cluster, now: Instant): Pair<Double, Breakdown> {
        val media = c.items.filter { it.platform in Sources.NEWS }.map { it.sourceName }.toSet()
        val platforms = c.platforms
        val n = c.items.size
        val gt = sumMetric(c, "google_trends", listOf("approx_traffic"))
        val yt = sumMetric(c, "youtube", listOf("views"))
        val last = c.lastSeen ?: now
        val ageH = max(0.0, Duration.between(last, now).toMinutes() / 60.0)
        val fresh = exp(-ageH / 10)
        val recent = c.items.count { it.publishedAt != null && Duration.between(it.publishedAt, now) <= Duration.ofHours(3) }
        val before = c.items.count {
            val a = it.publishedAt ?: return@count false
            val d = Duration.between(a, now)
            d > Duration.ofHours(3) && d <= Duration.ofHours(12)
        }
        val velocity = min(1.6, max(0.7, (recent / 3.0 + 0.5) / (before / 9.0 + 0.5)))
        val parts = linkedMapOf(
            "medias_distincts" to round(1.0 * ln(1.0 + media.size)),
            "publications" to round(0.6 * ln(1.0 + n)),
            "diversite_plateformes" to round(1.0 * max(0, platforms.size - 1)),
            "google_trends" to if (gt != null) round(0.7 * ln(1.0 + gt / 100.0)) else 0.0,
            "youtube" to if (yt != null) round(0.5 * ln(1.0 + yt / 10_000.0)) else 0.0,
            "reddit" to round(0.5 * ln(1.0 + c.items.count { it.platform == "reddit" })),
        )
        val s = round(20 * parts.values.sum() * fresh * velocity, 2)
        return s to Breakdown(parts, round(fresh), round(velocity), round(ageH, 1), recent, before,
            media.size, n, platforms, gt, yt)
    }

    /** confirme : >= 2 médias ou source officielle ; source_unique ; non_verifie (audience seule). */
    fun reliability(c: Cluster): String {
        val media = c.items.filter { it.platform == "media" || it.platform == "aggregator" }.map { it.sourceName }.toSet()
        return when {
            c.items.any { it.institutional } || media.size >= 2 -> "confirme"
            media.size == 1 -> "source_unique"
            else -> "non_verifie"
        }
    }

    /**
     * Sans serveur ni historique : on compare au score de l'actualisation précédente (en mémoire)
     * quand il existe, sinon on se fonde sur les dates de publication observées.
     */
    fun trendDirection(score: Double, prev: Double?, velocity: Double, firstSeen: Instant?, now: Instant): String {
        if (prev != null && prev > 0) {
            val r = score / prev
            if (r >= 1.15 || (r >= 1.0 && velocity >= 1.3)) return "hausse"
            if (r <= 0.85) return "baisse"
            return "stable"
        }
        if (firstSeen != null && Duration.between(firstSeen, now) <= Duration.ofHours(3)) return "nouveau"
        return when {
            velocity >= 1.3 -> "hausse"
            velocity <= 0.8 -> "baisse"
            else -> "stable"
        }
    }
}
